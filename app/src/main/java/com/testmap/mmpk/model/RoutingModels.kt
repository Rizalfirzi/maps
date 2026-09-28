package com.testmap.mmpk.model

data class DirectionStep(
    val index: Int,
    val text: String,
    val distanceMeters: Double,
    val estimatedMinutes: Double
)

data class LayerInfoItem(
    val name: String,
    val type: String,
    val isRoutingDataset: Boolean
)
