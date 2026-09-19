package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainViewModel
import com.example.ui.ScreenDestination
import com.example.ui.screens.NewProposalInputScreen
import com.example.ui.screens.PaymentLedgerScreen
import com.example.ui.screens.ProposalEditorScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TemplatesScreen
import com.example.ui.screens.WorkspaceHomeScreen
import com.example.ui.theme.QuoteCraftTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QuoteCraftTheme {
                val viewModel: MainViewModel = viewModel()
                QuoteCraftApp(viewModel)
            }
        }
    }
}

@Composable
fun QuoteCraftApp(viewModel: MainViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()

    Surface(modifier = Modifier.fillMaxSize()) {
        when (val screen = currentScreen) {
            is ScreenDestination.Home -> {
                WorkspaceHomeScreen(
                    viewModel = viewModel,
                    onNavigateToNew = { viewModel.navigateTo(ScreenDestination.NewProposal) },
                    onNavigateToProposal = { id -> viewModel.navigateTo(ScreenDestination.ProposalDetail(id)) },
                    onNavigateToLedger = { viewModel.navigateTo(ScreenDestination.PaymentLedger) },
                    onNavigateToTemplates = { viewModel.navigateTo(ScreenDestination.Templates) },
                    onNavigateToSettings = { viewModel.navigateTo(ScreenDestination.Settings) }
                )
            }
            is ScreenDestination.NewProposal -> {
                BackHandler { viewModel.navigateTo(ScreenDestination.Home) }
                NewProposalInputScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.navigateTo(ScreenDestination.Home) },
                    onProposalCreated = { newId ->
                        viewModel.navigateTo(ScreenDestination.ProposalDetail(newId, initialTab = 0))
                    }
                )
            }
            is ScreenDestination.ProposalDetail -> {
                BackHandler { viewModel.navigateTo(ScreenDestination.Home) }
                ProposalEditorScreen(
                    proposalId = screen.proposalId,
                    viewModel = viewModel,
                    initialTab = screen.initialTab,
                    onBack = { viewModel.navigateTo(ScreenDestination.Home) }
                )
            }
            is ScreenDestination.Templates -> {
                BackHandler { viewModel.navigateTo(ScreenDestination.Home) }
                TemplatesScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.navigateTo(ScreenDestination.Home) },
                    onUseTemplate = { template ->
                        viewModel.generateProposalFromInput(
                            title = template.title,
                            clientName = "Sample Client",
                            clientPhone = "0300-1234567",
                            clientEmail = "client@example.com",
                            clientAddress = "Lahore, Pakistan",
                            vertical = template.vertical,
                            rawNotes = "Generated from ${template.title} template: ${template.description}",
                            useHighThinking = false,
                            onSuccess = { newId ->
                                viewModel.navigateTo(ScreenDestination.ProposalDetail(newId, initialTab = 0))
                            }
                        )
                    }
                )
            }
            is ScreenDestination.PaymentLedger -> {
                BackHandler { viewModel.navigateTo(ScreenDestination.Home) }
                PaymentLedgerScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.navigateTo(ScreenDestination.Home) }
                )
            }
            is ScreenDestination.Settings -> {
                BackHandler { viewModel.navigateTo(ScreenDestination.Home) }
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.navigateTo(ScreenDestination.Home) }
                )
            }
        }
    }
}
