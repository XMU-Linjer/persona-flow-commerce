import json
from typing import Any

from persona_agent_service.schemas.artifacts import BehaviorFactReport, IntentReport, TrendReport, UserProfileVersion
from persona_agent_service.schemas.context import AgentProfileContext
from persona_agent_service.schemas.llm import DeepSeekRecommendationInsight


DEEPSEEK_SYSTEM_PROMPT = """你是电商用户画像与推荐洞察 Agent。
你不是营销文案生成器。
你只能基于输入的结构化行为事实分析。
你不能编造用户没有发生过的行为。
你不能编造不存在的 eventId、skuId、spuId、categoryId。
你不能输出手机号、地址、JWT、密码等敏感信息。
你不能判断库存、订单状态、支付状态是否有效。
你不能生成下单、扣库存、支付、取消订单等业务动作。
PAYMENT_SUCCESS 不表示继续推荐同一 SKU 或同一 SPU。
PAYMENT_SUCCESS 表示：
- preferenceConfirmed = true
- fulfilled = true
- complementTrigger = true
- repeatRecommendationSuppressed = true
已购买或 fulfilled 的 SKU/SPU 必须进入 doNotRecommend。
complementOpportunities 必须有 evidenceEventIds。
如果证据不足，要降低 confidence，不要强行推断。
只输出合法 JSON。
不要输出 Markdown。
不要输出解释文字。"""


SENSITIVE_KEYS = {
    "address",
    "detailAddress",
    "jwt",
    "password",
    "passwordHash",
    "phone",
    "recipientPhone",
    "token",
}


def build_deepseek_user_prompt(
    context: AgentProfileContext,
    behavior_report: BehaviorFactReport,
    intent_report: IntentReport,
    trend_report: TrendReport,
    rule_based_profile: UserProfileVersion,
) -> str:
    payload = {
        "AgentProfileContext": _safe_dump(context),
        "BehaviorFactReport": _safe_dump(behavior_report),
        "IntentReport": _safe_dump(intent_report),
        "TrendReport": _safe_dump(trend_report),
        "ruleBasedProfile": _safe_dump(rule_based_profile.profile),
        "allowedEventIds": context.all_evidence_event_ids,
        "fulfilledSkuSpu": _fulfilled_targets(rule_based_profile),
        "outputJsonSchema": DeepSeekRecommendationInsight.model_json_schema(),
    }
    return (
        "请基于以下结构化事实生成推荐洞察 JSON。"
        "不要输出 Markdown，不要输出解释文字。"
        "所有 evidenceEventIds 必须来自 allowedEventIds。"
        "\n\n"
        f"{json.dumps(payload, ensure_ascii=False, default=str)}"
    )


def _safe_dump(value: Any) -> Any:
    if hasattr(value, "model_dump"):
        value = value.model_dump(by_alias=True, mode="json")
    return _redact(value)


def _redact(value: Any) -> Any:
    if isinstance(value, dict):
        cleaned: dict[str, Any] = {}
        for key, item in value.items():
            if key in SENSITIVE_KEYS:
                continue
            cleaned[key] = _redact(item)
        return cleaned
    if isinstance(value, list):
        return [_redact(item) for item in value]
    return value


def _fulfilled_targets(profile: UserProfileVersion) -> list[dict[str, int | None]]:
    fulfilled = profile.profile.get("fulfilledNeeds", [])
    if not isinstance(fulfilled, list):
        return []
    targets = []
    for need in fulfilled:
        if isinstance(need, dict):
            targets.append({"skuId": need.get("skuId"), "spuId": need.get("spuId")})
    return targets

