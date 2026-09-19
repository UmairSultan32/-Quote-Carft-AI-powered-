package com.example.util

import com.example.data.model.ItemCategory
import com.example.data.model.LineItem
import com.example.data.model.PackageOption
import com.example.data.model.ProposalAssumption

data class MissingCostFlag(
    val title: String,
    val description: String,
    val severity: Severity,
    val relatedCategory: ItemCategory? = null,
    val relatedItem: LineItem? = null
) {
    enum class Severity { CRITICAL, WARNING, INFO }
}

data class DeterministicMarginBreakdown(
    val tierKey: String,
    val totalMaterialCost: Double,
    val totalLaborCost: Double,
    val totalEquipmentCost: Double,
    val totalLogisticsCost: Double,
    val grossCost: Double,
    val grossQuotedPrice: Double,
    val discountPercent: Double,
    val discountAmount: Double,
    val netSellingPrice: Double,
    val grossProfit: Double,
    val marginPercent: Double,
    val markupPercent: Double,
    val targetMarginPercent: Double,
    val marginHealth: MarginHealth,
    val flags: List<MissingCostFlag>,
    val unapprovedAssumptionsCount: Int,
    val canExportPdf: Boolean
) {
    enum class MarginHealth {
        EXCELLENT, // >= target + 5%
        HEALTHY,   // >= target
        LOW_RISK,  // within 5% below target
        DANGEROUS, // > 10% below target or < 12%
        LOSS       // negative profit
    }
}

object MarginCalculator {

    fun calculateBreakdown(
        tierKey: String,
        lineItems: List<LineItem>,
        packageOption: PackageOption?,
        assumptions: List<ProposalAssumption>,
        targetMargin: Double = 22.0
    ): DeterministicMarginBreakdown {
        // Items applicable to this tier or ALL tiers
        val tierItems = lineItems.filter { it.tierKey == tierKey || it.tierKey == "ALL" }

        var materialCost = 0.0
        var laborCost = 0.0
        var equipCost = 0.0
        var logCost = 0.0
        var grossPrice = 0.0

        val flags = mutableListOf<MissingCostFlag>()

        var hasLaborItem = false
        var hasLogisticsItem = false

        for (item in tierItems) {
            val cost = item.quantity * item.unitCost
            val price = item.quantity * item.unitSellingPrice

            when (item.category) {
                ItemCategory.MATERIAL -> materialCost += cost
                ItemCategory.LABOR -> {
                    laborCost += cost
                    hasLaborItem = true
                }
                ItemCategory.EQUIPMENT -> equipCost += cost
                ItemCategory.LOGISTICS -> {
                    logCost += cost
                    hasLogisticsItem = true
                }
            }
            grossPrice += price

            // Check if item has 0 cost but selling price > 0
            if (item.unitCost <= 0.0 && item.unitSellingPrice > 0.0) {
                flags.add(
                    MissingCostFlag(
                        title = "Zero cost entered for ${item.name}",
                        description = "Item is quoted at PKR ${formatPkrSimple(price)} but contractor purchase cost is PKR 0.",
                        severity = MissingCostFlag.Severity.CRITICAL,
                        relatedCategory = item.category,
                        relatedItem = item
                    )
                )
            }

            // Check if contractor selling price is lower than cost (selling at loss!)
            if (item.unitSellingPrice < item.unitCost && item.unitCost > 0) {
                flags.add(
                    MissingCostFlag(
                        title = "Selling below cost: ${item.name}",
                        description = "Quoted unit PKR ${formatPkrSimple(item.unitSellingPrice)} is below cost PKR ${formatPkrSimple(item.unitCost)}.",
                        severity = MissingCostFlag.Severity.CRITICAL,
                        relatedCategory = item.category,
                        relatedItem = item
                    )
                )
            }
        }

        val grossCost = materialCost + laborCost + equipCost + logCost
        val discountPct = packageOption?.discountPercent ?: 0.0
        val discountAmt = grossPrice * (discountPct / 100.0)
        val netPrice = maxOf(0.0, grossPrice - discountAmt)
        val grossProfit = netPrice - grossCost

        val marginPercent = if (netPrice > 0.0) {
            (grossProfit / netPrice) * 100.0
        } else 0.0

        val markupPercent = if (grossCost > 0.0) {
            (grossProfit / grossCost) * 100.0
        } else 0.0

        // Rule: Flag missing labor costs
        if (!hasLaborItem && tierItems.isNotEmpty()) {
            flags.add(
                MissingCostFlag(
                    title = "Missing Labor & Craftsmanship Costs",
                    description = "No labor line items found for this proposal tier. Contractor will incur out-of-pocket workforce costs.",
                    severity = MissingCostFlag.Severity.CRITICAL,
                    relatedCategory = ItemCategory.LABOR
                )
            )
        }

        // Rule: Flag missing logistics or transport
        if (!hasLogisticsItem && tierItems.size > 2) {
            flags.add(
                MissingCostFlag(
                    title = "Missing Logistics / Transport Allowance",
                    description = "Site transit, loading/unloading, or DISCO permit fees have not been allocated.",
                    severity = MissingCostFlag.Severity.WARNING,
                    relatedCategory = ItemCategory.LOGISTICS
                )
            )
        }

        // Margin health calculation
        val health = when {
            grossProfit < 0.0 -> DeterministicMarginBreakdown.MarginHealth.LOSS
            marginPercent < 12.0 -> DeterministicMarginBreakdown.MarginHealth.DANGEROUS
            marginPercent < (targetMargin - 3.0) -> DeterministicMarginBreakdown.MarginHealth.LOW_RISK
            marginPercent >= (targetMargin + 5.0) -> DeterministicMarginBreakdown.MarginHealth.EXCELLENT
            else -> DeterministicMarginBreakdown.MarginHealth.HEALTHY
        }

        if (health == DeterministicMarginBreakdown.MarginHealth.LOSS || health == DeterministicMarginBreakdown.MarginHealth.DANGEROUS) {
            flags.add(
                MissingCostFlag(
                    title = "Unsafe Profit Margin (${String.format("%.1f", marginPercent)}%)",
                    description = "Below the required target margin of ${String.format("%.1f", targetMargin)}%. Increase selling prices or renegotiate supplier rates.",
                    severity = MissingCostFlag.Severity.CRITICAL
                )
            )
        }

        val unapprovedAssumptionsCount = assumptions.count { !it.isApproved }
        if (unapprovedAssumptionsCount > 0) {
            flags.add(
                MissingCostFlag(
                    title = "$unapprovedAssumptionsCount Unapproved AI Assumption(s)",
                    description = "You must explicitly review and approve or reject all AI-written assumptions before exporting this proposal.",
                    severity = MissingCostFlag.Severity.CRITICAL
                )
            )
        }

        val hasCriticalFlag = flags.any { it.severity == MissingCostFlag.Severity.CRITICAL }
        val canExportPdf = !hasCriticalFlag && unapprovedAssumptionsCount == 0 && netPrice > 0

        return DeterministicMarginBreakdown(
            tierKey = tierKey,
            totalMaterialCost = materialCost,
            totalLaborCost = laborCost,
            totalEquipmentCost = equipCost,
            totalLogisticsCost = logCost,
            grossCost = grossCost,
            grossQuotedPrice = grossPrice,
            discountPercent = discountPct,
            discountAmount = discountAmt,
            netSellingPrice = netPrice,
            grossProfit = grossProfit,
            marginPercent = marginPercent,
            markupPercent = markupPercent,
            targetMarginPercent = targetMargin,
            marginHealth = health,
            flags = flags,
            unapprovedAssumptionsCount = unapprovedAssumptionsCount,
            canExportPdf = canExportPdf
        )
    }

    private fun formatPkrSimple(value: Double): String {
        return "%,.0f".format(value)
    }
}
