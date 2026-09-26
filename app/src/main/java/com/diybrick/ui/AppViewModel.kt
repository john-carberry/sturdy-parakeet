package com.diybrick.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.diybrick.AppContainer
import com.diybrick.DiyBrickApp

/** Creates a ViewModel with access to the app's [AppContainer]. */
@Composable
inline fun <reified VM : ViewModel> appViewModel(
    owner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current),
    crossinline create: (AppContainer) -> VM,
): VM {
    val container = (LocalContext.current.applicationContext as DiyBrickApp).container
    return viewModel(
        viewModelStoreOwner = owner,
        factory = viewModelFactory { initializer { create(container) } },
    )
}
