package com.stolyarchuk.multiplicationtable

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.CalendarViewDay
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import com.stolyarchuk.multiplicationtable.ui.theme.MultiplicationTableTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MultiplicationTableTheme {
                MultiplicationTableApp()
            }
        }
    }
}

@PreviewScreenSizes
@Composable
fun MultiplicationTableApp() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.TABLE) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            it.icon,
                            contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            if (currentDestination == AppDestinations.TABLE) {
                MultiplicationTableScreen(modifier = Modifier.padding(innerPadding))
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    TABLE("Table", Icons.Default.CalendarViewDay),
    FAVORITES("Favorites", Icons.Default.Favorite),
    PROFILE("Profile", Icons.Default.AccountBox),
}

@Composable
fun MultiplicationTableScreen(modifier: Modifier = Modifier) {
    var selectedRow by rememberSaveable { mutableStateOf<Int?>(null) }
    var selectedCol by rememberSaveable { mutableStateOf<Int?>(null) }
    val headerColor = Color(0xFFEEEEEE)
    val highlightColor = Color.Yellow.copy(alpha = 0.4f)
    val intersectionColor = Color.Green.copy(alpha = 0.4f)

    Column(modifier = modifier.padding(16.dp)) {
        // Header row for columns
        Row {
            Text(
                text = " ", modifier = Modifier
                    .weight(1f)
                    .background(headerColor)
                    .border(1.dp, Color.Gray)
                    .padding(4.dp)
                    .clickable {
                        selectedRow = null
                        selectedCol = null
                    }
            ) // Empty top-left cell
            for (j in 1..9) {
                val background = if (j == selectedCol) highlightColor else headerColor
                Text(
                    text = "$j",
                    modifier = Modifier
                        .weight(1f)
                        .background(background)
                        .border(1.dp, Color.Gray)
                        .padding(4.dp)
                        .clickable {
                            if (selectedCol == j) {
                                if (selectedRow != null) {
                                    selectedRow = null // collapse cell to col
                                } else {
                                    selectedCol = null // toggle col off
                                }
                            } else {
                                selectedCol = j // select new col
                            }
                        },
                    textAlign = TextAlign.Center
                )
            }
        }

        for (i in 1..9) {
            Row {
                // Header cell for rows
                val background = if (i == selectedRow) highlightColor else headerColor
                Text(
                    text = "$i",
                    modifier = Modifier
                        .weight(1f)
                        .background(background)
                        .border(1.dp, Color.Gray)
                        .padding(4.dp)
                        .clickable {
                            if (selectedRow == i) {
                                if (selectedCol != null) {
                                    selectedCol = null // collapse cell to row
                                } else {
                                    selectedRow = null // toggle row off
                                }
                            } else {
                                selectedRow = i // select new row
                            }
                        },
                    textAlign = TextAlign.Center
                )
                for (j in 1..9) {
                    val isSelected = i == selectedRow && j == selectedCol
                    val isInHighlightedArea = i == selectedRow || j == selectedCol
                    val selectionActive = selectedRow != null || selectedCol != null

                    val cellBackground = when {
                        isSelected -> intersectionColor
                        isInHighlightedArea -> highlightColor
                        else -> Color.Transparent
                    }

                    val textColor = if (selectionActive && !isInHighlightedArea) {
                        Color.LightGray
                    } else {
                        Color.Unspecified
                    }

                    Text(
                        text = "${i * j}",
                        modifier = Modifier
                            .weight(1f)
                            .background(cellBackground)
                            .border(1.dp, Color.Gray)
                            .padding(4.dp)
                            .clickable {
                                if (isSelected) {
                                    selectedRow = null
                                    selectedCol = null
                                } else {
                                    selectedRow = i
                                    selectedCol = j
                                }
                            },
                        textAlign = TextAlign.Center,
                        color = textColor
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MultiplicationTablePreview() {
    MultiplicationTableTheme {
        MultiplicationTableScreen()
    }
}
