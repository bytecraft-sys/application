package com.yourapp.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.application.ui.theme.ApplicationTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TraitSelectionScreen(
    selectedTraits: Set<String>,
    onTraitClicked: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Choose exactly $REQUIRED_TRAIT_COUNT traits",
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(modifier = Modifier.height(18.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TRAITS.forEach { trait ->
                val selected = trait in selectedTraits
                SuggestionChip(
                    onClick = { onTraitClicked(trait) },
                    label = { Text(trait) },
                    colors = if (selected) {
                        SuggestionChipDefaults.suggestionChipColors(
                            containerColor = TealSelected,
                            labelColor = Color.White,
                        )
                    } else {
                        SuggestionChipDefaults.suggestionChipColors()
                    },
                )
            }
        }
    }
}

private val TRAITS = listOf(
    "Curious",
    "Calm",
    "Analytical",
    "Creative",
    "Empathetic",
    "Direct",
    "Humorous",
    "Introverted",
)

private val TealSelected = Color(0xFF00897B)

@Preview(showBackground = true)
@Composable
fun TraitSelectionScreenPreview() {
    ApplicationTheme {
        TraitSelectionScreen(
            selectedTraits = setOf("Curious", "Creative", "Direct"),
            onTraitClicked = {},
        )
    }
}
