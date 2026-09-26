package com.diybrick.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.diybrick.ui.blocker.BlockerSetupScreen
import com.diybrick.ui.home.HomeScreen
import com.diybrick.ui.home.HomeViewModel
import com.diybrick.ui.keys.CreateQrScreen
import com.diybrick.ui.keys.CreateQrViewModel
import com.diybrick.ui.keys.KeysScreen
import com.diybrick.ui.keys.KeysViewModel
import com.diybrick.ui.keys.PairNfcScreen
import com.diybrick.ui.keys.PairNfcViewModel
import com.diybrick.ui.modes.ModeScreen
import com.diybrick.ui.modes.ModeViewModel
import com.diybrick.ui.scan.ScanQrScreen

private object Routes {
    const val HOME = "home"
    const val CHECK_QR = "check_qr"
    const val KEYS = "keys"
    const val PAIR_NFC = "pair_nfc"
    const val CREATE_QR = "create_qr"
    const val MODE = "mode"
    const val BLOCKER_SETUP = "blocker_setup"
}

@Composable
fun DiyBrickNavHost() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                viewModel = appViewModel {
                    HomeViewModel(it.keyRepository, it.brickRepository, it.modeRepository)
                },
                onScanQr = { nav.navigate(Routes.CHECK_QR) },
                onManageKeys = { nav.navigate(Routes.KEYS) },
                onChooseApps = { nav.navigate(Routes.MODE) },
                onSetUpBlocker = { nav.navigate(Routes.BLOCKER_SETUP) },
            )
        }
        composable(Routes.CHECK_QR) { entry ->
            // The result goes to the home screen's ViewModel, which shows it after we pop.
            val homeEntry = remember(entry) { nav.getBackStackEntry(Routes.HOME) }
            val homeViewModel = appViewModel(owner = homeEntry) {
                HomeViewModel(it.keyRepository, it.brickRepository, it.modeRepository)
            }
            ScanQrScreen(
                title = "Scan your QR key",
                onScanned = {
                    homeViewModel.onQrScanned(it)
                    nav.popBackStack()
                },
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.KEYS) {
            KeysScreen(
                viewModel = appViewModel { KeysViewModel(it.keyRepository, it.brickRepository) },
                onAddNfc = { nav.navigate(Routes.PAIR_NFC) },
                onAddQr = { nav.navigate(Routes.CREATE_QR) },
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.PAIR_NFC) {
            PairNfcScreen(
                viewModel = appViewModel { PairNfcViewModel(it.keyRepository) },
                onDone = { nav.popBackStack() },
            )
        }
        composable(Routes.MODE) {
            ModeScreen(
                viewModel = appViewModel { ModeViewModel(it.appContext, it.modeRepository, it.brickRepository) },
                onDone = { nav.popBackStack() },
            )
        }
        composable(Routes.BLOCKER_SETUP) {
            BlockerSetupScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.CREATE_QR) {
            CreateQrScreen(
                viewModel = appViewModel { CreateQrViewModel(it.keyRepository) },
                onDone = { nav.popBackStack() },
            )
        }
    }
}
