package com.yourapp.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.application.ui.theme.ApplicationTheme

@Composable
fun ProfileFormScreen(
    state: OnboardingState,
    onNameChanged: (String) -> Unit,
    onAgeChanged: (String) -> Unit,
    onPhoneChanged: (String) -> Unit,
    onOtpDigitChanged: (Int, String) -> Unit,
    onVerifyOtp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Create your profile",
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(modifier = Modifier.height(20.dp))
        TextField(
            value = state.name,
            onValueChange = onNameChanged,
            label = { Text("Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(12.dp))
        NumberTextField(
            value = state.age,
            onValueChange = onAgeChanged,
            error = state.ageError,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(12.dp))
        TextField(
            value = state.phone,
            onValueChange = onPhoneChanged,
            label = { Text("Phone") },
            singleLine = true,
            isError = state.phoneError != null,
            supportingText = {
                state.phoneError?.let { Text(it) }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "OTP",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            state.otpDigits.forEachIndexed { index, digit ->
                TextField(
                    value = digit,
                    onValueChange = { onOtpDigitChanged(index, it) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleLarge.copy(textAlign = TextAlign.Center),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.width(56.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(
                enabled = state.canVerifyOtp,
                onClick = onVerifyOtp,
            ) {
                Text("Verify OTP")
            }
            if (state.isOtpVerified) {
                AssistChip(
                    onClick = {},
                    label = { Text("Verified") },
                )
            }
        }
    }
}

@Composable
fun NumberTextField(
    value: String,
    onValueChange: (String) -> Unit,
    error: String?,
    modifier: Modifier = Modifier,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Age") },
        singleLine = true,
        isError = error != null,
        supportingText = {
            error?.let { Text(it) }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
fun ProfileFormScreenPreview() {
    ApplicationTheme {
        ProfileFormScreen(
            state = OnboardingState(
                name = "Alex",
                age = "28",
                phone = "5551234567",
                otpDigits = listOf("1", "2", "3", "4"),
                isOtpVerified = true,
            ),
            onNameChanged = {},
            onAgeChanged = {},
            onPhoneChanged = {},
            onOtpDigitChanged = { _, _ -> },
            onVerifyOtp = {},
        )
    }
}
