package com.yourapp.ui.home

sealed class AuraState {
    data object Breathing : AuraState()

    data object Listening : AuraState()
}
