package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@IgnoreExtraProperties
data class FirestoreSaleItem(
    @DocumentId val id: String = "",
    val branch: String = "",
    val type: String = "",
    val item: String = "",
    val detail: String = "",
    val price: Double = 0.0,
    val member: String = "",
    val pointsEarned: Int = 0,
    val time: String = "",
    val date: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@IgnoreExtraProperties
data class FirestoreStockItem(
    @DocumentId val id: String = "",
    val branch: String = "",
    val name: String = "",
    val stock_ml: Double = 0.0,
    val stock_botol: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)

class FirestoreSyncRepository(private val context: Context) {
    companion object {
        private const val TAG = "FirestoreSync"
        private const val COLLECTION_SALES = "sales"
        private const val COLLECTION_STOCK = "stock"
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    // Resolve custom Firestore database ID if provisioned, otherwise fallback to default
    private val firestoreDb: FirebaseFirestore by lazy {
        try {
            val resId = context.resources.getIdentifier("firestore_database_id", "string", context.packageName)
            if (resId != 0) {
                val dbId = context.getString(resId)
                if (dbId.isNotBlank()) {
                    FirebaseFirestore.getInstance(dbId)
                } else {
                    Firebase.firestore
                }
            } else {
                Firebase.firestore
            }
        } catch (e: Exception) {
            Log.w(TAG, "Using default Firestore instance: ${e.message}")
            Firebase.firestore
        }
    }

    private val _firestoreSyncStatus = MutableStateFlow<String>("Firestore Siap")
    val firestoreSyncStatus: StateFlow<String> = _firestoreSyncStatus.asStateFlow()

    /**
     * Real-time listener for Sales transactions from Firestore
     */
    fun observeSalesRealtime(branchFilter: String? = null): Flow<List<FirestoreSaleItem>> {
        return try {
            val query = if (!branchFilter.isNullOrBlank() && branchFilter != "Pusat") {
                firestoreDb.collection(COLLECTION_SALES).whereEqualTo("branch", branchFilter)
            } else {
                firestoreDb.collection(COLLECTION_SALES)
            }
            query.snapshots().map { snapshot ->
                snapshot.toObjects(FirestoreSaleItem::class.java)
            }.catch { e ->
                Log.e(TAG, "Error observing Firestore sales: ${e.message}", e)
                emit(emptyList())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize sales snapshot listener", e)
            flowOf(emptyList())
        }
    }

    /**
     * Real-time listener for Branch Stock from Firestore
     */
    fun observeStockRealtime(branchFilter: String? = null): Flow<List<FirestoreStockItem>> {
        return try {
            val query = if (!branchFilter.isNullOrBlank()) {
                firestoreDb.collection(COLLECTION_STOCK).whereEqualTo("branch", branchFilter)
            } else {
                firestoreDb.collection(COLLECTION_STOCK)
            }
            query.snapshots().map { snapshot ->
                snapshot.toObjects(FirestoreStockItem::class.java)
            }.catch { e ->
                Log.e(TAG, "Error observing Firestore stock: ${e.message}", e)
                emit(emptyList())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize stock snapshot listener", e)
            flowOf(emptyList())
        }
    }

    /**
     * Sync single transaction to Firestore
     */
    suspend fun syncSale(tx: TransactionItem): Result<Unit> {
        return try {
            val saleItem = FirestoreSaleItem(
                id = tx.id,
                branch = tx.branch,
                type = tx.type,
                item = tx.item,
                detail = tx.detail,
                price = tx.price,
                member = tx.member,
                pointsEarned = tx.pointsEarned,
                time = tx.time,
                date = tx.date,
                timestamp = System.currentTimeMillis()
            )
            firestoreDb.collection(COLLECTION_SALES)
                .document(tx.id)
                .set(saleItem, SetOptions.merge())
                .await()

            _firestoreSyncStatus.value = "Transaksi ${tx.id} tersinkron ke Firestore"
            Log.d(TAG, "Sale ${tx.id} synced to Firestore successfully.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync sale to Firestore: ${e.message}", e)
            _firestoreSyncStatus.value = "Offline / Gagal sinkron Firestore: ${e.localizedMessage ?: "error"}"
            Result.failure(e)
        }
    }

    /**
     * Sync branch stock items to Firestore
     */
    suspend fun syncBranchStock(branch: String, items: List<InventoryItem>): Result<Unit> {
        return try {
            val batch = firestoreDb.batch()
            for (item in items) {
                val docId = "${branch}_${item.id}"
                val stockItem = FirestoreStockItem(
                    id = docId,
                    branch = branch,
                    name = item.name,
                    stock_ml = item.stock_ml,
                    stock_botol = item.stock_botol,
                    lastUpdated = System.currentTimeMillis()
                )
                val docRef = firestoreDb.collection(COLLECTION_STOCK).document(docId)
                batch.set(docRef, stockItem, SetOptions.merge())
            }
            batch.commit().await()
            _firestoreSyncStatus.value = "Stok $branch (${items.size} item) tersinkron ke Firestore"
            Log.d(TAG, "Stock for $branch synced to Firestore.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync stock to Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Full sync: syncs all local transactions and inventory to Firestore
     */
    suspend fun syncAllData(
        transactions: List<TransactionItem>,
        inventory: Map<String, List<InventoryItem>>
    ): Result<Unit> {
        return try {
            _firestoreSyncStatus.value = "Menyinkronkan data ke Firestore..."
            for (tx in transactions) {
                syncSale(tx)
            }
            for ((branch, items) in inventory) {
                syncBranchStock(branch, items)
            }
            _firestoreSyncStatus.value = "Seluruh data penjualan & stok tersinkron ke Firestore"
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Full sync to Firestore failed: ${e.message}", e)
            _firestoreSyncStatus.value = "Gagal sinkron penuh Firestore: ${e.message}"
            Result.failure(e)
        }
    }
}
