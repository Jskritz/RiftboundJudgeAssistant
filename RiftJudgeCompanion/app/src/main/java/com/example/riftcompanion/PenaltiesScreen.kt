package com.example.riftcompanion

import android.content.Context
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

data class PenaltyCategory(
    val id: String,
    val title: String,
    val sections: List<PenaltySection>
)

data class PenaltySection(
    val id: String,
    val title: String,
    val penalty: String?,
    val description: String?,
    val items: List<PenaltyItem>
)

data class PenaltyItem(
    val id: String,
    val text: String
)

sealed interface PenaltyDisplayItem {
    val id: String
}

data class CategoryHeaderItem(val category: PenaltyCategory) : PenaltyDisplayItem {
    override val id: String = category.id
}

data class SectionItem(val section: PenaltySection) : PenaltyDisplayItem {
    override val id: String = section.id
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PenaltiesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val listState = rememberLazyListState()

    // Load the data
    val categories = remember { loadPenaltiesFromAssets(context) }

    // Flatten data for scrolling
    val flatItems = remember(categories) {
        val list = mutableListOf<PenaltyDisplayItem>()
        categories.forEach { cat ->
            list.add(CategoryHeaderItem(cat))
            cat.sections.forEach { sec ->
                list.add(SectionItem(sec))
            }
        }
        list
    }

    // State for Drawer Expansion
    val expandedSections = remember { mutableStateMapOf<String, Boolean>() }

    fun scrollToId(id: String) {
        val index = flatItems.indexOfFirst { it.id == id }
        if (index >= 0) {
            scope.launch {
                drawerState.close()
                // Scroll with a small offset if possible, or just snap
                listState.scrollToItem(index)
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    "Index",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleMedium
                )
                HorizontalDivider()
                LazyColumn {
                    items(categories) { category ->
                        val isExpanded = expandedSections[category.id] == true

                        NavigationDrawerItem(
                            label = {
                                Text(
                                    text = "${category.id} ${category.title}",
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            selected = false,
                            onClick = {
                                if (isExpanded) expandedSections.remove(category.id)
                                else expandedSections[category.id] = true
                                // If I want to only scroll when subitems are clicked, I should do nothing here
                                // or only scroll if there are no subsections?
                                // User request: "it should only go-to when it is one of the sub items"
                            },
                            badge = {
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (isExpanded) "Collapse" else "Expand"
                                )
                            }
                        )

                        if (isExpanded) {
                            category.sections.forEach { section ->
                                NavigationDrawerItem(
                                    label = {
                                        Text(
                                            text = "${section.id} ${section.title}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    selected = false,
                                    modifier = Modifier.padding(start = 16.dp),
                                    onClick = {
                                        scrollToId(section.id)
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
                CenterAlignedTopAppBar(
                    title = { Text("Penalties Reference") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Index")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        ) { innerPadding ->
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp), // applied horizontal padding here
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(flatItems) { item ->
                    when (item) {
                        is CategoryHeaderItem -> {
                            PenaltyCategoryHeaderView(item.category)
                        }
                        is SectionItem -> {
                            PenaltySectionView(item.section)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PenaltyCategoryHeaderView(category: PenaltyCategory) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text(
            text = "${category.id} ${category.title}",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        HorizontalDivider(color = Color.Gray.copy(alpha = 0.5f))
    }
}

@Composable
fun PenaltySectionView(section: PenaltySection) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Header Row: Number + Title
            Text(
                text = "${section.id} ${section.title}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )

            if (!section.penalty.isNullOrBlank()) {
                val penaltyColor = getPenaltyColor(section.penalty)
                Text(
                    text = "Penalty: ${section.penalty}",
                    style = MaterialTheme.typography.labelLarge,
                    color = penaltyColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            // Description - Removed as per request
            // if (!section.description.isNullOrBlank()) { ... }

            // Bullet Points (Items)
            section.items.forEach { item ->
                Row(modifier = Modifier.padding(start = 16.dp, top = 2.dp)) {
                    Text(
                        text = "• ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = item.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

fun getPenaltyColor(penalty: String): Color {
    return when {
        penalty.contains("No Penalty", ignoreCase = true) -> Color.Green
        penalty.contains("Warning", ignoreCase = true) -> Color(0xFFFFC107) // Amber/Yellow
        penalty.contains("Game Loss", ignoreCase = true) -> Color.Red
        penalty.contains("Disqualification", ignoreCase = true) -> Color.Red
        penalty.contains("Match Loss", ignoreCase = true) -> Color.Red
        else -> Color.Gray
    }
}

fun loadPenaltiesFromAssets(context: Context): List<PenaltyCategory> {
    val fileName = "penalties.json"
    val result = mutableListOf<PenaltyCategory>()
    try {
        val jsonString = context.assets.open(fileName).use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).readText()
        }
        val rootObj = JSONObject(jsonString)

        // Sort keys to maintain order (e.g. 702)
        val categoryKeys = rootObj.keys().asSequence().sortedWith { a, b ->
             compareVersionStrings(a, b)
        }.toList()

        for (catKey in categoryKeys) {
            val catObj = rootObj.getJSONObject(catKey)
            val title = catObj.optString("title", "")
            val sectionsObj = catObj.optJSONObject("sections")

            val sectionList = mutableListOf<PenaltySection>()

            if (sectionsObj != null) {
                val sectionKeys = sectionsObj.keys().asSequence().sortedWith { a, b ->
                     compareVersionStrings(a, b)
                }.toList()
                
                for (secKey in sectionKeys) {
                    if (secKey.endsWith(".1")) continue

                    val secObj = sectionsObj.getJSONObject(secKey)
                    val secTitle = secObj.optString("title", "")
                    val penalty = if (secObj.isNull("penalty")) null else secObj.getString("penalty")
                    val description = if (secObj.isNull("description")) null else secObj.getString("description")

                    val itemsJsonArray = secObj.optJSONArray("items")
                    val itemList = mutableListOf<PenaltyItem>()

                    if (itemsJsonArray != null) {
                        for (i in 0 until itemsJsonArray.length()) {
                            val itemObj = itemsJsonArray.getJSONObject(i)
                            val itemId = itemObj.optString("id", "")
                            val itemText = itemObj.optString("text", "")
                            itemList.add(PenaltyItem(itemId, itemText))
                        }
                    }

                    sectionList.add(PenaltySection(secKey, secTitle, penalty, description, itemList))
                }
            }

            result.add(PenaltyCategory(catKey, title, sectionList))
        }
    } catch (e: Exception) {
        Log.e("PenaltiesScreen", "Error loading penalties.json", e)
    }
    return result
}

fun compareVersionStrings(v1: String, v2: String): Int {
    val partsA = v1.split('.').map { it.toIntOrNull() ?: 0 }
    val partsB = v2.split('.').map { it.toIntOrNull() ?: 0 }
    val length = maxOf(partsA.size, partsB.size)
    for (i in 0 until length) {
        val pA = partsA.getOrElse(i) { 0 }
        val pB = partsB.getOrElse(i) { 0 }
        if (pA != pB) {
            return pA.compareTo(pB)
        }
    }
    return 0
}
