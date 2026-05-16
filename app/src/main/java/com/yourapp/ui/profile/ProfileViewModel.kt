package com.yourapp.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourapp.data.local.ChatMessageDao
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

sealed class ProfileState {
    data object Loading : ProfileState()

    data class Viewing(val profile: UserProfile) : ProfileState()

    data class Editing(val profile: UserProfile) : ProfileState()

    data object Saving : ProfileState()

    data class Error(val message: String) : ProfileState()
}

enum class ProfileField {
    NAME,
    AGE,
    PHONE,
}

data class ProfileValidationErrors(
    val name: String? = null,
    val age: String? = null,
    val phone: String? = null,
) {
    val hasErrors: Boolean
        get() = name != null || age != null || phone != null
}

sealed interface UiEvent {
    data object NavigateToOnboarding : UiEvent
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userProfileRepository: UserProfileRepository,
    private val chatMessageDao: ChatMessageDao,
) : ViewModel() {
    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val profileState: StateFlow<ProfileState> = _profileState.asStateFlow()

    private val _validationErrors = MutableStateFlow(ProfileValidationErrors())
    val validationErrors: StateFlow<ProfileValidationErrors> = _validationErrors.asStateFlow()

    private val _uiEvents = MutableSharedFlow<UiEvent>()
    val uiEvents: SharedFlow<UiEvent> = _uiEvents.asSharedFlow()

    private var lastViewedProfile: UserProfile? = null

    init {
        viewModelScope.launch {
            userProfileRepository.userProfileFlow.collect { profile ->
                if (_profileState.value is ProfileState.Editing || _profileState.value is ProfileState.Saving) {
                    return@collect
                }

                if (profile == null) {
                    _profileState.value = ProfileState.Error("Profile not found")
                } else {
                    lastViewedProfile = profile
                    _profileState.value = ProfileState.Viewing(profile)
                }
            }
        }
    }

    fun startEditing() {
        val current = _profileState.value
        if (current is ProfileState.Viewing) {
            _validationErrors.value = ProfileValidationErrors()
            _profileState.value = ProfileState.Editing(current.profile)
        }
    }

    fun cancelEditing() {
        val profile = lastViewedProfile ?: return
        _validationErrors.value = ProfileValidationErrors()
        _profileState.value = ProfileState.Viewing(profile)
    }

    fun updateField(field: ProfileField, value: String) {
        val current = _profileState.value as? ProfileState.Editing ?: return
        val updatedProfile = when (field) {
            ProfileField.NAME -> current.profile.copy(name = value)
            ProfileField.AGE -> current.profile.copy(age = value.filter(Char::isDigit).take(2).toIntOrNull() ?: 0)
            ProfileField.PHONE -> current.profile.copy(phone = value.filter(Char::isDigit).take(10))
        }
        _profileState.value = ProfileState.Editing(updatedProfile)
        _validationErrors.value = validateProfile(updatedProfile)
    }

    fun toggleTrait(trait: String) {
        val current = _profileState.value as? ProfileState.Editing ?: return
        val traits = current.profile.selectedTraits
        val updatedTraits = when {
            trait in traits -> traits - trait
            traits.size >= MAX_TRAITS -> traits
            else -> traits + trait
        }
        _profileState.value = ProfileState.Editing(
            current.profile.copy(selectedTraits = updatedTraits),
        )
    }

    fun saveProfile() {
        val current = _profileState.value as? ProfileState.Editing ?: return
        val errors = validateProfile(current.profile)
        _validationErrors.value = errors
        if (errors.hasErrors) return

        viewModelScope.launch {
            _profileState.value = ProfileState.Saving
            try {
                val profileToSave = current.profile.copy(
                    name = current.profile.name.trim(),
                    lastSyncedAt = 0L,
                )
                userProfileRepository.saveProfile(profileToSave)
                lastViewedProfile = profileToSave
                _validationErrors.value = ProfileValidationErrors()
                _profileState.value = ProfileState.Viewing(profileToSave)
            } catch (exception: Exception) {
                _profileState.value = ProfileState.Error(exception.message ?: "Unable to save profile")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _profileState.value = ProfileState.Saving
            try {
                chatMessageDao.deleteAll()
                userProfileRepository.clearProfile()
                _uiEvents.emit(UiEvent.NavigateToOnboarding)
            } catch (exception: Exception) {
                _profileState.value = ProfileState.Error(exception.message ?: "Unable to log out")
            }
        }
    }

    private fun validateProfile(profile: UserProfile): ProfileValidationErrors {
        val trimmedName = profile.name.trim()
        return ProfileValidationErrors(
            name = when {
                trimmedName.isBlank() -> "Name cannot be blank"
                trimmedName.length < 2 -> "Name must be at least 2 characters"
                else -> null
            },
            age = if (profile.age in 13..99) null else "Age must be between 13 and 99",
            phone = if (profile.phone.length == 10 && profile.phone.all(Char::isDigit)) {
                null
            } else {
                "Phone must be exactly 10 digits"
            },
        )
    }

    private companion object {
        private const val MAX_TRAITS = 3
    }
}
