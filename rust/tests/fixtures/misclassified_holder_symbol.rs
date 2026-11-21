use hkid_utils::{
    CardError, HkidCard, HkidCardFields, HkidSymbol, HkidSymbolCategory, HkidSymbols,
};

#[test]
fn holder_summary_rejects_right_of_abode_as_birthplace() {
    let card = HkidCard::new(HkidCardFields {
        symbols: "A".parse::<HkidSymbols>().unwrap(),
        ..Default::default()
    })
    .unwrap();
    let expected = Err(CardError::UnsupportedHolderSymbol {
        symbol: HkidSymbol::RightOfAbode,
        category: HkidSymbolCategory::ReportedPlaceOfBirth,
    });
    assert_eq!(card.describe_holder(Some("en")), expected);
    let date = "2026-07-21".parse().unwrap();
    assert_eq!(card.describe_holder_on(Some("zh-HK"), date), expected);
}
