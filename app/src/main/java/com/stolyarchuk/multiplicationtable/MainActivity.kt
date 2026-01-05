package com.stolyarchuk.multiplicationtable

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BorderAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stolyarchuk.multiplicationtable.ui.theme.MultiplicationTableTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

data class Stats(val correct: Int, val incorrect: Int, val avgTime: Float)
data class NumberStats(val correct: Map<Int, Int>, val incorrect: Map<Int, Int>)

class QuizStatsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("QuizStats", Context.MODE_PRIVATE)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    companion object {
        private const val DATES_SET = "dates_set"
    }

    private fun getKey(base: String, mode: String, scope: String) = "${scope}_${mode}_${base}"
    private fun getNumberKey(base: String, mode: String, scope: String, number: Int) = "${scope}_${mode}_${base}_$number"

    fun getStats(mode: String, scope: String): Stats {
        val correct = prefs.getInt(getKey("correct_answers", mode, scope), 0)
        val incorrect = prefs.getInt(getKey("incorrect_answers", mode, scope), 0)
        val totalTime = prefs.getLong(getKey("total_time_ms", mode, scope), 0L)
        val totalAnswers = prefs.getInt(getKey("total_answers", mode, scope), 0)
        val avgTime = if (totalAnswers > 0) totalTime.toFloat() / totalAnswers / 1000f else 0f
        return Stats(correct, incorrect, avgTime)
    }

    fun getNumberStats(mode: String, scope: String): NumberStats {
        val correctAnswers = mutableMapOf<Int, Int>()
        val incorrectAnswers = mutableMapOf<Int, Int>()
        for (i in 1..9) {
            correctAnswers[i] = prefs.getInt(getNumberKey("correct_answers_number", mode, scope, i), 0)
            incorrectAnswers[i] = prefs.getInt(getNumberKey("incorrect_answers_number", mode, scope, i), 0)
        }
        return NumberStats(correctAnswers, incorrectAnswers)
    }

    fun getDailyStatsForMode(mode: String): Map<String, Stats> {
        val dates = prefs.getStringSet(DATES_SET, emptySet()) ?: emptySet()
        return dates.associateWith { date -> getStats(mode, date) }.toSortedMap()
    }

    private fun _increment(scope: String, mode: String, isCorrect: Boolean, timeInMillis: Long, number1: Int, number2: Int) {
        val correctKey = getKey("correct_answers", mode, scope)
        val incorrectKey = getKey("incorrect_answers", mode, scope)
        val timeKey = getKey("total_time_ms", mode, scope)
        val answersKey = getKey("total_answers", mode, scope)

        if (isCorrect) {
            val currentCorrect = prefs.getInt(correctKey, 0)
            prefs.edit().putInt(correctKey, currentCorrect + 1).apply()
        } else {
            val currentIncorrect = prefs.getInt(incorrectKey, 0)
            prefs.edit().putInt(incorrectKey, currentIncorrect + 1).apply()
        }

        val totalTime = prefs.getLong(timeKey, 0L)
        val totalAnswers = prefs.getInt(answersKey, 0)
        prefs.edit()
            .putLong(timeKey, totalTime + timeInMillis)
            .putInt(answersKey, totalAnswers + 1)
            .apply()

        // Increment for each number
        val baseKey = if (isCorrect) "correct_answers_number" else "incorrect_answers_number"
        val n1key = getNumberKey(baseKey, "total", scope, number1)
        val n1Count = prefs.getInt(n1key, 0)
        prefs.edit().putInt(n1key, n1Count + 1).apply()

        if (number1 != number2) {
            val n2key = getNumberKey(baseKey, "total", scope, number2)
            val n2Count = prefs.getInt(n2key, 0)
            prefs.edit().putInt(n2key, n2Count + 1).apply()
        }
    }

    private fun addDate(date: String) {
        val dates = prefs.getStringSet(DATES_SET, mutableSetOf()) ?: mutableSetOf()
        if (date !in dates) {
            val newDates = dates.toMutableSet()
            newDates.add(date)
            prefs.edit().putStringSet(DATES_SET, newDates).apply()
        }
    }

    fun incrementCorrect(mode: String, timeInMillis: Long, number1: Int, number2: Int) {
        val today = dateFormat.format(Date())
        addDate(today)
        _increment(today, mode, isCorrect = true, timeInMillis, number1, number2)
        _increment("global", mode, isCorrect = true, timeInMillis, number1, number2)
    }

    fun incrementIncorrect(mode: String, timeInMillis: Long, number1: Int, number2: Int) {
        val today = dateFormat.format(Date())
        addDate(today)
        _increment(today, mode, isCorrect = false, timeInMillis, number1, number2)
        _increment("global", mode, isCorrect = false, timeInMillis, number1, number2)
    }

    fun resetScores() {
        prefs.edit().clear().apply()
    }
}

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
    val context = LocalContext.current
    val statsManager = remember { QuizStatsManager(context) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            it.icon,
                            contentDescription = stringResource(it.label)
                        )
                    },
                    label = { Text(stringResource(it.label)) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            when (currentDestination) {
                AppDestinations.TABLE -> MultiplicationTableScreen(modifier = Modifier.padding(innerPadding))
                AppDestinations.QUIZ -> QuizScreen(
                    modifier = Modifier.padding(innerPadding),
                    onNavigateToTable = { currentDestination = AppDestinations.TABLE },
                    onNavigateToStats = { currentDestination = AppDestinations.STATISTICS },
                    statsManager = statsManager
                )
                AppDestinations.STATISTICS -> StatisticsScreen(
                    modifier = Modifier.padding(innerPadding),
                    statsManager = statsManager
                )
            }
        }
    }
}

enum class AppDestinations(
    val label: Int,
    val icon: ImageVector,
) {
    TABLE(R.string.table, Icons.Default.BorderAll),
    QUIZ(R.string.quiz, Icons.Default.Quiz),
    STATISTICS(R.string.statistics, Icons.Default.Equalizer),
}

@Composable
fun MultiplicationTableScreen(modifier: Modifier = Modifier) {
    var selectedRow by rememberSaveable { mutableStateOf<Int?>(null) }
    var selectedCol by rememberSaveable { mutableStateOf<Int?>(null) }
    val headerColor = Color.Gray
    val highlightColor = Color.Yellow.copy(alpha = 0.4f)
    val intersectionColor = Color.Green.copy(alpha = 0.4f)

    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = stringResource(id = R.string.multiplication_table_title),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 16.dp)
        )

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

        if (selectedRow != null && selectedCol != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "$selectedRow",
                    fontSize = 48.sp,
                    color = Color.Blue,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.multiply),
                    tint = Color.Blue,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$selectedCol",
                    fontSize = 48.sp,
                    color = Color.Blue,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.equals_sign) + (selectedRow!! * selectedCol!!),
                    color = Color.Green,
                    fontSize = 48.sp
                )
            }
        } else {
            Spacer(modifier = Modifier.height(32.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.click_the_table_cell),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun QuizScreen(
    modifier: Modifier = Modifier,
    onNavigateToTable: () -> Unit,
    onNavigateToStats: () -> Unit,
    statsManager: QuizStatsManager
) {
    var number1 by rememberSaveable { mutableStateOf(Random.nextInt(1, 10)) }
    var number2 by rememberSaveable { mutableStateOf(Random.nextInt(1, 10)) }
    var userAnswer by rememberSaveable { mutableStateOf("") }
    var resultState by rememberSaveable { mutableStateOf<Boolean?>(null) }
    val focusRequester = remember { FocusRequester() }
    var sessionCorrectAnswers by rememberSaveable { mutableStateOf(0) }
    var sessionIncorrectAnswers by rememberSaveable { mutableStateOf(0) }
    var sessionTotalTime by rememberSaveable { mutableStateOf(0L) }
    var sessionAnswerCount by rememberSaveable { mutableStateOf(0) }
    var questionStartTime by remember { mutableStateOf(System.currentTimeMillis()) }

    var isSelectionMode by rememberSaveable { mutableStateOf(false) }
    var answerOptions by remember { mutableStateOf<List<Int>>(emptyList()) }
    var selectionResults by remember { mutableStateOf<Map<Int, Boolean>>(emptyMap()) }
    var firstAttemptMade by remember { mutableStateOf(false) }

    val correctAnswer = number1 * number2

    fun newQuestion() {
        number1 = Random.nextInt(1, 10)
        number2 = Random.nextInt(1, 10)
        userAnswer = ""
        resultState = null
        selectionResults = emptyMap()
        firstAttemptMade = false
        questionStartTime = System.currentTimeMillis()
    }

    fun checkAnswer() {
        val timeTaken = System.currentTimeMillis() - questionStartTime
        val isCorrect = userAnswer.toIntOrNull() == correctAnswer
        resultState = isCorrect
        sessionTotalTime += timeTaken
        sessionAnswerCount++
        if (isCorrect) {
            statsManager.incrementCorrect("input", timeTaken, number1, number2)
            sessionCorrectAnswers++
        } else {
            statsManager.incrementIncorrect("input", timeTaken, number1, number2)
            sessionIncorrectAnswers++
        }
    }

    LaunchedEffect(correctAnswer) {
        val options = mutableSetOf(correctAnswer)
        while (options.size < 4) {
            val randomAnswer = Random.nextInt(2, 100)
            if (randomAnswer != correctAnswer) {
                options.add(randomAnswer)
            }
        }
        answerOptions = options.toList().shuffled()
    }

    if (resultState == true) {
        LaunchedEffect(resultState) {
            kotlinx.coroutines.delay(1000)
            newQuestion()
        }
    }

    LaunchedEffect(number1, number2, isSelectionMode) {
        if (!isSelectionMode) {
            focusRequester.requestFocus()
        }
        questionStartTime = System.currentTimeMillis()
    }

    Box(modifier = modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateToTable, modifier = Modifier.border(2.dp, Color.Gray, CircleShape)) {
                    Icon(Icons.Default.BorderAll, contentDescription = stringResource(R.string.back_to_table))
                }
                Row {
                    Text(text = stringResource(R.string.correct), color = Color.Green, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(text = "$sessionCorrectAnswers", color = Color.Green, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Row {
                    Text(text = stringResource(R.string.wrong), color = Color.Red, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(text = "$sessionIncorrectAnswers", color = Color.Red, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onNavigateToStats, modifier = Modifier.border(2.dp, Color.Gray, CircleShape)) {
                    Icon(Icons.Default.Equalizer, contentDescription = stringResource(R.string.go_to_statistics))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            val avgTime = if (sessionAnswerCount > 0) sessionTotalTime.toFloat() / sessionAnswerCount / 1000f else 0f
            Text(stringResource(R.string.average_answer_time_formatted, avgTime), color = Color.Blue, fontSize = 20.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = { isSelectionMode = !isSelectionMode },
                modifier = Modifier.border(2.dp, Color.Gray, ButtonDefaults.shape)
            ) {
                Text(if (isSelectionMode) stringResource(R.string.switch_to_input_mode) else stringResource(R.string.switch_to_selection_mode))
            }
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isSelectionMode) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(text = "$number1", fontSize = 48.sp, color = Color.Green, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.multiply), tint = Color.Green, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "$number2", fontSize = 48.sp, color = Color.Green, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource(R.string.equals_question), fontSize = 32.sp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        answerOptions.chunked(2).forEach { rowOptions ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                rowOptions.forEach { answer ->
                                    val isCorrect = answer == correctAnswer
                                    val buttonColors = when (selectionResults[answer]) {
                                        true -> ButtonDefaults.buttonColors(containerColor = Color.Green)
                                        false -> ButtonDefaults.buttonColors(containerColor = Color.Red)
                                        null -> ButtonDefaults.buttonColors()
                                    }

                                    Button(
                                        onClick = {
                                            if (!firstAttemptMade) {
                                                val timeTaken = System.currentTimeMillis() - questionStartTime
                                                sessionTotalTime += timeTaken
                                                sessionAnswerCount++
                                                if (isCorrect) {
                                                    statsManager.incrementCorrect("selection", timeTaken, number1, number2)
                                                    sessionCorrectAnswers++
                                                } else {
                                                    statsManager.incrementIncorrect("selection", timeTaken, number1, number2)
                                                    sessionIncorrectAnswers++
                                                }
                                                firstAttemptMade = true
                                            }
                                            selectionResults = selectionResults + (answer to isCorrect)
                                            if (isCorrect) {
                                                resultState = true // Trigger new question
                                            }
                                        },
                                        colors = buttonColors,
                                        modifier = Modifier.widthIn(min = 100.dp)
                                    ) {
                                        Text("$answer", fontSize = 24.sp)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "$number1", fontSize = 48.sp, color = Color.Green, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.multiply), tint = Color.Green, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "$number2", fontSize = 48.sp, color = Color.Green, fontWeight = FontWeight.Bold)
                        Text(text = stringResource(R.string.equals_sign), fontSize = 32.sp)
                        TextField(
                            value = userAnswer,
                            onValueChange = { userAnswer = it.filter { char -> char.isDigit() } },
                            isError = resultState == false,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { checkAnswer() }),
                            modifier = Modifier
                                .width(100.dp)
                                .focusRequester(focusRequester),
                            textStyle = TextStyle(fontSize = 32.sp, textAlign = TextAlign.Center),
                            colors = if (resultState == true) TextFieldDefaults.colors(focusedContainerColor = Color.Green.copy(alpha = 0.2f), unfocusedContainerColor = Color.Green.copy(alpha = 0.2f)) else TextFieldDefaults.colors()
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = { checkAnswer() }) {
                            Text(stringResource(R.string.check))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    if (resultState != null) {
                        val color = if (resultState == true) Color.Green else Color.Red
                        Box(modifier = Modifier
                            .size(50.dp)
                            .background(color, CircleShape))
                    }
                }
            }
        }
    }
}

@Composable
fun StatisticsScreen(modifier: Modifier = Modifier, statsManager: QuizStatsManager) {
    var showDailyGraph by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableStateOf(0) }
    var showResetDialog by remember { mutableStateOf(false) }

    val inputGlobalStats = remember(refreshKey) { statsManager.getStats("input", "global") }
    val selectionGlobalStats = remember(refreshKey) { statsManager.getStats("selection", "global") }
    val globalNumberStats = remember(refreshKey) { statsManager.getNumberStats("total", "global") }

    val dailyInputStats = remember(refreshKey) { statsManager.getDailyStatsForMode("input") }
    val dailySelectionStats = remember(refreshKey) { statsManager.getDailyStatsForMode("selection") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.statistics), fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Row {
                IconButton(onClick = { showDailyGraph = !showDailyGraph }) {
                    Icon(Icons.Default.Equalizer, contentDescription = stringResource(R.string.toggle_view))
                }
                IconButton(onClick = { showResetDialog = true }) {
                    Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.reset_stats))
                }
            }
        }

        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text(text = stringResource(R.string.confirm_reset)) },
                text = { Text(stringResource(R.string.are_you_sure_reset)) },
                confirmButton = {
                    Button(
                        onClick = {
                            statsManager.resetScores()
                            refreshKey++
                            showResetDialog = false
                        }
                    ) {
                        Text(stringResource(R.string.yes))
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { showResetDialog = false }
                    ) {
                        Text(stringResource(R.string.no))
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (showDailyGraph) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.daily_input_mode), fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                CombinedStatsBarChart(
                    title = stringResource(R.string.correct_vs_wrong_answers),
                    data = dailyInputStats
                )
                StatsBarChart(title = stringResource(R.string.average_answer_time_sec), data = dailyInputStats.mapValues { it.value.avgTime }, color = Color.Blue)

                Spacer(modifier = Modifier.height(16.dp))

                Text(stringResource(R.string.daily_selection_mode), fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                CombinedStatsBarChart(
                    title = stringResource(R.string.correct_vs_wrong_answers),
                    data = dailySelectionStats
                )
                StatsBarChart(title = stringResource(R.string.average_answer_time_sec), data = dailySelectionStats.mapValues { it.value.avgTime }, color = Color.Blue)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.input_mode), fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    StatsTable(stats = inputGlobalStats)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.selection_mode), fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    StatsTable(stats = selectionGlobalStats)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.answers_per_number), fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    NumberStatsTable(stats = globalNumberStats)
                }
            }
        }
    }
}

@Composable
fun StatsTable(stats: Stats) {
    Column(modifier = Modifier
        .border(1.dp, Color.Gray)
        .padding(8.dp)
        .fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.correct_answers))
            Text("${stats.correct}", color = Color.Green)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.wrong_answers))
            Text("${stats.incorrect}", color = Color.Red)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.average_answer_time_label))
            Text(stringResource(R.string.seconds_format, stats.avgTime), color = Color.Blue)
        }
    }
}

@Composable
fun NumberStatsTable(stats: NumberStats) {
    Column(modifier = Modifier
        .border(1.dp, Color.Gray)
        .padding(8.dp)
        .fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text("Number", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.correct_percentage), modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
        for (i in 1..9) {
            val correct = stats.correct[i] ?: 0
            val incorrect = stats.incorrect[i] ?: 0
            val total = correct + incorrect
            val percentage = if (total > 0) (correct.toFloat() / total) * 100 else 0f
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("$i", modifier = Modifier.weight(1f))

                Text(stringResource(R.string.percentage_format, percentage), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun CombinedStatsBarChart(title: String, data: Map<String, Stats>) {
    val allDates = data.keys.sorted()
    if (allDates.isEmpty()) {
        Text(stringResource(R.string.no_data_yet, title))
        return
    }

    val maxCorrect = data.values.maxOfOrNull { it.correct }?.toFloat() ?: 0f
    val maxWrong = data.values.maxOfOrNull { it.incorrect }?.toFloat() ?: 0f
    val maxValue = maxOf(maxCorrect, maxWrong).takeIf { it > 0f } ?: 1f
    val barChartHeight = 150.dp

    Column(modifier = Modifier
        .border(1.dp, Color.Gray)
        .padding(8.dp)
        .fillMaxWidth()) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(barChartHeight + 30.dp), // Total height for bars and labels
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            allDates.forEach { date ->
                val stats = data[date]!!
                val correctValue = stats.correct.toFloat()
                val wrongValue = stats.incorrect.toFloat()

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.height(barChartHeight)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                            Text("%.0f".format(correctValue), fontSize = 10.sp)
                            Box(
                                modifier = Modifier
                                    .width(15.dp)
                                    .fillMaxHeight(correctValue / maxValue)
                                    .background(Color.Green)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                            Text("%.0f".format(wrongValue), fontSize = 10.sp)
                            Box(
                                modifier = Modifier
                                    .width(15.dp)
                                    .fillMaxHeight(wrongValue / maxValue)
                                    .background(Color.Red)
                            )
                        }
                    }
                    Text(date.substring(5).replace("-", "/"), fontSize = 12.sp)
                }
            }
        }
    }
}


@Composable
fun StatsBarChart(title: String, data: Map<String, Float>, color: Color) {
    if (data.isEmpty()) {
        Text(stringResource(R.string.no_data_yet, title))
        return
    }

    val maxValue = data.values.maxOrNull() ?: 1f
    val barChartHeight = 150.dp

    Column(modifier = Modifier
        .border(1.dp, Color.Gray)
        .padding(8.dp)
        .fillMaxWidth()) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(barChartHeight + 30.dp), // Total height for bars and labels
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            data.keys.sorted().forEach { date ->
                val value = data[date] ?: 0f
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.height(barChartHeight)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                            Text(stringResource(R.string.seconds_format, value), fontSize = 12.sp)
                            Box(
                                modifier = Modifier
                                    .width(15.dp)
                                    .fillMaxHeight(value / maxValue)
                                    .background(color)
                            )
                        }
                    }
                    Text(date.substring(5).replace("-", "/"), fontSize = 12.sp)
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

@Preview(showBackground = true)
@Composable
fun QuizScreenPreview() {
    MultiplicationTableTheme {
        QuizScreen(onNavigateToTable = {}, onNavigateToStats = {}, statsManager = QuizStatsManager(LocalContext.current))
    }
}

@Preview(showBackground = true)
@Composable
fun StatisticsScreenPreview() {
    MultiplicationTableTheme {
        StatisticsScreen(statsManager = QuizStatsManager(LocalContext.current))
    }
}
