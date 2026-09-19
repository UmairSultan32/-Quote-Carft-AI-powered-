package com.example.data.repository

import com.example.data.local.PaymentDao
import com.example.data.local.ProposalDao
import com.example.data.local.TemplateDao
import com.example.data.model.ClientTemplate
import com.example.data.model.LineItem
import com.example.data.model.PackageOption
import com.example.data.model.PaymentLedgerItem
import com.example.data.model.Proposal
import com.example.data.model.ProposalAssumption
import com.example.data.model.ProposalVersion
import com.example.data.model.VerificationStatus
import kotlinx.coroutines.flow.Flow

class ProposalRepository(
    private val proposalDao: ProposalDao,
    private val templateDao: TemplateDao
) {
    val allProposals: Flow<List<Proposal>> = proposalDao.getAllProposals()
    val allTemplates: Flow<List<ClientTemplate>> = templateDao.getAllTemplates()

    fun getProposal(id: Long): Flow<Proposal?> = proposalDao.getProposalByIdFlow(id)
    suspend fun getProposalSync(id: Long): Proposal? = proposalDao.getProposalById(id)

    fun getPackageOptions(proposalId: Long): Flow<List<PackageOption>> =
        proposalDao.getPackageOptions(proposalId)

    suspend fun getPackageOptionsSync(proposalId: Long): List<PackageOption> =
        proposalDao.getPackageOptionsSync(proposalId)

    fun getLineItems(proposalId: Long): Flow<List<LineItem>> =
        proposalDao.getLineItems(proposalId)

    suspend fun getLineItemsSync(proposalId: Long): List<LineItem> =
        proposalDao.getLineItemsSync(proposalId)

    fun getAssumptions(proposalId: Long): Flow<List<ProposalAssumption>> =
        proposalDao.getAssumptions(proposalId)

    suspend fun getAssumptionsSync(proposalId: Long): List<ProposalAssumption> =
        proposalDao.getAssumptionsSync(proposalId)

    fun getVersions(proposalId: Long): Flow<List<ProposalVersion>> =
        proposalDao.getProposalVersions(proposalId)

    suspend fun saveProposal(proposal: Proposal): Long =
        proposalDao.insertProposal(proposal)

    suspend fun updateProposal(proposal: Proposal) =
        proposalDao.updateProposal(proposal)

    suspend fun deleteProposal(id: Long) {
        proposalDao.deleteProposal(id)
        proposalDao.deleteLineItems(id)
        proposalDao.deletePackageOptions(id)
    }

    suspend fun savePackageOptions(options: List<PackageOption>) =
        proposalDao.insertPackageOptions(options)

    suspend fun updatePackageOption(option: PackageOption) =
        proposalDao.updatePackageOption(option)

    suspend fun saveLineItem(item: LineItem): Long =
        proposalDao.insertLineItem(item)

    suspend fun saveLineItems(items: List<LineItem>) =
        proposalDao.insertLineItems(items)

    suspend fun updateLineItem(item: LineItem) =
        proposalDao.updateLineItem(item)

    suspend fun deleteLineItem(id: Long) =
        proposalDao.deleteLineItem(id)

    suspend fun saveAssumption(assumption: ProposalAssumption): Long =
        proposalDao.insertAssumption(assumption)

    suspend fun saveAssumptions(assumptions: List<ProposalAssumption>) =
        proposalDao.insertAssumptions(assumptions)

    suspend fun updateAssumption(assumption: ProposalAssumption) =
        proposalDao.updateAssumption(assumption)

    suspend fun deleteAssumption(id: Long) =
        proposalDao.deleteAssumption(id)

    suspend fun saveVersion(version: ProposalVersion) =
        proposalDao.insertProposalVersion(version)
}

class PaymentRepository(private val paymentDao: PaymentDao) {
    val allPayments: Flow<List<PaymentLedgerItem>> = paymentDao.getAllPayments()

    suspend fun recordPayment(item: PaymentLedgerItem): Long =
        paymentDao.insertPayment(item)

    suspend fun verifyPayment(id: Long, verifiedBy: String) {
        paymentDao.updatePaymentStatus(
            id = id,
            status = VerificationStatus.VERIFIED,
            verifiedAt = System.currentTimeMillis(),
            verifiedBy = verifiedBy
        )
    }

    suspend fun rejectPayment(id: Long, rejectedBy: String) {
        paymentDao.updatePaymentStatus(
            id = id,
            status = VerificationStatus.REJECTED,
            verifiedAt = System.currentTimeMillis(),
            verifiedBy = rejectedBy
        )
    }

    suspend fun deletePayment(id: Long) =
        paymentDao.deletePayment(id)
}
