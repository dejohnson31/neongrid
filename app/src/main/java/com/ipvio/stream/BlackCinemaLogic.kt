package com.ipvio.stream

/**
 * Black Cinema collection (NEONGRID 0.2). Without Android, testable.
 *
 * A curated catalog (assets/black_cinema.json: title, year, categories) is matched against the movies of ALL loaded
 * playlists (the merged list from [MultiLogic]). Only titles that exist in the user's own sources become cards,
 * so every card in the collection is playable. Nothing is hosted or downloaded here.
 */
object BlackCinemaLogic {
    class Entry(val title: String, val year: Int, val cats: List<String>) {
        val decade: Int get() = year / 10 * 10
    }

    /** A catalog entry together with the playlist movie that plays it. */
    class Match(val entry: Entry, val item: Item)

    /** Display label of a decade: 1970 -> "1970s"; before 1970 -> "Classics". */
    fun decadeLabel(decade: Int): String = if (decade < 1970) "Classics" else "${decade}s"

    /** Parses the catalog JSON (see assets/black_cinema.json). Unknown or broken rows are skipped. */
    fun parse(json: String): List<Entry> {
        val out = ArrayList<Entry>()
        val arr = org.json.JSONObject(json).optJSONArray("movies") ?: return out
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val t = o.optString("t")
            val y = o.optInt("y", 0)
            if (t.isBlank() || y <= 0) continue
            val cs = o.optJSONArray("c")
            val cats = ArrayList<String>()
            if (cs != null) for (k in 0 until cs.length()) cs.optString(k).takeIf { it.isNotBlank() }?.let { cats.add(it) }
            out.add(Entry(t, y, cats))
        }
        return out
    }

    /**
     * Matches catalog entries with playlist movies: same normalized title (language tag such as "DE - " ignored),
     * year within +-1 (providers are often off by one). An item without a year only counts when it is the single candidate.
     * An exact year wins over +-1. Per entry the first hit in playlist order is used (the main playlist comes first).
     */
    fun match(catalog: List<Entry>, movies: List<Item>): List<Match> {
        val byTitle = HashMap<String, MutableList<Item>>()
        for (m in movies) {
            if (m.kind != Kind.MOVIE) continue
            val k = MatchLogic.norm(MatchLogic.searchTitle(m.title))
            if (k.isNotEmpty()) byTitle.getOrPut(k) { ArrayList() }.add(m)
        }
        val used = HashSet<String>()
        val out = ArrayList<Match>()
        for (e in catalog) {
            val c = byTitle[MatchLogic.norm(e.title)] ?: continue
            val pick = c.firstOrNull { it.year == e.year && it.id !in used }
                ?: c.firstOrNull { it.year != null && Math.abs(it.year - e.year) <= 1 && it.id !in used }
                ?: if (c.size == 1 && c[0].year == null && c[0].id !in used) c[0] else null
            if (pick != null) { used.add(pick.id); out.add(Match(e, pick)) }
        }
        return out
    }

    /** Rows by decade, newest decade last; titles inside a row sorted by year. */
    fun byDecade(matches: List<Match>): List<Pair<String, List<Match>>> =
        matches.groupBy { if (it.entry.decade < 1970) 1960 else it.entry.decade }
            .toSortedMap()
            .map { (d, l) -> decadeLabel(d) to l.sortedBy { it.entry.year } }

    /** Rows by category in the order of [order] (ids from the catalog); empty categories are dropped. */
    fun byCategory(matches: List<Match>, order: List<String>): List<Pair<String, List<Match>>> =
        order.mapNotNull { id ->
            val l = matches.filter { id in it.entry.cats }
            if (l.isEmpty()) null else id to l.sortedBy { it.entry.year }
        }
}
