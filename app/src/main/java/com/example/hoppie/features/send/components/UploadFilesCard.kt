package com.example.hoppie.features.send.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.hoppie.R
import com.example.hoppie.core.navigation.LocalSuppressReopen
import com.example.hoppie.core.theme.HoppiePurple

@Composable
internal fun UploadFilesCard(
    enabled: Boolean,
    onFilesPicked: (List<Uri>) -> Unit,
    modifier: Modifier = Modifier
) {
    val suppressReopen = LocalSuppressReopen.current
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
        onResult = onFilesPicked
    )

    Surface(
        onClick = {
            suppressReopen(true)
            picker.launch(arrayOf("*/*"))
        },
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        contentColor = HoppiePurple,
        border = BorderStroke(2.dp, HoppiePurple)
    ) {
        Column(
            Modifier.heightIn(min = 176.dp).padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(9.dp, Alignment.CenterVertically)
        ) {
            Image(
                painterResource(R.drawable.upload_icon),
                contentDescription = null,
                modifier = Modifier.size(width = 90.dp, height = 60.dp),
                contentScale = ContentScale.Fit
            )
            Text("Upload Files", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Text(
                "Select files from your device\n(Max 50 MB per file)",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF247C73),
                textAlign = TextAlign.Center
            )
        }
    }
}
