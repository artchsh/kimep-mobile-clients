package kz.kimep.mobile.data

import android.content.Context
import kz.kimep.mobile.data.model.CalendarData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class CalendarRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var cached: CalendarData? = null

    suspend fun load(): CalendarData = cached ?: withContext(Dispatchers.IO) {
        val text = context.assets.open("calendar.json").bufferedReader().use { it.readText() }
        json.decodeFromString(CalendarData.serializer(), text).also { cached = it }
    }
}
