#!/usr/bin/env python3
"""Check complete translations and compatible Android format placeholders."""
from pathlib import Path
import re
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
resources = root / "app/src/main/res"
translations = {}
for locale in ("values", "values-ru", "values-es"):
    elements = ET.parse(resources / locale / "strings.xml").getroot().findall("string")
    strings = {element.attrib["name"]: "".join(element.itertext()) for element in elements}
    assert len(strings) == len(elements), f"Duplicate strings in {locale}"
    translations[locale] = strings
default = translations["values"]
for locale, strings in translations.items():
    assert strings.keys() == default.keys(), f"Missing or extra translations in {locale}"
    for key, value in strings.items():
        assert value.strip(), f"Empty translation: {locale}/{key}"
        placeholders = lambda text: sorted(re.findall(r"%\d+\$[sd]", text))
        assert placeholders(value) == placeholders(default[key]), f"Invalid placeholders: {locale}/{key}"
references = set()
for source in (root / "app/src/main/java").rglob("*.kt"):
    text = source.read_text()
    references.update(re.findall(r"R\.string\.(\w+)", text))
    assert not re.search(r'"[^"\n]*[А-Яа-яЁё][^"\n]*"', text), f"Hardcoded Russian string: {source.name}"
assert references <= default.keys(), f"Undefined resources: {references - default.keys()}"
print(f"Checked {len(default)} strings in English, Spanish, and Russian.")
