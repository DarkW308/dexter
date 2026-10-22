package com.aistudio.dexter.ui.dialog

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import com.aistudio.dexter.R
import com.aistudio.dexter.engine.LayaEngine
import com.aistudio.dexter.engine.ModelDownloadManager
import com.aistudio.dexter.engine.ModelInfo
import kotlinx.coroutines.*

class ModelManagerDialog(
    private val context: Context,
    private val onModelChanged: () -> Unit = {}
) {

    private val downloadManager = ModelDownloadManager.getInstance(context)
    private val layaEngine = LayaEngine.getInstance(context)
    private val dialogScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun show() {
        val models = downloadManager.getPresetModels()
        val hasLaya = layaEngine.hasPhysicalModel()
        val layaFile = layaEngine.getModelFile()
        val needleFile = downloadManager.needleTargetFile
        val hasNeedle = needleFile.exists() && needleFile.length() > 0

        val builder = AlertDialog.Builder(context)
        builder.setTitle("Edge AI Models & Downloader")

        val scroll = ScrollView(context)
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (16 * context.resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
        }
        scroll.addView(container)

        // 1. Architecture Status Header
        val headerCard = createStatusCard(hasLaya, hasNeedle, layaFile.length(), needleFile.length())
        container.addView(headerCard)

        // 2. Section: Available Models
        val sectionTitle = TextView(context).apply {
            text = "PRESET ON-DEVICE MODELS"
            textSize = 12f
            setPadding(0, (16 * context.resources.displayMetrics.density).toInt(), 0, (8 * context.resources.displayMetrics.density).toInt())
            setTextColor(0xFF81D4FA.toInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        container.addView(sectionTitle)

        // Model Rows
        for (model in models) {
            val modelView = createModelRow(model)
            container.addView(modelView)
        }

        // 3. Custom Hugging Face Download Button
        val customButton = Button(context).apply {
            text = "📥 Download Custom Model from Hugging Face"
            setOnClickListener {
                showCustomModelInputDialog()
            }
        }
        val btnParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = (16 * context.resources.displayMetrics.density).toInt()
        }
        container.addView(customButton, btnParams)

        // 4. Compatibility Guidelines Card
        val guideCard = createGuideCard()
        container.addView(guideCard)

        builder.setView(scroll)
        builder.setPositiveButton("Close", null)
        builder.show()
    }

    private fun createStatusCard(hasLaya: Boolean, hasNeedle: Boolean, layaBytes: Long, needleBytes: Long): View {
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (12 * context.resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
            setBackgroundColor(0xFF1E2638.toInt())
        }

        val title = TextView(context).apply {
            val tierText = when {
                hasLaya && hasNeedle -> "🟢 Dual-Engine Full AI (Tier 2 ONNX + Tier 3 Needle)"
                hasLaya -> "🟢 Tier 2 (Laya ModernBERT ONNX Active)"
                hasNeedle -> "🟡 Tier 1 + Needle 3 Background Daemon Active"
                else -> "🟡 Tier 1 (Deterministic Fast-Path & Calibrated Fallback)"
            }
            text = tierText
            textSize = 14f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(if (hasLaya) 0xFF69F0AE.toInt() else 0xFFFFD54F.toInt())
        }
        layout.addView(title)

        val desc = TextView(context).apply {
            val layaStatus = if (hasLaya) "Preloaded & Active" else "Calibrated Heuristic Active"
            val needleStatus = if (hasNeedle) "Preloaded & Active" else "Deterministic Generator Active"
            text = "• Laya ONNX Engine: $layaStatus\n• Needle 3 Background Daemon: $needleStatus\n• Google Research Harness²: Active (Propose → Probe → Compose)\n• Offline Privacy: 100% Local (0 KB telemetry)"
            textSize = 12f
            setPadding(0, 8, 0, 0)
            setTextColor(0xFFCFD8DC.toInt())
        }
        layout.addView(desc)

        return layout
    }

    private fun createModelRow(model: ModelInfo): View {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (10 * context.resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
            val margin = (4 * context.resources.displayMetrics.density).toInt()
            setBackgroundColor(0xFF232D42.toInt())
        }
        val rowParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = (8 * context.resources.displayMetrics.density).toInt()
        }

        val topRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val nameText = TextView(context).apply {
            text = model.name
            textSize = 14f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(0xFFFFFFFF.toInt())
        }
        topRow.addView(nameText, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        val sizeBadge = TextView(context).apply {
            text = if (model.isInstalled) {
                "Installed (${model.installedSizeBytes / (1024 * 1024)} MB)"
            } else {
                "~${model.estimatedSizeMb.toInt()} MB"
            }
            textSize = 11f
            setTextColor(if (model.isInstalled) 0xFF69F0AE.toInt() else 0xFF90A4AE.toInt())
        }
        topRow.addView(sizeBadge)
        row.addView(topRow)

        val repoText = TextView(context).apply {
            text = "Repo: ${model.repoId} · Format: ${model.format}"
            textSize = 11f
            setTextColor(0xFF81D4FA.toInt())
            setPadding(0, 4, 0, 4)
        }
        row.addView(repoText)

        val roleText = TextView(context).apply {
            text = "${model.role}\n${model.compatibilityNote}"
            textSize = 11f
            setTextColor(0xFFB0BEC5.toInt())
            setPadding(0, 0, 0, 8)
        }
        row.addView(roleText)

        val actionLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        if (model.isInstalled) {
            val deleteBtn = Button(context).apply {
                text = "🗑 Delete / Free Space"
                textSize = 12f
                setOnClickListener {
                    confirmDeleteModel(model)
                }
            }
            actionLayout.addView(deleteBtn)
        } else {
            val downloadBtn = Button(context).apply {
                text = "⬇ Download from Hugging Face"
                textSize = 12f
                setOnClickListener {
                    startDownloadFlow(model.id, model.name, model.downloadUrl)
                }
            }
            actionLayout.addView(downloadBtn)
        }

        row.addView(actionLayout)
        row.layoutParams = rowParams
        return row
    }

    private fun createGuideCard(): View {
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (12 * context.resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
            val margin = (12 * context.resources.displayMetrics.density).toInt()
            setBackgroundColor(0xFF1B2333.toInt())
        }
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = (12 * context.resources.displayMetrics.density).toInt()
        }

        val title = TextView(context).apply {
            text = "💡 How Dexter Uses LLMs & Models on Your Phone"
            textSize = 12f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(0xFFFFCC80.toInt())
        }
        layout.addView(title)

        val body = TextView(context).apply {
            text = """
                • ONNX Runtime: Models like 'convaiinnovations/laya' run via Microsoft ONNX Runtime Android with INT8 quantization, using zero-copy mmap.
                • Background Daemon: Models like 'Cactus-Compute/needle3' run as an isolated background worker during idle time, freeing RAM immediately after synthesis.
                • Model Compatibility: For Helio P60 (6GB RAM), choose models ≤ 150 MB in ONNX format or ARM64 native binaries.
                • Zero Data Leakage: Once downloaded, models run 100% locally with no telemetry or internet calls.
            """.trimIndent()
            textSize = 11f
            setPadding(0, 6, 0, 0)
            setTextColor(0xFFCFD8DC.toInt())
        }
        layout.addView(body)
        layout.layoutParams = layoutParams
        return layout
    }

    private fun showCustomModelInputDialog() {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Download Custom Hugging Face Model")

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (16 * context.resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
        }

        val hintText = TextView(context).apply {
            text = "Enter a direct Hugging Face URL or repository model file (e.g. 'convaiinnovations/laya' or full resolve URL):"
            textSize = 12f
            setTextColor(0xFFECEFF1.toInt())
        }
        container.addView(hintText)

        val urlInput = EditText(context).apply {
            hint = "https://huggingface.co/.../model_int8.onnx"
            setText("https://huggingface.co/convaiinnovations/laya/resolve/main/model_int8.onnx")
            textSize = 12f
        }
        container.addView(urlInput)

        val typeLabel = TextView(context).apply {
            text = "Target Model Slot:"
            textSize = 12f
            setPadding(0, 16, 0, 4)
            setTextColor(0xFF81D4FA.toInt())
        }
        container.addView(typeLabel)

        val radioGroup = RadioGroup(context)
        val rbLaya = RadioButton(context).apply {
            text = "Decision & Intent Model (laya_int8.onnx)"
            id = View.generateViewId()
            isChecked = true
        }
        val rbNeedle = RadioButton(context).apply {
            text = "Background Planning Engine (needle3 binary)"
            id = View.generateViewId()
        }
        radioGroup.addView(rbLaya)
        radioGroup.addView(rbNeedle)
        container.addView(radioGroup)

        builder.setView(container)
        builder.setPositiveButton("Download") { _, _ ->
            val inputUrl = urlInput.text.toString().trim()
            val targetId = if (radioGroup.checkedRadioButtonId == rbLaya.id) "laya" else "needle3"
            val targetName = if (targetId == "laya") "Custom Laya ONNX" else "Custom Needle Daemon"
            startDownloadFlow(targetId, targetName, inputUrl)
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun startDownloadFlow(modelId: String, modelName: String, url: String) {
        val progressDialog = AlertDialog.Builder(context).create()
        progressDialog.setTitle("Downloading $modelName")
        progressDialog.setCancelable(false)

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (20 * context.resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
        }

        val statusText = TextView(context).apply {
            text = "Connecting to Hugging Face..."
            textSize = 13f
            setTextColor(0xFFECEFF1.toInt())
        }
        container.addView(statusText)

        val progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = true
            max = 100
            setPadding(0, 16, 0, 16)
        }
        container.addView(progressBar)

        val cancelBtn = Button(context).apply {
            text = "Cancel"
            setOnClickListener {
                downloadManager.cancelDownload()
                progressDialog.dismiss()
            }
        }
        container.addView(cancelBtn)
        progressDialog.setView(container)
        progressDialog.show()

        downloadManager.startDownload(
            modelId = modelId,
            customUrl = url,
            onProgress = { percent, downloaded, total ->
                dialogScope.launch {
                    if (percent >= 0) {
                        progressBar.isIndeterminate = false
                        progressBar.progress = percent
                        val mbDownloaded = downloaded / (1024.0 * 1024.0)
                        val mbTotal = total / (1024.0 * 1024.0)
                        statusText.text = String.format("Downloading: %.1f MB / %.1f MB (%d%%)", mbDownloaded, mbTotal, percent)
                    } else {
                        progressBar.isIndeterminate = true
                        val mbDownloaded = downloaded / (1024.0 * 1024.0)
                        statusText.text = String.format("Downloading: %.1f MB received...", mbDownloaded)
                    }
                }
            },
            onResult = { success, message ->
                dialogScope.launch {
                    progressDialog.dismiss()
                    val alert = AlertDialog.Builder(context)
                    if (success) {
                        alert.setTitle("Download Complete")
                        alert.setMessage(message)
                        alert.setPositiveButton("OK") { _, _ ->
                            onModelChanged()
                        }
                    } else {
                        alert.setTitle("Download Failed")
                        alert.setMessage("$message\n\nNote: Dexter's built-in calibrated fast-path and schema generator will continue running offline.")
                        alert.setPositiveButton("OK", null)
                    }
                    alert.show()
                }
            }
        )
    }

    private fun confirmDeleteModel(model: ModelInfo) {
        AlertDialog.Builder(context)
            .setTitle("Delete Model?")
            .setMessage("Are you sure you want to delete ${model.name}? This will free ${model.installedSizeBytes / (1024 * 1024)} MB of device storage. Dexter will fall back to its internal calibrated rule engine.")
            .setPositiveButton("Delete") { _, _ ->
                val deleted = downloadManager.deleteModel(model.id)
                if (deleted) {
                    Toast.makeText(context, "${model.name} deleted.", Toast.LENGTH_SHORT).show()
                    onModelChanged()
                } else {
                    Toast.makeText(context, "Could not delete model file.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
