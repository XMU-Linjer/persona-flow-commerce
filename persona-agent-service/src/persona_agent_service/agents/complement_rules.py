from collections.abc import Iterable


COMPLEMENT_RULES = {
    "枕头": ["枕套", "床品四件套", "香薰助眠灯"],
    "睡眠": ["枕套", "床品四件套", "香薰助眠灯"],
    "pillow": ["pillowcase", "bedding set", "aroma sleep lamp"],
    "sleep": ["pillowcase", "bedding set", "aroma sleep lamp"],
    "咖啡机": ["咖啡豆", "滤纸", "保温杯"],
    "咖啡": ["咖啡豆", "滤纸", "保温杯"],
    "coffee machine": ["coffee beans", "coffee filter", "thermos cup"],
    "coffee": ["coffee beans", "coffee filter", "thermos cup"],
    "背包": ["数码收纳包", "移动电源", "旅行分装瓶"],
    "旅行": ["数码收纳包", "移动电源", "旅行分装瓶"],
    "backpack": ["digital organizer", "power bank", "travel bottles"],
    "travel": ["digital organizer", "power bank", "travel bottles"],
    "瑜伽垫": ["运动水杯", "速干毛巾", "筋膜球"],
    "瑜伽": ["运动水杯", "速干毛巾", "筋膜球"],
    "运动": ["运动水杯", "速干毛巾", "筋膜球"],
    "yoga mat": ["sports bottle", "quick dry towel", "massage ball"],
    "yoga": ["sports bottle", "quick dry towel", "massage ball"],
    "sport": ["sports bottle", "quick dry towel", "massage ball"],
    "键盘": ["无线鼠标", "腕托", "桌垫"],
    "earphone": ["charging storage", "desk office", "storage accessories"],
    "audio": ["charging storage", "desk office", "storage accessories"],
    "monitor": ["keyboard mouse", "monitor stand", "expansion dock"],
    "display": ["keyboard mouse", "monitor stand", "expansion dock"],
    "office": ["keyboard mouse", "monitor stand", "expansion dock"],
    "furniture": ["bedding", "cushions", "cleaning supplies"],
    "home storage": ["bedding", "cushions", "cleaning supplies"],
    "storage": ["data cables", "chargers", "storage boxes"],
    "charging": ["data cables", "chargers", "storage boxes"],
    "keyboard": ["wrist rest", "mouse", "cleaning kit", "desk mat"],
    "mouse": ["mouse pad", "wrist rest", "desk organizer"],
}


def complement_labels(keywords: Iterable[str]) -> list[str]:
    labels: list[str] = []
    for keyword in keywords:
        normalized = keyword.lower().strip()
        for pattern, complements in COMPLEMENT_RULES.items():
            if pattern in normalized:
                labels.extend(complements)
    if not labels:
        labels.append("explore adjacent categories")
    return list(dict.fromkeys(labels))
