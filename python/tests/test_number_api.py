import unittest
from datetime import date

from fixtures import load_fixtures
from hkid_utils import (
    DefinedPrefix,
    HkidNumber,
    is_valid_ascii_without_whitespace,
    is_valid_check_digit,
)


class NumberAndPrefixTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.fixtures = load_fixtures("hkid-number")

    def test_number_components_and_formats(self):
        for case in self.fixtures["parseCases"]:
            with self.subTest(case=case["id"]):
                number = HkidNumber(case["input"])
                expected = case["expect"]
                self.assertEqual(number.prefix, expected["prefix"])
                self.assertEqual(number.numerals, expected["numerals"])
                self.assertEqual(number.check_digit, expected["checkDigit"])
                self.assertEqual(number.without_check_digit, expected["withoutCheckDigit"])
                self.assertEqual(number.without_parentheses, expected["withoutParentheses"])
                self.assertEqual(number.masked, expected["masked"])
                self.assertEqual(str(number), expected["complete"])
                self.assertEqual(
                    HkidNumber.from_parts(number.prefix, number.numerals, number.check_digit), number
                )
                self.assertEqual(HkidNumber.parse_with_check_digit(number.without_parentheses), number)

    def test_invalid_numbers(self):
        errors = {"INVALID_FORMAT": "format", "INVALID_CHECK_DIGIT": "check digit"}
        for case in self.fixtures["invalidParseCases"]:
            value = case["input"]
            with self.subTest(case=case["id"]):
                if value is None:
                    with self.assertRaises(TypeError):
                        HkidNumber(value)
                else:
                    with self.assertRaisesRegex(ValueError, errors[case["expectError"]]):
                        HkidNumber(value)

    def test_constructor_boundaries(self):
        number = HkidNumber("A000002")
        self.assertEqual({number, HkidNumber.from_parts("a", "000002")}, {number})
        self.assertEqual(HkidNumber.from_parts("a", "000002", ""), number)
        self.assertEqual(HkidNumber.parse_with_check_digit("ａ０００００２（ａ）"), number)
        with self.assertRaisesRegex(ValueError, "missing"):
            HkidNumber.parse_with_check_digit("A000002")
        with self.assertRaisesRegex(ValueError, "incorrect"):
            HkidNumber.from_parts("A", "000002", "0")
        with self.assertRaisesRegex(ValueError, "format"):
            HkidNumber.from_parts("Ａ", "000002")
        with self.assertRaises(AttributeError):
            number.prefix = "B"

    def test_ascii_validation(self):
        for value in ("AB123456", "A000002A", "a123456(3)"):
            self.assertTrue(is_valid_ascii_without_whitespace(value))
        for value in (None, "", "Ａ123456(3)", "A 123456", "A123456(4)", "\ud800"):
            self.assertFalse(is_valid_ascii_without_whitespace(value))
        with self.assertRaises(TypeError):
            is_valid_ascii_without_whitespace(123456)

    def test_separate_check_digit_validation(self):
        for case in self.fixtures["checkDigitValidationCases"]:
            with self.subTest(case=case["id"]):
                self.assertEqual(
                    is_valid_check_digit(case["input"], case["checkDigit"]), case["expectValid"]
                )
        self.assertFalse(is_valid_check_digit("A123456", "\ud800"))
        with self.assertRaises(TypeError):
            is_valid_check_digit("A123456", 3)

    def test_prefix_descriptions(self):
        for case in self.fixtures["prefixDescriptionCases"]:
            with self.subTest(case=case["id"]):
                number = HkidNumber(case["input"])
                self.assertEqual(number.prefix_description, case["expectEnglish"])
                self.assertEqual(
                    number.prefix_traditional_chinese_description, case["expectTraditionalChinese"]
                )
                prefix = DefinedPrefix.lookup(number.prefix)
                self.assertEqual(number.defined_prefix, prefix)
                if prefix is not None:
                    self.assertEqual(prefix.description, case["expectEnglish"])
                    self.assertEqual(
                        prefix.traditional_chinese_description, case["expectTraditionalChinese"]
                    )

    def test_birth_registration(self):
        for case in self.fixtures["birthRegistrationPrefixCases"]:
            with self.subTest(case=case["id"]):
                day = date.fromisoformat(case["date"])
                prefix = DefinedPrefix.from_birth_registration_date(day)
                self.assertEqual(prefix.id if prefix else None, case["expectPrefix"])
                matches = [p for p in DefinedPrefix.all() if p.supports_birth_registration_date(day)]
                self.assertEqual(matches, [prefix] if prefix else [])

    def test_first_issue(self):
        for case in self.fixtures["firstIssuePrefixCases"]:
            with self.subTest(case=case["id"]):
                year, month = map(int, case["month"].split("-"))
                matches = DefinedPrefix.from_first_issue_month(year, month)
                self.assertEqual([p.id for p in matches], case["expectPrefixes"])
                self.assertEqual(
                    [p for p in DefinedPrefix.all() if p.supports_first_issue_month(year, month)],
                    matches,
                )

    def test_periods_and_cutoffs(self):
        prefixes = DefinedPrefix.all()
        worker = DefinedPrefix.lookup("\u3000ｗＸ\t")
        self.assertIn(worker, set(prefixes))
        self.assertEqual(str(worker), "WX")
        self.assertIsNone(worker.first_issue_period)
        self.assertIsNone(DefinedPrefix.lookup("Q"))
        self.assertIsNone(DefinedPrefix.lookup(None))
        self.assertIsNone(DefinedPrefix.lookup("\ud800"))
        self.assertEqual(
            DefinedPrefix.lookup("Z").birth_registration_period,
            (date(1980, 1, 1), date(1989, 1, 1)),
        )
        self.assertEqual(DefinedPrefix.lookup("L").first_issue_period, (date(2026, 7, 27), None))
        self.assertEqual(
            [p.id for p in DefinedPrefix.from_first_issue_month(2026, 7, earliest=date(2026, 7, 27))],
            ["L"],
        )
        for day, expected in ((26, ["F"]), (27, ["F", "L"])):
            cutoff = date(2026, 7, day)
            matches = DefinedPrefix.from_first_issue_month(2026, 7, latest=cutoff)
            self.assertEqual([p.id for p in matches], expected)
            self.assertEqual(
                [p for p in prefixes if p.supports_first_issue_month(2026, 7, latest=cutoff)],
                matches,
            )
