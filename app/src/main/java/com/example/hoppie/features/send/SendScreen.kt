package com.example.hoppie.features.send

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hoppie.R
import com.example.hoppie.core.components.FileTransferBackground
import com.example.hoppie.core.theme.HoppiePurple
import com.example.hoppie.features.send.components.SendButton
import com.example.hoppie.features.send.components.SelectedFilesSummary
import com.example.hoppie.features.send.components.selectedFilesSection
import com.example.hoppie.features.send.components.UploadFilesCard

@Composable
internal fun SendScreen(onBack: () -> Unit, model: SendViewModel = viewModel()) {
    val state by model.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Surface(modifier = Modifier.fillMaxSize(), color = Color.White, contentColor = HoppiePurple) {
        FileTransferBackground {
            LazyColumn(
                modifier = Modifier.widthIn(max = 560.dp).fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
                verticalArrangement = SendContentArrangement
            ) {
                item(key = "header") { SendHeader(onBack) }
                item(key = "upload") {
                    UploadFilesCard(
                        enabled = !state.isCheckingFiles,
                        onFilesPicked = model::addFiles,
                        modifier = Modifier.padding(horizontal = 24.dp).padding(top = 28.dp, bottom = 24.dp)
                    )
                }
                selectedFilesSection(state, model::removeFile)
                item(key = "actions") {
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp).padding(top = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        SelectedFilesSummary(state.files.size, state.totalBytes)
                        SendButton(enabled = state.files.isNotEmpty()) {
                            Toast.makeText(
                                context,
                                "File transfer will be available once Nearby Connections is implemented.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SendHeader(onBack: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Send Files",
            modifier = Modifier.padding(horizontal = 52.dp, vertical = 6.dp).semantics { heading() },
            style = MaterialTheme.typography.headlineSmall,
            color = HoppiePurple,
            textAlign = TextAlign.Center
        )
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart).size(48.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.send_back_arrow),
                contentDescription = "Back",
                modifier = Modifier.size(26.dp),
                tint = HoppiePurple
            )
        }
    }
}

// The list only arranges items when they all fit. Otherwise the footer scrolls with the files.
private object SendContentArrangement : Arrangement.Vertical {
    override fun Density.arrange(totalSize: Int, sizes: IntArray, outPositions: IntArray) {
        with(Arrangement.Top) { arrange(totalSize, sizes, outPositions) }
        if (sizes.isNotEmpty()) {
            val last = sizes.lastIndex
            outPositions[last] = maxOf(outPositions[last], totalSize - sizes[last])
        }
    }
}
