package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ClientTemplate
import com.example.data.model.LineItem
import com.example.data.model.PackageOption
import com.example.data.model.PaymentLedgerItem
import com.example.data.model.Proposal
import com.example.data.model.ProposalAssumption
import com.example.data.model.ProposalVersion
import com.example.data.model.VerificationStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ProposalDao {
    @Query("SELECT * FROM proposals ORDER BY updatedAt DESC")
    fun getAllProposals(): Flow<List<Proposal>>

    @Query("SELECT * FROM proposals WHERE id = :id LIMIT 1")
    fun getProposalByIdFlow(id: Long): Flow<Proposal?>

    @Query("SELECT * FROM proposals WHERE id = :id LIMIT 1")
    suspend fun getProposalById(id: Long): Proposal?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProposal(proposal: Proposal): Long

    @Update
    suspend fun updateProposal(proposal: Proposal)

    @Query("DELETE FROM proposals WHERE id = :id")
    suspend fun deleteProposal(id: Long)

    // Package Options
    @Query("SELECT * FROM package_options WHERE proposalId = :proposalId")
    fun getPackageOptions(proposalId: Long): Flow<List<PackageOption>>

    @Query("SELECT * FROM package_options WHERE proposalId = :proposalId")
    suspend fun getPackageOptionsSync(proposalId: Long): List<PackageOption>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPackageOptions(options: List<PackageOption>)

    @Update
    suspend fun updatePackageOption(option: PackageOption)

    @Query("DELETE FROM package_options WHERE proposalId = :proposalId")
    suspend fun deletePackageOptions(proposalId: Long)

    // Line Items
    @Query("SELECT * FROM line_items WHERE proposalId = :proposalId")
    fun getLineItems(proposalId: Long): Flow<List<LineItem>>

    @Query("SELECT * FROM line_items WHERE proposalId = :proposalId")
    suspend fun getLineItemsSync(proposalId: Long): List<LineItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLineItems(items: List<LineItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLineItem(item: LineItem): Long

    @Update
    suspend fun updateLineItem(item: LineItem)

    @Query("DELETE FROM line_items WHERE id = :id")
    suspend fun deleteLineItem(id: Long)

    @Query("DELETE FROM line_items WHERE proposalId = :proposalId")
    suspend fun deleteLineItems(proposalId: Long)

    // Assumptions
    @Query("SELECT * FROM proposal_assumptions WHERE proposalId = :proposalId")
    fun getAssumptions(proposalId: Long): Flow<List<ProposalAssumption>>

    @Query("SELECT * FROM proposal_assumptions WHERE proposalId = :proposalId")
    suspend fun getAssumptionsSync(proposalId: Long): List<ProposalAssumption>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssumptions(assumptions: List<ProposalAssumption>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssumption(assumption: ProposalAssumption): Long

    @Update
    suspend fun updateAssumption(assumption: ProposalAssumption)

    @Query("DELETE FROM proposal_assumptions WHERE id = :id")
    suspend fun deleteAssumption(id: Long)

    // Versions
    @Query("SELECT * FROM proposal_versions WHERE proposalId = :proposalId ORDER BY versionNumber DESC")
    fun getProposalVersions(proposalId: Long): Flow<List<ProposalVersion>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProposalVersion(version: ProposalVersion): Long
}

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payment_ledger ORDER BY createdAt DESC")
    fun getAllPayments(): Flow<List<PaymentLedgerItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(item: PaymentLedgerItem): Long

    @Query("UPDATE payment_ledger SET status = :status, verifiedAt = :verifiedAt, verifiedBy = :verifiedBy WHERE id = :id")
    suspend fun updatePaymentStatus(id: Long, status: VerificationStatus, verifiedAt: Long?, verifiedBy: String)

    @Query("DELETE FROM payment_ledger WHERE id = :id")
    suspend fun deletePayment(id: Long)
}

@Dao
interface TemplateDao {
    @Query("SELECT * FROM client_templates ORDER BY id ASC")
    fun getAllTemplates(): Flow<List<ClientTemplate>>

    @Query("SELECT COUNT(*) FROM client_templates")
    suspend fun getTemplateCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplates(templates: List<ClientTemplate>)
}
