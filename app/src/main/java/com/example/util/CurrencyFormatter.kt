package com.example.util

import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {
    private val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }

    fun formatPkr(amount: Double): String {
        return "PKR ${formatter.format(amount)}"
    }

    fun formatNumber(amount: Double): String {
        return formatter.format(amount)
    }

    fun formatCompactPkr(amount: Double): String {
        return when {
            amount >= 10_000_000 -> "PKR %.2f Cr".format(amount / 10_000_000.0)
            amount >= 100_000 -> "PKR %.2f Lakh".format(amount / 100_000.0)
            amount >= 1_000 -> "PKR %.1fk".format(amount / 1_000.0)
            else -> "PKR %.0f".format(amount)
        }
    }
}
