package com.sktc.ticketprinter

import android.content.Context
import android.util.Log

class RouteDslParser(private val context: Context) {

    data class ParsedRoute(
        val routeLabel: String,
        val source: String,
        val destination: String,
        val division: String,
        val routeNumber: String,
        val busTypeCode: String,
        val stops: List<String>,
        val forwardFares: List<List<Double>>
    ) {
        val displayLabel: String
            get() = if (routeNumber.isNotBlank()) {
                "$routeLabel (Route $routeNumber)"
            } else {
                routeLabel
            }

        fun fareBetween(fromStop: String, toStop: String): Double? {
            val fromIndex = stops.indexOf(fromStop)
            val toIndex = stops.indexOf(toStop)
            if (fromIndex == -1 || toIndex == -1 || fromIndex == toIndex) {
                return null
            }

            val low = minOf(fromIndex, toIndex)
            val high = maxOf(fromIndex, toIndex)
            val offset = high - low - 1
            if (low < 0 || low >= forwardFares.size || offset < 0) {
                return null
            }
            return forwardFares[low].getOrNull(offset)
        }
    }

    fun getRoutesForBusType(configuredBusType: String): List<ParsedRoute> {
        return parseAllRoutes().filter { matchesConfiguredBusType(it.busTypeCode, configuredBusType) }
    }

    fun getRoutesByDivision(division: String): List<ParsedRoute> {
        return parseAllRoutes().filter { it.division.equals(division, ignoreCase = true) }.distinctBy { it.displayLabel }
    }

    fun findRouteByLabel(configuredBusType: String, label: String): ParsedRoute? {
        return getRoutesForBusType(configuredBusType).firstOrNull { it.displayLabel == label }
    }

    fun findRouteByLabelAnyBusType(label: String): ParsedRoute? {
        return parseAllRoutes().firstOrNull { it.displayLabel == label }
    }

    private fun parseAllRoutes(): List<ParsedRoute> {
        val lines = readRouteLines()
        val routes = mutableListOf<ParsedRoute>()

        var upRouteLabel = ""
        var downRouteLabel = ""
        var division = ""
        var routeNumber = ""
        var busTypeCode = ""
        var stops = mutableListOf<String>()
        var fares = mutableListOf<List<Double>>()

        fun finalizeCurrentRoute() {
            if (upRouteLabel.isBlank() || stops.size < 2) {
                return
            }

            val normalizedBusType = normalizeToken(busTypeCode)
            if (normalizedBusType.contains("CITY") && routeNumber.isBlank()) {
                Log.w("RouteDslParser", "Skipping city route with missing route number: $upRouteLabel")
                return
            }

            val expectedStops = stops.size
            val normalizedFares = MutableList(expectedStops) { emptyList<Double>() }
            var valid = true
            for (i in 0 until expectedStops) {
                val expectedCount = expectedStops - i - 1
                val row = fares.getOrNull(i) ?: emptyList()
                if (row.size != expectedCount) {
                    valid = false
                    break
                }
                normalizedFares[i] = row
            }

            if (!valid) {
                Log.w("RouteDslParser", "Skipping invalid fare matrix route: $upRouteLabel")
                return
            }

            val sourceStop = stops.first()
            val destinationStop = stops.last()
            val resolvedUpLabel = upRouteLabel.ifBlank { "$sourceStop - $destinationStop" }
            val resolvedDownLabel = downRouteLabel.ifBlank { "$destinationStop - $sourceStop" }

            val reversedStops = stops.reversed()
            val reversedFares = buildReverseFares(stops, normalizedFares)

            routes.add(
                ParsedRoute(
                    routeLabel = resolvedUpLabel,
                    source = sourceStop,
                    destination = destinationStop,
                    division = division,
                    routeNumber = routeNumber,
                    busTypeCode = busTypeCode,
                    stops = stops.toList(),
                    forwardFares = normalizedFares.toList()
                )
            )

            routes.add(
                ParsedRoute(
                    routeLabel = resolvedDownLabel,
                    source = destinationStop,
                    destination = sourceStop,
                    division = division,
                    routeNumber = routeNumber,
                    busTypeCode = busTypeCode,
                    stops = reversedStops,
                    forwardFares = reversedFares
                )
            )
        }

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isBlank()) continue

            val parts = line.split(",").map { it.trim() }
            if (parts.isEmpty()) continue

            when {
                line.startsWith("Route,", ignoreCase = true) -> {
                    finalizeCurrentRoute()
                    upRouteLabel = parts.getOrNull(1).orEmpty()
                    downRouteLabel = parts.getOrNull(2).orEmpty()
                    routeNumber = parts.getOrNull(3).orEmpty().removeSuffix(".")
                    busTypeCode = parts.getOrNull(4).orEmpty()
                    division = ""
                    stops = mutableListOf()
                    fares = mutableListOf()
                }

                line.startsWith("Division:", ignoreCase = true) -> {
                    division = line.substringAfter(":", "").trim().trimEnd(',')
                }

                line.startsWith("Type:", ignoreCase = true) -> {
                    val rawTypePart = line.substringAfter(":", "").trim()
                    val typeToken = rawTypePart.substringBefore(",").trim()
                    if (typeToken.isNotBlank()) {
                        busTypeCode = typeToken
                    }

                    if (line.contains("Route No.", ignoreCase = true)) {
                        routeNumber = line.substringAfter("Route No.", "")
                            .substringAfter(":", "")
                            .trim()
                            .trimEnd(',')
                    }
                }

                line.startsWith("Route No.", ignoreCase = true) || line.startsWith("Route No:", ignoreCase = true) -> {
                    routeNumber = line.substringAfter(":", "").trim().trimEnd(',')
                }

                parts[0].equals("Stop", ignoreCase = true) -> {
                    val stopName = parts.getOrNull(1).orEmpty()
                    if (stopName.isBlank()) continue
                    stops.add(stopName)
                    val fareRow = parts.drop(2).mapNotNull { token -> token.toDoubleOrNull() }
                    fares.add(fareRow)
                }

                else -> {
                    // Ignore non-route directives (Pass, Note, Expense, etc.)
                }
            }
        }

        finalizeCurrentRoute()
        return routes
    }

    private fun buildReverseFares(stops: List<String>, forwardFares: List<List<Double>>): List<List<Double>> {
        val n = stops.size
        val reversedFares = mutableListOf<List<Double>>()

        for (i in 0 until n) {
            val row = mutableListOf<Double>()
            for (j in i + 1 until n) {
                val originalHigh = n - 1 - i
                val originalLow = n - 1 - j
                val offset = originalHigh - originalLow - 1
                val fare = forwardFares.getOrNull(originalLow)?.getOrNull(offset)
                if (fare != null) {
                    row.add(fare)
                }
            }
            reversedFares.add(row)
        }

        return reversedFares
    }

    private fun readRouteLines(): List<String> {
        return try {
            context.assets.open("routes").bufferedReader().use { it.readLines() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun matchesConfiguredBusType(routeBusTypeCode: String, configuredBusType: String): Boolean {
        val routeToken = normalizeToken(routeBusTypeCode)
        return when (configuredBusType) {
            "Ordinary Bus" -> routeToken.contains("ORD") && !routeToken.contains("CITY")
            "City Bus" -> routeToken.contains("CITY") && !routeToken.contains("AC")
            "City AC Bus" -> routeToken.contains("CITY") && routeToken.contains("AC")
            "Express Bus" -> routeToken.contains("EXPRESS")
            "Non-Stop Bus" -> routeToken.contains("NONSTOP") && !routeToken.contains("AC")
            "Non-Stop AC Bus" -> routeToken.contains("NONSTOP") && routeToken.contains("AC")
            "Sleeper Bus" -> routeToken.contains("SLEEPER") && !routeToken.contains("AC")
            "AC Sleeper Bus" -> routeToken.contains("SLEEPER") && routeToken.contains("AC")
            else -> true
        }
    }

    private fun normalizeToken(raw: String): String {
        return raw.uppercase().replace("_", "").replace("-", "").replace(" ", "")
    }
}
