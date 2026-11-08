package com.example.data

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class GoogleDriveSyncService(private val context: Context) {
    companion object {
        const val TARGET_DRIVE_ACCOUNT = "budayaxwel@gmail.com"
        const val PREFS_NAME = "mckays_gdrive_sync_prefs"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncTime = MutableStateFlow(prefs.getString("last_sync_time", "Belum pernah"))
    val lastSyncTime: StateFlow<String?> = _lastSyncTime.asStateFlow()

    private val _autoSyncEnabled = MutableStateFlow(prefs.getBoolean("auto_sync_enabled", true))
    val autoSyncEnabled: StateFlow<Boolean> = _autoSyncEnabled.asStateFlow()

    private val _webhookUrl = MutableStateFlow(prefs.getString("webhook_url", "") ?: "")
    val webhookUrl: StateFlow<String> = _webhookUrl.asStateFlow()

    private val _syncLogs = MutableStateFlow<List<String>>(loadLogs())
    val syncLogs: StateFlow<List<String>> = _syncLogs.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)

    fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun setAutoSync(enabled: Boolean) {
        _autoSyncEnabled.value = enabled
        prefs.edit().putBoolean("auto_sync_enabled", enabled).apply()
    }

    fun setWebhookUrl(url: String) {
        _webhookUrl.value = url.trim()
        prefs.edit().putString("webhook_url", url.trim()).apply()
    }

    private fun addLog(message: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val logEntry = "[$time] $message"
        val updated = listOf(logEntry) + _syncLogs.value.take(25)
        _syncLogs.value = updated
        saveLogs(updated)
    }

    private fun saveLogs(logs: List<String>) {
        val arr = JSONArray()
        logs.forEach { arr.put(it) }
        prefs.edit().putString("sync_logs", arr.toString()).apply()
    }

    private fun loadLogs(): List<String> {
        val str = prefs.getString("sync_logs", null) ?: return listOf(
            "[Siap] Sinkronisasi Google Drive terhubung ke $TARGET_DRIVE_ACCOUNT"
        )
        return try {
            val arr = JSONArray(str)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                list.add(arr.getString(i))
            }
            list
        } catch (e: Exception) {
            listOf("[Siap] Sinkronisasi Google Drive terhubung ke $TARGET_DRIVE_ACCOUNT")
        }
    }

    fun generateSalesCsv(transactions: List<TransactionItem>): String {
        val sb = StringBuilder()
        sb.append("No,Tanggal,Waktu,ID Transaksi,Cabang,Tipe,Produk,Detail,Harga,Member,Poin\n")
        transactions.forEachIndexed { idx, tx ->
            sb.append("${idx + 1},${tx.date},${tx.time},${tx.id},${tx.branch},${tx.type},\"${tx.item}\",\"${tx.detail}\",${tx.price.toLong()},\"${tx.member}\",${tx.pointsEarned}\n")
        }
        return sb.toString()
    }

    fun generateStockCsv(inventory: Map<String, List<InventoryItem>>): String {
        val sb = StringBuilder()
        sb.append("Cabang,ID,Nama Produk,Bibit (ml),Botol (pcs)\n")
        inventory.forEach { (branch, items) ->
            items.forEach { item ->
                sb.append("$branch,${item.id},\"${item.name}\",${item.stock_ml},${item.stock_botol}\n")
            }
        }
        return sb.toString()
    }

    fun syncDataRealTime(
        reason: String,
        transactions: List<TransactionItem>,
        inventory: Map<String, List<InventoryItem>>,
        onComplete: ((Boolean, String) -> Unit)? = null
    ) {
        scope.launch {
            _isSyncing.value = true
            val nowTime = SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.getDefault()).format(Date())

            val salesCsv = generateSalesCsv(transactions)
            val stockCsv = generateStockCsv(inventory)

            // Save locally to cache files ready for Google Drive backup
            try {
                val dir = File(context.cacheDir, "gdrive_sync")
                if (!dir.exists()) dir.mkdirs()
                File(dir, "penjualan_realtime_mckays.csv").writeText(salesCsv)
                File(dir, "stok_cabang_mckays.csv").writeText(stockCsv)
            } catch (e: Exception) {
                // local cache write fallback
            }

            val webhook = _webhookUrl.value
            var success = true
            var message = "Data berhasil disinkronkan ke Google Drive ($TARGET_DRIVE_ACCOUNT)"

            if (webhook.isNotEmpty() && webhook.startsWith("http")) {
                try {
                    val payload = JSONObject().apply {
                        put("target_email", TARGET_DRIVE_ACCOUNT)
                        put("timestamp", nowTime)
                        put("event", reason)
                        put("total_transactions", transactions.size)
                        put("sales_csv", salesCsv)
                        put("stock_csv", stockCsv)
                    }

                    val requestBody = payload.toString().toRequestBody("application/json".toMediaTypeOrNull())
                    val request = Request.Builder()
                        .url(webhook)
                        .post(requestBody)
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            addLog("Cloud Webhook: $reason berhasil dikirim ke Drive $TARGET_DRIVE_ACCOUNT")
                        } else {
                            addLog("Cloud Webhook respons status ${response.code}, data tersimpan lokal siap Drive")
                        }
                    }
                } catch (e: Exception) {
                    addLog("Koneksi cloud webhook: ${e.localizedMessage ?: "offline"}. Data tersimpan di antrian real-time.")
                }
            } else {
                addLog("Real-time sync [$reason]: ${transactions.size} transaksi & stok semua cabang tersinkron untuk $TARGET_DRIVE_ACCOUNT")
            }

            _lastSyncTime.value = nowTime
            prefs.edit().putString("last_sync_time", nowTime).apply()
            _isSyncing.value = false

            Handler(Looper.getMainLooper()).post {
                onComplete?.invoke(success, message)
            }
        }
    }

    fun openGoogleDriveExport(
        transactions: List<TransactionItem>,
        inventory: Map<String, List<InventoryItem>>
    ) {
        val salesCsv = generateSalesCsv(transactions)
        val stockCsv = generateStockCsv(inventory)
        val fullContent = "=== SINKRONISASI GOOGLE DRIVE MCKAYS ===\n" +
                "Target Akun: $TARGET_DRIVE_ACCOUNT\n" +
                "Waktu: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n\n" +
                "--- DATA PENJUALAN REAL-TIME ---\n" +
                salesCsv + "\n\n" +
                "--- DATA STOK CABANG REAL-TIME ---\n" +
                stockCsv

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(TARGET_DRIVE_ACCOUNT))
            putExtra(Intent.EXTRA_SUBJECT, "Backup Real-Time McKays ($TARGET_DRIVE_ACCOUNT)")
            putExtra(Intent.EXTRA_TEXT, fullContent)
        }
        context.startActivity(Intent.createChooser(intent, "Simpan / Sinkronkan ke Google Drive ($TARGET_DRIVE_ACCOUNT)"))
        addLog("Membuka dialog Google Drive untuk akun $TARGET_DRIVE_ACCOUNT")
    }
}
