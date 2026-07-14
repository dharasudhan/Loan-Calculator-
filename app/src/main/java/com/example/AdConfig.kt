package com.example

import kotlinx.coroutines.flow.MutableStateFlow

object AdConfig {
    val canRequestAds = MutableStateFlow(false)
}
