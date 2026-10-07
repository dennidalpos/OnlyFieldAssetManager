package com.onlyfield.assetmanager


import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.onlyfield.assetmanager.data.local.EncryptedDatabase
import com.onlyfield.assetmanager.data.repository.ProjectRepository
import com.onlyfield.assetmanager.ui.AppRoot
import com.onlyfield.assetmanager.ui.ProjectViewModel

/** Process-wide database and repository, created on first use. */
object AppGraph {
    @Volatile private var repository: ProjectRepository? = null

    fun repository(context: Context): ProjectRepository = repository ?: synchronized(this) {
        repository ?: ProjectRepository(
            EncryptedDatabase.open(context),
            attachmentsRoot = java.io.File(context.applicationContext.filesDir, "attachments"),
            recoveryPassword = EncryptedDatabase.recoveryPassword(context)
        ).also { repository = it }
    }
}

class MainActivity : ComponentActivity() {

    private val viewModel: ProjectViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = ProjectViewModel(AppGraph.repository(applicationContext)) as T
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val preferences = getSharedPreferences("ui", MODE_PRIVATE)
        viewModel.changeLanguage(com.onlyfield.assetmanager.core.i18n.AppLanguage.entries.firstOrNull {
            it.tag == preferences.getString("language", "")
        } ?: com.onlyfield.assetmanager.core.i18n.AppLanguage.SYSTEM)
        viewModel.saveLanguage = { preferences.edit().putString("language", it.tag).commit() }
        setContent {
            androidx.compose.runtime.CompositionLocalProvider(com.onlyfield.assetmanager.ui.LocalMessages provides viewModel.i18n) {
            AppRoot(viewModel, onExit = ::finish)
            }
        }
    }
}
