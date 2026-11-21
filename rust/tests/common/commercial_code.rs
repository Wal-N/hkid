use hkid_utils::ChineseCommercialCode;
use serde::Deserialize;

#[derive(Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub(crate) struct CodeInput {
    #[serde(deserialize_with = "Option::deserialize")]
    pub(crate) code: Option<String>,
    pub(crate) extension_number: Option<String>,
}

impl CodeInput {
    pub(crate) fn build(&self) -> Result<ChineseCommercialCode, String> {
        let code = self.code.as_deref().ok_or("null main code")?;
        ChineseCommercialCode::new(code, self.extension_number.as_deref())
            .map_err(|error| error.to_string())
    }
}
