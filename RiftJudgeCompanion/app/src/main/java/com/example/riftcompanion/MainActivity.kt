package com.example.riftcompanion

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.zIndex
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll



// --- Data Classes & Enums ---

enum class Screen {
    HOME, CORE_RULES, TOURNAMENT_RULES, REFERENCE_IMAGES
}

enum class RuleType {
    SECTION_HEADER, SUBSECTION_HEADER, RULE
}

data class FlatRuleItem(
    val id: String,
    val title: String,
    val text: String,
    val type: RuleType,
    val indentation: Int = 0
)

data class DrawerSection(
    val id: String,
    val title: String,
    val subsections: List<DrawerSubsection>
)

data class DrawerSubsection(
    val id: String,
    val title: String
)

// --- Main Activity ---

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Applied Yellow/Black Theme
            MaterialTheme(colorScheme = AppColorScheme) {
                AppNavigation()
            }
        }
    }
}

// --- Theme (Yellow/Black) ---
// --- Theme (Soft Yellow/Black Edition) ---
// --- Theme (Adjusted Yellow/Black Edition) ---
val AppColorScheme = darkColorScheme(
    // A darker, richer yellow (Amber 500) - less "bright/pale", easier to read on dark mode
    primary = Color(0xFFFFC107),
    // Black text ensures maximum readability
    onPrimary = Color(0xFF000000),

    // Secondary accent (Orange-ish)
    secondary = Color(0xFFFFD180),
    onSecondary = Color(0xFF000000),

    // Backgrounds (Dark Grey/Black)
    surface = Color(0xFF121212),
    onSurface = Color(0xFFE6E1E5),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE6E1E5)
)



// --- Navigation ---

@Composable
fun AppNavigation() {
    var currentScreen by remember { mutableStateOf(Screen.HOME) }

    // Handle system back button
    BackHandler(enabled = currentScreen != Screen.HOME) {
        currentScreen = Screen.HOME
    }

    when (currentScreen) {
        Screen.HOME -> HomeScreen(
            onCoreSelected = { currentScreen = Screen.CORE_RULES },
            onTournamentSelected = { currentScreen = Screen.TOURNAMENT_RULES },
            onImagesSelected = { currentScreen = Screen.REFERENCE_IMAGES }
        )
        Screen.CORE_RULES -> GenericRuleBookScreen(
            jsonFileName = "riftbound_core_rules_optionB.json",
            screenTitle = "Core Rules",
            onBack = { currentScreen = Screen.HOME }
        )
        Screen.TOURNAMENT_RULES -> GenericRuleBookScreen(
            jsonFileName = "tournament_rules.json",
            screenTitle = "Tournament Rules",
            onBack = { currentScreen = Screen.HOME }
        )
        Screen.REFERENCE_IMAGES -> ReferenceImagesScreen(
            onBack = { currentScreen = Screen.HOME }
        )
    }
}

// --- Screens ---
@Composable
fun HomeScreen(
    onCoreSelected: () -> Unit,
    onTournamentSelected: () -> Unit,
    onImagesSelected: () -> Unit
) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        // 1. Create the scroll state
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                // 2. Add the verticalScroll modifier here
                .verticalScroll(scrollState)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title for the Home Screen
            Text(
                text = "Riftbound Judge Companion",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 48.dp)
            )

            // Core Rules Button
            Button(
                onClick = onCoreSelected,
                modifier = Modifier.fillMaxWidth().height(80.dp),
                shape = CutCornerShape(topEnd = 24.dp, bottomStart = 24.dp),            ) {
                Text(
                    text = "Core Rules",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Tournament Rules Button
            Button(
                onClick = onTournamentSelected,
                modifier = Modifier.fillMaxWidth().height(80.dp),
                shape = CutCornerShape(topEnd = 24.dp, bottomStart = 24.dp),            ) {
                Text(
                    text = "Tournament Rules",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Reference Images Button
            Button(
                onClick = onImagesSelected,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ),
                modifier = Modifier.fillMaxWidth().height(80.dp),
                shape = CutCornerShape(topEnd = 24.dp, bottomStart = 24.dp),
            ) {
                Text(
                    text = "Reference Images",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferenceImagesScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Reference Images") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                // Ensure your file is named "setup_turn" in res/drawable
                ReferenceImageItem("Setup & Turn", R.drawable.setup_turn)
            }
            item {
                // Ensure your file is named "showdown" in res/drawable
                ReferenceImageItem("Showdown", R.drawable.showdown)
            }
        }
    }
}


@Composable
fun ReferenceImageItem(title: String, imageResId: Int) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = AppColorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            ZoomableImage(imageResId = imageResId, contentDescription = title)
        }
        Text(
            text = "Pinch to zoom, drag to pan",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
        )
    }
}

@Composable
fun ZoomableImage(imageResId: Int, contentDescription: String) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 3f)
                    if (scale > 1f) {
                        // Calculate bounds to prevent panning outside the zoomed image
                        val maxTranslateX = (size.width * (scale - 1)) / 2
                        val maxTranslateY = (size.height * (scale - 1)) / 2

                        offset = Offset(
                            x = (offset.x + pan.x).coerceIn(-maxTranslateX, maxTranslateX),
                            y = (offset.y + pan.y).coerceIn(-maxTranslateY, maxTranslateY)
                        )
                    } else {
                        // Reset position if zoomed out
                        offset = Offset.Zero
                    }
                }
            }
    ) {
        Image(
            painter = painterResource(id = imageResId),
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit, // Ensures the image fits within the card initially
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenericRuleBookScreen(
    jsonFileName: String,
    screenTitle: String,
    onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val listState = rememberLazyListState()

    // Load data
    val (sections, allRules) = remember(jsonFileName) {
        loadRulesFromAssets(context, jsonFileName)
    }

    // State for search
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    // State for target scroll ID
    var targetScrollId by remember { mutableStateOf<String?>(null) }

    // State for Drawer Expansion (Map of Section ID -> Boolean)
    val expandedSections = remember { mutableStateMapOf<String, Boolean>() }

    // Filter logic
    val displayedRules = remember(searchQuery, allRules) {
        if (searchQuery.isBlank()) allRules else allRules.filter {
            it.text.contains(searchQuery, ignoreCase = true) ||
                    it.title.contains(searchQuery, ignoreCase = true) ||
                    it.id.contains(searchQuery, true)
        }
    }
    var highlightedRuleId by remember { mutableStateOf<String?>(null) }


    // Effect to handle scrolling AFTER the list has updated to the full list
    LaunchedEffect(targetScrollId) {
        targetScrollId?.let { id ->
            // 1. Wait a tiny bit for the list to switch from "Filtered" to "Full" datakotlinx.coroutines.delay(50)

            // 2. Find index in the FULL list
            val index = allRules.indexOfFirst { it.id == id }

            if (index != -1) {
                val currentVisibleIndex = listState.firstVisibleItemIndex

                // 3. "Teleport" closer if we are too far away to avoid lag/bugs
                val jumpStep = 10
                if (kotlin.math.abs(index - currentVisibleIndex) > jumpStep) {
                    val snapIndex = if (index > currentVisibleIndex) index - jumpStep else index + jumpStep
                    listState.scrollToItem(snapIndex.coerceIn(0, allRules.size - 1))
                }

                // 4. Smoothly animate to the item with an OFFSET
                // A negative offset (-150) pushes the item DOWN, leaving space at the top
                // so it's not hidden behind the top gradient shade.
                listState.animateScrollToItem(index, -150)
            }
            targetScrollId = null
        }
    }

    // Helper to initiate the scroll process
    fun initiateScroll(ruleId: String) {
        searchQuery = ""
        isSearchActive = false
        targetScrollId = ruleId

        // Trigger the highlight
        highlightedRuleId = ruleId
        // Clear the highlight after 1 seconds so it fades out
        scope.launch {
            kotlinx.coroutines.delay(1000)
            highlightedRuleId = null
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    "Table of Contents",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleMedium
                )
                HorizontalDivider()
                LazyColumn {
                    items(sections) { section ->
                        val isExpanded = expandedSections[section.id] == true

                        NavigationDrawerItem(
                            label = {
                                Text(
                                    text = section.title,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            selected = false,
                            onClick = {
                                if (isExpanded) expandedSections.remove(section.id)
                                else expandedSections[section.id] = true
                            },
                            badge = {
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (isExpanded) "Collapse" else "Expand"
                                )
                            }
                        )

                        if (isExpanded) {
                            section.subsections.forEach { sub ->
                                NavigationDrawerItem(
                                    label = {
                                        Text(
                                            text = sub.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    selected = false,
                                    modifier = Modifier.padding(start = 16.dp),
                                    onClick = {
                                        scope.launch {
                                            drawerState.close()
                                            initiateScroll(sub.id)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                if (isSearchActive) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search rules...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding(), // <--- Add this line here
                        leadingIcon = { Icon(Icons.Default.Search, "Search") },
                        trailingIcon = {
                            IconButton(onClick = {
                                isSearchActive = false
                                searchQuery = ""
                            }) { Text("X") }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                } else {
                    CenterAlignedTopAppBar(
                        title = { Text(screenTitle) },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            }
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "TOC")
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(displayedRules) { rule ->
                        // Determine background color based on highlight state
                        val isHighlighted = rule.id == highlightedRuleId
                        val backgroundColor by animateColorAsState(
                            targetValue = if (isHighlighted)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            else
                                Color.Transparent,
                            animationSpec = tween(durationMillis = 500),
                            label = "HighlightAnimation"
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(backgroundColor)
                                .clickable(enabled = searchQuery.isNotBlank()) {
                                    initiateScroll(rule.id)
                                }
                        ) {
                            RuleItemView(
                                rule = rule,
                                onLinkClick = { linkId -> initiateScroll(linkId) },
                                isSearchActive = searchQuery.isNotBlank()
                            )
                        }
                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                    }
                }

                // --- Top Fade Shadow ---
                // Only show if we have scrolled down a bit
                if (listState.canScrollBackward) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .align(Alignment.TopCenter)
                            .zIndex(1f) // Ensure it's on top
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.background,
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }

                // --- Bottom Fade Shadow ---
                // Only show if we can scroll further down
                if (listState.canScrollForward) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .align(Alignment.BottomCenter)
                            .zIndex(1f) // Ensure it's on top
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.background
                                    )
                                )
                            )
                    )
                }
            }
        }
    }
}



@Composable
fun RuleItemView(
    rule: FlatRuleItem,
    onLinkClick: (String) -> Unit,
    isSearchActive: Boolean = false
) {
    // Determine if this is a major section header
    val isSectionHeader = rule.type == RuleType.SECTION_HEADER

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                // Less padding for headers so the background strip stretches
                start = if (isSectionHeader) 0.dp else (16 + (rule.indentation * 8)).dp,
                end = if (isSectionHeader) 0.dp else 16.dp,
                top = 4.dp,
                bottom = 4.dp
            )
    ) {
        val hasTitle = rule.title.isNotEmpty()
        val isSimpleRule = rule.type == RuleType.RULE && !hasTitle

        // --- VISUAL UPGRADE: Header Bar ---
        if (hasTitle) {
            val style = when (rule.type) {
                RuleType.SECTION_HEADER -> MaterialTheme.typography.headlineSmall
                RuleType.SUBSECTION_HEADER -> MaterialTheme.typography.titleMedium
                RuleType.RULE -> MaterialTheme.typography.titleSmall
            }

            // Yellow text for headers, white for others
            val textColor = if (isSectionHeader)
                MaterialTheme.colorScheme.onPrimary
            else
                MaterialTheme.colorScheme.primary

            // If it's a main section, wrap it in a colored Box (The "Bar")
            if (isSectionHeader) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.primary,
                            shape = CutCornerShape(topEnd = 16.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "${rule.id} ${rule.title}",
                        style = style,
                        color = textColor, // Black text on Yellow background
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Serif, // Official Rulebook feel
                        letterSpacing = 0.5.sp
                    )
                }
            } else {
                // Standard Title (Subsections)
                Text(
                    text = "${rule.id} ${rule.title}",
                    style = style,
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif
                )
            }
        }

        // --- Text Content ---
        if (rule.text.isNotEmpty()) {
            if (hasTitle) {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // For section headers, add padding to the intro text since we removed parent padding
            val textModifier = if (isSectionHeader) Modifier.padding(horizontal = 16.dp) else Modifier

            if (isSimpleRule) {
                Row(modifier = textModifier.fillMaxWidth()) {
                    Text(
                        text = "${rule.id} ",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.secondary, // Amber color for rule numbers
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace // Tactical number feel
                    )
                    if (isSearchActive) {
                        Text(
                            text = rule.text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        RuleTextWithLinks(
                            text = rule.text,
                            onLinkClick = onLinkClick
                        )
                    }
                }
            } else {
                Box(modifier = textModifier) {
                    if (isSearchActive) {
                        Text(
                            text = rule.text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        RuleTextWithLinks(
                            text = rule.text,
                            onLinkClick = onLinkClick
                        )
                    }
                }
            }
        }
    }
}




@Composable
fun RuleTextWithLinks(text: String, onLinkClick: (String) -> Unit) {
    val regex = Regex("\\b\\d{3}(?:\\.[\\w\\d]+)*\\b")

    val annotatedString = buildAnnotatedString {
        var lastIndex = 0
        val cleanText = text.replace("**", "")

        for (match in regex.findAll(cleanText)) {
            val startIndex = match.range.first
            val endIndex = match.range.last + 1

            append(cleanText.substring(lastIndex, startIndex))

            pushStringAnnotation(tag = "RULE", annotation = match.value)
            withStyle(
                style = SpanStyle(
                    // Links will now be Yellow
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline
                )
            ) {
                append(match.value)
            }
            pop()

            lastIndex = endIndex
        }
        append(cleanText.substring(lastIndex))
    }

    ClickableText(
        text = annotatedString,
        style = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onBackground
        ),
        onClick = { offset ->
            annotatedString.getStringAnnotations(tag = "RULE", start = offset, end = offset)
                .firstOrNull()?.let {
                    onLinkClick(it.item)
                }
        }
    )
}

// --- JSON Parsing Logic ---

// --- JSON Parsing Logic ---

fun loadRulesFromAssets(context: Context, fileName: String): Pair<List<DrawerSection>, List<FlatRuleItem>> {
    try {
        val jsonString = context.assets.open(fileName).use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).readText()
        }

        val rootObj = JSONObject(jsonString)
        if (!rootObj.has("sections")) return Pair(emptyList(), emptyList())

        val sectionsObj = rootObj.getJSONObject("sections")
        val drawerSections = mutableListOf<DrawerSection>()
        val flatRules = mutableListOf<FlatRuleItem>()

        // Sort keys to ensure 100 comes before 200, etc.
        val sectionKeys = sectionsObj.keys().asSequence().sorted().toList()

        for (sectionKey in sectionKeys) {
            val sectionObj = sectionsObj.getJSONObject(sectionKey)
            val sectionTitle = sectionObj.getString("title")
            val sectionIntro = sectionObj.optString("intro", "")

            // Level 1: Major Section (e.g., "100 General")
            // We prepend the ID (sectionKey) to the title for display in the Drawer/List
            val displayTitle = "$sectionKey $sectionTitle"

            flatRules.add(FlatRuleItem(sectionKey, sectionTitle, sectionIntro, RuleType.SECTION_HEADER, 0))

            val subsectionsList = mutableListOf<DrawerSubsection>()
            val subsectionsObj = sectionObj.optJSONObject("subsections")

            if (subsectionsObj != null) {
                val subKeys = subsectionsObj.keys().asSequence().sorted().toList()
                for (subKey in subKeys) {
                    val subObj = subsectionsObj.getJSONObject(subKey)
                    val subTitle = subObj.getString("title")
                    // Some Level 2s have "content" text directly under them
                    val subContent = subObj.optString("content", "")

                    // Level 2: Subsection (e.g., "101 Game Concepts")
                    val subDisplayTitle = "$subKey $subTitle"

                    subsectionsList.add(DrawerSubsection(subKey, subDisplayTitle))
                    flatRules.add(FlatRuleItem(subKey, subTitle, subContent, RuleType.SUBSECTION_HEADER, 1))

                    // Level 3: Sub-subsections (e.g., "101.1", "101.2")
                    val subSubObj = subObj.optJSONObject("subsubsections")
                    if (subSubObj != null) {
                        val ruleKeys = subSubObj.keys().asSequence().sorted().toList()
                        for (ruleKey in ruleKeys) {
                            val ruleObj = subSubObj.getJSONObject(ruleKey)
                            val ruleTitle = ruleObj.optString("title", "") // Often empty for rules
                            val ruleText = ruleObj.getString("text")

                            // Calculate indentation based on depth (dots count)
                            // 101.1 -> 1 dot -> indent 2
                            // 101.1.a -> 2 dots -> indent 3
                            val dots = ruleKey.count { it == '.' }

                            flatRules.add(FlatRuleItem(ruleKey, ruleTitle, ruleText, RuleType.RULE, dots + 1))
                        }
                    }
                }
            }
            // Add the section and its subsections to the Drawer list
            drawerSections.add(DrawerSection(sectionKey, displayTitle, subsectionsList))
        }

        return Pair(drawerSections, flatRules)

    } catch (e: Exception) {
        Log.e("RuleBook", "Error loading $fileName", e)
        return Pair(emptyList(), emptyList())
    }
}

