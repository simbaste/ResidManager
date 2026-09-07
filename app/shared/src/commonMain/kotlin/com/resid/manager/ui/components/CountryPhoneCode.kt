package com.resid.manager.ui.components

data class CountryPhoneCode(
    val iso: String,
    val dialCode: String,
    val name: String
)

val SupportedCountryPhoneCodes: List<CountryPhoneCode> = listOf(
    CountryPhoneCode(iso = "FR", dialCode = "+33", name = "France"),
    CountryPhoneCode(iso = "CI", dialCode = "+225", name = "Côte d'Ivoire"),
    CountryPhoneCode(iso = "CM", dialCode = "+237", name = "Cameroun"),
    CountryPhoneCode(iso = "SN", dialCode = "+221", name = "Sénégal"),
    CountryPhoneCode(iso = "BE", dialCode = "+32", name = "Belgique"),
    CountryPhoneCode(iso = "CH", dialCode = "+41", name = "Suisse"),
    CountryPhoneCode(iso = "CA", dialCode = "+1", name = "Canada"),
    CountryPhoneCode(iso = "US", dialCode = "+1", name = "États-Unis"),
    CountryPhoneCode(iso = "MA", dialCode = "+212", name = "Maroc"),
    CountryPhoneCode(iso = "DZ", dialCode = "+213", name = "Algérie"),
    CountryPhoneCode(iso = "TN", dialCode = "+216", name = "Tunisie"),
    CountryPhoneCode(iso = "GA", dialCode = "+241", name = "Gabon"),
    CountryPhoneCode(iso = "CD", dialCode = "+243", name = "RDC"),
    CountryPhoneCode(iso = "CG", dialCode = "+242", name = "Congo"),
    CountryPhoneCode(iso = "TG", dialCode = "+228", name = "Togo"),
    CountryPhoneCode(iso = "BJ", dialCode = "+229", name = "Bénin"),
    CountryPhoneCode(iso = "ML", dialCode = "+223", name = "Mali"),
    CountryPhoneCode(iso = "BF", dialCode = "+226", name = "Burkina Faso"),
    CountryPhoneCode(iso = "GN", dialCode = "+224", name = "Guinée"),
    CountryPhoneCode(iso = "GB", dialCode = "+44", name = "Royaume-Uni"),
    CountryPhoneCode(iso = "DE", dialCode = "+49", name = "Allemagne"),
    CountryPhoneCode(iso = "ES", dialCode = "+34", name = "Espagne"),
    CountryPhoneCode(iso = "IT", dialCode = "+39", name = "Italie"),
    CountryPhoneCode(iso = "PT", dialCode = "+351", name = "Portugal")
)

fun findCountryOrDefault(iso: String): CountryPhoneCode {
    val upper = iso.trim().uppercase()
    return SupportedCountryPhoneCodes.firstOrNull { it.iso == upper }
        ?: SupportedCountryPhoneCodes.first { it.iso == "FR" }
}
