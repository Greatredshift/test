package com.beatmachine.sp404.model

data class Bank(
    val id: Int,
    val name: String = bankName(id),
    val pads: List<Pad> = List(16) { padIndex ->
        Pad(index = padIndex, bankId = id, color = defaultColor(padIndex))
    }
) {
    companion object {
        fun bankName(id: Int) = ('A' + id).toString()

        fun defaultColor(padIndex: Int) = when (padIndex % 4) {
            0 -> PadColor.RED
            1 -> PadColor.ORANGE
            2 -> PadColor.AMBER
            3 -> PadColor.GREEN
            else -> PadColor.GREY
        }

        fun createDefault(): List<Bank> = List(10) { Bank(it) }
    }
}
