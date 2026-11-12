package com.ipvio.stream

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * NEONGRID Lite mode (Fire TV Stick and other low-memory devices): instead of loading every movie and series,
 * the viewer picks one category; only that category is downloaded and kept in memory.
 */
@Composable
fun LiteBrowser() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var movieCats by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var seriesCats by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    LaunchedEffect(Unit) {
        movieCats = PlaylistStore.liteCats(ctx, true)
        seriesCats = PlaylistStore.liteCats(ctx, false)
    }
    Column(Modifier.fillMaxWidth().padding(start = 24.dp, top = 24.dp, end = 24.dp)) {
        Text("Lite mode: choose a category to load movies or series", color = Tok.TextHi, fontSize = 16.sp)
        val status = when {
            PlaylistStore.liteBusy -> "Loading category …"
            PlaylistStore.liteLabel != null -> "Loaded: ${PlaylistStore.liteLabel} (${PlaylistStore.liteVod.size} titles). Open Movies or Series on the left."
            else -> "This device has limited memory, so one category is loaded at a time."
        }
        Text(status, color = Tok.TextMid, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
        CatRow("Movies", movieCats) { id -> scope.launch { PlaylistStore.liteLoad(ctx, true, id, movieCats) } }
        CatRow("Series", seriesCats) { id -> scope.launch { PlaylistStore.liteLoad(ctx, false, id, seriesCats) } }
    }
}

@Composable
private fun CatRow(title: String, cats: Map<String, String>, onPick: (String) -> Unit) {
    if (cats.isEmpty()) return
    Text(title, color = Tok.TextMid, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp, bottom = 6.dp))
    val sorted = remember(cats) { cats.entries.sortedBy { it.value.lowercase() } }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(sorted, key = { it.key }) { e -> SmallPill(e.value) { onPick(e.key) } }
    }
}
