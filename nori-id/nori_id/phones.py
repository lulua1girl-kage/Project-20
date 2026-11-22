from __future__ import annotations

import phonenumbers
import pycountry
from phonenumbers import NumberParseException, PhoneNumberFormat


def flag_emoji(region: str) -> str:
    if len(region) != 2 or not region.isalpha():
        return "🌐"
    return "".join(chr(127397 + ord(char.upper())) for char in region)


def region_catalog() -> list[dict]:
    entries = []
    for region in sorted(phonenumbers.SUPPORTED_REGIONS):
        calling_code = phonenumbers.country_code_for_region(region)
        if not calling_code:
            continue
        country = pycountry.countries.get(alpha_2=region)
        entries.append({
            "region": region,
            "name": country.name if country else region,
            "calling_code": f"+{calling_code}",
            "flag": flag_emoji(region),
        })
    return sorted(entries, key=lambda item: (item["name"].casefold(), item["region"]))


def parse_and_validate_phone(raw_phone: str, region: str | None = None) -> dict:
    value = raw_phone.strip()
    selected_region = (region or "").upper() or None
    if not value:
        raise ValueError("Enter a phone number.")
    if not value.startswith("+") and not selected_region:
        raise ValueError("Select a country calling code before entering a national number.")
    try:
        number = phonenumbers.parse(value, None if value.startswith("+") else selected_region)
    except NumberParseException as exc:
        raise ValueError("The phone number could not be parsed. Check the country code and number.") from exc
    if not phonenumbers.is_possible_number(number):
        raise ValueError("The phone number length is not possible for this numbering plan.")
    if not phonenumbers.is_valid_number(number):
        raise ValueError("The number does not match a valid range in the selected numbering plan.")
    e164 = phonenumbers.format_number(number, PhoneNumberFormat.E164)
    resolved_region = phonenumbers.region_code_for_number(number) or selected_region or "ZZ"
    country = pycountry.countries.get(alpha_2=resolved_region)
    return {
        "phone_e164": e164, "region": resolved_region,
        "country": country.name if country else resolved_region,
        "flag": flag_emoji(resolved_region), "calling_code": f"+{number.country_code}",
    }
