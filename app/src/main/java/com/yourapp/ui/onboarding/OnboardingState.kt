package com.yourapp.ui.onboarding

data class OnboardingState(
    val name: String = "",
    val age: String = "",
    val phone: String = "",
    val otpDigits: List<String> = List(4) { "" },
    val isOtpVerified: Boolean = false,
    val selectedTraits: Set<String> = emptySet(),
    val phoneError: String? = null,
    val ageError: String? = null,
    val isSaving: Boolean = false,
) {
    val otp: String
        get() = otpDigits.joinToString(separator = "")

    val canVerifyOtp: Boolean
        get() = otpDigits.all { it.length == 1 }

    val isProfileStepValid: Boolean
        get() = name.isNotBlank() &&
            age.toIntOrNull() in 13..99 &&
            phone.length == 10 &&
            phone.all(Char::isDigit) &&
            isOtpVerified
}
