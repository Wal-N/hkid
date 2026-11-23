use std::sync::LazyLock;

use super::{GenerationError, HkidGenerator};
use crate::{ChineseCommercialCode, ChineseName, EnglishName, Sex};

const HEADER: [&str; 6] = [
    "commercialCode",
    "character",
    "romanisation",
    "commonSurname",
    "sexAssociation",
    "weight",
];
static ENTRIES: LazyLock<Result<Vec<ChineseNameEntry>, String>> =
    LazyLock::new(|| load_entries(include_str!("../../../data/chinese-name-seed.csv")));

/// A generated Chinese name with commercial codes and its matching romanisation.
#[derive(Clone, Debug, Eq, PartialEq)]
pub struct GeneratedName {
    /// Chinese surname and personal name with commercial codes in printed order.
    pub chinese: ChineseName,
    /// English surname and spaced romanisations of the Chinese personal name.
    pub english: EnglishName,
}

/// One character in the bundled name seed used by [`HkidGenerator::name`].
#[derive(Debug, Eq, PartialEq)]
pub struct ChineseNameEntry {
    code: ChineseCommercialCode,
    character: char,
    romanisation: String,
    surname: bool,
    sex: Option<Sex>,
    weight: u64,
}

impl ChineseNameEntry {
    /// All entries in CSV order. A malformed seed returns [`GenerationError::InvalidSeed`].
    pub fn all() -> Result<&'static [Self], GenerationError> {
        ENTRIES
            .as_deref()
            .map_err(|message| GenerationError::InvalidSeed(message.clone()))
    }

    /// The Han character represented by this entry.
    pub fn character(&self) -> char {
        self.character
    }

    /// The character's four-digit Chinese commercial code.
    pub fn commercial_code(&self) -> &ChineseCommercialCode {
        &self.code
    }

    /// The title-cased romanisation used in generated English names.
    pub fn romanisation(&self) -> &str {
        &self.romanisation
    }

    /// Whether the generator uses this entry as a surname.
    pub fn is_common_surname(&self) -> bool {
        self.surname
    }

    /// The card sex values for which the generator may select this entry.
    pub fn supported_sexes(&self) -> &'static [Sex] {
        match self.sex {
            Some(Sex::Male) => &[Sex::Male],
            Some(Sex::Female) => &[Sex::Female],
            None => &[Sex::Male, Sex::Female],
        }
    }

    /// Whether the generator may select this entry for the requested sex.
    pub fn supports_sex(&self, sex: Sex) -> bool {
        self.supported_sexes().contains(&sex)
    }

    /// The positive relative selection weight within the eligible pool.
    pub fn weight(&self) -> u64 {
        self.weight
    }
}

fn fields(line: &str) -> Result<[&str; 6], &'static str> {
    // None of the six field types permits an embedded comma or quotation mark.
    // Quoting an entire field is valid in both the header and the data rows.
    line.split(',')
        .map(|field| {
            let field = field.trim();
            field
                .strip_prefix('"')
                .and_then(|s| s.strip_suffix('"'))
                .unwrap_or(field)
                .trim()
        })
        .collect::<Vec<_>>()
        .try_into()
        .map_err(|_| "expected six fields")
}

fn parse_entry(row: [&str; 6]) -> Result<ChineseNameEntry, &'static str> {
    let [code, character, romanisation, surname, sex, weight] = row;
    let code = ChineseCommercialCode::new(code, None).map_err(|_| "invalid commercialCode")?;
    if !ChineseName::is_valid_part(character) {
        return Err("expected one Han letter");
    }
    let character = character
        .parse::<char>()
        .map_err(|_| "expected one Han letter")?;
    if romanisation.is_empty() || !romanisation.bytes().all(|b| b.is_ascii_alphabetic()) {
        return Err("romanisation must contain ASCII letters");
    }
    let mut romanisation = romanisation.to_ascii_lowercase();
    romanisation[..1].make_ascii_uppercase();
    let surname = surname
        .to_ascii_lowercase()
        .parse::<bool>()
        .map_err(|_| "invalid commonSurname")?;
    let sex = match sex.to_ascii_uppercase().as_str() {
        "UNISEX" => None,
        "FEMALE" => Some(Sex::Female),
        "MALE" => Some(Sex::Male),
        _ => return Err("invalid sexAssociation"),
    };
    if surname && sex.is_some() {
        return Err("surname must be UNISEX");
    }
    let weight = weight
        .parse::<i32>()
        .ok()
        .filter(|&n| n > 0)
        .ok_or("weight must be a positive integer")?;
    Ok(ChineseNameEntry {
        code,
        character,
        romanisation,
        surname,
        sex,
        weight: weight as u64,
    })
}

fn load_entries(input: &str) -> Result<Vec<ChineseNameEntry>, String> {
    // Count a CRLF pair as one record separator, including in mixed-ending files.
    let normalized = input.replace("\r\n", "\n");
    let mut records = normalized
        .split(['\r', '\n'])
        .enumerate()
        .filter(|(_, line)| {
            let line = line.trim();
            !line.is_empty() && !line.starts_with('#')
        });
    let (index, header) = records.next().ok_or("missing name seed header")?;
    if fields(header) != Ok(HEADER) {
        return Err(format!("invalid name seed header at line {}", index + 1));
    }
    records
        .map(|(index, line)| {
            fields(line)
                .and_then(parse_entry)
                .map_err(|cause| format!("line {}: {cause}", index + 1))
        })
        .collect()
}

fn draw<'a>(rng: &mut fastrand::Rng, pool: &mut Vec<&'a ChineseNameEntry>) -> &'a ChineseNameEntry {
    let total = pool.iter().map(|entry| entry.weight).sum::<u64>();
    let mut ticket = rng.u64(..total);
    for (index, entry) in pool.iter().enumerate() {
        if ticket < entry.weight {
            return pool.swap_remove(index);
        }
        ticket -= entry.weight;
    }
    unreachable!("ticket is below the sum of the positive weights")
}

impl HkidGenerator {
    /// Generates a weighted surname and distinct given-name entries from the bundled CSV.
    ///
    /// A supplied sex permits matching and unisex given names. The personal name can
    /// have one to five characters. By default it has one character with probability
    /// 10% and two characters with probability 90%.
    pub fn name(
        &mut self,
        sex: Option<Sex>,
        personal_name_length: Option<usize>,
    ) -> Result<GeneratedName, GenerationError> {
        let length =
            personal_name_length.unwrap_or_else(|| if self.rng.u8(..10) == 0 { 1 } else { 2 });
        if !(1..ChineseName::MAX_LENGTH).contains(&length) {
            return Err(GenerationError::InvalidNameLength);
        }
        let entries = ChineseNameEntry::all()?;
        let (mut surnames, mut given): (Vec<_>, Vec<_>) = entries
            .iter()
            .filter(|entry| sex.is_none_or(|sex| entry.supports_sex(sex)))
            .partition(|e| e.surname);
        if surnames.is_empty() || given.len() < length {
            return Err(GenerationError::InsufficientNameEntries);
        }
        let surname = draw(&mut self.rng, &mut surnames);
        let mut personal_name = String::new();
        let mut romanisations = Vec::with_capacity(length);
        let mut codes = vec![surname.code.clone()];
        for _ in 0..length {
            let entry = draw(&mut self.rng, &mut given);
            personal_name.push(entry.character);
            romanisations.push(entry.romanisation.as_str());
            codes.push(entry.code.clone());
        }
        let chinese = ChineseName::with_commercial_codes(
            Some(&surname.character.to_string()),
            Some(&personal_name),
            codes,
        )
        .map_err(|e| GenerationError::InvalidSeed(e.to_string()))?;
        let english = EnglishName::new(Some(&surname.romanisation), Some(&romanisations.join(" ")))
            .map_err(|e| GenerationError::InvalidSeed(e.to_string()))?;
        Ok(GeneratedName { chinese, english })
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::collections::HashSet;

    #[test]
    fn loads_quoted_fields_comments_and_supplementary_han_with_each_line_ending() {
        let header = format!("\"commercialCode\",{}", HEADER[1..].join(","));
        let lines = [
            "  # note",
            &header,
            "",
            "\"0001\",𠀀, \"cHaN\",TRUE,unisex,7",
            "2429,文,Man,false,UNISEX,1",
        ];
        for separator in ["\n", "\r\n", "\r"] {
            let entries = load_entries(&lines.join(separator)).unwrap();
            assert_eq!(entries.len(), 2);
            let entry = &entries[0];
            assert_eq!(entry.character, '𠀀');
            assert_eq!(entry.code.code(), "0001");
            assert_eq!(entry.romanisation, "Chan");
            assert!(entry.surname);
            assert_eq!(entry.sex, None);
            assert_eq!(entry.weight, 7);
            assert_eq!(entries[1].character, '文');
        }
    }

    #[test]
    fn rejects_invalid_seed_rows_with_physical_line_numbers() {
        let header = HEADER.join(",");
        for row in [
            "0001,陳,Chan,true,UNISEX",
            "0001,陳,Chan,true,UNISEX,1,extra",
            "001,陳,Chan,true,UNISEX,1",
            "0001,陳文,Chan,true,UNISEX,1",
            "0001,〇,Chan,true,UNISEX,1",
            "0001,陳,Ch-an,true,UNISEX,1",
            "0001,陳,\"Chan,true,UNISEX,1",
            "0001,陳,Chan,yes,UNISEX,1",
            "0001,陳,Chan,true,MALE,1",
            "0001,陳,Chan,false,unknown,1",
            "0001,陳,Chan,false,UNISEX,0",
        ] {
            let error = load_entries(&format!("{header}\n# note\n\n{row}"))
                .err()
                .unwrap();
            assert!(error.contains("line 4"), "{row}: {error}");
        }
        assert!(load_entries("# empty\n").is_err());
        assert!(load_entries(&header.replace("weight", "other")).is_err());
        let mixed = format!("# note\r{header}\r\n\n2429,文,Man,false,UNISEX,0\r");
        assert_eq!(
            load_entries(&mixed).err().unwrap(),
            "line 4: weight must be a positive integer"
        );
    }

    #[test]
    fn weighted_draw_uses_entry_weights_and_removes_the_selected_row() {
        let seed = format!(
            "{}\n0001,文,Man,False,UNISEX,1\n0002,明,Ming,False,MALE,3",
            HEADER.join(",")
        );
        let entries = load_entries(&seed).unwrap();
        let mut rng = fastrand::Rng::with_seed(23);
        let mut expected = rng.clone();
        let mut seen = [false; 4];
        for _ in 0..64 {
            let mut pool: Vec<_> = entries.iter().collect();
            let ticket = expected.u64(..4);
            seen[ticket as usize] = true;
            let selected = draw(&mut rng, &mut pool);
            assert_eq!(selected.character, if ticket == 0 { '文' } else { '明' });
            assert_eq!(pool.len(), 1);
            assert_ne!(selected.character, pool[0].character);
        }
        assert_eq!(seen, [true; 4]);
    }

    #[test]
    fn default_length_uses_one_of_ten_equally_likely_rolls_for_one_character() {
        let mut seen = [false; 10];
        for seed in 0..128 {
            let roll = fastrand::Rng::with_seed(seed).u8(..10);
            seen[usize::from(roll)] = true;
            let name = HkidGenerator::with_seed(seed).name(None, None).unwrap();
            assert_eq!(
                name.chinese.personal_name().chars().count(),
                if roll == 0 { 1 } else { 2 }
            );
        }
        assert_eq!(seen, [true; 10]);
    }

    #[test]
    fn complete_cards_use_the_holders_sex_for_given_name_selection() {
        let entries = ENTRIES.as_ref().unwrap();
        let mut generator = HkidGenerator::with_seed(82);
        let reference = crate::NaiveDate::from_ymd_opt(2026, 7, 27).unwrap();
        for requested in [None, Some(Sex::Male), Some(Sex::Female)] {
            for _ in 0..64 {
                let card = generator.card_on(reference, requested).unwrap();
                let fields = card.fields();
                let sex = fields.sex.unwrap();
                for ch in fields.chinese_name.personal_name().chars() {
                    let row = entries
                        .iter()
                        .find(|e| !e.surname && e.character == ch)
                        .unwrap();
                    if let Some(association) = row.sex {
                        assert_eq!(association, sex, "given-name character {ch}");
                    }
                }
            }
        }
    }

    #[test]
    fn aligns_sex_romanisation_and_codes_for_each_character_at_all_lengths() {
        let entries = ENTRIES.as_ref().unwrap();
        let mut generator = HkidGenerator::with_seed(91);
        for sex in [None, Some(Sex::Male), Some(Sex::Female)] {
            for length in 1..ChineseName::MAX_LENGTH {
                for _ in 0..24 {
                    let name = generator.name(sex, Some(length)).unwrap();
                    let given = name.chinese.personal_name();
                    assert_eq!(given.chars().collect::<HashSet<_>>().len(), length);
                    let codes = name.chinese.commercial_codes();
                    assert_eq!(codes.len(), length + 1);
                    let romanisations: Vec<_> = std::iter::once(name.english.surname())
                        .chain(name.english.personal_name().split(' '))
                        .collect();
                    assert_eq!(romanisations.len(), length + 1);
                    for (i, ch) in name.chinese.to_string().chars().enumerate() {
                        let row = entries
                            .iter()
                            .find(|e| e.character == ch && e.surname == (i == 0))
                            .unwrap();
                        assert_eq!(codes[i], row.code);
                        assert_eq!(romanisations[i], row.romanisation);
                        assert!(sex.is_none() || row.sex.is_none() || row.sex == sex);
                    }
                }
            }
        }
        for length in [0, ChineseName::MAX_LENGTH, usize::MAX] {
            assert!(matches!(
                generator.name(None, Some(length)),
                Err(GenerationError::InvalidNameLength)
            ));
        }
    }
}
