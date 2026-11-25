package com.example.hoppie.features.transfers

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.hoppie.core.theme.HOPPIETheme

@Preview(showBackground = true, widthDp = 320, heightDp = 480)
@Composable
private fun TransfersPreview() {
    HOPPIETheme { TransfersContent(emptyList(), null, null) {} }
}
