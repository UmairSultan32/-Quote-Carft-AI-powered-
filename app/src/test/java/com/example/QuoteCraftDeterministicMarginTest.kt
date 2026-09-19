package com.example

import com.example.data.model.ItemCategory
import com.example.data.model.LineItem
import com.example.data.model.PackageOption
import com.example.data.model.PackageTier
import com.example.data.model.ProposalAssumption
import com.example.util.CurrencyFormatter
import com.example.util.DeterministicMarginBreakdown
import com.example.util.MarginCalculator
import com.example.util.MissingCostFlag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuoteCraftDeterministicMarginTest {

    @Test
    fun `deterministic margin calculation calculates materials, labor, discount and profit correctly`() {
        val tierKey = PackageTier.EXECUTIVE.tierKey

        val items = listOf(
            LineItem(
                proposalId = 1,
                tierKey = tierKey,
                name = "10kW Tier-1 Solar Panels",
                category = ItemCategory.MATERIAL,
                quantity = 10000.0,
                unit = "Watts",
                unitCost = 30.0, // Total material cost: 300,000
                unitSellingPrice = 40.0 // Gross material selling: 400,000
            ),
            LineItem(
                proposalId = 1,
                tierKey = tierKey,
                name = "Installation Team Labor",
                category = ItemCategory.LABOR,
                quantity = 1.0,
                unit = "Job",
                unitCost = 50000.0, // Total labor cost: 50,000
                unitSellingPrice = 70000.0 // Gross labor selling: 70,000
            )
        )

        val pkg = PackageOption(
            proposalId = 1,
            tierKey = tierKey,
            title = "Executive Solar Option",
            summary = "Best value option",
            warrantyText = "2 Years Full System Warranty",
            discountPercent = 5.0, // 5% promotional discount
            isRecommended = true
        )

        val approvedAssumption = ProposalAssumption(
            proposalId = 1,
            assumptionText = "Standard RCC rooftop structure",
            impactNote = "",
            isApproved = true
        )

        val breakdown = MarginCalculator.calculateBreakdown(
            tierKey = tierKey,
            lineItems = items,
            packageOption = pkg,
            assumptions = listOf(approvedAssumption),
            targetMargin = 20.0
        )

        // Verifications
        // Gross Cost = 300,000 + 50,000 = 350,000
        assertEquals(300000.0, breakdown.totalMaterialCost, 0.01)
        assertEquals(50000.0, breakdown.totalLaborCost, 0.01)
        assertEquals(350000.0, breakdown.grossCost, 0.01)

        // Gross Quoted = 400,000 + 70,000 = 470,000
        assertEquals(470000.0, breakdown.grossQuotedPrice, 0.01)

        // Discount (5%) = 470,000 * 0.05 = 23,500
        assertEquals(23500.0, breakdown.discountAmount, 0.01)

        // Net Selling = 470,000 - 23,500 = 446,500
        assertEquals(446500.0, breakdown.netSellingPrice, 0.01)

        // Profit = 446,500 - 350,000 = 96,500
        assertEquals(96500.0, breakdown.grossProfit, 0.01)

        // Margin % = (96,500 / 446,500) * 100 = 21.61%
        val expectedMargin = (96500.0 / 446500.0) * 100.0
        assertEquals(expectedMargin, breakdown.marginPercent, 0.01)

        // All assumptions approved, so canExportPdf should be true
        assertTrue(breakdown.canExportPdf)
        assertEquals(0, breakdown.unapprovedAssumptionsCount)
    }

    @Test
    fun `unapproved AI assumption prevents PDF export and raises flag`() {
        val tierKey = PackageTier.ECONOMY.tierKey

        val items = listOf(
            LineItem(
                proposalId = 1,
                tierKey = tierKey,
                name = "Standard Solar Panels",
                category = ItemCategory.MATERIAL,
                quantity = 5000.0,
                unit = "Watts",
                unitCost = 25.0,
                unitSellingPrice = 35.0
            ),
            LineItem(
                proposalId = 1,
                tierKey = tierKey,
                name = "Mounting Labor",
                category = ItemCategory.LABOR,
                quantity = 1.0,
                unit = "Job",
                unitCost = 20000.0,
                unitSellingPrice = 30000.0
            )
        )

        val unapprovedAssumption = ProposalAssumption(
            proposalId = 1,
            assumptionText = "Assumed standard roof elevation without extra structural trusses",
            impactNote = "Adds PKR 30,000 if elevation exceeds 6ft",
            isApproved = false // Pending approval
        )

        val breakdown = MarginCalculator.calculateBreakdown(
            tierKey = tierKey,
            lineItems = items,
            packageOption = null,
            assumptions = listOf(unapprovedAssumption),
            targetMargin = 20.0
        )

        // Must require approval before including AI-written assumptions
        assertFalse("Unapproved assumption must block export", breakdown.canExportPdf)
        assertEquals(1, breakdown.unapprovedAssumptionsCount)
        assertTrue(breakdown.flags.any { it.title.contains("Unapproved AI Assumption") })
    }

    @Test
    fun `flags zero-cost items and negative profit correctly`() {
        val tierKey = PackageTier.ECONOMY.tierKey

        val items = listOf(
            LineItem(
                proposalId = 1,
                tierKey = tierKey,
                name = "Inverter with zero cost entered",
                category = ItemCategory.MATERIAL,
                quantity = 1.0,
                unit = "Unit",
                unitCost = 0.0, // Missing cost!
                unitSellingPrice = 120000.0
            ),
            LineItem(
                proposalId = 1,
                tierKey = tierKey,
                name = "Underpriced Cable Run",
                category = ItemCategory.MATERIAL,
                quantity = 1.0,
                unit = "Job",
                unitCost = 50000.0,
                unitSellingPrice = 30000.0 // Selling below cost!
            )
        )

        val breakdown = MarginCalculator.calculateBreakdown(
            tierKey = tierKey,
            lineItems = items,
            packageOption = null,
            assumptions = emptyList(),
            targetMargin = 20.0
        )

        assertTrue(breakdown.flags.any { it.title.contains("Zero cost entered for") })
        assertTrue(breakdown.flags.any { it.title.contains("Selling below cost") })
    }

    @Test
    fun `currency formatter formats PKR amounts accurately`() {
        val formatted = CurrencyFormatter.formatPkr(1500000.0)
        assertTrue("Formatted string should contain PKR", formatted.contains("PKR"))
        assertTrue("Formatted string should contain 1,500,000", formatted.contains("1,500,000"))
    }
}
