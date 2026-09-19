package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Chair
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.HomeRepairService
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ContractorVertical
import com.example.data.model.PipelineStage
import com.example.data.model.Proposal
import com.example.ui.MainViewModel
import com.example.ui.ScreenDestination
import com.example.ui.theme.CopperPrimary
import com.example.ui.theme.CopperSoft
import com.example.ui.theme.CopperVibrant
import com.example.ui.theme.MarginSafeBg
import com.example.ui.theme.MarginSafeGreen
import com.example.ui.theme.MarginWarningAmber
import com.example.ui.theme.MarginWarningBg
import com.example.ui.theme.NavyCardDark
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.NavyMedium
import com.example.ui.theme.NavySurfaceDark
import com.example.ui.theme.NavyTextLight
import com.example.ui.theme.NavyTextMuted
import com.example.util.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceHomeScreen(
    viewModel: MainViewModel,
    onNavigateToNew: () -> Unit,
    onNavigateToProposal: (Long) -> Unit,
    onNavigateToLedger: () -> Unit,
    onNavigateToTemplates: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val proposals by viewModel.allProposals.collectAsState()
    val payments by viewModel.allPayments.collectAsState()

    var selectedVerticalFilter by remember { mutableStateOf<ContractorVertical?>(null) }
    var selectedStageFilter by remember { mutableStateOf<PipelineStage?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }

    val filteredProposals = proposals.filter { p ->
        val matchesVertical = selectedVerticalFilter == null || p.vertical == selectedVerticalFilter
        val matchesStage = selectedStageFilter == null || p.stage == selectedStageFilter
        val matchesSearch = searchQuery.isBlank() ||
                p.title.contains(searchQuery, ignoreCase = true) ||
                p.clientName.contains(searchQuery, ignoreCase = true) ||
                p.proposalNumber.contains(searchQuery, ignoreCase = true)
        matchesVertical && matchesStage && matchesSearch
    }

    // Calculations for KPIs
    val totalProposalsCount = proposals.size
    val wonProposalsCount = proposals.count { it.stage == PipelineStage.WON }
    val marginReviewCount = proposals.count { it.stage == PipelineStage.MARGIN_CHECK }
    val totalVerifiedPaymentsPkr = payments
        .filter { it.status == com.example.data.model.VerificationStatus.VERIFIED }
        .sumOf { it.amountPkr }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CopperPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "QC",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "QuoteCraft",
                                color = NavyTextLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "Contractor Proposal & Margin Studio",
                                color = CopperVibrant,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showSearch = !showSearch },
                        modifier = Modifier.testTag("home_search_toggle_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = NavyTextLight
                        )
                    }
                    IconButton(
                        onClick = onNavigateToTemplates,
                        modifier = Modifier.testTag("home_templates_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = "Templates",
                            tint = CopperPrimary
                        )
                    }
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("home_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Settings",
                            tint = NavyTextLight
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NavyDeep
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToNew,
                containerColor = CopperPrimary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = "New Quote") },
                text = { Text("Prepare Quote", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("create_proposal_fab")
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Search Bar
            item {
                AnimatedVisibility(visible = showSearch) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search client, quote #, or project...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("home_search_input"),
                            singleLine = true,
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    TextButton(onClick = { searchQuery = "" }) {
                                        Text("Clear", color = CopperPrimary)
                                    }
                                }
                            }
                        )
                    }
                }
            }

            // Executive KPI Workspace Card
            item {
                ExecutiveKpiCard(
                    totalProposals = totalProposalsCount,
                    wonProposals = wonProposalsCount,
                    marginReviewCount = marginReviewCount,
                    verifiedRevenuePkr = totalVerifiedPaymentsPkr,
                    onNavigateToLedger = onNavigateToLedger
                )
            }

            // Client Pipeline Kanban Carousel
            item {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CLIENT PIPELINE",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                        if (selectedStageFilter != null) {
                            TextButton(onClick = { selectedStageFilter = null }) {
                                Text("Reset Filter", fontSize = 12.sp, color = CopperPrimary)
                            }
                        }
                    }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(PipelineStage.values()) { stage ->
                            val stageCount = proposals.count { it.stage == stage }
                            val isSelected = selectedStageFilter == stage
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) NavyDeep else MaterialTheme.colorScheme.surface,
                                shadowElevation = 1.dp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) CopperPrimary else MaterialTheme.colorScheme.outline,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        selectedStageFilter = if (isSelected) null else stage
                                    }
                                    .testTag("stage_chip_${stage.name}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stage.displayName,
                                        fontSize = 12.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) CopperPrimary else MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = stageCount.toString(),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Industry Vertical Selector
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = selectedVerticalFilter == null,
                        onClick = { selectedVerticalFilter = null },
                        label = { Text("All Industries") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CopperSoft,
                            selectedLabelColor = NavyDeep
                        )
                    )
                    FilterChip(
                        selected = selectedVerticalFilter == ContractorVertical.SOLAR,
                        onClick = {
                            selectedVerticalFilter = if (selectedVerticalFilter == ContractorVertical.SOLAR) null else ContractorVertical.SOLAR
                        },
                        leadingIcon = { Icon(Icons.Default.WbSunny, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("Solar") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CopperSoft,
                            selectedLabelColor = NavyDeep
                        )
                    )
                    FilterChip(
                        selected = selectedVerticalFilter == ContractorVertical.RENOVATION,
                        onClick = {
                            selectedVerticalFilter = if (selectedVerticalFilter == ContractorVertical.RENOVATION) null else ContractorVertical.RENOVATION
                        },
                        leadingIcon = { Icon(Icons.Default.HomeRepairService, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("Renovation") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CopperSoft,
                            selectedLabelColor = NavyDeep
                        )
                    )
                    FilterChip(
                        selected = selectedVerticalFilter == ContractorVertical.INTERIOR,
                        onClick = {
                            selectedVerticalFilter = if (selectedVerticalFilter == ContractorVertical.INTERIOR) null else ContractorVertical.INTERIOR
                        },
                        leadingIcon = { Icon(Icons.Default.Chair, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("Interior") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CopperSoft,
                            selectedLabelColor = NavyDeep
                        )
                    )
                }
            }

            // Section Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ACTIVE PROPOSALS (${filteredProposals.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Proposals List
            if (filteredProposals.isEmpty()) {
                item {
                    EmptyProposalsCard(onNewQuote = onNavigateToNew)
                }
            } else {
                items(filteredProposals) { proposal ->
                    ProposalItemCard(
                        proposal = proposal,
                        onClick = { onNavigateToProposal(proposal.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun ExecutiveKpiCard(
    totalProposals: Int,
    wonProposals: Int,
    marginReviewCount: Int,
    verifiedRevenuePkr: Double,
    onNavigateToLedger: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NavyDeep)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "VERIFIED SERVICE REVENUE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CopperVibrant,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = CurrencyFormatter.formatPkr(verifiedRevenuePkr),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CopperPrimary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onNavigateToLedger() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = "Ledger",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Ledger",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sub-metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                KpiMetricColumn(
                    label = "Total Proposals",
                    value = totalProposals.toString(),
                    subtext = "Solar / Reno / Int"
                )
                KpiMetricColumn(
                    label = "Won & Closed",
                    value = wonProposals.toString(),
                    subtext = "Client approved"
                )
                KpiMetricColumn(
                    label = "Margin Review",
                    value = marginReviewCount.toString(),
                    subtext = "Deterministic check"
                )
            }
        }
    }
}

@Composable
fun KpiMetricColumn(label: String, value: String, subtext: String) {
    Column {
        Text(text = label, fontSize = 10.sp, color = NavyTextMuted)
        Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NavyTextLight)
        Text(text = subtext, fontSize = 9.sp, color = NavyTextMuted)
    }
}

@Composable
fun ProposalItemCard(
    proposal: Proposal,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag("proposal_card_${proposal.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header: Quote #, Vertical icon, Stage Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val (icon, tint) = when (proposal.vertical) {
                        ContractorVertical.SOLAR -> Icons.Default.WbSunny to Color(0xFFF59E0B)
                        ContractorVertical.RENOVATION -> Icons.Default.HomeRepairService to Color(0xFF0284C7)
                        ContractorVertical.INTERIOR -> Icons.Default.Chair to Color(0xFF8B5CF6)
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = proposal.proposalNumber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• v${proposal.version}",
                        fontSize = 11.sp,
                        color = CopperPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Stage Chip
                val (stageBg, stageFg) = when (proposal.stage) {
                    PipelineStage.WON -> MarginSafeBg to MarginSafeGreen
                    PipelineStage.MARGIN_CHECK -> MarginWarningBg to MarginWarningAmber
                    PipelineStage.SENT -> Color(0xFFEFF6FF) to Color(0xFF2563EB)
                    else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = stageBg
                ) {
                    Text(
                        text = proposal.stage.displayName,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = stageFg,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Proposal Title
            Text(
                text = proposal.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Client Name & Details
            Text(
                text = "Client: ${proposal.clientName}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Footer info: Target Margin & Package indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Target Margin: ${proposal.targetMarginPercent.toInt()}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = CopperPrimary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "3 Tiers Compared",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Review & Margin",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyDeep
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = NavyDeep,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyProposalsCard(onNewQuote: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Business,
                contentDescription = null,
                tint = CopperPrimary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No Proposals Found",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Convert contractor price lists, measurements, and voice notes into 3 compared packages with deterministic margin checks.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = CopperPrimary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onNewQuote() }
            ) {
                Text(
                    text = "Prepare New Proposal",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    }
}
