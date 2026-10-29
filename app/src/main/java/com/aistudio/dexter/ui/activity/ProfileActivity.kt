package com.aistudio.dexter.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.*
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.aistudio.dexter.DexterApp
import com.aistudio.dexter.data.backup.DexterBackupManager
import com.aistudio.dexter.data.preferences.UserPreferencesRepository
import com.aistudio.dexter.data.repository.DexterRepository
import com.aistudio.dexter.ui.screens.ProfileScreen
import com.aistudio.dexter.ui.theme.DexterTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileActivity : ComponentActivity() {

    private lateinit var repository: DexterRepository
    private lateinit var preferencesRepo: UserPreferencesRepository

    private val importFileLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            handleImportUri(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        repository = (application as DexterApp).repository
        preferencesRepo = UserPreferencesRepository.getInstance(this)

        setContent {
            DexterTheme {
                val preferences by preferencesRepo.preferences.collectAsState()
                var domainCounts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
                var totalRecords by remember { mutableStateOf(0) }

                fun refreshMetrics() {
                    lifecycleScope.launch(Dispatchers.IO) {
                        val counts = repository.getDomainRecordCounts()
                        val total = counts.values.sum()
                        withContext(Dispatchers.Main) {
                            domainCounts = counts
                            totalRecords = total
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    refreshMetrics()
                }

                ProfileScreen(
                    preferences = preferences,
                    domainCounts = domainCounts,
                    totalRecords = totalRecords,
                    onBack = { finish() },
                    onUpdateName = { preferencesRepo.updateUserName(it) },
                    onUpdateAvatar = { preferencesRepo.updateAvatar(it) },
                    onUpdateLanguage = {
                        preferencesRepo.updateLanguage(it)
                        Toast.makeText(this, "Language set to $it", Toast.LENGTH_SHORT).show()
                    },
                    onUpdateCurrency = { code, symbol ->
                        preferencesRepo.updateCurrency(code, symbol)
                        Toast.makeText(this, "Currency set to $code ($symbol)", Toast.LENGTH_SHORT).show()
                    },
                    onUpdateTimeFormat = { preferencesRepo.updateTimeFormat(it) },
                    onUpdateDateFormat = { preferencesRepo.updateDateFormat(it) },
                    onExportBackup = {
                        exportBackupFile()
                    },
                    onImportBackup = {
                        importFileLauncher.launch("*/*")
                    },
                    onPurgeDomains = { domains ->
                        purgeSelectedDomains(domains) { refreshMetrics() }
                    },
                    onFactoryReset = {
                        performFactoryReset { refreshMetrics() }
                    },
                    onOpenVoiceLab = {
                        startActivity(Intent(this, VoiceLabActivity::class.java))
                    }
                )
            }
        }
    }

    private fun exportBackupFile() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val file = repository.exportBackupFile(this@ProfileActivity)
                val uri = FileProvider.getUriForFile(
                    this@ProfileActivity,
                    "${packageName}.fileprovider",
                    file
                )

                withContext(Dispatchers.Main) {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/json"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_SUBJECT, "Dexter Migration Backup")
                        putExtra(Intent.EXTRA_TEXT, "Dexter database cross-device migration backup file.")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(Intent.createChooser(shareIntent, "Share or Save Dexter Backup"))
                    Toast.makeText(this@ProfileActivity, "Backup file prepared", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ProfileActivity, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun handleImportUri(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val jsonString = inputStream?.bufferedReader()?.use { it.readText() } ?: ""

                val validation = repository.validateBackup(jsonString)
                withContext(Dispatchers.Main) {
                    if (!validation.isValid || validation.metadata == null) {
                        Toast.makeText(
                            this@ProfileActivity,
                            validation.errorMessage ?: "Selected file is not a valid Dexter backup",
                            Toast.LENGTH_LONG
                        ).show()
                        return@withContext
                    }

                    val meta = validation.metadata
                    AlertDialog.Builder(this@ProfileActivity)
                        .setTitle("Restore Database Backup")
                        .setMessage("Backup file verified!\n\n• Version: v${meta.version}\n• Total Records: ${meta.totalRecords}\n• Segments: ${meta.tableCounts.size} tables\n\nHow would you like to restore this data?")
                        .setPositiveButton("Merge / Update") { _, _ ->
                            executeRestore(jsonString, clearExisting = false)
                        }
                        .setNegativeButton("Clean Restore (Replace All)") { _, _ ->
                            executeRestore(jsonString, clearExisting = true)
                        }
                        .setNeutralButton("Cancel", null)
                        .show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ProfileActivity, "Failed to read backup: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun executeRestore(jsonString: String, clearExisting: Boolean) {
        lifecycleScope.launch(Dispatchers.IO) {
            val result = repository.restoreBackup(jsonString, clearExisting)
            withContext(Dispatchers.Main) {
                if (result.isSuccess) {
                    Toast.makeText(this@ProfileActivity, result.message, Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@ProfileActivity, "Restore failed: ${result.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun purgeSelectedDomains(domains: List<String>, onComplete: () -> Unit) {
        lifecycleScope.launch(Dispatchers.IO) {
            var purgedItems = 0
            for (domain in domains) {
                purgedItems += repository.purgeDomain(domain)
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@ProfileActivity,
                    "Purged $purgedItems records across ${domains.size} segments",
                    Toast.LENGTH_SHORT
                ).show()
                onComplete()
            }
        }
    }

    private fun performFactoryReset(onComplete: () -> Unit) {
        lifecycleScope.launch(Dispatchers.IO) {
            val success = repository.factoryReset(this@ProfileActivity)
            withContext(Dispatchers.Main) {
                if (success) {
                    Toast.makeText(
                        this@ProfileActivity,
                        "Dexter reset to factory defaults",
                        Toast.LENGTH_SHORT
                    ).show()
                    onComplete()
                } else {
                    Toast.makeText(
                        this@ProfileActivity,
                        "Factory reset encountered an issue",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}
