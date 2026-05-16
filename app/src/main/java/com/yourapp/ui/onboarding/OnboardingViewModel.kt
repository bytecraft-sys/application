package com.yourapp.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourapp.data.local.UserProfile
import com.yourapp.data.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val DEV_OTP = "1234"

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userProfileRepository: UserProfileRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    fun onNameChanged(value: String) {
        _state.update { it.copy(name = value) }
    }

    fun onAgeChanged(value: String) {
        val digits = value.filter(Char::isDigit).take(2)
        _state.update {
            it.copy(
                age = digits,
                ageError = validateAge(digits),
            )
        }
    }

    fun onPhoneChanged(value: String) {
        val digits = value.filter(Char::isDigit).take(10)
        _state.update {
            it.copy(
                phone = digits,
                phoneError = validatePhone(digits),
            )
        }
    }

    fun onOtpDigitChanged(index: Int, value: String) {
        if (index !in 0..3) return
        val digit = value.filter(Char::isDigit).takeLast(1)
        _state.update { current ->
            val updatedDigits = current.otpDigits.toMutableList()
            updatedDigits[index] = digit
            current.copy(
                otpDigits = updatedDigits,
                isOtpVerified = updatedDigits.joinToString(separator = "") == DEV_OTP,
            )
        }
    }

    fun verifyOtp() {
        _state.update { it.copy(isOtpVerified = it.otp == DEV_OTP) }
    }

    fun toggleTrait(trait: String): Boolean {
        var reachedMax = false
        _state.update { current ->
            val selected = current.selectedTraits
            when {
                trait in selected -> current.copy(selectedTraits = selected - trait)
                selected.size >= REQUIRED_TRAIT_COUNT -> {
                    reachedMax = true
                    current
                }
                else -> current.copy(selectedTraits = selected + trait)
            }
        }
        return reachedMax
    }

    fun onOnboardingComplete() {
        val snapshot = _state.value
        if (!snapshot.isProfileStepValid || !snapshot.isTraitsStepValid || snapshot.isSaving) return

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val now = System.currentTimeMillis()
            userProfileRepository.saveProfile(
                UserProfile(
                    id = LOCAL_USER_ID,
                    name = snapshot.name.trim(),
                    age = snapshot.age.toInt(),
                    phone = snapshot.phone,
                    otp = snapshot.otp,
                    selectedTraits = snapshot.selectedTraits.toList(),
                    createdAt = now,
                    lastSyncedAt = 0L,
                ),
            )
            _state.update { it.copy(isSaving = false) }
            _events.emit(UiEvent.NavigateToHome)
        }
    }

    fun saveProfile() {
        onOnboardingComplete()
    }

    private fun validateAge(value: String): String? {
        if (value.isBlank()) return null
        return if (value.toIntOrNull() in 13..99) null else "Age must be between 13 and 99"
    }

    private fun validatePhone(value: String): String? {
        if (value.isBlank()) return null
        return if (value.length == 10 && value.all(Char::isDigit)) null else "Enter a valid 10-digit number"
    }

    private companion object {
        private const val LOCAL_USER_ID = "local-user"
    }
}
