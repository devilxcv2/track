// TrailGraph.kt
/*
 * Costruisce un grafo da segmenti sentiero (mock) e calcola il percorso più breve con A*.
 */
package com.example.treknavigator

import kotlin.math.*

data class LatLon(val lat: Double, val lon: Double)

class TrailGraph(private val edges: Map<LatLon, List<LatLon>>) {

    fun findShortestRoute(start: LatLon, goal: LatLon): List<LatLon> {
        val openSet = mutableSetOf(start)
        val cameFrom = mutableMapOf<LatLon, LatLon>()
        val gScore = mutableMapOf(start to 0.0)
        val fScore = mutableMapOf(start to haversine(start, goal))

        while (openSet.isNotEmpty()) {
            val current = openSet.minByOrNull { fScore[it] ?: Double.MAX_VALUE } ?: break
            if (current == goal) return reconstructPath(cameFrom, current)

            openSet.remove(current)
            for (neighbor in edges[current] ?: emptyList()) {
                val tentativeG = gScore[current]!! + haversine(current, neighbor)
                if (tentativeG < (gScore[neighbor] ?: Double.MAX_VALUE)) {
                    cameFrom[neighbor] = current
                    gScore[neighbor] = tentativeG
                    fScore[neighbor] = tentativeG + haversine(neighbor, goal)
                    openSet.add(neighbor)
                }
            }
        }
        return emptyList() // Nessun percorso trovato
    }

    private fun reconstructPath(cameFrom: Map<LatLon, LatLon>, current: LatLon): List<LatLon> {
        val path = mutableListOf(current)
        var curr = current
        while (cameFrom.containsKey(curr)) {
            curr = cameFrom[curr]!!
            path.add(0, curr)
        }
        return path
    }

    private fun haversine(a: LatLon, b: LatLon): Double {
        val R = 6371000.0 // metri
        val dLat = Math.toRadians(b.lat - a.lat)
        val dLon = Math.toRadians(b.lon - a.lon)
        val lat1 = Math.toRadians(a.lat)
        val lat2 = Math.toRadians(b.lat)

        val aVal = sin(dLat/2).pow(2) + sin(dLon/2).pow(2) * cos(lat1) * cos(lat2)
        return 2 * R * atan2(sqrt(aVal), sqrt(1 - aVal))
    }
}
