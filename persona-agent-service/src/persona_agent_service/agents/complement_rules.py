from collections.abc import Iterable


COMPLEMENT_RULES = {
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
