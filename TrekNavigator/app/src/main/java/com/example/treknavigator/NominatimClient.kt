// NominatimClient.kt
/*
 * Richiede coordinate e bounding box da un nome località usando Nominatim.
 */
package com.example.treknavigator

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray

object NominatimClient {
    suspend fun getCoordinatesFromName(query: String): LatLon? = withContext(Dispatchers.IO) {
        val url = "https://nominatim.openstreetmap.org/search?q=$query&format=json&limit=1"
        val res = OkHttpClient().newCall(Request.Builder().url(url).build()).execute()
        val json = JSONArray(res.body?.string() ?: return@withContext null)
        if (json.length() == 0) return@withContext null
        val obj = json.getJSONObject(0)
        LatLon(obj.getDouble("lat"), obj.getDouble("lon"))
    }
}

data class LatLon(val lat: Double, val lon: Double)
