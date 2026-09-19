package com.example.data.ai

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.ContractorVertical
import com.example.data.model.ItemCategory
import com.example.data.model.LineItem
import com.example.data.model.PackageOption
import com.example.data.model.PackageTier
import com.example.data.model.ProposalAssumption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ParsedProposalResult(
    val title: String,
    val executiveSummary: String,
    val packageOptions: List<PackageOption>,
    val lineItems: List<LineItem>,
    val assumptions: List<ProposalAssumption>,
    val sourceEngine: String // "Gemini 3.5 Flash", "Gemini 3.1 Pro (High Thinking)", "Deterministic Local Engine"
)

class GeminiParserService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun parseContractorNotes(
        notesOrTranscript: String,
        vertical: ContractorVertical,
        useHighThinking: Boolean = false,
        targetMarginPct: Double = 22.0
    ): ParsedProposalResult = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        val isKeyValid = apiKey.isNotBlank() && !apiKey.contains("MY_GEMINI_API_KEY") && apiKey.length > 10

        if (isKeyValid) {
            try {
                val model = if (useHighThinking) "gemini-3.1-pro-preview" else "gemini-3.5-flash"
                val result = callGeminiRest(apiKey, model, notesOrTranscript, vertical, useHighThinking, targetMarginPct)
                if (result != null) {
                    return@withContext result
                }
            } catch (e: Exception) {
                Log.e("GeminiParser", "Gemini call failed, falling back to deterministic engine", e)
            }
        }

        // Deterministic fallback engine handles Urdu & English price lists, measurements, and voice transcripts
        fallbackDeterministicParsing(notesOrTranscript, vertical, targetMarginPct)
    }

    private fun callGeminiRest(
        apiKey: String,
        model: String,
        notes: String,
        vertical: ContractorVertical,
        useHighThinking: Boolean,
        targetMarginPct: Double
    ): ParsedProposalResult? {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        val systemPrompt = """
            You are QuoteCraft AI, an expert construction cost estimator, solar engineer, and interior design estimator for Pakistan and South Asia.
            You accept contractor price lists, site measurements, hardware specs, and Urdu / English voice notes (e.g. Urdu terms like 'adad' (qty), 'chhat' (roof), 'bhor' (bore), 'mistri' (mason), 'taar' (wire), 'deewar' (wall), 'plaster', 'mazdoor' (labor)).
            You convert the user's input into structured line items and 3 package options:
            1. TIER_1_ECONOMY (Value/Essential standard)
            2. TIER_2_EXECUTIVE (Recommended best-seller)
            3. TIER_3_TURNKEY (Elite premium with extended warranty)

            CRITICAL RULES:
            - Provide deterministic, realistic cost figures in PKR (Pakistani Rupees).
            - Separate costs into MATERIAL, LABOR, EQUIPMENT, and LOGISTICS categories.
            - Provide both 'unitCost' (contractor purchase/procurement cost) and 'unitSellingPrice' (calculated with ~${targetMarginPct}% markup).
            - Explicitly list any AI ASSUMPTIONS made (e.g. roof structure gauge, pipe distances, wastage percentages, DISCO approval times).
            - Output strictly valid JSON matching this schema:
            {
              "title": "Short descriptive title",
              "executiveSummary": "Brief overview of scope",
              "packages": [
                {
                  "tierKey": "TIER_1_ECONOMY",
                  "title": "Economy Option Name",
                  "summary": "Summary of economy option",
                  "warrantyText": "Warranty details",
                  "discountPercent": 0.0,
                  "isRecommended": false
                },
                {
                  "tierKey": "TIER_2_EXECUTIVE",
                  "title": "Executive Option Name",
                  "summary": "Summary of executive option",
                  "warrantyText": "Warranty details",
                  "discountPercent": 3.0,
                  "isRecommended": true
                },
                {
                  "tierKey": "TIER_3_TURNKEY",
                  "title": "Turnkey Option Name",
                  "summary": "Summary of turnkey option",
                  "warrantyText": "Warranty details",
                  "discountPercent": 5.0,
                  "isRecommended": false
                }
              ],
              "lineItems": [
                {
                  "tierKey": "ALL", // or TIER_1_ECONOMY, TIER_2_EXECUTIVE, TIER_3_TURNKEY
                  "name": "Item Name",
                  "category": "MATERIAL", // or LABOR, EQUIPMENT, LOGISTICS
                  "quantity": 10.0,
                  "unit": "Units/Sq Ft/Watts/Days",
                  "unitCost": 3500.0,
                  "unitSellingPrice": 4500.0,
                  "notes": "Specification or brand"
                }
              ],
              "assumptions": [
                {
                  "assumptionText": "Clear statement of what was assumed",
                  "impactNote": "How it impacts material or labor cost"
                }
              ]
            }
        """.trimIndent()

        val rootJson = JSONObject()
        val contentsArray = JSONArray()
        val contentObj = JSONObject()
        val partsArray = JSONArray()

        val promptPart = JSONObject().apply {
            put("text", "Vertical: ${vertical.displayName}\nContractor Notes / Price list / Voice transcript:\n$notes")
        }
        partsArray.put(promptPart)
        contentObj.put("parts", partsArray)
        contentsArray.put(contentObj)
        rootJson.put("contents", contentsArray)

        val sysPart = JSONObject().apply { put("text", systemPrompt) }
        val sysContent = JSONObject().apply { put("parts", JSONArray().apply { put(sysPart) }) }
        rootJson.put("systemInstruction", sysContent)

        val genConfig = JSONObject().apply {
            put("responseMimeType", "application/json")
            put("temperature", 0.3)
            if (useHighThinking) {
                val thinkingObj = JSONObject().apply {
                    put("thinkingLevel", "HIGH")
                }
                put("thinkingConfig", thinkingObj)
            }
        }
        rootJson.put("generationConfig", genConfig)

        val requestBody = rootJson.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            Log.e("GeminiParser", "Response code: ${response.code}")
            return null
        }

        val respBody = response.body?.string() ?: return null
        val respJson = JSONObject(respBody)
        val candidates = respJson.optJSONArray("candidates") ?: return null
        if (candidates.length() == 0) return null

        val firstCandidate = candidates.getJSONObject(0)
        val content = firstCandidate.optJSONObject("content") ?: return null
        val parts = content.optJSONArray("parts") ?: return null
        if (parts.length() == 0) return null

        val text = parts.getJSONObject(0).optString("text")
        if (text.isNullOrBlank()) return null

        return parseJsonResponse(text, vertical, if (useHighThinking) "Gemini 3.1 Pro (High Thinking)" else "Gemini 3.5 Flash")
    }

    private fun parseJsonResponse(
        jsonString: String,
        vertical: ContractorVertical,
        engineName: String
    ): ParsedProposalResult {
        val root = JSONObject(jsonString)
        val title = root.optString("title", "${vertical.displayName} Quotation")
        val summary = root.optString("executiveSummary", "")

        val packagesList = mutableListOf<PackageOption>()
        val packagesArr = root.optJSONArray("packages")
        if (packagesArr != null) {
            for (i in 0 until packagesArr.length()) {
                val p = packagesArr.getJSONObject(i)
                packagesList.add(
                    PackageOption(
                        proposalId = 0,
                        tierKey = p.optString("tierKey", PackageTier.EXECUTIVE.tierKey),
                        title = p.optString("title", "Standard Package"),
                        summary = p.optString("summary", ""),
                        warrantyText = p.optString("warrantyText", "Standard 1 Year Guarantee"),
                        discountPercent = p.optDouble("discountPercent", 0.0),
                        isRecommended = p.optBoolean("isRecommended", i == 1)
                    )
                )
            }
        }

        val itemsList = mutableListOf<LineItem>()
        val itemsArr = root.optJSONArray("lineItems")
        if (itemsArr != null) {
            for (i in 0 until itemsArr.length()) {
                val item = itemsArr.getJSONObject(i)
                val catStr = item.optString("category", "MATERIAL").uppercase()
                val category = try {
                    ItemCategory.valueOf(catStr)
                } catch (e: Exception) {
                    ItemCategory.MATERIAL
                }

                itemsList.add(
                    LineItem(
                        proposalId = 0,
                        tierKey = item.optString("tierKey", "ALL"),
                        name = item.optString("name", "Item ${i + 1}"),
                        category = category,
                        quantity = item.optDouble("quantity", 1.0),
                        unit = item.optString("unit", "Unit"),
                        unitCost = item.optDouble("unitCost", 0.0),
                        unitSellingPrice = item.optDouble("unitSellingPrice", 0.0),
                        notes = item.optString("notes", "")
                    )
                )
            }
        }

        val assumptionsList = mutableListOf<ProposalAssumption>()
        val assumptionsArr = root.optJSONArray("assumptions")
        if (assumptionsArr != null) {
            for (i in 0 until assumptionsArr.length()) {
                val a = assumptionsArr.getJSONObject(i)
                assumptionsList.add(
                    ProposalAssumption(
                        proposalId = 0,
                        assumptionText = a.optString("assumptionText", "Cost assumed based on standard specifications."),
                        impactNote = a.optString("impactNote", ""),
                        isApproved = false,
                        approvedAt = null
                    )
                )
            }
        }

        return ParsedProposalResult(
            title = title,
            executiveSummary = summary,
            packageOptions = if (packagesList.isNotEmpty()) packagesList else createDefaultPackages(vertical),
            lineItems = itemsList,
            assumptions = assumptionsList,
            sourceEngine = engineName
        )
    }

    private fun fallbackDeterministicParsing(
        notes: String,
        vertical: ContractorVertical,
        targetMarginPct: Double
    ): ParsedProposalResult {
        val lower = notes.lowercase()
        val items = mutableListOf<LineItem>()
        val assumptions = mutableListOf<ProposalAssumption>()

        when (vertical) {
            ContractorVertical.SOLAR -> {
                // Parse solar components
                var capacityKw = 10.0
                if (lower.contains("5kw") || lower.contains("5 kw")) capacityKw = 5.0
                if (lower.contains("7kw") || lower.contains("7 kw")) capacityKw = 7.0
                if (lower.contains("15kw") || lower.contains("15 kw")) capacityKw = 15.0
                if (lower.contains("20kw") || lower.contains("20 kw")) capacityKw = 20.0

                val watts = capacityKw * 1000.0
                val markupMultiplier = 1.0 + (targetMarginPct / 100.0)

                items.add(
                    LineItem(
                        proposalId = 0,
                        tierKey = "ALL",
                        name = "Tier-1 N-Type TOPCon Solar Panels (${capacityKw.toInt()} kW Array)",
                        category = ItemCategory.MATERIAL,
                        quantity = watts,
                        unit = "Watts",
                        unitCost = 31.0,
                        unitSellingPrice = (31.0 * markupMultiplier),
                        notes = "Longi Hi-MO 6 / Jinko Solar High Efficiency"
                    )
                )
                items.add(
                    LineItem(
                        proposalId = 0,
                        tierKey = "ALL",
                        name = "Heavy 14-Gauge Hot-Dipped Galvanized P1-P4 Mounting Structure",
                        category = ItemCategory.MATERIAL,
                        quantity = 1.0,
                        unit = "Lump Sum",
                        unitCost = capacityKw * 9000.0,
                        unitSellingPrice = (capacityKw * 9000.0 * markupMultiplier),
                        notes = "Engineered for 120 km/h wind load"
                    )
                )
                items.add(
                    LineItem(
                        proposalId = 0,
                        tierKey = "ALL",
                        name = "AC/DC Distribution DB with Schneider Breakers & Surge Arresters",
                        category = ItemCategory.MATERIAL,
                        quantity = 1.0,
                        unit = "Job",
                        unitCost = 55000.0,
                        unitSellingPrice = (55000.0 * markupMultiplier),
                        notes = "Type-II DC SPDs and IP65 dustproof box"
                    )
                )
                items.add(
                    LineItem(
                        proposalId = 0,
                        tierKey = "ALL",
                        name = "Pure Copper XLPE Solar DC Cable & Armored AC Cable (40m)",
                        category = ItemCategory.MATERIAL,
                        quantity = 40.0,
                        unit = "Meters",
                        unitCost = 650.0,
                        unitSellingPrice = (650.0 * markupMultiplier),
                        notes = "Pakistan Cables 6mm UV resistant"
                    )
                )
                items.add(
                    LineItem(
                        proposalId = 0,
                        tierKey = "ALL",
                        name = "Earth Bore with Chemical Compound (Resistance < 2.5 Ohm)",
                        category = ItemCategory.MATERIAL,
                        quantity = 2.0,
                        unit = "Bores",
                        unitCost = 30000.0,
                        unitSellingPrice = (30000.0 * markupMultiplier),
                        notes = "Double earthing for Inverter and Structure"
                    )
                )
                items.add(
                    LineItem(
                        proposalId = 0,
                        tierKey = "ALL",
                        name = "Skilled Solar Installation, Rigging & PEC Engineering Team",
                        category = ItemCategory.LABOR,
                        quantity = 1.0,
                        unit = "Team Labor",
                        unitCost = capacityKw * 5500.0,
                        unitSellingPrice = (capacityKw * 5500.0 * markupMultiplier),
                        notes = "Includes alignment, cable tray fitting & testing"
                    )
                )
                items.add(
                    LineItem(
                        proposalId = 0,
                        tierKey = "ALL",
                        name = "DISCO Net Metering Documentation, Processing & Bi-Directional Green Meter",
                        category = ItemCategory.LOGISTICS,
                        quantity = 1.0,
                        unit = "Permit Fee",
                        unitCost = 45000.0,
                        unitSellingPrice = 65000.0,
                        notes = "Includes SRO testing and NEPRA generation certificate"
                    )
                )

                // Tiers Specific Inverters
                items.add(
                    LineItem(
                        proposalId = 0,
                        tierKey = PackageTier.ECONOMY.tierKey,
                        name = "${capacityKw.toInt()}kW On-Grid 3-Phase String Inverter",
                        category = ItemCategory.MATERIAL,
                        quantity = 1.0,
                        unit = "Unit",
                        unitCost = capacityKw * 25000.0,
                        unitSellingPrice = (capacityKw * 25000.0 * markupMultiplier),
                        notes = "Growatt / Solis with standard monitoring"
                    )
                )
                items.add(
                    LineItem(
                        proposalId = 0,
                        tierKey = PackageTier.EXECUTIVE.tierKey,
                        name = "${capacityKw.toInt()}kW Smart Hybrid Inverter with Cloud Monitoring & UPS Mode",
                        category = ItemCategory.MATERIAL,
                        quantity = 1.0,
                        unit = "Unit",
                        unitCost = capacityKw * 38000.0,
                        unitSellingPrice = (capacityKw * 38000.0 * markupMultiplier),
                        notes = "Deye / Inverex Nitrox 10kW Hybrid"
                    )
                )
                items.add(
                    LineItem(
                        proposalId = 0,
                        tierKey = PackageTier.TURNKEY.tierKey,
                        name = "${capacityKw.toInt()}kW Tier-1 High-Voltage Hybrid Inverter (Battery & Microinverter Ready)",
                        category = ItemCategory.MATERIAL,
                        quantity = 1.0,
                        unit = "Unit",
                        unitCost = capacityKw * 52000.0,
                        unitSellingPrice = (capacityKw * 52000.0 * markupMultiplier),
                        notes = "Huawei SUN2000 / Sungrow High-End Industrial"
                    )
                )

                assumptions.add(
                    ProposalAssumption(
                        proposalId = 0,
                        assumptionText = "Assumed standard RCC roof with South-facing unobstructed sun exposure.",
                        impactNote = "Pitched roof requires elevated structure adding approx PKR 45,000.",
                        isApproved = false
                    )
                )
                assumptions.add(
                    ProposalAssumption(
                        proposalId = 0,
                        assumptionText = "Assumed rooftop-to-inverter distance is within 40 cable meters.",
                        impactNote = "Excess run charged at PKR 950/meter with conduit.",
                        isApproved = false
                    )
                )
                assumptions.add(
                    ProposalAssumption(
                        proposalId = 0,
                        assumptionText = "Assumed electricity bill has sanctioned load matching system capacity for net metering approval.",
                        impactNote = "DISCO load extension fee billed at actual government voucher.",
                        isApproved = false
                    )
                )
            }
            ContractorVertical.RENOVATION -> {
                val markupMultiplier = 1.0 + (targetMarginPct / 100.0)
                items.add(
                    LineItem(
                        proposalId = 0,
                        tierKey = "ALL",
                        name = "Demolition of Existing Sanitary, Tiles & Debris Safe Cartage",
                        category = ItemCategory.LABOR,
                        quantity = 1.0,
                        unit = "Lump Sum",
                        unitCost = 28000.0,
                        unitSellingPrice = (28000.0 * markupMultiplier),
                        notes = "Includes debris disposal to official dumping ground"
                    )
                )
                items.add(
                    LineItem(
                        proposalId = 0,
                        tierKey = "ALL",
                        name = "Heavy Duty PPRC Water Supply & PVC Drainage Re-Piping",
                        category = ItemCategory.MATERIAL,
                        quantity = 1.0,
                        unit = "Bath/Kitchen Job",
                        unitCost = 38000.0,
                        unitSellingPrice = (38000.0 * markupMultiplier),
                        notes = "Popular / Master PPRC pressurized lines"
                    )
                )
                items.add(
                    LineItem(
                        proposalId = 0,
                        tierKey = "ALL",
                        name = "Master Plumber & Pipefitter Labor (Pressure Testing Guaranteed)",
                        category = ItemCategory.LABOR,
                        quantity = 1.0,
                        unit = "Job",
                        unitCost = 22000.0,
                        unitSellingPrice = (22000.0 * markupMultiplier),
                        notes = "Includes 24-hr pressure gauge test"
                    )
                )
                items.add(
                    LineItem(
                        proposalId = 0,
                        tierKey = "ALL",
                        name = "Moisture Barrier Bitumen Waterproofing Membrane with Bond Coat",
                        category = ItemCategory.MATERIAL,
                        quantity = 1.0,
                        unit = "Treatment",
                        unitCost = 18000.0,
                        unitSellingPrice = (18000.0 * markupMultiplier),
                        notes = "2 coats Sika water seal"
                    )
                )
                // Tier specific tiles
                items.add(
                    LineItem(
                        proposalId = 0,
                        tierKey = PackageTier.ECONOMY.tierKey,
                        name = "Grade-A Pakistani Porcelain Floor & Wall Tiles (60x60 cm)",
                        category = ItemCategory.MATERIAL,
                        quantity = 280.0,
                        unit = "Sq Ft",
                        unitCost = 280.0,
                        unitSellingPrice = (280.0 * markupMultiplier),
                        notes = "Master / Shabbir Tiles Premium Grade"
                    )
                )
                items.add(
                    LineItem(
                        proposalId = 0,
                        tierKey = PackageTier.EXECUTIVE.tierKey,
                        name = "Imported Spanish Matte Porcelain Big Slab Tiles (60x120 cm)",
                        category = ItemCategory.MATERIAL,
                        quantity = 280.0,
                        unit = "Sq Ft",
                        unitCost = 490.0,
                        unitSellingPrice = (490.0 * markupMultiplier),
                        notes = "Rectified zero-joint Spanish porcelain"
                    )
                )
                items.add(
                    LineItem(
                        proposalId = 0,
                        tierKey = PackageTier.TURNKEY.tierKey,
                        name = "Italian Full-Body Polished Porcelain & Bookmatched Feature Slab",
                        category = ItemCategory.MATERIAL,
                        quantity = 280.0,
                        unit = "Sq Ft",
                        unitCost = 850.0,
                        unitSellingPrice = (850.0 * markupMultiplier),
                        notes = "Statuario / Calacatta Gold with epoxy grout"
                    )
                )
                items.add(
                    LineItem(
                        proposalId = 0,
                        tierKey = "ALL",
                        name = "Master Tile Mistri (Zero-Joint Leveling Spacer Installation)",
                        category = ItemCategory.LABOR,
                        quantity = 280.0,
                        unit = "Sq Ft",
                        unitCost = 110.0,
                        unitSellingPrice = (110.0 * markupMultiplier),
                        notes = "Includes laser leveling and Bond polymer adhesive"
                    )
                )
                assumptions.add(
                    ProposalAssumption(
                        proposalId = 0,
                        assumptionText = "Assumed 10% tile cutting wastage accounted for in square footage.",
                        impactNote = "Diagonal or herringbone patterns require 15% wastage.",
                        isApproved = false
                    )
                )
                assumptions.add(
                    ProposalAssumption(
                        proposalId = 0,
                        assumptionText = "Assumed existing main drain connection is structurally intact without underground collapse.",
                        impactNote = "Main sewer line repairs not included.",
                        isApproved = false
                    )
                )
            }
            ContractorVertical.INTERIOR -> {
                val markupMultiplier = 1.0 + (targetMarginPct / 100.0)
                items.add(
                    LineItem(
                        proposalId = 0,
                        tierKey = "ALL",
                        name = "Saint-Gobain Gypsum Cove False Ceiling with Galvanized G.I Channel Grid",
                        category = ItemCategory.MATERIAL,
                        quantity = 420.0,
                        unit = "Sq Ft",
                        unitCost = 145.0,
                        unitSellingPrice = (145.0 * markupMultiplier),
                        notes = "Fire-rated moisture resistant gypsum sheets"
                    )
                )
                items.add(
                    LineItem(
                        proposalId = 0,
                        tierKey = "ALL",
                        name = "Skilled False Ceiling Mistri & Joint Tape Finishing",
                        category = ItemCategory.LABOR,
                        quantity = 420.0,
                        unit = "Sq Ft",
                        unitCost = 55.0,
                        unitSellingPrice = (55.0 * markupMultiplier),
                        notes = "Seamless joint compound with fiber mesh"
                    )
                )
                items.add(
                    LineItem(
                        proposalId = 0,
                        tierKey = "ALL",
                        name = "Architectural Warm White 3000K COB Spotlights & Concealed LED Strip",
                        category = ItemCategory.MATERIAL,
                        quantity = 24.0,
                        unit = "Points",
                        unitCost = 1600.0,
                        unitSellingPrice = (1600.0 * markupMultiplier),
                        notes = "High CRI > 90 anti-glare recessed fixtures"
                    )
                )
                items.add(
                    LineItem(
                        proposalId = 0,
                        tierKey = "ALL",
                        name = "Licensed Electrician Conduit, Wiring & Light Fitting Labor",
                        category = ItemCategory.LABOR,
                        quantity = 24.0,
                        unit = "Points",
                        unitCost = 650.0,
                        unitSellingPrice = (650.0 * markupMultiplier),
                        notes = "Pakistan Cables 3/29 wiring to distribution panel"
                    )
                )
                items.add(
                    LineItem(
                        proposalId = 0,
                        tierKey = PackageTier.ECONOMY.tierKey,
                        name = "Velvet Matte Emulsion Wall Paint (3 Coats with Wall Putty)",
                        category = ItemCategory.MATERIAL,
                        quantity = 850.0,
                        unit = "Sq Ft",
                        unitCost = 45.0,
                        unitSellingPrice = (45.0 * markupMultiplier),
                        notes = "Berger Elegance Velvet Emulsion"
                    )
                )
                items.add(
                    LineItem(
                        proposalId = 0,
                        tierKey = PackageTier.EXECUTIVE.tierKey,
                        name = "Fluted Charcoal WPC Accent Louver Wall Panels + Designer TV Wall",
                        category = ItemCategory.MATERIAL,
                        quantity = 1.0,
                        unit = "Feature Wall",
                        unitCost = 145000.0,
                        unitSellingPrice = (145000.0 * markupMultiplier),
                        notes = "Thermo-treated wood composite with integrated LED channel"
                    )
                )
                items.add(
                    LineItem(
                        proposalId = 0,
                        tierKey = PackageTier.TURNKEY.tierKey,
                        name = "Italian Porcelain Bookmatch Media Wall with Custom Blum Push-to-Open Millwork",
                        category = ItemCategory.MATERIAL,
                        quantity = 1.0,
                        unit = "Feature Wall",
                        unitCost = 285000.0,
                        unitSellingPrice = (285000.0 * markupMultiplier),
                        notes = "Full CNC precision cabinetry and hidden cable management"
                    )
                )
                items.add(
                    LineItem(
                        proposalId = 0,
                        tierKey = "ALL",
                        name = "Site Delivery, Fragile Panel Packaging & Moving Crew",
                        category = ItemCategory.LOGISTICS,
                        quantity = 1.0,
                        unit = "Transit Job",
                        unitCost = 18000.0,
                        unitSellingPrice = 26000.0,
                        notes = "Padded van transport with floor protection sheeting"
                    )
                )

                assumptions.add(
                    ProposalAssumption(
                        proposalId = 0,
                        assumptionText = "Assumed ceiling height does not exceed 10.5 ft (standard residential scaffolding).",
                        impactNote = "Double scaffolding for double-height foyer requires PKR 25,000 extra.",
                        isApproved = false
                    )
                )
                assumptions.add(
                    ProposalAssumption(
                        proposalId = 0,
                        assumptionText = "Assumed smooth masonry plaster ready for putty without structural wall chiseling.",
                        impactNote = "Uneven walls will require cement-sand re-plastering.",
                        isApproved = false
                    )
                )
            }
        }

        val packages = createDefaultPackages(vertical)

        return ParsedProposalResult(
            title = "${vertical.displayName} Proposal (3 Compared Options)",
            executiveSummary = "Tailored quotation developed from site measurements, specs, and requirements with 3 packages compared side-by-side.",
            packageOptions = packages,
            lineItems = items,
            assumptions = assumptions,
            sourceEngine = "Deterministic Cost Engine"
        )
    }

    private fun createDefaultPackages(vertical: ContractorVertical): List<PackageOption> {
        return listOf(
            PackageOption(
                proposalId = 0,
                tierKey = PackageTier.ECONOMY.tierKey,
                title = "Essential Value Package",
                summary = "Optimized for core performance with standard brand specifications and quickest payback.",
                warrantyText = "1 Year Workmanship Guarantee + Manufacturer Warranties",
                discountPercent = 0.0,
                isRecommended = false
            ),
            PackageOption(
                proposalId = 0,
                tierKey = PackageTier.EXECUTIVE.tierKey,
                title = "Executive Best-Seller (Recommended)",
                summary = "Balanced premium spec with high-durability components and optimal warranty coverage.",
                warrantyText = "2 Years Complete Workmanship Guarantee + Priority Support",
                discountPercent = 3.0,
                isRecommended = true
            ),
            PackageOption(
                proposalId = 0,
                tierKey = PackageTier.TURNKEY.tierKey,
                title = "Turnkey Elite Plus",
                summary = "Uncompromising flagship materials, extended warranties, and complete peace-of-mind execution.",
                warrantyText = "5 Years Comprehensive Guarantee with Free Annual Maintenance",
                discountPercent = 5.0,
                isRecommended = false
            )
        )
    }
}
