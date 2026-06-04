package com.example.mystockselector.ui.import_

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mystockselector.R
import com.example.mystockselector.ui.components.FeatureScreen

@Composable
fun ImportScreen(
    contentPadding: PaddingValues,
    @Suppress("UNUSED_PARAMETER") viewModel: ImportViewModel = hiltViewModel(),
) {
    FeatureScreen(
        titleRes = R.string.nav_import,
        messageRes = R.string.placeholder_import,
        contentPadding = contentPadding,
    )
}
