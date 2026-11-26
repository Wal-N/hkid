import json
import unittest
from pathlib import Path

from hkid_utils import check_digit, format_number, is_valid


class NumberTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        path = Path(__file__).resolve().parents[2] / "conformance" / "hkid-number.json"
        cls.fixtures = json.loads(path.read_text(encoding="utf-8"))
        version = cls.fixtures["schemaVersion"]
        if type(version) is not int or version != 1:
            raise ValueError(f"Unsupported HKID number fixture schema version {version!r}")

    def test_valid_numbers(self):
        cases = self.fixtures["parseCases"]
        self.assertTrue(cases)
        for case in cases:
            with self.subTest(case=case["id"]):
                value = case["input"]
                expected = case["expect"]
                self.assertIs(is_valid(value), True)
                self.assertEqual(check_digit(value), expected["checkDigit"])
                self.assertEqual(format_number(value), expected["complete"])

    def test_invalid_numbers(self):
        error_details = {"INVALID_FORMAT": "format", "INVALID_CHECK_DIGIT": "check digit"}
        cases = self.fixtures["invalidParseCases"]
        self.assertTrue(cases)
        for case in cases:
            with self.subTest(case=case["id"]):
                value = case["input"]
                expected_error = case["expectError"]
                if value is None:
                    self.assertEqual(expected_error, "INVALID_FORMAT")
                    self.assertIs(is_valid(None), False)
                    for function in (check_digit, format_number):
                        with self.subTest(function=function.__name__), self.assertRaises(TypeError):
                            function(None)
                else:
                    self.assertIsInstance(value, str)
                    self.assert_invalid_text(value, error_details[expected_error])

    def test_unpaired_surrogates(self):
        for value in ("\ud800123456", "A123456\udfff"):
            with self.subTest(value=value):
                self.assert_invalid_text(value)

    def test_non_text_arguments(self):
        for value in (b"A123456", 123456):
            with self.subTest(value=value):
                for function in (is_valid, check_digit, format_number):
                    with self.subTest(function=function.__name__), self.assertRaises(TypeError):
                        function(value)

    def test_input_is_required(self):
        for function in (is_valid, check_digit, format_number):
            with self.subTest(function=function.__name__), self.assertRaises(TypeError):
                function()

    def assert_invalid_text(self, value, expected_detail=None):
        self.assertIs(is_valid(value), False)
        for function in (check_digit, format_number):
            with self.subTest(function=function.__name__):
                with self.assertRaises(ValueError) as caught:
                    function(value)
                if expected_detail is not None:
                    self.assertIn(expected_detail, str(caught.exception))
