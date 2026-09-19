package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.ClientTemplate
import com.example.data.model.ContractorVertical
import com.example.data.model.ItemCategory
import com.example.data.model.LineItem
import com.example.data.model.PackageOption
import com.example.data.model.PackageTier
import com.example.data.model.PaymentLedgerItem
import com.example.data.model.PaymentMethod
import com.example.data.model.PipelineStage
import com.example.data.model.Proposal
import com.example.data.model.ProposalAssumption
import com.example.data.model.ProposalVersion
import com.example.data.model.VerificationStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Proposal::class,
        PackageOption::class,
        LineItem::class,
        ProposalAssumption::class,
        ProposalVersion::class,
        ClientTemplate::class,
        PaymentLedgerItem::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun proposalDao(): ProposalDao
    abstract fun paymentDao(): PaymentDao
    abstract fun templateDao(): TemplateDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "quotecraft.db"
                ).addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            INSTANCE?.let { seedDatabase(it) }
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun seedDatabase(database: AppDatabase) {
            val templateDao = database.templateDao()
            val proposalDao = database.proposalDao()
            val paymentDao = database.paymentDao()

            // 1. Seed Reusable Industry Templates
            val templates = listOf(
                ClientTemplate(
                    title = "10kW Hybrid On-Grid Solar Proposal",
                    vertical = ContractorVertical.SOLAR,
                    description = "Tier-1 N-Type TOPCon Panels, 3-Phase Inverter, Net Metering & LFP Battery Ready",
                    typicalTurnaroundDays = 2,
                    sampleNotes = "Residential client DHA Phase 6. Average monthly summer units 1,400 kWh. Roof area: 1,800 sq ft RCC roof facing South. Customer needs Longi or Canadian 585W bifacial panels, 10kW On-Grid inverter with backup option, net metering DISCO fee included, and 10-year workmanship guarantee.",
                    defaultTargetMargin = 22.0
                ),
                ClientTemplate(
                    title = "Complete Master Bath & Kitchen Renovation",
                    vertical = ContractorVertical.RENOVATION,
                    description = "Civil demolition, Spanish porcelain floor & wall tiles, copper plumbing & vanity",
                    typicalTurnaroundDays = 3,
                    sampleNotes = "Master bath 9x12 ft + Kitchen 14x10 ft. Complete sanitary revamp, Grohe concealed mixer, 60x120cm Spanish porcelain tiles, Corian kitchen countertop, marine ply cabinets with soft-close Blum hinges, and electrical re-wiring with warm LED task lighting.",
                    defaultTargetMargin = 28.0
                ),
                ClientTemplate(
                    title = "Executive Living Room & Ceiling Interior",
                    vertical = ContractorVertical.INTERIOR,
                    description = "Gypsum cove false ceiling, fluted charcoal wall panels, profile lighting, custom media wall",
                    typicalTurnaroundDays = 3,
                    sampleNotes = "Living & Dining area 28x16 ft. Design concept: Modern luxury warm minimalist. Moisture-resistant Saint-Gobain gypsum false ceiling, magnetic track lights, fluted wooden charcoal louvers, Italian Statuario marble TV wall with concealed conduits, and velvet matte emulsion paint.",
                    defaultTargetMargin = 30.0
                )
            )
            templateDao.insertTemplates(templates)

            // 2. Seed Sample Active Proposal to immediately showcase Margin Checker & 3 Package Options
            val sampleProposal = Proposal(
                proposalNumber = "QC-2026-0101",
                title = "10kW Tier-1 Solar Net Metering Solution",
                clientName = "Malik Asad & Sons Commercial Plaza",
                clientPhone = "+92 300 8421900",
                clientEmail = "asad@maliktextiles.pk",
                clientAddress = "Gulberg III, Main Boulevard, Lahore",
                vertical = ContractorVertical.SOLAR,
                stage = PipelineStage.MARGIN_CHECK,
                rawNotes = "10kW Solar setup with net metering. Client requested 3 package tiers: Economy (Standard Inverter), Executive (Hybrid with backup), and Elite Turnkey (Bifacial + Lithium battery ready). Include all DISCO approvals.",
                selectedTier = PackageTier.EXECUTIVE.tierKey,
                targetMarginPercent = 22.0,
                version = 1
            )
            val proposalId = proposalDao.insertProposal(sampleProposal)

            // 3 Packages for this proposal
            val packages = listOf(
                PackageOption(
                    proposalId = proposalId,
                    tierKey = PackageTier.ECONOMY.tierKey,
                    title = "Essential Value (On-Grid)",
                    summary = "Standard Tier-1 mono panels with standard string inverter. Quickest payback period.",
                    warrantyText = "12 Year Product Warranty, 25 Year Performance",
                    discountPercent = 0.0,
                    isRecommended = false
                ),
                PackageOption(
                    proposalId = proposalId,
                    tierKey = PackageTier.EXECUTIVE.tierKey,
                    title = "Executive Hybrid (Recommended)",
                    summary = "High-efficiency bifacial panels with hybrid inverter and critical-load switchover.",
                    warrantyText = "15 Year Product Warranty, 30 Year Performance, 5 Year Inverter",
                    discountPercent = 3.0,
                    isRecommended = true
                ),
                PackageOption(
                    proposalId = proposalId,
                    tierKey = PackageTier.TURNKEY.tierKey,
                    title = "Turnkey Elite Plus (Zero-Hassle)",
                    summary = "Flagship TopCon bifacial array, hybrid inverter, surge arresters, LFP battery ready + Net Metering fast-track.",
                    warrantyText = "20 Year Comprehensive Warranty & Free Annual Maintenance (2 Years)",
                    discountPercent = 5.0,
                    isRecommended = false
                )
            )
            proposalDao.insertPackageOptions(packages)

            // Line items for Economy, Executive, Turnkey
            val lineItems = listOf(
                // Common / All Tiers
                LineItem(
                    proposalId = proposalId,
                    tierKey = "ALL",
                    name = "Tier-1 N-Type TOPCon Solar PV Modules (10.2 kW)",
                    category = ItemCategory.MATERIAL,
                    quantity = 10200.0,
                    unit = "Watts",
                    unitCost = 31.5,
                    unitSellingPrice = 41.0,
                    notes = "Longi Hi-MO 6 / Canadian Solar 585W"
                ),
                LineItem(
                    proposalId = proposalId,
                    tierKey = "ALL",
                    name = "Hot-Dipped Galvanized Steel Mounting Structure (14-Gauge)",
                    category = ItemCategory.MATERIAL,
                    quantity = 1.0,
                    unit = "Lump Sum",
                    unitCost = 95000.0,
                    unitSellingPrice = 135000.0,
                    notes = "Engineered for 130 km/h wind resilience"
                ),
                LineItem(
                    proposalId = proposalId,
                    tierKey = "ALL",
                    name = "AC & DC Distribution Panels with Schneider Switchgear",
                    category = ItemCategory.MATERIAL,
                    quantity = 1.0,
                    unit = "Job",
                    unitCost = 65000.0,
                    unitSellingPrice = 92000.0,
                    notes = "Includes Type-II surge arresters and DC isolators"
                ),
                LineItem(
                    proposalId = proposalId,
                    tierKey = "ALL",
                    name = "Pure Copper Solar DC Cable & AC Armored Interconnect",
                    category = ItemCategory.MATERIAL,
                    quantity = 120.0,
                    unit = "Meters",
                    unitCost = 550.0,
                    unitSellingPrice = 780.0,
                    notes = "Pakistan Cables 6mm PV & 16mm 4-Core AC"
                ),
                LineItem(
                    proposalId = proposalId,
                    tierKey = "ALL",
                    name = "Deep Earthing Bore with Chemical Bentonite Compound",
                    category = ItemCategory.MATERIAL,
                    quantity = 2.0,
                    unit = "Bores",
                    unitCost = 32000.0,
                    unitSellingPrice = 48000.0,
                    notes = "Resistance guaranteed below 2.5 Ohms"
                ),
                LineItem(
                    proposalId = proposalId,
                    tierKey = "ALL",
                    name = "Certified PEC Engineer Installation, Alignment & Rigging",
                    category = ItemCategory.LABOR,
                    quantity = 1.0,
                    unit = "Turnkey Labor",
                    unitCost = 60000.0,
                    unitSellingPrice = 90000.0,
                    notes = "4-man specialized rooftop solar technician team"
                ),
                LineItem(
                    proposalId = proposalId,
                    tierKey = "ALL",
                    name = "DISCO Net Metering Documentation, Inspection & Bi-Directional Meter",
                    category = ItemCategory.LOGISTICS,
                    quantity = 1.0,
                    unit = "Permit Package",
                    unitCost = 45000.0,
                    unitSellingPrice = 75000.0,
                    notes = "Includes NEPRA generation license application"
                ),
                // Specific to Inverters by tier
                LineItem(
                    proposalId = proposalId,
                    tierKey = PackageTier.ECONOMY.tierKey,
                    name = "10kW On-Grid 3-Phase String Inverter (Standard)",
                    category = ItemCategory.MATERIAL,
                    quantity = 1.0,
                    unit = "Unit",
                    unitCost = 280000.0,
                    unitSellingPrice = 360000.0,
                    notes = "Growatt / Solis 10kW On-Grid"
                ),
                LineItem(
                    proposalId = proposalId,
                    tierKey = PackageTier.EXECUTIVE.tierKey,
                    name = "10kW Hybrid 3-Phase Smart Inverter with Remote WiFi Monitoring",
                    category = ItemCategory.MATERIAL,
                    quantity = 1.0,
                    unit = "Unit",
                    unitCost = 390000.0,
                    unitSellingPrice = 495000.0,
                    notes = "Deye / Inverex Nitrox 10kW Hybrid"
                ),
                LineItem(
                    proposalId = proposalId,
                    tierKey = PackageTier.TURNKEY.tierKey,
                    name = "12kW High-Voltage Hybrid Inverter with Battery Management & Rapid Shutdown",
                    category = ItemCategory.MATERIAL,
                    quantity = 1.0,
                    unit = "Unit",
                    unitCost = 540000.0,
                    unitSellingPrice = 690000.0,
                    notes = "Huawei SUN2000 / Sungrow Tier-1 Industrial Grade"
                )
            )
            proposalDao.insertLineItems(lineItems)

            // Seed Assumptions (Signature Feature: require approval before including AI assumptions!)
            val assumptions = listOf(
                ProposalAssumption(
                    proposalId = proposalId,
                    assumptionText = "Assumed standard RCC roof slab thickness without requiring structural reinforcement beams.",
                    impactNote = "If pitched GI roof truss is discovered on site, add PKR 40,000 for custom unistrut clamps.",
                    isApproved = true,
                    approvedAt = System.currentTimeMillis()
                ),
                ProposalAssumption(
                    proposalId = proposalId,
                    assumptionText = "Assumed cable run distance from rooftop inverter to ground distribution board is within 35 meters.",
                    impactNote = "Every additional 10 meters will require 16mm armored cable at PKR 12,000.",
                    isApproved = false, // Unapproved! Flags margin checker
                    approvedAt = null
                ),
                ProposalAssumption(
                    proposalId = proposalId,
                    assumptionText = "Assumed LESCO/DISCO sanction load on client's consumer electricity bill is at least 10kW for net metering eligibility.",
                    impactNote = "If current sanction load is 5kW, load extension fee of ~PKR 35,000 will be billed at actuals.",
                    isApproved = false, // Unapproved!
                    approvedAt = null
                )
            )
            proposalDao.insertAssumptions(assumptions)

            // 3. Seed Initial Payment Ledger items showcasing PKR 3,000 & PKR 10,000 service packages
            val payments = listOf(
                PaymentLedgerItem(
                    clientName = "SunPower Solar Engineering",
                    clientPhone = "+92 321 4501234",
                    servicePackageName = "5-Proposal Pro Pack (PKR 10,000)",
                    proposalCount = 5,
                    amountPkr = 10000.0,
                    paymentMethod = PaymentMethod.BANK_TRANSFER,
                    transactionReference = "MEEZAN-FT-99482103",
                    proofNotes = "Direct bank transfer from Meezan corporate account. Confirmed with bank statement.",
                    status = VerificationStatus.VERIFIED,
                    verifiedAt = System.currentTimeMillis() - 86400000,
                    verifiedBy = "Admin (Umair)"
                ),
                PaymentLedgerItem(
                    clientName = "Elite Living Interiors",
                    clientPhone = "+92 301 7765432",
                    servicePackageName = "Single Proposal Preparation (PKR 3,000)",
                    proposalCount = 1,
                    amountPkr = 3000.0,
                    paymentMethod = PaymentMethod.RAAST,
                    transactionReference = "RAAST-88349210-91",
                    proofNotes = "Client shared Raast reference receipt on WhatsApp. Awaiting manual statement reconciliation.",
                    status = VerificationStatus.PENDING,
                    verifiedAt = null,
                    verifiedBy = ""
                )
            )
            for (p in payments) {
                paymentDao.insertPayment(p)
            }
        }
    }
}
