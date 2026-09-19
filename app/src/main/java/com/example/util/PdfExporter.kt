package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.LineItem
import com.example.data.model.PackageOption
import com.example.data.model.Proposal
import com.example.data.model.ProposalAssumption
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExporter {

    fun generateAndShareProposalPdf(
        context: Context,
        proposal: Proposal,
        packages: List<PackageOption>,
        lineItems: List<LineItem>,
        assumptions: List<ProposalAssumption>,
        marginBreakdown: DeterministicMarginBreakdown?
    ): File? {
        val pdfFile = generateProposalPdf(context, proposal, packages, lineItems, assumptions, marginBreakdown) ?: return null

        try {
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, pdfFile)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Quotation: ${proposal.title} [${proposal.proposalNumber}]")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Please find attached the official branded proposal '${proposal.title}' prepared via QuoteCraft."
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share Proposal PDF via...")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            return pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun generateProposalPdf(
        context: Context,
        proposal: Proposal,
        packages: List<PackageOption>,
        lineItems: List<LineItem>,
        assumptions: List<ProposalAssumption>,
        marginBreakdown: DeterministicMarginBreakdown?
    ): File? {
        val document = PdfDocument()

        // A4 page dimensions in points: 595 x 842
        val pageWidth = 595
        val pageHeight = 842

        // Paint objects
        val navyPaint = Paint().apply {
            color = Color.parseColor("#0A192F")
            isAntiAlias = true
        }
        val copperPaint = Paint().apply {
            color = Color.parseColor("#C87D55")
            isAntiAlias = true
        }
        val copperLightPaint = Paint().apply {
            color = Color.parseColor("#F6E7DD")
            isAntiAlias = true
        }
        val textWhiteBold = Paint().apply {
            color = Color.WHITE
            textSize = 16f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val textWhiteSub = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            textSize = 9.5f
            isAntiAlias = true
        }
        val textDarkBold = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 12f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val textDarkRegular = Paint().apply {
            color = Color.parseColor("#334155")
            textSize = 9f
            isAntiAlias = true
        }
        val textMuted = Paint().apply {
            color = Color.parseColor("#64748B")
            textSize = 8f
            isAntiAlias = true
        }
        val borderPaint = Paint().apply {
            color = Color.parseColor("#CBD5E1")
            strokeWidth = 0.8f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        val rowAltBgPaint = Paint().apply {
            color = Color.parseColor("#F8FAFC")
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        // ================= PAGE 1: Executive Summary & 3 Packages Comparison =================
        val pageInfo1 = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page1 = document.startPage(pageInfo1)
        val canvas1: Canvas = page1.canvas

        // Header Background Navy Banner
        canvas1.drawRect(0f, 0f, pageWidth.toFloat(), 110f, navyPaint)
        // Copper Accent Strip
        canvas1.drawRect(0f, 110f, pageWidth.toFloat(), 115f, copperPaint)

        // Brand Logo / Title
        canvas1.drawText("QUOTECRAFT", 35f, 42f, textWhiteBold.apply { textSize = 20f })
        canvas1.drawText("ENGINEERED PROPOSALS & QUOTATION MANAGEMENT", 35f, 58f, textWhiteSub.apply { textSize = 8f })

        // Quote Metadata Box (Right aligned)
        val quoteNum = proposal.proposalNumber.ifBlank { "QC-${proposal.id}" }
        val dateStr = SimpleDateFormat("dd MMMM yyyy", Locale.US).format(Date(proposal.updatedAt))
        canvas1.drawText("PROPOSAL #: $quoteNum", 410f, 38f, textWhiteSub.apply { isFakeBoldText = true })
        canvas1.drawText("DATE: $dateStr", 410f, 52f, textWhiteSub)
        canvas1.drawText("VERSION: v${proposal.version}", 410f, 66f, textWhiteSub)
        canvas1.drawText("VERTICAL: ${proposal.vertical.displayName}", 410f, 80f, textWhiteSub)

        // Proposal Title & Client Details Card
        var yPos = 135f
        val cardRect = RectF(35f, yPos, (pageWidth - 35).toFloat(), yPos + 75f)
        canvas1.drawRoundRect(cardRect, 6f, 6f, rowAltBgPaint)
        canvas1.drawRoundRect(cardRect, 6f, 6f, borderPaint)

        canvas1.drawText("PROJECT TITLE: ${proposal.title.take(55)}", 48f, yPos + 22f, textDarkBold.apply { textSize = 11f })
        canvas1.drawText("PREPARED FOR: ${proposal.clientName}", 48f, yPos + 40f, textDarkRegular)
        val contactLine = listOfNotNull(
            proposal.clientPhone.takeIf { it.isNotBlank() },
            proposal.clientEmail.takeIf { it.isNotBlank() },
            proposal.clientAddress.takeIf { it.isNotBlank() }
        ).joinToString("  •  ")
        canvas1.drawText(contactLine.ifBlank { "Direct Client Quotation" }.take(75), 48f, yPos + 58f, textMuted)

        yPos += 95f

        // SECTION 1: 3 COMPARED PACKAGE OPTIONS
        canvas1.drawText("COMPARED PACKAGE TIERS", 35f, yPos, textDarkBold.apply { textSize = 12f })
        yPos += 12f

        // Draw 3 Package Cards side-by-side
        val cardMargin = 8f
        val startX = 35f
        val availableWidth = (pageWidth - 70).toFloat()
        val pkgCardWidth = (availableWidth - (cardMargin * 2)) / 3f
        val pkgCardHeight = 160f

        val tierKeys = listOf("TIER_1_ECONOMY", "TIER_2_EXECUTIVE", "TIER_3_TURNKEY")
        for (i in 0 until 3) {
            val tierKey = tierKeys[i]
            val pkg = packages.find { it.tierKey == tierKey }
            val cardX = startX + i * (pkgCardWidth + cardMargin)
            val pRect = RectF(cardX, yPos, cardX + pkgCardWidth, yPos + pkgCardHeight)

            // Highlight recommended package with copper border and tint
            val isRecommended = pkg?.isRecommended ?: (i == 1)
            if (isRecommended) {
                canvas1.drawRoundRect(pRect, 8f, 8f, copperLightPaint)
                canvas1.drawRoundRect(pRect, 8f, 8f, copperPaint.apply { strokeWidth = 2f; style = Paint.Style.STROKE })
            } else {
                canvas1.drawRoundRect(pRect, 8f, 8f, rowAltBgPaint)
                canvas1.drawRoundRect(pRect, 8f, 8f, borderPaint)
            }

            // Card Header
            val tierTitle = pkg?.title ?: when (i) {
                0 -> "Economy"
                1 -> "Executive (Best Value)"
                else -> "Turnkey Elite"
            }
            canvas1.drawText(tierTitle.take(22), cardX + 10f, yPos + 22f, textDarkBold.apply { textSize = 10f })

            if (isRecommended) {
                val badgeRect = RectF(cardX + pkgCardWidth - 78f, yPos + 8f, cardX + pkgCardWidth - 8f, yPos + 22f)
                canvas1.drawRoundRect(badgeRect, 4f, 4f, copperPaint.apply { style = Paint.Style.FILL })
                canvas1.drawText("RECOMMENDED", cardX + pkgCardWidth - 74f, yPos + 18f, textWhiteSub.apply { textSize = 7f; isFakeBoldText = true })
            }

            // Calculate price for this tier
            val tierItems = lineItems.filter { it.tierKey == tierKey || it.tierKey == "ALL" }
            val gross = tierItems.sumOf { it.quantity * it.unitSellingPrice }
            val discPct = pkg?.discountPercent ?: 0.0
            val net = gross * (1.0 - (discPct / 100.0))

            canvas1.drawText(CurrencyFormatter.formatPkr(net), cardX + 10f, yPos + 48f, textDarkBold.apply { textSize = 12.5f })
            if (discPct > 0) {
                canvas1.drawText("Includes ${discPct.toInt()}% Promotional Savings", cardX + 10f, yPos + 60f, textMuted)
            }

            // Summary notes
            val summaryText = pkg?.summary?.ifBlank { "Engineered components & certified installation" }
                ?: "Engineered components & certified installation"
            drawWrappedText(canvas1, summaryText, cardX + 10f, yPos + 76f, pkgCardWidth - 20f, textDarkRegular.apply { textSize = 8f }, 3)

            // Warranty text
            val warranty = pkg?.warrantyText ?: "Standard 1 Year Workmanship Guarantee"
            canvas1.drawText("Warranty & Support:", cardX + 10f, yPos + 124f, textDarkBold.apply { textSize = 7.5f })
            drawWrappedText(canvas1, warranty, cardX + 10f, yPos + 135f, pkgCardWidth - 20f, textMuted.apply { textSize = 7.5f }, 2)
        }

        yPos += pkgCardHeight + 25f

        // SECTION 2: APPROVED ENGINEERING ASSUMPTIONS
        canvas1.drawText("APPROVED SPECIFICATIONS & TECHNICAL ASSUMPTIONS", 35f, yPos, textDarkBold.apply { textSize = 11f })
        yPos += 14f

        val approvedAssumptions = assumptions.filter { it.isApproved }
        if (approvedAssumptions.isNotEmpty()) {
            for (assump in approvedAssumptions.take(4)) {
                canvas1.drawCircle(42f, yPos - 3f, 2.5f, copperPaint.apply { style = Paint.Style.FILL })
                canvas1.drawText(assump.assumptionText.take(85), 52f, yPos, textDarkRegular.apply { textSize = 8.5f })
                yPos += 14f
            }
        } else {
            canvas1.drawText("• Standard manufacturer guidelines and standard site access assumed.", 42f, yPos, textDarkRegular.apply { textSize = 8.5f })
            yPos += 14f
        }

        yPos += 15f

        // Terms & Acceptance box at bottom of page 1
        val termsRect = RectF(35f, yPos, (pageWidth - 35).toFloat(), yPos + 115f)
        canvas1.drawRoundRect(termsRect, 6f, 6f, rowAltBgPaint)
        canvas1.drawRoundRect(termsRect, 6f, 6f, borderPaint)

        canvas1.drawText("TERMS OF ENGAGEMENT & PAYMENT MILESTONES", 48f, yPos + 20f, textDarkBold.apply { textSize = 9.5f })
        canvas1.drawText("1. Validity: Quotation valid for 14 calendar days due to market currency and commodity fluctuations.", 48f, yPos + 35f, textMuted)
        canvas1.drawText("2. Payment Schedule: 70% advance on mobilization, 20% on delivery of primary hardware, 10% on commissioning.", 48f, yPos + 48f, textMuted)
        canvas1.drawText("3. Government Approvals: DISCO net metering and municipal approvals processed based on official authority schedules.", 48f, yPos + 61f, textMuted)

        // Signatures lines
        canvas1.drawLine(50f, yPos + 95f, 200f, yPos + 95f, borderPaint)
        canvas1.drawText("Client Acceptance Signature", 50f, yPos + 107f, textMuted)

        canvas1.drawLine((pageWidth - 210).toFloat(), yPos + 95f, (pageWidth - 60).toFloat(), yPos + 95f, borderPaint)
        canvas1.drawText("Authorized QuoteCraft Engineer", (pageWidth - 210).toFloat(), yPos + 107f, textMuted)

        // Page 1 footer
        canvas1.drawText("Page 1 of 2  •  QuoteCraft Professional Quotation Suite", 210f, (pageHeight - 20).toFloat(), textMuted)

        document.finishPage(page1)

        // ================= PAGE 2: Itemized Bill of Quantities (BOQ) =================
        val pageInfo2 = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 2).create()
        val page2 = document.startPage(pageInfo2)
        val canvas2: Canvas = page2.canvas

        // Slim Header
        canvas2.drawRect(0f, 0f, pageWidth.toFloat(), 55f, navyPaint)
        canvas2.drawRect(0f, 55f, pageWidth.toFloat(), 58f, copperPaint)
        canvas2.drawText("ITEMIZED BILL OF QUANTITIES (BOQ)", 35f, 35f, textWhiteBold.apply { textSize = 14f })
        canvas2.drawText("Quote #: $quoteNum  •  Selected Tier: ${proposal.selectedTier}", (pageWidth - 250).toFloat(), 35f, textWhiteSub)

        var boqY = 85f

        // Table Header
        val thBg = RectF(35f, boqY - 12f, (pageWidth - 35).toFloat(), boqY + 12f)
        canvas2.drawRect(thBg, navyPaint)

        canvas2.drawText("ITEM DESCRIPTION", 45f, boqY + 4f, textWhiteBold.apply { textSize = 8f })
        canvas2.drawText("CATEGORY", 280f, boqY + 4f, textWhiteBold.apply { textSize = 8f })
        canvas2.drawText("QTY", 370f, boqY + 4f, textWhiteBold.apply { textSize = 8f })
        canvas2.drawText("UNIT PRICE", 420f, boqY + 4f, textWhiteBold.apply { textSize = 8f })
        canvas2.drawText("TOTAL (PKR)", 495f, boqY + 4f, textWhiteBold.apply { textSize = 8f })

        boqY += 24f

        val activeTier = proposal.selectedTier
        val filteredItems = lineItems.filter { it.tierKey == activeTier || it.tierKey == "ALL" }

        var itemCounter = 0
        for (item in filteredItems.take(22)) { // Fits comfortably on page 2
            if (itemCounter % 2 == 1) {
                val rowRect = RectF(35f, boqY - 10f, (pageWidth - 35).toFloat(), boqY + 10f)
                canvas2.drawRect(rowRect, rowAltBgPaint)
            }

            val itemTotal = item.quantity * item.unitSellingPrice
            canvas2.drawText(item.name.take(45), 45f, boqY + 3f, textDarkRegular.apply { textSize = 8.5f })
            canvas2.drawText(item.category.displayName.take(18), 280f, boqY + 3f, textMuted.apply { textSize = 7.5f })
            val qtyStr = "${"%.1f".format(item.quantity).removeSuffix(".0")} ${item.unit}"
            canvas2.drawText(qtyStr, 370f, boqY + 3f, textDarkRegular.apply { textSize = 8f })
            canvas2.drawText(CurrencyFormatter.formatNumber(item.unitSellingPrice), 420f, boqY + 3f, textDarkRegular.apply { textSize = 8f })
            canvas2.drawText(CurrencyFormatter.formatNumber(itemTotal), 495f, boqY + 3f, textDarkBold.apply { textSize = 8.5f })

            canvas2.drawLine(35f, boqY + 11f, (pageWidth - 35).toFloat(), boqY + 11f, borderPaint)
            boqY += 20f
            itemCounter++
        }

        // Totals Box
        boqY += 15f
        val grossTotal = filteredItems.sumOf { it.quantity * it.unitSellingPrice }
        val activePkg = packages.find { it.tierKey == activeTier }
        val discountPercent = activePkg?.discountPercent ?: 0.0
        val discountAmount = grossTotal * (discountPercent / 100.0)
        val netTotal = grossTotal - discountAmount

        val summaryCardRect = RectF(320f, boqY, (pageWidth - 35).toFloat(), boqY + 80f)
        canvas2.drawRoundRect(summaryCardRect, 6f, 6f, rowAltBgPaint)
        canvas2.drawRoundRect(summaryCardRect, 6f, 6f, borderPaint)

        canvas2.drawText("Gross Subtotal:", 335f, boqY + 20f, textDarkRegular)
        canvas2.drawText(CurrencyFormatter.formatPkr(grossTotal), 460f, boqY + 20f, textDarkRegular)

        if (discountPercent > 0) {
            canvas2.drawText("Promotional Savings (${discountPercent.toInt()}%):", 335f, boqY + 38f, textMuted)
            canvas2.drawText("- ${CurrencyFormatter.formatPkr(discountAmount)}", 460f, boqY + 38f, textMuted)
        }

        canvas2.drawLine(335f, boqY + 46f, (pageWidth - 45).toFloat(), boqY + 46f, borderPaint)
        canvas2.drawText("FINAL QUOTED NET:", 335f, boqY + 66f, textDarkBold.apply { textSize = 10f })
        canvas2.drawText(CurrencyFormatter.formatPkr(netTotal), 450f, boqY + 66f, textDarkBold.apply { textSize = 11f; color = Color.parseColor("#0A192F") })

        // Page 2 footer
        canvas2.drawText("Page 2 of 2  •  Engineered & Calculated Deterministically via QuoteCraft", 160f, (pageHeight - 20).toFloat(), textMuted)

        document.finishPage(page2)

        // Write to Cache file
        val outputDir = File(context.cacheDir, "proposals").apply { mkdirs() }
        val fileName = "Quote_${quoteNum.replace("[^a-zA-Z0-9_-]".toRegex(), "_")}.pdf"
        val outputFile = File(outputDir, fileName)

        try {
            FileOutputStream(outputFile).use { out ->
                document.writeTo(out)
            }
            document.close()
            return outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            document.close()
            return null
        }
    }

    private fun drawWrappedText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        maxWidth: Float,
        paint: Paint,
        maxLines: Int
    ) {
        val words = text.split(" ")
        var line = ""
        var currentY = y
        var lineCount = 0

        for (word in words) {
            val testLine = if (line.isEmpty()) word else "$line $word"
            val width = paint.measureText(testLine)
            if (width > maxWidth) {
                canvas.drawText(line, x, currentY, paint)
                line = word
                currentY += paint.textSize + 3f
                lineCount++
                if (lineCount >= maxLines - 1) {
                    break
                }
            } else {
                line = testLine
            }
        }
        if (line.isNotEmpty() && lineCount < maxLines) {
            canvas.drawText(line, x, currentY, paint)
        }
    }
}
