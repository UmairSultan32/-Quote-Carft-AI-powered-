package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Chair
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.HomeRepairService
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import com.example.BuildConfig
import com.example.data.model.ContractorVertical
import com.example.ui.MainViewModel
import com.example.ui.theme.CopperPrimary
import com.example.ui.theme.CopperSoft
import com.example.ui.theme.CopperVibrant
import com.example.ui.theme.MarginSafeGreen
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.NavyTextLight
import com.example.ui.theme.NavyTextMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewProposalInputScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onProposalCreated: (Long) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var clientName by remember { mutableStateOf("") }
    var clientPhone by remember { mutableStateOf("") }
    var clientEmail by remember { mutableStateOf("") }
    var clientAddress by remember { mutableStateOf("") }
    var vertical by remember { mutableStateOf(ContractorVertical.SOLAR) }
    var rawNotes by remember { mutableStateOf("") }
    var useHighThinking by remember { mutableStateOf(false) }

    val isGenerating by viewModel.isGenerating.collectAsState()

    // Check live API key state
    val hasValidApiKey = try {
        val k = BuildConfig.GEMINI_API_KEY
        k.isNotBlank() && !k.contains("MY_GEMINI_API_KEY")
    } catch (e: Exception) {
        false
    }

    val sampleInputs = remember {
        listOf(
            Triple(
                ContractorVertical.SOLAR,
                "10kW Solar Net Metering (Urdu/English)",
                "10kW solar system Bahria Town Lahore. Longi 585W bifacial panels 18 adad (rate 32/watt). Inverter options: standard string vs 10kW hybrid backup. 14-gauge GI structure 95,000. Schneider breakers box 45,000. 16mm copper wire 40m 35,000. Earthing bore 2 adad 60,000. DISCO Net Metering green meter fee 70,000. Labor team 4 days 55,000. Client wants 3 tiers: economy, recommended, turnkey."
            ),
            Triple(
                ContractorVertical.RENOVATION,
                "Master Bath & Kitchen (Measurements)",
                "Master Bath 9x11 ft + Kitchen 14x10 ft. Demolition & debris removal 30,000. Plumbing PPRC & PVC replacement 45,000. Waterproofing membrane 20,000. Master tile mistri labor 45,000. Plumber sanitary labor 25,000. Spanish porcelain tiles 300 sq ft @ 550/sqft. Vanity Corian top 75,000. Provide 3 tiers with Pakistani tiles, Spanish tiles, and Italian luxury."
            ),
            Triple(
                ContractorVertical.INTERIOR,
                "Living Room False Ceiling & TV Media Wall",
                "Executive living room 24x16 ft. Saint-Gobain gypsum cove false ceiling 380 sq ft with GI channel. Warm white 3000K anti-glare COB lights 20 points @ 1,800. Mistri ceiling labor 22,000. Electrician 15,000. Feature TV media wall 14x10 ft: WPC charcoal fluted louvers vs Italian marble bookmatched slab with custom Blum push-to-open cabinets. Paint Berger velvet emulsion 40,000."
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "New Quotation Studio",
                            color = NavyTextLight,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "Parse Voice, Specs & Price Lists into 3 Packages",
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyDeep)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Setup / AI Engine Status Notice
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (hasValidApiKey) Color(0xFFECFDF5) else Color(0xFFFFFBEB)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (hasValidApiKey) Icons.Default.CheckCircle else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (hasValidApiKey) MarginSafeGreen else Color(0xFFD97706),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (hasValidApiKey)
                                    "Gemini Intelligence Ready"
                                else
                                    "Deterministic Engine Active (No API Key Required)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (hasValidApiKey) Color(0xFF065F46) else Color(0xFF92400E)
                            )
                            Text(
                                text = if (hasValidApiKey)
                                    "Calls will securely use your configured Gemini API key."
                                else
                                    "Extracts lines, 3 package tiers, and assumptions offline deterministically without pretending connections.",
                                fontSize = 11.sp,
                                color = if (hasValidApiKey) Color(0xFF047857) else Color(0xFFB45309)
                            )
                        }
                    }
                }
            }

            // Industry Sector Selection
            item {
                Text(
                    text = "CONTRACTOR INDUSTRY VERTICAL",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = vertical == ContractorVertical.SOLAR,
                        onClick = { vertical = ContractorVertical.SOLAR },
                        leadingIcon = { Icon(Icons.Default.WbSunny, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("Solar") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CopperSoft, selectedLabelColor = NavyDeep),
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = vertical == ContractorVertical.RENOVATION,
                        onClick = { vertical = ContractorVertical.RENOVATION },
                        leadingIcon = { Icon(Icons.Default.HomeRepairService, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("Renovation") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CopperSoft, selectedLabelColor = NavyDeep),
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = vertical == ContractorVertical.INTERIOR,
                        onClick = { vertical = ContractorVertical.INTERIOR },
                        leadingIcon = { Icon(Icons.Default.Chair, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("Interior") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CopperSoft, selectedLabelColor = NavyDeep),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Quick Preset Loader Chips
            item {
                Column {
                    Text(
                        text = "QUICK LOAD SAMPLE CONTRACTOR NOTES & VOICE TRANSCRIPTS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(sampleInputs) { sample ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        vertical = sample.first
                                        title = sample.second
                                        rawNotes = sample.third
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FormatQuote,
                                        contentDescription = null,
                                        tint = CopperPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = sample.second,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Client & Project Information
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "CLIENT & PROJECT DETAILS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Proposal Title / Project Name") },
                            placeholder = { Text("e.g. 10kW Solar Net Metering or Master Bath Remodel") },
                            modifier = Modifier.fillMaxWidth().testTag("new_proposal_title_input"),
                            singleLine = true
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = clientName,
                                onValueChange = { clientName = it },
                                label = { Text("Client Name") },
                                modifier = Modifier.weight(1f).testTag("new_proposal_client_name"),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = clientPhone,
                                onValueChange = { clientPhone = it },
                                label = { Text("Phone / WhatsApp") },
                                modifier = Modifier.weight(1f).testTag("new_proposal_client_phone"),
                                singleLine = true
                            )
                        }
                        OutlinedTextField(
                            value = clientAddress,
                            onValueChange = { clientAddress = it },
                            label = { Text("Site Address / City") },
                            placeholder = { Text("e.g. DHA Phase 5, Lahore") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }

            // Raw Notes / Urdu / English Voice Input Area
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "RAW CONTRACTOR INPUT / VOICE TRANSCRIPT",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Voice input",
                                    tint = CopperPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Urdu / English",
                                    fontSize = 11.sp,
                                    color = CopperPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        OutlinedTextField(
                            value = rawNotes,
                            onValueChange = { rawNotes = it },
                            placeholder = {
                                Text(
                                    "Paste contractor price list, site measurements, or Urdu/English voice note transcript here...\n\nExample:\n'10kW Bifacial panels 18 adad, 10kW hybrid inverter, 14-gauge structure 95,000, 40m copper wire, 2 earthing bores, net metering fee 70,000, labor 55,000.'"
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .testTag("raw_notes_input"),
                            maxLines = 10
                        )

                        // Thinking Mode switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = null,
                                    tint = if (useHighThinking) CopperPrimary else NavyTextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "High Thinking Mode (Gemini 3.1 Pro)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Deep engineering reasoning for complex BOQ calculations",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = useHighThinking,
                                onCheckedChange = { useHighThinking = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = CopperPrimary,
                                    checkedTrackColor = CopperSoft
                                )
                            )
                        }
                    }
                }
            }

            // Submit Button
            item {
                Button(
                    onClick = {
                        viewModel.generateProposalFromInput(
                            title = title,
                            clientName = clientName,
                            clientPhone = clientPhone,
                            clientEmail = clientEmail,
                            clientAddress = clientAddress,
                            vertical = vertical,
                            rawNotes = rawNotes.ifBlank { "Standard ${vertical.displayName} proposal with default line items" },
                            useHighThinking = useHighThinking,
                            onSuccess = { newId ->
                                onProposalCreated(newId)
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("generate_proposal_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NavyDeep),
                    enabled = !isGenerating
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            color = CopperPrimary,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Structuring 3 Packages & Margin Checks...", color = Color.White)
                    } else {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = CopperVibrant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Parse & Generate 3 Compared Packages",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
