package com.ipvio.stream

import android.content.Context

/** Loads the curated Black Cinema catalog from assets once (see [BlackCinemaLogic]). */
object BlackCinema {
    @Volatile private var cache: List<BlackCinemaLogic.Entry>? = null

    fun catalog(c: Context): List<BlackCinemaLogic.Entry> = cache ?: synchronized(this) {
        cache ?: runCatching {
            BlackCinemaLogic.parse(c.applicationContext.assets.open("black_cinema.json").bufferedReader(Charsets.UTF_8).use { it.readText() })
        }.getOrDefault(emptyList()).also { cache = it }
    }
}
