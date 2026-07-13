from collections.abc import Iterable


def complement_labels(keywords: Iterable[str]) -> list[str]:
    """Generate recommendation keyword labels from user behavior signals.

    Design rationale:
    - Previous hardcoded COMPLEMENT_RULES mapping table is removed.
    - In RULE_BASED mode, we return the user's own search/browse keywords
      as recommendation seeds. Every keyword the user has shown interest in
      becomes a recommendation signal.
    - In DEEPSEEK_ENHANCED mode, the DeepSeek API produces semantically
      meaningful complement labels that augment these organic keywords.

    This ensures:
    1. No keyword is ever "unmapped" — every user behavior produces
       actionable recommendation terms.
    2. The system works without any pre-defined knowledge of product categories.
    3. DeepSeek provides the semantic association layer, not brittle string matching.
    """
    labels = list(dict.fromkeys(keywords))
    if not labels:
        labels.append("explore adjacent categories")
    return labels