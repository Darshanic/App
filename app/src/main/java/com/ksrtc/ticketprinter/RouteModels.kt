package com.sktc.ticketprinter

data class Stop(
    val id: Int,
    val nameEn: String,
    val nameKa: String
)

data class RouteStage(
    val id: Int,
    val stopId: Int,
    val stageNumber: Int,
    val distanceKm: Double
)
