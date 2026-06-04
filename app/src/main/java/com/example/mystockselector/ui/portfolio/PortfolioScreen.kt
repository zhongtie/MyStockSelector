package com.example.mystockselector.ui.portfolio

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mystockselector.R
import com.example.mystockselector.ui.components.FeatureScreen

@Composable
fun PortfolioScreen(
    contentPadding: PaddingValues,
    viewModel: PortfolioViewModel = hiltViewModel(),
) {
    FeatureScreen(
        titleRes = R.string.nav_portfolio,
        messageRes = R.string.placeholder_portfolio,
        contentPadding = contentPadding,
    )
}
