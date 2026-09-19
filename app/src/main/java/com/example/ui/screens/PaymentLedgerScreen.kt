package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PaymentLedgerItem
import com.example.data.model.PaymentMethod
import com.example.data.model.ServicePackageOption
import com.example.data.model.VerificationStatus
import com.example.ui.MainViewModel
import com.example.ui.theme.CopperDark
import com.example.ui.theme.CopperPrimary
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentLedgerScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val payments by viewModel.allPayments.collectAsState()
    val packages by viewModel.servicePackages.collectAsState()

    var showRecordDialog by remember { mutableStateOf(false) }
    var selectedPackageForRecord by remember { mutableStateOf<ServicePackageOption?>(null) }
    var filterStatus by remember { mutableStateOf<VerificationStatus?>(null) }

    val filteredPayments = payments.filter {
        filterStatus == null || it.status == filterStatus
    }

    val totalVerified = payments
        .filter { it.status == VerificationStatus.VERIFIED }
        .sumOf { it.amountPkr }
    val totalPending = payments
        .filter { it.status == VerificationStatus.PENDING }
        .sumOf { it.amountPkr }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Service Payment Ledger",
                            color = NavyTextLight,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "Configurable Packages & Manually Verified Receipts",
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
        },
        floatingActionButton = {
            Button(
                onClick = {
                    selectedPackageForRecord = packages.firstOrNull()
                    showRecordDialog = true
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CopperPrimary),
                modifier = Modifier.testTag("record_payment_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Log Service Payment", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 90.dp, top = 16.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Hero KPI Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = NavyDeep)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "PROPOSAL PREPARATION REVENUE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CopperVibrant,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = CurrencyFormatter.formatPkr(totalVerified),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Reconciled & Verified Funds",
                                    fontSize = 10.sp,
                                    color = MarginSafeGreen
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF1E293B)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                                    Text(
                                        text = "Pending Audit",
                                        fontSize = 9.sp,
                                        color = NavyTextMuted
                                    )
                                    Text(
                                        text = CurrencyFormatter.formatPkr(totalPending),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MarginWarningAmber
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Configurable Service Packages section
            item {
                Text(
                    text = "CONFIGURABLE SERVICE PACKAGES",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
            }

            items(packages) { pkg ->
                ServicePackageCard(
                    pkg = pkg,
                    onBookPackage = {
                        selectedPackageForRecord = pkg
                        showRecordDialog = true
                    }
                )
            }

            // Ledger Transactions Section Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MANUAL PAYMENT LEDGER (${filteredPayments.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(
                            selected = filterStatus == null,
                            onClick = { filterStatus = null },
                            label = { Text("All", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = filterStatus == VerificationStatus.PENDING,
                            onClick = { filterStatus = if (filterStatus == VerificationStatus.PENDING) null else VerificationStatus.PENDING },
                            label = { Text("Pending", fontSize = 11.sp) }
                        )
                    }
                }
            }

            if (filteredPayments.isEmpty()) {
                item {
                    Text(
                        text = "No payments found for this filter.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            } else {
                items(filteredPayments) { item ->
                    PaymentItemCard(
                        item = item,
                        onVerify = { viewModel.verifyPayment(item.id) },
                        onReject = { viewModel.rejectPayment(item.id) },
                        onDelete = { viewModel.deletePayment(item.id) }
                    )
                }
            }
        }
    }

    // Record Payment Dialog
    if (showRecordDialog) {
        RecordPaymentDialog(
            packages = packages,
            initialPackage = selectedPackageForRecord ?: packages.first(),
            onDismiss = { showRecordDialog = false },
            onConfirm = { clientName, phone, selectedPkg, method, ref, notes ->
                viewModel.recordServicePayment(clientName, phone, selectedPkg, method, ref, notes)
                showRecordDialog = false
            }
        )
    }
}

@Composable
fun ServicePackageCard(
    pkg: ServicePackageOption,
    onBookPackage: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (pkg.isPopular) Color(0xFFFDF8F5) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = pkg.name,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (pkg.isPopular) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = CopperPrimary
                    ) {
                        Text(
                            text = "POPULAR CHOICE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = CurrencyFormatter.formatPkr(pkg.pricePkr),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = NavyDeep
                    )
                    Text(
                        text = "${pkg.proposalCount} Comprehensive Proposals",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onBookPackage,
                    colors = ButtonDefaults.buttonColors(containerColor = CopperPrimary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Log Order", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = pkg.description,
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PaymentItemCard(
    item: PaymentLedgerItem,
    onVerify: () -> Unit,
    onReject: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date(item.createdAt))

    val (badgeBg, badgeFg, badgeText) = when (item.status) {
        VerificationStatus.VERIFIED -> Triple(MarginSafeBg, MarginSafeGreen, "VERIFIED")
        VerificationStatus.PENDING -> Triple(MarginWarningBg, MarginWarningAmber, "PENDING AUDIT")
        VerificationStatus.REJECTED -> Triple(MarginCriticalBg, MarginCriticalRed, "REJECTED")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = item.clientName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = item.clientPhone.ifBlank { "Direct Client" },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeBg
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeFg,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Package Ordered", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = item.servicePackageName, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Amount Paid", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = CurrencyFormatter.formatPkr(item.amountPkr),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyDeep
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Method and Reference
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Via: ${item.paymentMethod.displayName} • Ref: ${item.transactionReference.ifBlank { "N/A" }}",
                    fontSize = 11.sp,
                    color = CopperDark,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = dateStr,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (item.proofNotes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Proof Note: ${item.proofNotes}",
                    fontSize = 10.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Action Buttons for Verification
            if (item.status == VerificationStatus.PENDING) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onReject) {
                        Text("Reject Proof", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onVerify,
                        colors = ButtonDefaults.buttonColors(containerColor = MarginSafeGreen),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Verify & Reconcile", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun RecordPaymentDialog(
    packages: List<ServicePackageOption>,
    initialPackage: ServicePackageOption,
    onDismiss: () -> Unit,
    onConfirm: (String, String, ServicePackageOption, PaymentMethod, String, String) -> Unit
) {
    var clientName by remember { mutableStateOf("") }
    var clientPhone by remember { mutableStateOf("") }
    var selectedPkg by remember { mutableStateOf<ServicePackageOption>(initialPackage) }
    var method by remember { mutableStateOf<PaymentMethod>(PaymentMethod.BANK_TRANSFER) }
    var txRef by remember { mutableStateOf("") }
    var proofNotes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Service Payment Proof", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    OutlinedTextField(
                        value = clientName,
                        onValueChange = { clientName = it },
                        label = { Text("Contractor / Client Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = clientPhone,
                        onValueChange = { clientPhone = it },
                        label = { Text("WhatsApp / Phone") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    Text("Select Service Package:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    packages.forEach { pkg ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPkg = pkg }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.RadioButton(
                                selected = selectedPkg.id == pkg.id,
                                onClick = { selectedPkg = pkg },
                                colors = androidx.compose.material3.RadioButtonDefaults.colors(selectedColor = CopperPrimary)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${pkg.name} (${CurrencyFormatter.formatPkr(pkg.pricePkr)})",
                                fontSize = 12.sp
                            )
                        }
                    }
                }
                item {
                    Text("Payment Method:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        PaymentMethod.values().forEach { m ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { method = m }
                                    .padding(vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                androidx.compose.material3.RadioButton(
                                    selected = method == m,
                                    onClick = { method = m },
                                    colors = androidx.compose.material3.RadioButtonDefaults.colors(selectedColor = CopperPrimary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = m.displayName, fontSize = 12.sp)
                            }
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = txRef,
                        onValueChange = { txRef = it },
                        label = { Text("Bank / Raast / Trx ID") },
                        placeholder = { Text("e.g. PK78MEZN0001928374") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = proofNotes,
                        onValueChange = { proofNotes = it },
                        label = { Text("Proof Verification Notes") },
                        placeholder = { Text("e.g. Screenshot confirmed via WhatsApp") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (clientName.isNotBlank()) {
                        onConfirm(clientName, clientPhone, selectedPkg, method, txRef, proofNotes)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CopperPrimary)
            ) {
                Text("Log Payment Record")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
