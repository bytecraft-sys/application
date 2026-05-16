package com.yourapp.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.application.ui.theme.ApplicationTheme
import kotlinx.coroutines.delay

@Composable
fun ValuePropsScreen(
    modifier: Modifier = Modifier,
) {
    val visibleItems = remember {
        mutableStateListOf(false, false, false)
    }

    LaunchedEffect(Unit) {
        VALUE_PROPS.forEachIndexed { index, _ ->
            delay(index * VALUE_PROP_STAGGER_MS)
            visibleItems[index] = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 32.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "A calmer way to think out loud",
            style = MaterialTheme.typography.headlineLarge,
        )
        Spacer(modifier = Modifier.height(32.dp))
        VALUE_PROPS.forEachIndexed { index, item ->
            AnimatedVisibility(
                visible = visibleItems[index],
                enter = valuePropEnterTransition(),
            ) {
                ValuePropItem(text = item)
            }
            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

@Composable
private fun ValuePropItem(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = CircleShape,
            color = Color(0xFF1DB954),
            modifier = Modifier.size(10.dp),
            content = {},
        )
        Text(
            text = text,
            modifier = Modifier.padding(start = 14.dp),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

private fun valuePropEnterTransition(): EnterTransition {
    return fadeIn() + slideInVertically(initialOffsetY = { 40 })
}

private val VALUE_PROPS = listOf(
    "Your thoughts, understood",
    "Remembers context across sessions",
    "Private and on-device",
)

private const val VALUE_PROP_STAGGER_MS = 400L

@Preview(showBackground = true)
@Composable
fun ValuePropsScreenPreview() {
    ApplicationTheme {
        ValuePropsScreen()
    }
}
