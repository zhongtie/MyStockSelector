package com.example.mystockselector.ui.screener

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mystockselector.R
import com.example.mystockselector.ui.components.FeatureScreen

@Composable
fun ScreenerScreen(
    contentPadding: PaddingValues,
    viewModel: ScreenerViewModel = hiltViewModel(),
) {
    FeatureScreen(
        titleRes = R.string.nav_screener,
        messageRes = R.string.placeholder_screener,
        contentPadding = contentPadding,
    )
}
