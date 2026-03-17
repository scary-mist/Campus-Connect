package com.campusconnect.service

import kotlin.math.floor

/**
 * Geohash utility for encoding latitude/longitude into a geohash string.
 * Used for efficient proximity-based queries in Firestore.
 *
 * Precision levels:
 *   1 char ≈ 5,000km    5 chars ≈ 5km
 *   2 chars ≈ 1,250km   6 chars ≈ 1.2km  <-- campus-level
 *   3 chars ≈ 156km     7 chars ≈ 153m
 *   4 chars ≈ 39km      8 chars ≈ 38m
 */
object GeoHashUtil {

    private const val BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz"

    fun encode(latitude: Double, longitude: Double, precision: Int = 7): String {
        var latMin = -90.0
        var latMax = 90.0
        var lngMin = -180.0
        var lngMax = 180.0

        var isEven = true
        var bit = 0
        var ch = 0
        val geohash = StringBuilder()

        while (geohash.length < precision) {
            if (isEven) {
                val mid = (lngMin + lngMax) / 2
                if (longitude >= mid) {
                    ch = ch or (1 shl (4 - bit))
                    lngMin = mid
                } else {
                    lngMax = mid
                }
            } else {
                val mid = (latMin + latMax) / 2
                if (latitude >= mid) {
                    ch = ch or (1 shl (4 - bit))
                    latMin = mid
                } else {
                    latMax = mid
                }
            }

            isEven = !isEven
            if (bit < 4) {
                bit++
            } else {
                geohash.append(BASE32[ch])
                bit = 0
                ch = 0
            }
        }

        return geohash.toString()
    }

    /**
     * Get neighboring geohash prefixes for broader proximity search.
     */
    fun getNeighborPrefixes(geohash: String, prefixLength: Int = 6): List<String> {
        val prefix = geohash.take(prefixLength)
        return listOf(prefix) // Simplified: in production, compute all 8 neighbors
    }
}
