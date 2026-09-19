package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ItemCategory
import com.example.data.model.LineItem
import com.example.data.model.PackageOption
import com.example.data.model.PackageTier
import com.example.data.model.PipelineStage
import com.example.data.model.ProposalAssumption
import com.example.ui.MainViewModel
import com.example.ui.theme.CopperDark
import com.example.ui.theme.CopperLight
import com.example.ui.theme.CopperPrimary
import com.example.ui.theme.CopperSoft
import com.example.ui.theme.CopperVibrant
import com.example.ui.theme.MarginCriticalBg
import com.example.ui.theme.MarginCriticalRed
import com.example.ui.theme.MarginSafeBg
import com.example.ui.theme.MarginSafeGreen
import com.example.ui.theme.MarginWarningAmber
import com.example.ui.theme.MarginWarningBg
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.NavyTextLight
import com.example.ui.theme.NavyTextMuted
import com.example.util.CurrencyFormatter
import com.example.util.DeterministicMarginBreakdown
import com.example.util.MissingCostFlag
import com.example.util.PdfExporter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProposalEditorScreen(
    proposalId: Long,
    viewModel: MainViewModel,
    initialTab: Int = 0,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(proposalId) {
        viewModel.loadProposal(proposalId)
    }

    val proposal by viewModel.activeProposal.collectAsState()
    val packages by viewModel.activePackages.collectAsState()
    val lineItems by viewModel.activeLineItems.collectAsState()
    val assumptions by viewModel.activeAssumptions.collectAsState()
    val versions by viewModel.activeVersions.collectAsState()
    val marginBreakdown by viewModel.marginBreakdown.collectAsState()
    val uiMessage by viewModel.uiMessage.collectAsState()

    var selectedTab by remember { mutableIntStateOf(initialTab) }
    val tabTitles = listOf("Margin Checker", "3 Packages", "Itemized BOQ", "Versions", "PDF Export")

    var showAddItemDialog by remember { mutableStateOf(false) }
    var showAddAssumptionDialog by remember { mutableStateOf(false) }
    var showStageMenu by remember { mutableStateOf(false) }

    LaunchedEffect(uiMessage) {
        uiMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearUiMessage()
        }
    }

    if (proposal == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading proposal...")
        }
        return
    }

    val currentProp = proposal!!

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = currentProp.title.take(28),
                                color = NavyTextLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = CopperPrimary
                            ) {
                                Text(
                                    text = "v${currentProp.version}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "${currentProp.proposalNumber} • ${currentProp.clientName}",
                            color = CopperVibrant,
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = NavyTextLight
                        )
                    }
                },
                actions = {
                    // Stage selector button
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { showStageMenu = true }
                    ) {
                        Text(
                            text = currentProp.stage.displayName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyDeep,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyDeep)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Stage Selection Dropdown Dialog
            if (showStageMenu) {
                AlertDialog(
                    onDismissRequest = { showStageMenu = false },
                    title = { Text("Update Pipeline Stage", fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            PipelineStage.values().forEach { stage ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.updateProposalStage(stage)
                                            showStageMenu = false
                                        }
                                        .padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = currentProp.stage == stage,
                                        onClick = {
                                            viewModel.updateProposalStage(stage)
                                            showStageMenu = false
                                        },
                                        colors = RadioButtonDefaults.colors(selectedColor = CopperPrimary)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stage.displayName,
                                        fontWeight = if (currentProp.stage == stage) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showStageMenu = false }) { Text("Close") }
                    }
                )
            }

            // Tab Navigation
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = NavyDeep,
                contentColor = Color.White,
                edgePadding = 12.dp
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) CopperLight else NavyTextLight,
                                fontSize = 12.5.sp
                            )
                        },
                        modifier = Modifier.testTag("tab_${title.lowercase().replace(" ", "_")}")
                    )
                }
            }

            // Tab Contents
            when (selectedTab) {
                0 -> MarginCheckerTab(
                    marginBreakdown = marginBreakdown,
                    targetMargin = currentProp.targetMarginPercent,
                    assumptions = assumptions,
                    selectedTier = currentProp.selectedTier,
                    onTierSelected = { viewModel.setSelectedTier(it) },
                    onTargetMarginChanged = { viewModel.setTargetMargin(it) },
                    onToggleAssumption = { viewModel.toggleAssumptionApproval(it) },
                    onAddAssumption = { showAddAssumptionDialog = true },
                    onDeleteAssumption = { viewModel.deleteAssumption(it) }
                )
                1 -> ComparedPackagesTab(
                    packages = packages,
                    lineItems = lineItems,
                    selectedTier = currentProp.selectedTier,
                    onSelectTier = { viewModel.setSelectedTier(it) },
                    onUpdatePackage = { viewModel.updatePackageOption(it) }
                )
                2 -> ItemizedBoqTab(
                    lineItems = lineItems,
                    activeTier = currentProp.selectedTier,
                    onAddItem = { showAddItemDialog = true },
                    onDeleteItem = { viewModel.deleteLineItem(it) }
                )
                3 -> VersionsTab(
                    versions = versions,
                    currentProposal = currentProp,
                    marginBreakdown = marginBreakdown,
                    onSaveSnapshot = { viewModel.createVersionSnapshot() }
                )
                4 -> PdfExportTab(
                    proposal = currentProp,
                    packages = packages,
                    lineItems = lineItems,
                    assumptions = assumptions,
                    marginBreakdown = marginBreakdown
                )
            }
        }
    }

    // Add Line Item Dialog
    if (showAddItemDialog) {
        AddLineItemDialog(
            proposalId = proposalId,
            currentTier = currentProp.selectedTier,
            onDismiss = { showAddItemDialog = false },
            onConfirm = { name, cat, tier, qty, unit, cost, price, notes ->
                viewModel.addLineItem(proposalId, name, cat, tier, qty, unit, cost, price, notes)
                showAddItemDialog = false
            }
        )
    }

    // Add Assumption Dialog
    if (showAddAssumptionDialog) {
        AddAssumptionDialog(
            onDismiss = { showAddAssumptionDialog = false },
            onConfirm = { text, impact ->
                viewModel.addManualAssumption(proposalId, text, impact)
                showAddAssumptionDialog = false
            }
        )
    }
}

// ==========================================
// 1. MARGIN CHECKER TAB (Signature Feature)
// ==========================================
@Composable
fun MarginCheckerTab(
    marginBreakdown: DeterministicMarginBreakdown?,
    targetMargin: Double,
    assumptions: List<ProposalAssumption>,
    selectedTier: String,
    onTierSelected: (String) -> Unit,
    onTargetMarginChanged: (Double) -> Unit,
    onToggleAssumption: (ProposalAssumption) -> Unit,
    onAddAssumption: () -> Unit,
    onDeleteAssumption: (ProposalAssumption) -> Unit
) {
    if (marginBreakdown == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Calculating deterministic margins...")
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Active Tier Selector for Margin Analysis
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "ACTIVE PRICING TIER ANALYZED",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            PackageTier.ECONOMY.tierKey to "Economy",
                            PackageTier.EXECUTIVE.tierKey to "Executive",
                            PackageTier.TURNKEY.tierKey to "Turnkey"
                        ).forEach { (tierKey, label) ->
                            val isSelected = selectedTier == tierKey
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) NavyDeep else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onTierSelected(tierKey) }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }

        // Primary Margin Metric Card (Hero)
        item {
            val (healthColor, healthBg, healthLabel) = when (marginBreakdown.marginHealth) {
                DeterministicMarginBreakdown.MarginHealth.EXCELLENT -> Triple(MarginSafeGreen, MarginSafeBg, "Optimal Margin (Target Exceeded)")
                DeterministicMarginBreakdown.MarginHealth.HEALTHY -> Triple(MarginSafeGreen, MarginSafeBg, "Healthy Margin (Target Met)")
                DeterministicMarginBreakdown.MarginHealth.LOW_RISK -> Triple(MarginWarningAmber, MarginWarningBg, "Marginal Risk (Slightly Below Target)")
                DeterministicMarginBreakdown.MarginHealth.DANGEROUS -> Triple(MarginCriticalRed, MarginCriticalBg, "High Risk Margin (Low Buffer)")
                DeterministicMarginBreakdown.MarginHealth.LOSS -> Triple(MarginCriticalRed, MarginCriticalBg, "Selling at Loss!")
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NavyDeep)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "DETERMINISTIC NET MARGIN",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CopperVibrant,
                                letterSpacing = 1.sp
                            )
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "%.1f%%".format(marginBreakdown.marginPercent),
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Markup: %.1f%%".format(marginBreakdown.markupPercent),
                                    fontSize = 12.sp,
                                    color = NavyTextMuted,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = healthBg
                        ) {
                            Text(
                                text = healthLabel,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = healthColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Color(0xFF2B4168))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Profit and Revenue Breakdown
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Contractor Net Price", fontSize = 10.sp, color = NavyTextMuted)
                            Text(
                                text = CurrencyFormatter.formatPkr(marginBreakdown.netSellingPrice),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = NavyTextLight
                            )
                        }
                        Column {
                            Text(text = "Total Direct Costs", fontSize = 10.sp, color = NavyTextMuted)
                            Text(
                                text = CurrencyFormatter.formatPkr(marginBreakdown.grossCost),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = NavyTextLight
                            )
                        }
                        Column {
                            Text(text = "Net Profit (PKR)", fontSize = 10.sp, color = NavyTextMuted)
                            Text(
                                text = CurrencyFormatter.formatPkr(marginBreakdown.grossProfit),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (marginBreakdown.grossProfit >= 0) CopperLight else MarginCriticalRed
                            )
                        }
                    }
                }
            }
        }

        // Cost Category Breakdown Bar & Values
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "DIRECT COST ALLOCATION (PKR)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    CostItemRow(
                        label = "Materials & Hardware",
                        cost = marginBreakdown.totalMaterialCost,
                        totalCost = marginBreakdown.grossCost,
                        accentColor = Color(0xFF0284C7)
                    )
                    CostItemRow(
                        label = "Labor & Craftsmanship",
                        cost = marginBreakdown.totalLaborCost,
                        totalCost = marginBreakdown.grossCost,
                        accentColor = Color(0xFFD97706)
                    )
                    CostItemRow(
                        label = "Machinery & Equipment",
                        cost = marginBreakdown.totalEquipmentCost,
                        totalCost = marginBreakdown.grossCost,
                        accentColor = Color(0xFF8B5CF6)
                    )
                    CostItemRow(
                        label = "Logistics, DISCO & Transport",
                        cost = marginBreakdown.totalLogisticsCost,
                        totalCost = marginBreakdown.grossCost,
                        accentColor = Color(0xFF10B981)
                    )
                }
            }
        }

        // Missing Cost & Risk Flags
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MISSING COST AUDIT & ALERTS (${marginBreakdown.flags.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (marginBreakdown.flags.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MarginSafeBg,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MarginSafeGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "All critical costs accounted for! No missing labor, zero-cost lines, or unapproved assumptions detected.",
                                fontSize = 12.sp,
                                color = Color(0xFF065F46),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    marginBreakdown.flags.forEach { flag ->
                        MissingCostAlertCard(flag)
                    }
                }
            }
        }

        // AI ASSUMPTION APPROVAL SECTION (MANDATORY APPROVAL RULE)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "AI & TECHNICAL ASSUMPTIONS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "MANDATORY: Approval required before proposal export",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CopperPrimary
                            )
                        }
                        IconButton(onClick = onAddAssumption) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Assumption",
                                tint = CopperPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (assumptions.isEmpty()) {
                        Text(
                            text = "No assumptions recorded.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        assumptions.forEach { assumption ->
                            AssumptionApprovalItem(
                                assumption = assumption,
                                onToggle = { onToggleAssumption(assumption) },
                                onDelete = { onDeleteAssumption(assumption) }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }

        // Target Margin Adjuster Slider
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "CONTRACTOR TARGET MARGIN",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${targetMargin.toInt()}%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = CopperPrimary
                        )
                    }
                    Slider(
                        value = targetMargin.toFloat(),
                        onValueChange = { onTargetMarginChanged(it.toDouble()) },
                        valueRange = 10f..45f,
                        steps = 7,
                        colors = SliderDefaults.colors(
                            thumbColor = CopperPrimary,
                            activeTrackColor = CopperPrimary
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf(15.0, 20.0, 25.0, 30.0).forEach { marginPreset ->
                            TextButton(onClick = { onTargetMarginChanged(marginPreset) }) {
                                Text("${marginPreset.toInt()}%", fontSize = 11.sp, color = NavyDeep)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CostItemRow(label: String, cost: Double, totalCost: Double, accentColor: Color) {
    val pct = if (totalCost > 0) (cost / totalCost) * 100.0 else 0.0
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
            }
            Row {
                Text(
                    text = CurrencyFormatter.formatPkr(cost),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "(%.0f%%)".format(pct),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun MissingCostAlertCard(flag: MissingCostFlag) {
    val (bg, fg, icon) = when (flag.severity) {
        MissingCostFlag.Severity.CRITICAL -> Triple(MarginCriticalBg, MarginCriticalRed, Icons.Default.Error)
        MissingCostFlag.Severity.WARNING -> Triple(MarginWarningBg, MarginWarningAmber, Icons.Default.Warning)
        MissingCostFlag.Severity.INFO -> Triple(Color(0xFFEFF6FF), Color(0xFF2563EB), Icons.Default.Tune)
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = bg,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = flag.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = fg
                )
                Text(
                    text = flag.description,
                    fontSize = 11.sp,
                    color = fg.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
fun AssumptionApprovalItem(
    assumption: ProposalAssumption,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (assumption.isApproved) MarginSafeBg else MarginWarningBg,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (assumption.isApproved) Color(0xFF6EE7B7) else Color(0xFFFCD34D),
                shape = RoundedCornerShape(10.dp)
            )
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = assumption.assumptionText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (assumption.impactNote.isNotBlank()) {
                    Text(
                        text = "Cost Impact: ${assumption.impactNote}",
                        fontSize = 10.5.sp,
                        color = if (assumption.isApproved) Color(0xFF047857) else Color(0xFFB45309)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Approval Toggle Button
            Button(
                onClick = onToggle,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (assumption.isApproved) MarginSafeGreen else CopperPrimary
                ),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.testTag("toggle_assumption_${assumption.id}")
            ) {
                if (assumption.isApproved) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Approved",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Approved", fontSize = 11.sp, color = Color.White)
                } else {
                    Text("Approve", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ==========================================
// 2. COMPARED PACKAGES TAB
// ==========================================
@Composable
fun ComparedPackagesTab(
    packages: List<PackageOption>,
    lineItems: List<LineItem>,
    selectedTier: String,
    onSelectTier: (String) -> Unit,
    onUpdatePackage: (PackageOption) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "THREE COMPARED PACKAGE TIERS",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
            Text(
                text = "Compare Economy, Recommended Executive, and Turnkey options side-by-side with distinct pricing and warranties.",
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(packages) { pkg ->
            val isSelected = selectedTier == pkg.tierKey
            val tierItems = lineItems.filter { it.tierKey == pkg.tierKey || it.tierKey == "ALL" }
            val gross = tierItems.sumOf { it.quantity * it.unitSellingPrice }
            val discountAmt = gross * (pkg.discountPercent / 100.0)
            val net = gross - discountAmt

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) CopperPrimary else MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(14.dp)
                    ),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) Color(0xFFFDF8F5) else MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { onSelectTier(pkg.tierKey) },
                                colors = RadioButtonDefaults.colors(selectedColor = CopperPrimary)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = pkg.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Tier: ${pkg.tierKey}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (pkg.isRecommended) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = CopperPrimary
                            ) {
                                Text(
                                    text = "RECOMMENDED",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Price display
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(text = "Net Package Price", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = CurrencyFormatter.formatPkr(net),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = NavyDeep
                            )
                        }
                        if (pkg.discountPercent > 0) {
                            Text(
                                text = "${pkg.discountPercent.toInt()}% Promo Discount",
                                fontSize = 11.sp,
                                color = MarginSafeGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = pkg.summary,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Warranty: ${pkg.warrantyText}",
                        fontSize = 11.sp,
                        color = CopperDark,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { onSelectTier(pkg.tierKey) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (isSelected) "Active Selected Package" else "Select as Primary Proposal Package")
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. ITEMIZED BOQ TAB
// ==========================================
@Composable
fun ItemizedBoqTab(
    lineItems: List<LineItem>,
    activeTier: String,
    onAddItem: () -> Unit,
    onDeleteItem: (LineItem) -> Unit
) {
    val tierItems = lineItems.filter { it.tierKey == activeTier || it.tierKey == "ALL" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ITEMIZED BILL OF QUANTITIES",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${tierItems.size} line items for current tier",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onAddItem,
                    colors = ButtonDefaults.buttonColors(containerColor = CopperPrimary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Item", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (tierItems.isEmpty()) {
            item {
                Text(
                    text = "No line items found for this package. Click 'Add Item' to create one.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
        } else {
            items(tierItems) { item ->
                LineItemCard(item = item, onDelete = { onDeleteItem(item) })
            }
        }
    }
}

@Composable
fun LineItemCard(item: LineItem, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${item.category.displayName} • ${item.quantity.toInt()} ${item.unit}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Unit Cost", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = CurrencyFormatter.formatPkr(item.unitCost),
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column {
                    Text(text = "Quoted Unit Price", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = CurrencyFormatter.formatPkr(item.unitSellingPrice),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Line Total", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = CurrencyFormatter.formatPkr(item.totalSellingPrice),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyDeep
                    )
                }
            }
        }
    }
}

// ==========================================
// 4. VERSIONS TAB (Version Comparison v1 vs v2)
// ==========================================
@Composable
fun VersionsTab(
    versions: List<com.example.data.model.ProposalVersion>,
    currentProposal: com.example.data.model.Proposal,
    marginBreakdown: DeterministicMarginBreakdown?,
    onSaveSnapshot: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = NavyDeep)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "CURRENT REVISION: v${currentProposal.version}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Save snapshots before applying client revision negotiations",
                                fontSize = 11.sp,
                                color = CopperLight
                            )
                        }
                        Button(
                            onClick = onSaveSnapshot,
                            colors = ButtonDefaults.buttonColors(containerColor = CopperPrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save v${currentProposal.version} Snapshot", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "VERSION HISTORY & COMPARISON",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (versions.isEmpty()) {
            item {
                Text(
                    text = "No earlier version snapshots recorded yet. When the client requests scope modifications, click 'Save Snapshot' to archive v${currentProposal.version} and track margin variations.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        } else {
            items(versions) { ver ->
                val currentNet = marginBreakdown?.netSellingPrice ?: 0.0
                val priceDiff = currentNet - ver.netSellingPrice
                val currentMargin = marginBreakdown?.marginPercent ?: 0.0
                val marginDiff = currentMargin - ver.marginPercent

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Archived Snapshot v${ver.versionNumber}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Margin: %.1f%%".format(ver.marginPercent),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = CopperPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "Snapshot Price", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = CurrencyFormatter.formatPkr(ver.netSellingPrice), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Column {
                                Text(text = "Total Cost", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = CurrencyFormatter.formatPkr(ver.totalCost), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "Delta vs Current", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = if (priceDiff >= 0) "+${CurrencyFormatter.formatPkr(priceDiff)}" else CurrencyFormatter.formatPkr(priceDiff),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (priceDiff >= 0) MarginSafeGreen else MarginCriticalRed
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. PDF EXPORT & SHARE TAB
// ==========================================
@Composable
fun PdfExportTab(
    proposal: com.example.data.model.Proposal,
    packages: List<PackageOption>,
    lineItems: List<LineItem>,
    assumptions: List<ProposalAssumption>,
    marginBreakdown: DeterministicMarginBreakdown?
) {
    val context = LocalContext.current
    val canExport = marginBreakdown?.canExportPdf == true

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NavyDeep)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = CopperVibrant,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Branded PDF Proposal Export",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "2-Page Official Proposal with 3-Package Comparison & BOQ",
                                fontSize = 11.sp,
                                color = CopperLight
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Pre-export audit checks
                    Text(
                        text = "PRE-EXPORT AUDIT CHECKS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CopperVibrant,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val unapprovedCount = assumptions.count { !it.isApproved }
                    AuditCheckRow(
                        label = "All AI Assumptions Reviewed & Approved",
                        passed = unapprovedCount == 0,
                        detail = if (unapprovedCount == 0) "All approved" else "$unapprovedCount unapproved pending"
                    )
                    val hasCriticalFlag = marginBreakdown?.flags?.any { it.severity == MissingCostFlag.Severity.CRITICAL } ?: false
                    AuditCheckRow(
                        label = "Zero-Cost & Negative Margin Audit",
                        passed = !hasCriticalFlag,
                        detail = if (!hasCriticalFlag) "Cleared" else "Critical cost warnings present"
                    )
                    AuditCheckRow(
                        label = "Line Items & Quantities Defined",
                        passed = lineItems.isNotEmpty(),
                        detail = "${lineItems.size} items present"
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (!canExport) {
                                Toast.makeText(
                                    context,
                                    "Please resolve critical flags and approve all assumptions before exporting.",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                val file = PdfExporter.generateAndShareProposalPdf(
                                    context = context,
                                    proposal = proposal,
                                    packages = packages,
                                    lineItems = lineItems,
                                    assumptions = assumptions,
                                    marginBreakdown = marginBreakdown
                                )
                                if (file != null) {
                                    Toast.makeText(context, "Opening share menu for PDF...", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Failed to generate PDF", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("export_pdf_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (canExport) CopperPrimary else Color(0xFF475569)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (canExport) "Generate & Share PDF Proposal" else "Resolve Audit Items to Export",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Preview Highlights Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "PDF DOCUMENT PREVIEW STRUCTURE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "• Page 1: Branded Navy & Copper Header with Quote #${proposal.proposalNumber}", fontSize = 12.sp)
                    Text(text = "• Page 1: Side-by-side 3 Packages Comparison Table with warranties", fontSize = 12.sp)
                    Text(text = "• Page 1: Explicit approved technical assumptions & engagement terms", fontSize = 12.sp)
                    Text(text = "• Page 2: Itemized Bill of Quantities (BOQ) with materials, labor & total PKR", fontSize = 12.sp)
                    Text(text = "• Compatible with WhatsApp, Gmail, Drive & Print services via Android Share", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun AuditCheckRow(label: String, passed: Boolean, detail: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (passed) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = null,
                tint = if (passed) MarginSafeGreen else MarginCriticalRed,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = label, fontSize = 11.sp, color = NavyTextLight)
        }
        Text(
            text = detail,
            fontSize = 10.sp,
            color = if (passed) MarginSafeGreen else MarginWarningAmber,
            fontWeight = FontWeight.Medium
        )
    }
}

// Dialog: Add Line Item
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLineItemDialog(
    proposalId: Long,
    currentTier: String,
    onDismiss: () -> Unit,
    onConfirm: (String, ItemCategory, String, Double, String, Double, Double, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(ItemCategory.MATERIAL) }
    var tierKey by remember { mutableStateOf("ALL") }
    var qtyStr by remember { mutableStateOf("1") }
    var unit by remember { mutableStateOf("Unit") }
    var costStr by remember { mutableStateOf("") }
    var priceStr by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Line Item", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Item Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    Text("Category:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ItemCategory.values().forEach { cat ->
                            val isSelected = category == cat
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) CopperPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { category = cat }
                            ) {
                                Text(
                                    text = cat.displayName.take(5),
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = qtyStr,
                            onValueChange = { qtyStr = it },
                            label = { Text("Qty") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = unit,
                            onValueChange = { unit = it },
                            label = { Text("Unit") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = costStr,
                            onValueChange = { costStr = it },
                            label = { Text("Cost (PKR)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = priceStr,
                            onValueChange = { priceStr = it },
                            label = { Text("Selling (PKR)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes / Spec") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = qtyStr.toDoubleOrNull() ?: 1.0
                    val cost = costStr.toDoubleOrNull() ?: 0.0
                    val price = priceStr.toDoubleOrNull() ?: cost
                    if (name.isNotBlank()) {
                        onConfirm(name, category, tierKey, qty, unit, cost, price, notes)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CopperPrimary)
            ) {
                Text("Add Item")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// Dialog: Add Assumption
@Composable
fun AddAssumptionDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var impact by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Specification Assumption", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Assumption Statement") },
                    placeholder = { Text("e.g. Standard RCC roof slab with South exposure") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = impact,
                    onValueChange = { impact = it },
                    label = { Text("Cost / Scope Impact") },
                    placeholder = { Text("e.g. If pitched roof, adds PKR 35k for clamps") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (text.isNotBlank()) {
                        onConfirm(text, impact)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CopperPrimary)
            ) {
                Text("Add Assumption")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
