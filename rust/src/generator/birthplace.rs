use super::HkidGenerator;
use crate::HkidSymbol;

// Cumulative resident counts by birthplace in the 2021 Hong Kong Population Census.
const HONG_KONG_END: u32 = 4_574_834;
const MAINLAND_END: u32 = HONG_KONG_END + 2_144_126;
const MACAO_END: u32 = MAINLAND_END + 51_975;
const POPULATION: u32 = MACAO_END + 17_761 + 624_374; // Taiwan and all other places

impl HkidGenerator {
    pub(super) fn census_birthplace(&mut self) -> HkidSymbol {
        birthplace_for_ticket(self.rng.u32(..POPULATION))
    }
}

fn birthplace_for_ticket(ticket: u32) -> HkidSymbol {
    match ticket {
        0..HONG_KONG_END => HkidSymbol::BornInHongKong,
        HONG_KONG_END..MAINLAND_END => HkidSymbol::BornInMainland,
        MAINLAND_END..MACAO_END => HkidSymbol::BornInMacao,
        MACAO_END..POPULATION => HkidSymbol::BornElsewhere,
        _ => unreachable!("ticket must be within the census population"),
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn census_tickets_match_the_2021_birthplace_counts() {
        assert_eq!(POPULATION, 7_413_070);
        for (first, last, birthplace) in [
            (0, 4_574_833, HkidSymbol::BornInHongKong),
            (4_574_834, 6_718_959, HkidSymbol::BornInMainland),
            (6_718_960, 6_770_934, HkidSymbol::BornInMacao),
            (6_770_935, 7_413_069, HkidSymbol::BornElsewhere),
        ] {
            assert_eq!(birthplace_for_ticket(first), birthplace);
            assert_eq!(birthplace_for_ticket(last), birthplace);
        }
    }
}
