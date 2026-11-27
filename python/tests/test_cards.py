import os
import subprocess
import sys
import time
import unittest
from datetime import date, timedelta
from unittest.mock import patch

from fixtures import load_fixtures
from hkid_utils import (
    ChineseCommercialCode, ChineseName, EnglishName, HkidCard, HkidNumber, HkidSymbols, Sex,
)


def build_card(fields):
    def optional_date(key):
        value = fields.get(key)
        return date.fromisoformat(value) if value is not None else None

    codes = [
        ChineseCommercialCode(code["code"], code.get("extensionNumber"))
        for code in fields.get("chineseCommercialCodes") or []
    ]
    return HkidCard(
        hkid_number=fields.get("hkidNumber"),
        chinese_name=ChineseName(fields.get("chineseSurname"), fields.get("chinesePersonalName"), codes),
        english_name=EnglishName(fields.get("englishSurname"), fields.get("englishPersonalName")),
        sex=fields.get("sex"),
        date_of_birth=optional_date("dateOfBirth"),
        symbols=fields.get("symbols"),
        first_registration_year_month=fields.get("firstRegistrationYearMonth"),
        date_of_registration=optional_date("dateOfRegistration"),
    )


class CardTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.fixtures = load_fixtures("hkid-card")

    def test_fields(self):
        for case in self.fixtures["cardCases"]:
            with self.subTest(case=case["id"]):
                card = build_card(case["input"])
                reference = date.fromisoformat(case["referenceDate"])
                expected = case["expect"]
                number = card.hkid_number
                self.assertEqual(str(number) if number else None, expected["hkidComplete"])
                self.assertEqual(number.without_check_digit if number else None, expected["hkidWithoutCheckDigit"])
                self.assertEqual(number.masked if number else None, expected["hkidMasked"])
                self.assertEqual(str(card.chinese_name), expected["chineseName"])
                self.assertEqual(str(card.english_name), expected["englishName"])
                self.assertEqual(str(card.sex) if card.sex else None, expected["sexPrintedValue"])
                self.assertEqual(str(card.symbols), expected["symbols"])
                self.assertEqual(card.printed_date_of_birth, expected["dateOfBirth"])
                self.assertEqual(card.printed_first_registration_year_month, expected["firstRegistrationYearMonth"])
                self.assertEqual(card.printed_date_of_registration, expected["dateOfRegistration"])
                self.assertEqual(card.age_on(reference), expected["age"])
                self.assertIsNone(card.validate_as_of(reference))
                if "chineseCommercialCodes" in expected:
                    self.assertEqual([
                        {"code": code.code, "extensionNumber": code.extension_number, "display": str(code)}
                        for code in card.chinese_name.commercial_codes
                    ], expected["chineseCommercialCodes"])

    def test_age(self):
        for case in self.fixtures["ageCases"]:
            with self.subTest(case=case["id"]):
                card = build_card(case)
                self.assertEqual(card.age_on(date.fromisoformat(case["referenceDate"])), case["expectAge"])
        self.assertIsNone(HkidCard().age_on(date.today()))
        with self.assertRaises(ValueError):
            HkidCard(date_of_birth=date(2000, 1, 1)).age_on(date(1999, 12, 31))

    def test_invalid_cards(self):
        for case in self.fixtures["invalidCardCases"]:
            with self.subTest(case=case["id"]), self.assertRaises(ValueError):
                build_card(case)
        for field in ("sex", "hkid_number", "first_registration_year_month"):
            with self.subTest(field=field), self.assertRaises(ValueError):
                HkidCard(**{field: ""})

    def test_as_of_validation(self):
        for case in self.fixtures["asOfValidationCases"]:
            with self.subTest(case=case["id"]):
                card = build_card(case)
                reference = date.fromisoformat(case["referenceDate"])
                if case["expectValid"]:
                    self.assertIsNone(card.validate_as_of(reference))
                else:
                    with self.assertRaises(ValueError):
                        card.validate_as_of(reference)
        with self.assertRaises(TypeError):
            HkidCard().validate_as_of(None)

    def test_suspicions(self):
        for case in self.fixtures["suspicionCases"]:
            with self.subTest(case=case["id"]):
                card = build_card(case["input"])
                self.assertEqual(card.find_suspicions(), case["expectReasons"])
                self.assertEqual(card.is_suspicious(), bool(case["expectReasons"]))

    def test_holder_descriptions(self):
        for case in self.fixtures["holderDescriptionCases"]:
            with self.subTest(case=case["id"]):
                card = build_card(case["input"])
                reference = date.fromisoformat(case["referenceDate"])
                locale = case.get("locale")
                default = case.get("defaultLocale")
                if default and sys.platform.startswith("linux"):
                    with patch.dict(os.environ, {"LANGUAGE": default.replace("-", "_")}):
                        description = card.describe_holder_on(reference, locale)
                else:
                    description = card.describe_holder_on(reference, locale or default)
                self.assertEqual(description, case["expectDescription"])

    def test_card_objects_dates_and_masked_representation(self):
        number = HkidNumber("A123456")
        sex = Sex(" f\t")
        symbols = HkidSymbols("AZ")
        birth = date(2000, 2, 29)
        issue = date(2019, 3, 1)
        card = HkidCard(hkid_number=number, sex=sex, symbols=symbols, date_of_birth=birth,
                        first_registration_year_month="2011-03", date_of_registration=issue)
        self.assertEqual(card.hkid_number, number)
        self.assertEqual(card.sex, Sex.FEMALE)
        self.assertEqual(card.sex.chinese_marker, "女")
        self.assertEqual(card.sex.english_marker, "F")
        self.assertEqual(card.symbols, symbols)
        self.assertEqual(card.date_of_birth, birth)
        self.assertEqual(card.first_registration_year_month, "2011-03")
        self.assertEqual(card.date_of_registration, issue)
        self.assertEqual(HkidCard.SMART_CARD_START_DATE, date(2018, 11, 26))
        self.assertIn(number.masked, repr(card))
        self.assertNotIn(number.without_check_digit, repr(card))
        self.assertEqual(card.describe_holder("en"), card.describe_holder_on(date.today(), "en"))
        self.assertEqual(HkidCard(), HkidCard())
        self.assertEqual(hash(HkidCard()), hash(HkidCard()))
        with self.assertRaises(TypeError):
            card.describe_holder_on(None)

    def test_replace_validates_partial_updates(self):
        card = HkidCard(date_of_birth=date(2005, 6, 1), date_of_registration=date(2020, 6, 1),
                        symbols="*AZ", hkid_number="A123456")
        changed = card.replace(hkid_number=None, sex="M")
        self.assertIsNone(changed.hkid_number)
        self.assertEqual(changed.date_of_birth, card.date_of_birth)
        self.assertEqual(changed.symbols, card.symbols)
        self.assertEqual(changed.sex, Sex.MALE)
        self.assertEqual(card.hkid_number, HkidNumber("A123456"))
        with self.assertRaises(ValueError):
            card.replace(symbols="***AZ")
        with self.assertRaises(TypeError):
            card.replace(symbol_codes="A")

    @unittest.skipUnless(hasattr(time, "tzset"), "requires runtime timezone changes")
    def test_holder_uses_python_local_date_on_each_call(self):
        try:
            with patch.dict(os.environ):
                for zone in ("UTC-14", "UTC+12"):
                    os.environ["TZ"] = zone
                    time.tzset()
                    today = date.today()
                    card = HkidCard(date_of_birth=today)
                    self.assertEqual(card.describe_holder("en"), card.describe_holder_on(today, "en"))
                    future = HkidCard(date_of_birth=today + timedelta(days=1))
                    with self.assertRaises(ValueError):
                        future.describe_holder("en")
        finally:
            time.tzset()

    @unittest.skipUnless(sys.platform == "win32", "Windows reads TZ at interpreter startup")
    def test_windows_default_date_follows_python_timezone(self):
        program = """
from datetime import date, timedelta
from hkid_utils import HkidCard

today = date.today()
card = HkidCard(date_of_birth=today)
assert card.describe_holder("en") == card.describe_holder_on(today, "en")
try:
    HkidCard(date_of_birth=today + timedelta(days=1)).describe_holder("en")
except ValueError:
    pass
else:
    raise AssertionError("The default reference date accepted tomorrow's birth date")
print(today.isoformat())
"""
        observed = []
        for zone in ("UTC-14", "UTC12"):
            result = subprocess.run(
                [sys.executable, "-I", "-c", program], env={**os.environ, "TZ": zone},
                capture_output=True, text=True,
            )
            self.assertEqual(result.returncode, 0, f"{zone}: {result.stderr}")
            observed.append(date.fromisoformat(result.stdout.strip()))
        self.assertGreater(observed[0], observed[1])
