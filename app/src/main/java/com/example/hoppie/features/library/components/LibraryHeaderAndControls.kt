package com.example.hoppie.features.library.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hoppie.R
import com.example.hoppie.core.theme.HoppieFileSize
import com.example.hoppie.core.theme.HoppiePurple
import com.example.hoppie.core.theme.HoppieWordmark
import com.example.hoppie.data.repository.LibrarySortOption
import com.example.hoppie.features.library.LibraryCategoryFilter
import com.example.hoppie.features.library.LibraryViewMode

private val ActiveChipFill = Color(0xFF45429A)
private val ControlBorder = Color(0xFFDCE4F2)
private val SearchBarFill = Color(0xFFF3F6FC)

@Composable
internal fun LibraryBrandHeader(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.hoppie_head),
            contentDescription = null,
            modifier = Modifier.size(54.dp)
        )
        Text(
            text = "HOPPIE",
            color = HoppiePurple,
            style = HoppieWordmark,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun LibraryTitleSection(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Library",
            color = HoppiePurple,
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 28.sp, lineHeight = 34.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "Your received files",
            color = HoppieFileSize,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun LibrarySearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(CircleShape)
            .background(SearchBarFill, CircleShape)
            .border(1.dp, ControlBorder, CircleShape)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.ic_library_search),
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            if (query.isEmpty()) {
                Text(
                    text = "Search files...",
                    color = HoppieFileSize.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = HoppiePurple),
                cursorBrush = SolidColor(HoppiePurple),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (query.isNotEmpty()) {
            Text(
                text = "×",
                color = HoppieFileSize,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClickLabel = "Clear search") { onQueryChange("") }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
internal fun LibraryCategoryChips(
    selected: LibraryCategoryFilter,
    onSelect: (LibraryCategoryFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LibraryCategoryFilter.entries.forEach { category ->
            val isActive = category == selected
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isActive) ActiveChipFill else Color.White, CircleShape)
                    .border(
                        width = 1.dp,
                        color = if (isActive) ActiveChipFill else ControlBorder,
                        shape = CircleShape
                    )
                    .clickable(role = Role.Tab) { onSelect(category) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = category.label,
                    color = if (isActive) Color.White else HoppiePurple,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
internal fun LibrarySortAndLayoutRow(
    sortOption: LibrarySortOption,
    viewMode: LibraryViewMode,
    onSelectSort: (LibrarySortOption) -> Unit,
    onToggleViewMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(role = Role.Button, onClickLabel = "Change sort order") {
                        sortMenuExpanded = true
                    }
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_library_sort_down),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = sortOption.label,
                    color = HoppiePurple,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            DropdownMenu(
                expanded = sortMenuExpanded,
                onDismissRequest = { sortMenuExpanded = false },
                containerColor = Color.White
            ) {
                LibrarySortOption.entries.forEach { option ->
                    val prefix = if (option == sortOption) "✓ " else "   "
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "$prefix${option.label}",
                                color = HoppiePurple,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        onClick = {
                            sortMenuExpanded = false
                            onSelectSort(option)
                        }
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable(
                    role = Role.Button,
                    onClickLabel = if (viewMode == LibraryViewMode.List) "Switch to grid view" else "Switch to list view",
                    onClick = onToggleViewMode
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(
                    if (viewMode == LibraryViewMode.List) R.drawable.ic_library_grid else R.drawable.ic_library_list
                ),
                contentDescription = if (viewMode == LibraryViewMode.List) "Grid view" else "List view",
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
