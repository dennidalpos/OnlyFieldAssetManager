package com.onlyfield.assetmanager

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.onlyfield.assetmanager.data.local.AppDatabase
import com.onlyfield.assetmanager.data.repository.ProjectRepository
import com.onlyfield.assetmanager.ui.AppRoot
import com.onlyfield.assetmanager.ui.ProjectViewModel

/** Process-wide database and repository, created on first use. */
object AppGraph {
    @Volatile private var repository: ProjectRepository? = null

    fun repository(context: Context): ProjectRepository = repository ?: synchronized(this) {
        repository ?: ProjectRepository(
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "onlyfield_asset_manager.db")
                .addMigrations(
                    AppDatabase.MIGRATION_1_2,
                    AppDatabase.MIGRATION_2_3,
                    AppDatabase.MIGRATION_3_4,
                    AppDatabase.MIGRATION_4_5,
                    AppDatabase.MIGRATION_5_6,
                    AppDatabase.MIGRATION_6_7,
                    AppDatabase.MIGRATION_7_8,
                    AppDatabase.MIGRATION_8_9,
                )
                .build(),
            attachmentsRoot = java.io.File(context.applicationContext.filesDir, "attachments")
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
        setContent {
            AppRoot(viewModel, onExit = ::finish)
        }
    }
}
