import unittest

from fixtures import load_fixtures
from hkid_utils import (
    ChineseCommercialCode,
    ChineseName,
    ChineseNameEntry,
    EnglishName,
    GeneratedName,
)


def commercial_code(value):
    return ChineseCommercialCode(value["code"], value.get("extensionNumber"))


class NameTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.fixtures = load_fixtures("hkid-names")

    def test_commercial_codes(self):
        for case in self.fixtures["commercialCodeCases"]:
            with self.subTest(case=case["id"]):
                expected = case["expect"]
                actual = commercial_code(case["input"])
                self.assertTrue(ChineseCommercialCode.is_valid(actual.code, actual.extension_number))
                self.assertEqual(actual.code, expected["code"])
                self.assertEqual(actual.extension_number, expected["extensionNumber"])
                self.assertEqual(str(actual), expected["display"])

        for case in self.fixtures["invalidCommercialCodeCases"]:
            with self.subTest(case=case["id"]):
                value = case["input"]
                self.assertFalse(
                    ChineseCommercialCode.is_valid(value["code"], value.get("extensionNumber"))
                )
                with self.assertRaises(TypeError if value["code"] is None else ValueError):
                    commercial_code(value)

        for case in self.fixtures["commercialCodeComparisonCases"]:
            with self.subTest(case=case["id"]):
                left = commercial_code(case["left"])
                right = commercial_code(case["right"])
                self.assertEqual(left == right, case["expectSameValue"])
                self.assertEqual(len({left, right}), 1 if case["expectSameValue"] else 2)

    def test_chinese_names(self):
        for case in self.fixtures["chineseNameCases"]:
            with self.subTest(case=case["id"]):
                codes = [commercial_code(code) for code in case["commercialCodes"] or []]
                surname, personal = case["surname"], case["personalName"]
                self.assertTrue(ChineseName.is_valid(surname, personal, codes))
                name = ChineseName(surname, personal, codes)
                self.assertEqual(name.surname, surname or "")
                self.assertEqual(name.personal_name, personal or "")
                self.assertEqual(str(name), case["expectFullName"])
                expected = case.get("expectCommercialCodes", case["commercialCodes"]) or []
                self.assertEqual(name.commercial_codes, [commercial_code(code) for code in expected])

        for case in self.fixtures["invalidChineseNameCases"]:
            with self.subTest(case=case["id"]), self.assertRaises(ValueError):
                codes = [commercial_code(code) for code in case["commercialCodes"] or []]
                self.assertFalse(ChineseName.is_valid(case["surname"], case["personalName"], codes))
                ChineseName(case["surname"], case["personalName"], codes)

    def test_english_names(self):
        for case in self.fixtures["englishNameCases"]:
            with self.subTest(case=case["id"]):
                name = EnglishName(case["surname"], case["personalName"])
                self.assertEqual(name.surname, case.get("expectSurname", case["surname"]))
                self.assertEqual(
                    name.personal_name, case.get("expectPersonalName", case["personalName"])
                )
                self.assertEqual(str(name), case["expectFullName"])

        for case in self.fixtures["invalidEnglishNameCases"]:
            with self.subTest(case=case["id"]), self.assertRaises(ValueError):
                EnglishName(case["surname"], case["personalName"])

        for case in self.fixtures["englishNameValidationCases"]:
            with self.subTest(case=case["id"]):
                self.assertEqual(
                    EnglishName.is_valid(case.get("surname"), case.get("personalName")),
                    case["expectValid"],
                )

    def test_name_parts_and_codes(self):
        self.assertTrue(ChineseName.is_valid_part("陳" * 7))
        self.assertTrue(EnglishName.is_valid_part("Chan" * 11))
        self.assertFalse(ChineseName.is_valid_part(None))
        self.assertFalse(EnglishName.is_valid_part(""))
        self.assertFalse(ChineseName.is_valid("\ud800", None))
        self.assertFalse(EnglishName.is_valid("Chan", "\udfff"))
        self.assertFalse(ChineseCommercialCode.is_valid("0001", "\ud800"))
        self.assertTrue(ChineseName.is_valid())
        self.assertFalse(EnglishName.is_valid())
        self.assertEqual(ChineseName(None, "文"), ChineseName("", "文"))
        self.assertEqual(EnglishName(None, "Man"), EnglishName("", "Man"))
        code = ChineseCommercialCode("0001")
        self.assertFalse(ChineseName.is_valid("文", None, [None]))
        self.assertTrue(ChineseName.are_valid_commercial_codes(None))
        max_codes = [code] * ChineseName.MAX_LENGTH
        self.assertTrue(ChineseName.are_valid_commercial_codes(max_codes))
        self.assertFalse(ChineseName.are_valid_commercial_codes(max_codes + [code]))
        self.assertFalse(ChineseName.are_valid_commercial_codes([None]))

    def test_bundled_name_entries(self):
        entries = ChineseNameEntry.all()
        self.assertTrue(entries)
        for entry in entries:
            with self.subTest(character=entry.character):
                self.assertEqual(len(entry.character), 1)
                self.assertTrue(ChineseName.is_valid_part(entry.character))
                self.assertTrue(EnglishName.is_valid_part(entry.romanisation))
                self.assertTrue(ChineseCommercialCode.is_valid(entry.commercial_code.code))
                self.assertGreater(entry.weight, 0)
                self.assertTrue(entry.supported_sexes)
                self.assertEqual(
                    sum(entry.supports_sex(marker) for marker in ("M", "F")),
                    len(entry.supported_sexes),
                )
                for sex in entry.supported_sexes:
                    self.assertTrue(entry.supports_sex(sex))
                if entry.is_common_surname:
                    self.assertEqual(len(entry.supported_sexes), 2)

    def test_generated_name_fields(self):
        chinese, english = ChineseName("陳", "文"), EnglishName("Chan", "Man")
        name = GeneratedName(chinese, english)
        self.assertEqual(name.chinese, chinese)
        self.assertEqual(name.english, english)
