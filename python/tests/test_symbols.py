import json
import unittest
from datetime import date
from pathlib import Path

from fixtures import load_fixtures
from hkid_utils import HkidSymbol, HkidSymbolCategory, HkidSymbols


class SymbolTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.fixtures = load_fixtures("hkid-symbols")

    def test_parse(self):
        for case in self.fixtures["parseCases"]:
            with self.subTest(case=case["id"]):
                value = case["input"]
                self.assertTrue(HkidSymbols.is_valid(value))
                self.assertEqual(str(HkidSymbols(value)), case["expectCanonical"])

    def test_invalid_parse(self):
        for case in self.fixtures["invalidParseCases"]:
            with self.subTest(case=case["id"]):
                value = case["input"]
                self.assertFalse(HkidSymbols.is_valid(value))
                with self.assertRaises(TypeError if value is None else ValueError):
                    HkidSymbols(value)
        self.assertFalse(HkidSymbols.is_valid("\ud800"))
        with self.assertRaises(TypeError):
            HkidSymbols.is_valid(b"A")

    def test_age_validation(self):
        for case in self.fixtures["ageValidationCases"]:
            with self.subTest(case=case["id"]):
                symbols = HkidSymbols(case["symbols"])
                birth = date.fromisoformat(case["dateOfBirth"])
                reference = date.fromisoformat(case["referenceDate"])
                if case["expectValid"]:
                    self.assertIsNone(symbols.validate_age(birth, reference))
                else:
                    with self.assertRaises(ValueError):
                        symbols.validate_age(birth, reference)
        with self.assertRaises(TypeError):
            HkidSymbols().validate_age(None, date.today())

    def test_symbol_metadata(self):
        path = Path(__file__).resolve().parents[2] / "data" / "hkid-symbols.json"
        records = json.loads(path.read_text(encoding="utf-8"))["symbols"]
        self.assertEqual([s.code for s in HkidSymbol.all()], [r["code"] for r in records])
        for record in records:
            with self.subTest(code=record["code"]):
                symbol = HkidSymbol(record["code"])
                self.assertEqual(symbol, getattr(HkidSymbol, record["id"]))
                self.assertEqual(symbol, HkidSymbol.lookup(record["code"].lower()))
                self.assertEqual(str(symbol), record["code"])
                self.assertEqual(symbol.category, getattr(HkidSymbolCategory, record["category"]))
                self.assertEqual(symbol.category.allows_multiple, record["category"] == "OTHER_INFORMATION")
                self.assertEqual(symbol.description, record["descriptions"]["en"])
                self.assertEqual(symbol.traditional_chinese_description, record["descriptions"]["zh-Hant"])
        self.assertEqual(HkidSymbol.lookup(" ｚ "), HkidSymbol.BORN_IN_HONG_KONG)
        for code in ("", "AZ", None, "\udfff"):
            self.assertIsNone(HkidSymbol.lookup(code))

    def test_typed_collection(self):
        symbols = HkidSymbols.from_symbols(iter([HkidSymbol("N"), "z", "A", "b"]))
        self.assertEqual(symbols, HkidSymbols("AZBN"))
        self.assertEqual(hash(symbols), hash(HkidSymbols("AZBN")))
        self.assertEqual([str(s) for s in symbols], ["A", "Z", "B", "N"])
        self.assertEqual(len(symbols), 4)
        self.assertIn(HkidSymbol.RIGHT_OF_ABODE, symbols)
        self.assertNotIn("*", symbols)
        self.assertFalse(HkidSymbols())
        self.assertEqual(symbols.by_category(HkidSymbolCategory.OTHER_INFORMATION),
                         [HkidSymbol.BIRTH_DETAILS_CHANGED, HkidSymbol.NAME_CHANGED])
        for values in (["Z", HkidSymbol.BORN_IN_HONG_KONG], ["Z", "X"]):
            with self.assertRaises(ValueError):
                HkidSymbols.from_symbols(values)
