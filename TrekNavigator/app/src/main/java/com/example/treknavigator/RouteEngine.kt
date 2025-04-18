// RouteEngine.kt
/*
 * Gestisce input partenza/arrivo, geocodifica, scarica sentieri, calcola percorso, dislivello.
 */
package com.example.treknavigator

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.math.*

object RouteEngine {

    suspend fun resolveInput(input: String): LatLon? {
        return if ("," in input) {
            val parts = input.split(",")
            LatLon(parts[0].trim().toDouble(), parts[1].trim().toDouble())
        } else {
            NominatimClient.getCoordinatesFromName(input)
        }
    }

    suspend fun getElevation(point: LatLon): Double = withContext(Dispatchers.IO) {
        val url = "https://api.open-elevation.com/api/v1/lookup?locations=\${point.lat},\${point.lon}"
        val res = OkHttpClient().newCall(Request.Builder().url(url).build()).execute()
        val body = res.body?.string() ?: return@withContext 0.0
        val json = JSONObject(body)
        json.getJSONArray("results").getJSONObject(0).getDouble("elevation")
    }

    suspend fun calculateRoute(start: LatLon, end: LatLon): RouteResult = withContext(Dispatchers.IO) {
        // Definisci bounding box leggermente più grande
        val bbox = LatLonBounds(
            south = min(start.lat, end.lat) - 0.01,
            north = max(start.lat, end.lat) + 0.01,
            west = min(start.lon, end.lon) - 0.01,
            east = max(start.lon, end.lon) + 0.01
        )
        // Scarica JSON da Overpass
        val jsonWays = TrailDownloader.getPaths(bbox)
        // Costruisci grafo di sentieri
        val elements = JSONObject(jsonWays).getJSONArray("elements")
        val edges = mutableMapOf<LatLon, MutableList<LatLon>>()
        for (i in 0 until elements.length()) {
            val el = elements.getJSONObject(i)
            if (el.getString("type") == "way") {
                val geom = el.getJSONArray("geometry")
                val pts = mutableListOf<LatLon>()
                for (j in 0 until geom.length()) {
                    val pt = geom.getJSONObject(j)
                    pts.add(LatLon(pt.getDouble("lat"), pt.getDouble("lon")))
                }
                for (j in 0 until pts.size - 1) {
                    val a = pts[j]; val b = pts[j + 1]
                    edges.getOrPut(a) { mutableListOf() }.add(b)
                    edges.getOrPut(b) { mutableListOf() }.add(a)
                }
            }
        }
        val graph = TrailGraph(edges)
        val path = graph.findShortestRoute(start, end)
        // Calcola distanza
        val totalDist = path.zipWithNext { a, b -> haversine(a, b) }.sum()
        // Calcola dislivello
        val elevStart = getElevation(start)
        val elevEnd = getElevation(end)
        RouteResult(path, totalDist, elevEnd - elevStart)
    }

    private fun haversine(a: LatLon, b: LatLon): Double {
        val R = 6371000.0
        val dLat = toRadians(b.lat - a.lat)
        val dLon = toRadians(b.lon - a.lon)
        val lat1 = toRadians(a.lat)
        val lat2 = toRadians(b.lat)
        val aVal = sin(dLat/2).pow(2.0) + sin(dLon/2).pow(2.0) * cos(lat1) * cos(lat2)
        return 2 * R * atan2(sqrt(aVal), sqrt(1 - aVal))
    }

}
