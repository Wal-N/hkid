import unittest
from datetime import date, timedelta

from hkid_utils import (
    ChineseNameEntry,
    DefinedPrefix,
    HkidCard,
    HkidGenerator,
    HkidNumber,
    HkidSymbol,
    Sex,
)


DAY = timedelta(days=1)


class GenerationTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.entries = {
            (entry.is_common_surname, entry.character): entry for entry in ChineseNameEntry.all()
        }

    def setUp(self):
        self.generator = HkidGenerator()

    def test_number_modes(self):
        defined = self.generator.number()
        self.assertIsNotNone(defined.defined_prefix)
        arbitrary = self.generator.number_with_any_prefix()
        for number in (defined, arbitrary):
            self.assertRegex(str(number), r"^[A-Z]{1,2}[0-9]{6}\([0-9A]\)$")
            self.assertEqual(number, HkidNumber.parse_with_check_digit(str(number)))

        for prefix in (DefinedPrefix.lookup("F"), DefinedPrefix.lookup("XA")):
            with self.subTest(prefix=prefix):
                selected = self.generator.number_from_prefixes([prefix])
                self.assertEqual(selected.defined_prefix, prefix)
                self.assertEqual(selected, HkidNumber.parse_with_check_digit(str(selected)))
        with self.assertRaises(ValueError):
            self.generator.number_from_prefixes([])
        with self.assertRaises(TypeError):
            self.generator.number_from_prefixes(["F"])

    def test_half_open_date_ranges(self):
        for start in (date.min, date(2024, 2, 28), date(2024, 2, 29), date.max - DAY):
            with self.subTest(start=start):
                self.assertEqual(self.generator.date_in_range(start, start + DAY), start)
                with self.assertRaises(ValueError):
                    self.generator.date_in_range(start, start)
                with self.assertRaises(ValueError):
                    self.generator.date_in_range(start + DAY, start)
        sampled = self.generator.date_in_range(date.min, date.max)
        self.assertLessEqual(date.min, sampled)
        self.assertLess(sampled, date.max)

        yesterday = date.today() - DAY
        sampled = self.generator.date_in_range(yesterday)
        self.assertLessEqual(yesterday, sampled)
        self.assertLess(sampled, date.today())

    def assert_name_matches_seed(self, chinese, english, sex, requested_length=None):
        length = len(chinese.personal_name)
        if requested_length is None:
            self.assertIn(length, (1, 2))
        else:
            self.assertEqual(length, requested_length)
        self.assertEqual(len(chinese.surname), 1)
        self.assertEqual(len(set(chinese.personal_name)), length)
        entries = [self.entries[True, chinese.surname]]
        entries.extend(self.entries[False, char] for char in chinese.personal_name)
        self.assertEqual(chinese.commercial_codes, [entry.commercial_code for entry in entries])
        self.assertEqual(english.surname, entries[0].romanisation)
        expected_personal = " ".join(entry.romanisation for entry in entries[1:])
        self.assertEqual(english.personal_name, expected_personal)
        if sex is not None:
            self.assertTrue(all(entry.supports_sex(sex) for entry in entries))

    def test_names(self):
        default = self.generator.name()
        self.assert_name_matches_seed(default.chinese, default.english, None)
        for sex in (Sex.FEMALE, "m", None):
            for length in range(1, 6):
                with self.subTest(sex=sex, length=length):
                    name = self.generator.name(sex=sex, personal_name_length=length)
                    self.assert_name_matches_seed(name.chinese, name.english, sex, length)
        for length in (-1, 0, 6):
            with self.subTest(length=length), self.assertRaises(ValueError):
                self.generator.name(personal_name_length=length)
        with self.assertRaises(ValueError):
            self.generator.name(sex="?")

    def test_card_constraints(self):
        places = (HkidSymbol.BORN_IN_HONG_KONG, "X", HkidSymbol.BORN_IN_MACAO, "O")
        for reference in (HkidCard.SMART_CARD_START_DATE, date(2024, 2, 29), date.max):
            for index, age in enumerate((11, 17, 18, 85, 100)):
                sex = "m" if index % 2 else Sex.FEMALE
                birthplace = places[index % len(places)]
                with self.subTest(reference=reference, age=age, birthplace=str(birthplace)):
                    card = self.generator.card_on(
                        reference, sex=sex, min_age=age, max_age=age, birthplace=birthplace
                    )
                    self.assertIsNone(card.validate_as_of(reference))
                    self.assertEqual(card.age_on(reference), age)
                    self.assertEqual(card.sex, Sex.MALE if sex == "m" else Sex.FEMALE)
                    self.assert_name_matches_seed(card.chinese_name, card.english_name, card.sex)
                    self.assertEqual(card.find_suspicions(), [])
                    self.assertEqual(card.hkid_number, HkidNumber(str(card.hkid_number)))
                    self.assertIsNotNone(card.hkid_number.defined_prefix)

                    birth = card.date_of_birth
                    had_birthday = (reference.month, reference.day) >= (birth.month, birth.day)
                    self.assertEqual(reference.year - birth.year - int(not had_birthday), age)
                    permit = "***" if age >= 18 else "*"
                    self.assertEqual(str(card.symbols), permit + "A" + str(birthplace))
                    card.symbols.validate_age(birth, card.date_of_registration)
                    self.assertLessEqual(HkidCard.SMART_CARD_START_DATE, card.date_of_registration)
                    self.assertLessEqual(card.date_of_registration, reference)
                    first_month = card.first_registration_year_month
                    self.assertLessEqual("1949-01", first_month)
                    self.assertLessEqual(first_month, card.date_of_registration.isoformat()[:7])

    def test_card_defaults(self):
        reference = date(2024, 2, 29)
        card = self.generator.card_on(reference)
        card.validate_as_of(reference)
        self.assertIn(card.age_on(reference), range(11, 86))
        self.assertIn(card.sex, (Sex.MALE, Sex.FEMALE))

        before = date.today()
        card = self.generator.card()
        constrained = self.generator.card(sex="F", min_age=100, max_age=100, birthplace="W")
        after = date.today()
        card.validate_as_of(after)
        self.assertGreaterEqual(card.age_on(after), 11)
        self.assertLessEqual(card.age_on(before), 85)
        constrained.validate_as_of(after)
        self.assertIn(100, (constrained.age_on(before), constrained.age_on(after)))
        self.assertEqual(constrained.sex, Sex.FEMALE)
        self.assertIn(HkidSymbol.BORN_IN_MACAO, constrained.symbols)

    def test_invalid_card_options(self):
        reference = HkidCard.SMART_CARD_START_DATE
        with self.assertRaises(ValueError):
            self.generator.card_on(reference - DAY)
        for options in (
            {"min_age": 10},
            {"max_age": 101},
            {"min_age": 19, "max_age": 18},
            {"birthplace": HkidSymbol.RIGHT_OF_ABODE},
            {"birthplace": "?"},
            {"sex": "?"},
        ):
            with self.subTest(options=options), self.assertRaises(ValueError):
                self.generator.card_on(reference, **options)
