package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ContractorVertical(val displayName: String, val iconName: String) {
    SOLAR("Solar Installer", "wb_sunny"),
    RENOVATION("Renovation Contractor", "home_repair_service"),
    INTERIOR("Interior Designer", "chair")
}

enum class PipelineStage(val displayName: String, val order: Int) {
    LEAD("Lead / Inquiry", 1),
    INPUTS_RECEIVED("Inputs Received", 2),
    DRAFTING("Drafting Quote", 3),
    MARGIN_CHECK("Margin Review", 4),
    SENT("Sent to Client", 5),
    WON("Won / Approved", 6),
    LOST("Lost / Declined", 7)
}

enum class PackageTier(val tierKey: String, val label: String, val subtitle: String) {
    ECONOMY("TIER_1_ECONOMY", "Economy", "Value-focused essential standard"),
    EXECUTIVE("TIER_2_EXECUTIVE", "Executive", "Recommended best-seller standard"),
    TURNKEY("TIER_3_TURNKEY", "Turnkey / Elite", "Flagship premium with extended warranty")
}

enum class ItemCategory(val displayName: String) {
    MATERIAL("Materials & Hardware"),
    LABOR("Skilled Labor & Craftsmanship"),
    EQUIPMENT("Machinery & Tools"),
    LOGISTICS("Permits, DISCO & Transport")
}

@Entity(tableName = "proposals")
data class Proposal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val proposalNumber: String,
    val title: String,
    val clientName: String,
    val clientPhone: String = "",
    val clientEmail: String = "",
    val clientAddress: String = "",
    val vertical: ContractorVertical = ContractorVertical.SOLAR,
    val stage: PipelineStage = PipelineStage.DRAFTING,
    val rawNotes: String = "",
    val selectedTier: String = PackageTier.EXECUTIVE.tierKey,
    val targetMarginPercent: Double = 22.0,
    val version: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "package_options")
data class PackageOption(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val proposalId: Long,
    val tierKey: String,
    val title: String,
    val summary: String,
    val warrantyText: String,
    val discountPercent: Double = 0.0,
    val isRecommended: Boolean = false
)

@Entity(tableName = "line_items")
data class LineItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val proposalId: Long,
    val tierKey: String, // TIER_1_ECONOMY, TIER_2_EXECUTIVE, TIER_3_TURNKEY, or ALL
    val name: String,
    val category: ItemCategory = ItemCategory.MATERIAL,
    val quantity: Double = 1.0,
    val unit: String = "Unit",
    val unitCost: Double = 0.0, // Cost to contractor
    val unitSellingPrice: Double = 0.0, // Quoted selling price
    val notes: String = ""
) {
    val totalCost: Double get() = quantity * unitCost
    val totalSellingPrice: Double get() = quantity * unitSellingPrice
    val profit: Double get() = totalSellingPrice - totalCost
    val marginPercent: Double
        get() = if (totalSellingPrice > 0) (profit / totalSellingPrice) * 100.0 else 0.0
}

@Entity(tableName = "proposal_assumptions")
data class ProposalAssumption(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val proposalId: Long,
    val assumptionText: String,
    val impactNote: String = "",
    val isApproved: Boolean = false,
    val approvedAt: Long? = null
)

@Entity(tableName = "proposal_versions")
data class ProposalVersion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val proposalId: Long,
    val versionNumber: Int,
    val versionName: String,
    val totalMaterialCost: Double,
    val totalLaborCost: Double,
    val totalCost: Double,
    val netSellingPrice: Double,
    val grossProfit: Double,
    val marginPercent: Double,
    val snapshotJson: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "client_templates")
data class ClientTemplate(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val vertical: ContractorVertical,
    val description: String,
    val typicalTurnaroundDays: Int = 3,
    val sampleNotes: String,
    val defaultTargetMargin: Double = 25.0
)

enum class PaymentMethod(val displayName: String) {
    BANK_TRANSFER("Bank Transfer (Meezan/HBL/Allied)"),
    RAAST("Raast Instant ID"),
    EASYPAISA("EasyPaisa"),
    JAZZCASH("JazzCash"),
    CASH("Cash on Delivery / On Site")
}

enum class VerificationStatus(val displayName: String) {
    PENDING("Pending Verification"),
    VERIFIED("Verified & Reconciled"),
    REJECTED("Rejected / Invalid Proof")
}

@Entity(tableName = "payment_ledger")
data class PaymentLedgerItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientName: String,
    val clientPhone: String = "",
    val servicePackageName: String,
    val proposalCount: Int = 1,
    val amountPkr: Double,
    val paymentMethod: PaymentMethod = PaymentMethod.BANK_TRANSFER,
    val transactionReference: String = "",
    val proofNotes: String = "",
    val status: VerificationStatus = VerificationStatus.PENDING,
    val verifiedAt: Long? = null,
    val verifiedBy: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class ServicePackageOption(
    val id: String,
    val name: String,
    val proposalCount: Int,
    val pricePkr: Double,
    val description: String,
    val isPopular: Boolean = false
)
