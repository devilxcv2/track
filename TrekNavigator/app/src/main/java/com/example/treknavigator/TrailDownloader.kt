// TrailDownloader.kt
/*
 * Scarica sentieri da Overpass API e memorizza in cache locale (Room).
 */
package com.example.treknavigator

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object TrailDownloader {
    private val client = OkHttpClient()

    suspend fun getPathsCached(bbox: LatLonBounds, context: android.content.Context): String = 
        withContext(Dispatchers.IO) {
        val dao = AppDatabase.getInstance(context).trailDao()
        val cached = dao.getAll()
        if (cached.isNotEmpty()) {
            // costruisci JSON mock da cache
            val json = JSONObject()
            val elements = cached.map { seg ->
                JSONObject().apply {
                    put("type", "way")
                    put("geometry", listOf(
                        mapOf("lat" to seg.startLat, "lon" to seg.startLon),
                        mapOf("lat" to seg.endLat, "lon" to seg.endLon)
                    ))
                }
            }
            json.put("elements", elements)
            json.toString()
        } else {
            val query = """
                [out:json];
                (
                  way["highway"~"path|footway|track"](${bbox.south},${bbox.west},${bbox.north},${bbox.east});
                );
                out body;
                >;
                out skel qt;
            """.trimIndent()
            val res = client.newCall(
                Request.Builder()
                    .url("https://overpass-api.de/api/interpreter")
                    .post(query.toRequestBody())
                    .build()
            ).execute()
            val body = res.body?.string() ?: ""
            // cache segments
            val elements = JSONObject(body).getJSONArray("elements")
            val list = mutableListOf<TrailEntity>()
            for (i in 0 until elements.length()) {
                val el = elements.getJSONObject(i)
                if (el.getString("type") == "way") {
                    val geom = el.getJSONArray("geometry")
                    for (j in 0 until geom.length()-1) {
                        val a = geom.getJSONObject(j)
                        val b = geom.getJSONObject(j+1)
                        list.add(TrailEntity(
                            startLat = a.getDouble("lat"),
                            startLon = a.getDouble("lon"),
                            endLat = b.getDouble("lat"),
                            endLon = b.getDouble("lon")
                        ))
                    }
                }
            }
            dao.insertAll(list)
            body
        }
    }
}
