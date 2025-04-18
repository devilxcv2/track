// MockGraph.kt
/*
 * Costruisce un esempio statico di grafo con 4 punti collegati da sentieri.
 */
package com.example.treknavigator

object MockGraph {
    fun buildSampleGraph(): TrailGraph {
        val a = LatLon(46.540, 12.135) // punto A
        val b = LatLon(46.542, 12.137) // punto B
        val c = LatLon(46.544, 12.139) // punto C
        val d = LatLon(46.546, 12.141) // punto D

        val edges = mapOf(
            a to listOf(b),
            b to listOf(a, c),
            c to listOf(b, d),
            d to listOf(c)
        )
        return TrailGraph(edges)
    }
}
