package com.example

import com.example.data.InventoryItem
import com.example.data.Pricing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testMasterPricesAndCalculation() {
    val masterPrices = mapOf(
      "VC Scandal" to Pricing(4000.0, 6000.0),
      "PL Romantichwish" to Pricing(3500.0, 5000.0),
      "Pink Shiffon" to Pricing(3000.0, 5000.0),
      "ES Cherry" to Pricing(4500.0, 6000.0),
      "Air ZR Nude" to Pricing(3500.0, 5000.0)
    )

    val pricing = masterPrices["VC Scandal"]!!
    val ml = 30.0
    val botol = 1

    val totalKombinasi = (pricing.price_ml * ml) + (pricing.price_botol * botol)
    assertEquals(126000.0, totalKombinasi, 0.01)

    val pointsEarned = (totalKombinasi / 10000.0).toInt()
    assertEquals(12, pointsEarned)
  }

  @Test
  fun testInventoryDeduction() {
    val item = InventoryItem("1", "VC Scandal", 150.0, 50)
    val updatedItem = item.copy(
      stock_ml = item.stock_ml - 30.0,
      stock_botol = item.stock_botol - 1
    )
    assertEquals(120.0, updatedItem.stock_ml, 0.01)
    assertEquals(49, updatedItem.stock_botol)
  }
}
