pub(crate) fn is_separator(character: char) -> bool {
    matches!(character, ' ' | '\u{00a0}' | '\u{3000}')
}

pub(crate) fn trim_outer(input: &str) -> &str {
    input.trim_matches(|ch| is_separator(ch) || matches!(ch, '\t' | '\n' | '\r'))
}

pub(crate) fn fold_fullwidth(character: char) -> char {
    match character {
        '０'..='９' | 'Ａ'..='Ｚ' | 'ａ'..='ｚ' => {
            char::from((u32::from(character) - 0xfee0) as u8)
        }
        other => other,
    }
}
