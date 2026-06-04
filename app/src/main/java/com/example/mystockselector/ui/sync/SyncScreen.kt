package com.example.mystockselector.ui.sync

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mystockselector.R
import com.example.mystockselector.ui.components.FeatureScreen

@Composable
fun SyncScreen(
    contentPadding: PaddingValues,
    viewModel: SyncViewModel = hiltViewModel(),
) {
    FeatureScreen(
        titleRes = R.string.nav_sync,
        messageRes = R.string.placeholder_sync,
        contentPadding = contentPadding,
    )
}
