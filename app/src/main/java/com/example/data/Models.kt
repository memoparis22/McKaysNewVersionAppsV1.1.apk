package com.example.data

data class User(
    val username: String,
    val role: String, // "admin" or "staff"
    val branch: String,
    val profilePic: String? = null
)

data class Pricing(
    val price_ml: Double,
    val price_botol: Double
)

data class StaffAccount(
    val id: String,
    val username: String,
    val password: String,
    val branch: String
)

data class InventoryItem(
    val id: String,
    val name: String,
    val stock_ml: Double,
    val stock_botol: Int
)

data class StockOrder(
    val id: String,
    val branch: String,
    val item: String,
    val ml: Double,
    val botol: Int,
    val status: String // "Menunggu Persetujuan", "Disetujui", "Ditolak"
)

data class TransactionItem(
    val id: String,
    val branch: String,
    val type: String,
    val item: String,
    val detail: String,
    val price: Double,
    val member: String,
    val pointsEarned: Int,
    val time: String,
    val date: String // YYYY-MM-DD
)

data class Member(
    val id: String,
    val name: String,
    val phone: String,
    val points: Int
)

enum class AppTab(val label: String) {
    HOME("Home"),
    STOCK("Stok"),
    HISTORY("Riwayat"),
    MEMBER("Member"),
    PROFILE("Akun")
}
