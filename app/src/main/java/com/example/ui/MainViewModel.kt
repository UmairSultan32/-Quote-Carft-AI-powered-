package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.GeminiParserService
import com.example.data.ai.ParsedProposalResult
import com.example.data.local.AppDatabase
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
import com.example.data.model.ServicePackageOption
import com.example.data.model.VerificationStatus
import com.example.data.repository.PaymentRepository
import com.example.data.repository.ProposalRepository
import com.example.util.DeterministicMarginBreakdown
import com.example.util.MarginCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class ScreenDestination {
    object Home : ScreenDestination()
    data class ProposalDetail(val proposalId: Long, val initialTab: Int = 0) : ScreenDestination()
    object NewProposal : ScreenDestination()
    object Templates : ScreenDestination()
    object PaymentLedger : ScreenDestination()
    object Settings : ScreenDestination()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val proposalRepo = ProposalRepository(database.proposalDao(), database.templateDao())
    private val paymentRepo = PaymentRepository(database.paymentDao())
    private val geminiService = GeminiParserService()

    // Navigation State
    private val _currentScreen = MutableStateFlow<ScreenDestination>(ScreenDestination.Home)
    val currentScreen: StateFlow<ScreenDestination> = _currentScreen.asStateFlow()

    fun navigateTo(dest: ScreenDestination) {
        _currentScreen.value = dest
    }

    // Proposals Flow
    val allProposals: StateFlow<List<Proposal>> = proposalRepo.allProposals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Templates Flow
    val allTemplates: StateFlow<List<ClientTemplate>> = proposalRepo.allTemplates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Payments Flow
    val allPayments: StateFlow<List<PaymentLedgerItem>> = paymentRepo.allPayments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Configurable Service Packages (PKR 3,000 per proposal or PKR 10,000 for five)
    private val _servicePackages = MutableStateFlow(
        listOf(
            ServicePackageOption(
                id = "PKG_SINGLE",
                name = "Single Proposal Preparation",
                proposalCount = 1,
                pricePkr = 3000.0,
                description = "Complete proposal with 3 package options, deterministic margin check, and branded PDF export.",
                isPopular = false
            ),
            ServicePackageOption(
                id = "PKG_BUNDLE_5",
                name = "5-Proposal Agency Pack",
                proposalCount = 5,
                pricePkr = 10000.0,
                description = "Best value for active contractors (PKR 2,000/proposal). Includes revision support and priority formatting.",
                isPopular = true
            ),
            ServicePackageOption(
                id = "PKG_RETAINER_15",
                name = "Monthly Retainer (15 Proposals)",
                proposalCount = 15,
                pricePkr = 25000.0,
                description = "Dedicated quotation engineer desk, same-day turnaround, and custom branding templates.",
                isPopular = false
            )
        )
    )
    val servicePackages: StateFlow<List<ServicePackageOption>> = _servicePackages.asStateFlow()

    // Active Proposal Detail State
    private val _activeProposalId = MutableStateFlow<Long?>(null)
    val activeProposalId: StateFlow<Long?> = _activeProposalId.asStateFlow()

    private val _activeProposal = MutableStateFlow<Proposal?>(null)
    val activeProposal: StateFlow<Proposal?> = _activeProposal.asStateFlow()

    private val _activePackages = MutableStateFlow<List<PackageOption>>(emptyList())
    val activePackages: StateFlow<List<PackageOption>> = _activePackages.asStateFlow()

    private val _activeLineItems = MutableStateFlow<List<LineItem>>(emptyList())
    val activeLineItems: StateFlow<List<LineItem>> = _activeLineItems.asStateFlow()

    private val _activeAssumptions = MutableStateFlow<List<ProposalAssumption>>(emptyList())
    val activeAssumptions: StateFlow<List<ProposalAssumption>> = _activeAssumptions.asStateFlow()

    private val _activeVersions = MutableStateFlow<List<ProposalVersion>>(emptyList())
    val activeVersions: StateFlow<List<ProposalVersion>> = _activeVersions.asStateFlow()

    // Deterministic Margin State for Active Proposal
    val marginBreakdown: StateFlow<DeterministicMarginBreakdown?> = combine(
        _activeProposal,
        _activePackages,
        _activeLineItems,
        _activeAssumptions
    ) { proposal, pkgs, items, assumptions ->
        if (proposal == null) return@combine null
        val currentTier = proposal.selectedTier
        val pkg = pkgs.find { it.tierKey == currentTier }
        MarginCalculator.calculateBreakdown(
            tierKey = currentTier,
            lineItems = items,
            packageOption = pkg,
            assumptions = assumptions,
            targetMargin = proposal.targetMarginPercent
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // AI Generation / Parsing State
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _generationEngineUsed = MutableStateFlow<String?>(null)
    val generationEngineUsed: StateFlow<String?> = _generationEngineUsed.asStateFlow()

    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage.asStateFlow()

    fun clearUiMessage() {
        _uiMessage.value = null
    }

    fun loadProposal(id: Long) {
        _activeProposalId.value = id
        viewModelScope.launch {
            proposalRepo.getProposal(id).collect { p ->
                _activeProposal.value = p
            }
        }
        viewModelScope.launch {
            proposalRepo.getPackageOptions(id).collect { list ->
                _activePackages.value = list
            }
        }
        viewModelScope.launch {
            proposalRepo.getLineItems(id).collect { list ->
                _activeLineItems.value = list
            }
        }
        viewModelScope.launch {
            proposalRepo.getAssumptions(id).collect { list ->
                _activeAssumptions.value = list
            }
        }
        viewModelScope.launch {
            proposalRepo.getVersions(id).collect { list ->
                _activeVersions.value = list
            }
        }
    }

    fun setSelectedTier(tierKey: String) {
        val current = _activeProposal.value ?: return
        viewModelScope.launch {
            val updated = current.copy(selectedTier = tierKey, updatedAt = System.currentTimeMillis())
            proposalRepo.updateProposal(updated)
            _activeProposal.value = updated
        }
    }

    fun updateProposalStage(stage: PipelineStage) {
        val current = _activeProposal.value ?: return
        viewModelScope.launch {
            val updated = current.copy(stage = stage, updatedAt = System.currentTimeMillis())
            proposalRepo.updateProposal(updated)
            _activeProposal.value = updated
            _uiMessage.value = "Pipeline status moved to: ${stage.displayName}"
        }
    }

    fun setTargetMargin(targetMargin: Double) {
        val current = _activeProposal.value ?: return
        viewModelScope.launch {
            val updated = current.copy(targetMarginPercent = targetMargin, updatedAt = System.currentTimeMillis())
            proposalRepo.updateProposal(updated)
            _activeProposal.value = updated
        }
    }

    // Assumption Approval (Signature Feature!)
    fun toggleAssumptionApproval(assumption: ProposalAssumption) {
        viewModelScope.launch {
            val updated = assumption.copy(
                isApproved = !assumption.isApproved,
                approvedAt = if (!assumption.isApproved) System.currentTimeMillis() else null
            )
            proposalRepo.updateAssumption(updated)
            _uiMessage.value = if (updated.isApproved) "Assumption Approved & Added to Proposal" else "Assumption Set to Pending"
        }
    }

    fun addManualAssumption(proposalId: Long, text: String, impact: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            proposalRepo.saveAssumption(
                ProposalAssumption(
                    proposalId = proposalId,
                    assumptionText = text.trim(),
                    impactNote = impact.trim(),
                    isApproved = true,
                    approvedAt = System.currentTimeMillis()
                )
            )
            _uiMessage.value = "Specification assumption added"
        }
    }

    fun deleteAssumption(assumption: ProposalAssumption) {
        viewModelScope.launch {
            proposalRepo.deleteAssumption(assumption.id)
            _uiMessage.value = "Assumption removed"
        }
    }

    // Line Item Actions
    fun addLineItem(
        proposalId: Long,
        name: String,
        category: ItemCategory,
        tierKey: String,
        qty: Double,
        unit: String,
        cost: Double,
        sellingPrice: Double,
        notes: String
    ) {
        viewModelScope.launch {
            proposalRepo.saveLineItem(
                LineItem(
                    proposalId = proposalId,
                    tierKey = tierKey,
                    name = name.trim(),
                    category = category,
                    quantity = qty,
                    unit = unit.trim(),
                    unitCost = cost,
                    unitSellingPrice = sellingPrice,
                    notes = notes.trim()
                )
            )
            _uiMessage.value = "Line item added"
        }
    }

    fun updateLineItem(item: LineItem) {
        viewModelScope.launch {
            proposalRepo.updateLineItem(item)
            _uiMessage.value = "Item updated"
        }
    }

    fun deleteLineItem(item: LineItem) {
        viewModelScope.launch {
            proposalRepo.deleteLineItem(item.id)
            _uiMessage.value = "Item deleted"
        }
    }

    // Package Option Updates
    fun updatePackageOption(option: PackageOption) {
        viewModelScope.launch {
            proposalRepo.updatePackageOption(option)
            _uiMessage.value = "Package updated"
        }
    }

    // Version Snapshot Creation (for Version Comparison v1 vs v2)
    fun createVersionSnapshot(versionName: String = "") {
        val proposal = _activeProposal.value ?: return
        val breakdown = marginBreakdown.value ?: return
        viewModelScope.launch {
            val newVersionNumber = proposal.version + 1
            val label = versionName.ifBlank { "Revision v$newVersionNumber" }
            proposalRepo.saveVersion(
                ProposalVersion(
                    proposalId = proposal.id,
                    versionNumber = proposal.version,
                    versionName = "Snapshot v${proposal.version}",
                    totalMaterialCost = breakdown.totalMaterialCost,
                    totalLaborCost = breakdown.totalLaborCost,
                    totalCost = breakdown.grossCost,
                    netSellingPrice = breakdown.netSellingPrice,
                    grossProfit = breakdown.grossProfit,
                    marginPercent = breakdown.marginPercent,
                    timestamp = System.currentTimeMillis()
                )
            )
            val updated = proposal.copy(version = newVersionNumber, updatedAt = System.currentTimeMillis())
            proposalRepo.updateProposal(updated)
            _activeProposal.value = updated
            _uiMessage.value = "Snapshot saved as v${proposal.version}. Now editing v$newVersionNumber."
        }
    }

    // AI Proposal Generation from Raw Contractor Notes / Voice Transcripts
    fun generateProposalFromInput(
        title: String,
        clientName: String,
        clientPhone: String,
        clientEmail: String,
        clientAddress: String,
        vertical: ContractorVertical,
        rawNotes: String,
        useHighThinking: Boolean = false,
        onSuccess: (Long) -> Unit
    ) {
        viewModelScope.launch {
            _isGenerating.value = true
            try {
                val parsed: ParsedProposalResult = geminiService.parseContractorNotes(
                    notesOrTranscript = rawNotes,
                    vertical = vertical,
                    useHighThinking = useHighThinking,
                    targetMarginPct = 22.0
                )
                _generationEngineUsed.value = parsed.sourceEngine

                val proposalNum = "QC-${System.currentTimeMillis().toString().takeLast(6)}"
                val newProposal = Proposal(
                    proposalNumber = proposalNum,
                    title = if (title.isNotBlank()) title else parsed.title,
                    clientName = if (clientName.isNotBlank()) clientName else "Valued Client",
                    clientPhone = clientPhone,
                    clientEmail = clientEmail,
                    clientAddress = clientAddress,
                    vertical = vertical,
                    stage = PipelineStage.MARGIN_CHECK,
                    rawNotes = rawNotes,
                    selectedTier = PackageTier.EXECUTIVE.tierKey,
                    targetMarginPercent = 22.0,
                    version = 1
                )
                val newId = proposalRepo.saveProposal(newProposal)

                // Save packages
                val optionsWithId = parsed.packageOptions.map { it.copy(proposalId = newId) }
                proposalRepo.savePackageOptions(optionsWithId)

                // Save items
                val itemsWithId = parsed.lineItems.map { it.copy(proposalId = newId) }
                proposalRepo.saveLineItems(itemsWithId)

                // Save assumptions
                val assumptionsWithId = parsed.assumptions.map { it.copy(proposalId = newId) }
                proposalRepo.saveAssumptions(assumptionsWithId)

                _uiMessage.value = "Proposal parsed via ${parsed.sourceEngine}!"
                onSuccess(newId)
            } catch (e: Exception) {
                _uiMessage.value = "Error parsing proposal: ${e.message}"
            } finally {
                _isGenerating.value = false
            }
        }
    }

    // Delete Proposal
    fun deleteProposal(id: Long) {
        viewModelScope.launch {
            proposalRepo.deleteProposal(id)
            _uiMessage.value = "Proposal removed"
            if (_activeProposalId.value == id) {
                _currentScreen.value = ScreenDestination.Home
            }
        }
    }

    // Payment Ledger Actions
    fun recordServicePayment(
        clientName: String,
        clientPhone: String,
        servicePackage: ServicePackageOption,
        method: PaymentMethod,
        txRef: String,
        proofNotes: String
    ) {
        viewModelScope.launch {
            val item = PaymentLedgerItem(
                clientName = clientName.trim(),
                clientPhone = clientPhone.trim(),
                servicePackageName = "${servicePackage.name} (${servicePackage.proposalCount} proposals)",
                proposalCount = servicePackage.proposalCount,
                amountPkr = servicePackage.pricePkr,
                paymentMethod = method,
                transactionReference = txRef.trim(),
                proofNotes = proofNotes.trim(),
                status = VerificationStatus.PENDING,
                createdAt = System.currentTimeMillis()
            )
            paymentRepo.recordPayment(item)
            _uiMessage.value = "Service payment logged. Pending verification."
        }
    }

    fun verifyPayment(id: Long) {
        viewModelScope.launch {
            paymentRepo.verifyPayment(id, "Admin (Manual Verification)")
            _uiMessage.value = "Payment verified & reconciled in ledger"
        }
    }

    fun rejectPayment(id: Long) {
        viewModelScope.launch {
            paymentRepo.rejectPayment(id, "Admin")
            _uiMessage.value = "Payment marked as rejected"
        }
    }

    fun deletePayment(id: Long) {
        viewModelScope.launch {
            paymentRepo.deletePayment(id)
            _uiMessage.value = "Payment record deleted"
        }
    }
}
