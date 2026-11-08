package com.example.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class McKaysRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("mckays_app_prefs", Context.MODE_PRIVATE)

    companion object {
        fun getTodayDateString(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return sdf.format(Date())
        }

        fun getCurrentTimeString(): String {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            return sdf.format(Date())
        }
    }

    // ----------------- Current User Session -----------------
    fun saveCurrentUser(user: User?) {
        if (user == null) {
            prefs.edit().remove("current_user").commit()
        } else {
            val json = JSONObject().apply {
                put("username", user.username)
                put("role", user.role)
                put("branch", user.branch)
                put("profilePic", user.profilePic ?: "")
            }
            prefs.edit().putString("current_user", json.toString()).commit()
        }
    }

    fun loadCurrentUser(): User? {
        val str = prefs.getString("current_user", null) ?: return null
        return try {
            val obj = JSONObject(str)
            User(
                username = obj.getString("username"),
                role = obj.getString("role"),
                branch = obj.getString("branch"),
                profilePic = obj.optString("profilePic").takeIf { it.isNotEmpty() }
            )
        } catch (e: Exception) {
            null
        }
    }

    // ----------------- Dark Mode -----------------
    fun saveDarkMode(isDark: Boolean) {
        prefs.edit().putBoolean("dark_mode", isDark).apply()
    }

    fun loadDarkMode(): Boolean {
        return prefs.getBoolean("dark_mode", false)
    }

    // ----------------- App Logo -----------------
    fun saveAppLogo(logoUri: String?) {
        if (logoUri == null) {
            prefs.edit().remove("app_logo").apply()
        } else {
            prefs.edit().putString("app_logo", logoUri).apply()
        }
    }

    fun loadAppLogo(): String? {
        return prefs.getString("app_logo", null)
    }

    // ----------------- Master Prices -----------------
    fun saveMasterPrices(prices: Map<String, Pricing>) {
        val root = JSONObject()
        for ((name, p) in prices) {
            val itemObj = JSONObject().apply {
                put("price_ml", p.price_ml)
                put("price_botol", p.price_botol)
            }
            root.put(name, itemObj)
        }
        prefs.edit().putString("master_prices", root.toString()).apply()
    }

    fun loadMasterPrices(): Map<String, Pricing> {
        val str = prefs.getString("master_prices", null)
        if (str.isNullOrEmpty()) {
            return mapOf(
                "VC Scandal" to Pricing(4000.0, 6000.0),
                "PL Romantichwish" to Pricing(3500.0, 5000.0),
                "Pink Shiffon" to Pricing(3000.0, 5000.0),
                "ES Cherry" to Pricing(4500.0, 6000.0),
                "Air ZR Nude" to Pricing(3500.0, 5000.0)
            )
        }
        return try {
            val root = JSONObject(str)
            val result = mutableMapOf<String, Pricing>()
            val keys = root.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val o = root.getJSONObject(k)
                result[k] = Pricing(o.getDouble("price_ml"), o.getDouble("price_botol"))
            }
            result
        } catch (e: Exception) {
            mapOf(
                "VC Scandal" to Pricing(4000.0, 6000.0),
                "PL Romantichwish" to Pricing(3500.0, 5000.0),
                "Pink Shiffon" to Pricing(3000.0, 5000.0),
                "ES Cherry" to Pricing(4500.0, 6000.0),
                "Air ZR Nude" to Pricing(3500.0, 5000.0)
            )
        }
    }

    // ----------------- Staff Accounts -----------------
    fun saveStaffAccounts(accounts: List<StaffAccount>) {
        val arr = JSONArray()
        for (acc in accounts) {
            val obj = JSONObject().apply {
                put("id", acc.id)
                put("username", acc.username)
                put("password", acc.password)
                put("branch", acc.branch)
            }
            arr.put(obj)
        }
        prefs.edit().putString("staff_accounts", arr.toString()).apply()
    }

    fun loadStaffAccounts(): List<StaffAccount> {
        val str = prefs.getString("staff_accounts", null)
        if (str.isNullOrEmpty()) {
            return listOf(
                StaffAccount("1", "budi", "123", "Bandung"),
                StaffAccount("2", "surabaya", "123", "Surabaya")
            )
        }
        return try {
            val arr = JSONArray(str)
            val list = mutableListOf<StaffAccount>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    StaffAccount(
                        id = o.getString("id"),
                        username = o.getString("username"),
                        password = o.getString("password"),
                        branch = o.getString("branch")
                    )
                )
            }
            list
        } catch (e: Exception) {
            listOf(
                StaffAccount("1", "budi", "123", "Bandung"),
                StaffAccount("2", "surabaya", "123", "Surabaya")
            )
        }
    }

    // ----------------- Branch Inventory -----------------
    fun saveBranchInventory(inv: Map<String, List<InventoryItem>>) {
        val root = JSONObject()
        for ((branch, items) in inv) {
            val arr = JSONArray()
            for (item in items) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("stock_ml", item.stock_ml)
                    put("stock_botol", item.stock_botol)
                }
                arr.put(obj)
            }
            root.put(branch, arr)
        }
        prefs.edit().putString("branch_inventory", root.toString()).commit()
    }

    fun loadBranchInventory(): Map<String, List<InventoryItem>> {
        val str = prefs.getString("branch_inventory", null)
        if (str.isNullOrEmpty()) {
            return mapOf(
                "Pusat" to listOf(
                    InventoryItem("1", "VC Scandal", 150.0, 50),
                    InventoryItem("2", "PL Romantichwish", 100.0, 30),
                    InventoryItem("3", "Pink Shiffon", 80.0, 40),
                    InventoryItem("4", "ES Cherry", 9.0, 5)
                ),
                "Bandung" to listOf(
                    InventoryItem("1", "VC Scandal", 50.0, 20),
                    InventoryItem("2", "Air ZR Nude", 8.0, 2)
                ),
                "Surabaya" to listOf(
                    InventoryItem("1", "Pink Shiffon", 45.0, 15)
                )
            )
        }
        return try {
            val root = JSONObject(str)
            val result = mutableMapOf<String, List<InventoryItem>>()
            val keys = root.keys()
            while (keys.hasNext()) {
                val branch = keys.next()
                val arr = root.getJSONArray(branch)
                val list = mutableListOf<InventoryItem>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    list.add(
                        InventoryItem(
                            id = o.getString("id"),
                            name = o.getString("name"),
                            stock_ml = o.getDouble("stock_ml"),
                            stock_botol = o.getInt("stock_botol")
                        )
                    )
                }
                result[branch] = list
            }
            result
        } catch (e: Exception) {
            mapOf(
                "Pusat" to listOf(
                    InventoryItem("1", "VC Scandal", 150.0, 50),
                    InventoryItem("2", "PL Romantichwish", 100.0, 30),
                    InventoryItem("3", "Pink Shiffon", 80.0, 40),
                    InventoryItem("4", "ES Cherry", 9.0, 5)
                ),
                "Bandung" to listOf(
                    InventoryItem("1", "VC Scandal", 50.0, 20),
                    InventoryItem("2", "Air ZR Nude", 8.0, 2)
                ),
                "Surabaya" to listOf(
                    InventoryItem("1", "Pink Shiffon", 45.0, 15)
                )
            )
        }
    }

    // ----------------- Stock Orders -----------------
    fun saveStockOrders(orders: List<StockOrder>) {
        val arr = JSONArray()
        for (ord in orders) {
            val obj = JSONObject().apply {
                put("id", ord.id)
                put("branch", ord.branch)
                put("item", ord.item)
                put("ml", ord.ml)
                put("botol", ord.botol)
                put("status", ord.status)
            }
            arr.put(obj)
        }
        prefs.edit().putString("stock_orders", arr.toString()).apply()
    }

    fun loadStockOrders(): List<StockOrder> {
        val str = prefs.getString("stock_orders", null)
        if (str.isNullOrEmpty()) {
            return listOf(
                StockOrder("ORD-01", "Bandung", "VC Scandal", 100.0, 20, "Menunggu Persetujuan")
            )
        }
        return try {
            val arr = JSONArray(str)
            val list = mutableListOf<StockOrder>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    StockOrder(
                        id = o.getString("id"),
                        branch = o.getString("branch"),
                        item = o.getString("item"),
                        ml = o.getDouble("ml"),
                        botol = o.getInt("botol"),
                        status = o.getString("status")
                    )
                )
            }
            list
        } catch (e: Exception) {
            listOf(
                StockOrder("ORD-01", "Bandung", "VC Scandal", 100.0, 20, "Menunggu Persetujuan")
            )
        }
    }

    // ----------------- Transactions -----------------
    fun saveTransactions(txs: List<TransactionItem>) {
        val arr = JSONArray()
        for (tx in txs) {
            val obj = JSONObject().apply {
                put("id", tx.id)
                put("branch", tx.branch)
                put("type", tx.type)
                put("item", tx.item)
                put("detail", tx.detail)
                put("price", tx.price)
                put("member", tx.member)
                put("pointsEarned", tx.pointsEarned)
                put("time", tx.time)
                put("date", tx.date)
            }
            arr.put(obj)
        }
        prefs.edit().putString("transactions", arr.toString()).commit()
    }

    fun loadTransactions(): List<TransactionItem> {
        val str = prefs.getString("transactions", null)
        if (str.isNullOrEmpty()) {
            return listOf(
                TransactionItem(
                    id = "TRX-1001",
                    branch = "Bandung",
                    type = "Kombinasi (Bibit + Botol)",
                    item = "VC Scandal (30ml) + Botol (1pcs)",
                    detail = "Kombinasi | 30ml | 1pcs",
                    price = 150000.0,
                    member = "Siti Rahma",
                    pointsEarned = 15,
                    time = "10:30",
                    date = getTodayDateString()
                )
            )
        }
        return try {
            val arr = JSONArray(str)
            val list = mutableListOf<TransactionItem>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    TransactionItem(
                        id = o.getString("id"),
                        branch = o.getString("branch"),
                        type = o.getString("type"),
                        item = o.getString("item"),
                        detail = o.getString("detail"),
                        price = o.getDouble("price"),
                        member = o.getString("member"),
                        pointsEarned = o.getInt("pointsEarned"),
                        time = o.getString("time"),
                        date = o.getString("date")
                    )
                )
            }
            list
        } catch (e: Exception) {
            listOf(
                TransactionItem(
                    id = "TRX-1001",
                    branch = "Bandung",
                    type = "Kombinasi (Bibit + Botol)",
                    item = "VC Scandal (30ml) + Botol (1pcs)",
                    detail = "Kombinasi | 30ml | 1pcs",
                    price = 150000.0,
                    member = "Siti Rahma",
                    pointsEarned = 15,
                    time = "10:30",
                    date = getTodayDateString()
                )
            )
        }
    }

    // ----------------- Members -----------------
    fun saveMembers(members: List<Member>) {
        val arr = JSONArray()
        for (m in members) {
            val obj = JSONObject().apply {
                put("id", m.id)
                put("name", m.name)
                put("phone", m.phone)
                put("points", m.points)
            }
            arr.put(obj)
        }
        prefs.edit().putString("members", arr.toString()).apply()
    }

    fun loadMembers(): List<Member> {
        val str = prefs.getString("members", null)
        if (str.isNullOrEmpty()) {
            return listOf(
                Member("1", "Siti Rahma", "081234567890", 45),
                Member("2", "Ahmad Fauzi", "089876543210", 20)
            )
        }
        return try {
            val arr = JSONArray(str)
            val list = mutableListOf<Member>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    Member(
                        id = o.getString("id"),
                        name = o.getString("name"),
                        phone = o.getString("phone"),
                        points = o.getInt("points")
                    )
                )
            }
            list
        } catch (e: Exception) {
            listOf(
                Member("1", "Siti Rahma", "081234567890", 45),
                Member("2", "Ahmad Fauzi", "089876543210", 20)
            )
        }
    }
}
