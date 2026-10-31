package com.example.tasteindia

import com.example.tasteindia.data.remote.MealLookupResponse
import com.example.tasteindia.data.remote.toMealDetail
import com.example.tasteindia.data.remote.validHttpUrlOrNull
import com.example.tasteindia.domain.IngredientLine
import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** Protects against: blank ingredient rows, raw whitespace, malformed URLs reaching the UI. */
class MealMapperTest {

    private val moshi = Moshi.Builder().build()

    private fun loadFixture(): Map<String, String?> {
        val json = javaClass.classLoader!!.getResource("fixtures/lookup_sample.json")!!.readText()
        val response = moshi.adapter(MealLookupResponse::class.java).fromJson(json)
        return response!!.meals!!.first()
    }

    @Test
    fun `ingredients are trimmed, ordered, and blank pairs are dropped`() {
        val meal = loadFixture().toMealDetail()
        assertNotNull(meal)
        assertEquals(
            listOf(
                IngredientLine("Chicken", "1.2 kg"),
                IngredientLine("Onion", "5 thinly sliced"),
                IngredientLine("Salt", null),   // blank measure -> null, ingredient kept
            ),
            meal!!.ingredients,   // measure with no ingredient (slot 5) is dropped
        )
    }

    @Test
    fun `tags are split and empty fragments removed`() {
        assertEquals(listOf("Curry", "Spicy"), loadFixture().toMealDetail()!!.tags)
    }

    @Test
    fun `instructions keep meaningful line breaks as paragraphs`() {
        assertEquals(
            listOf("Take a large pot.", "Heat the oil.", "Serve hot."),
            loadFixture().toMealDetail()!!.instructions,
        )
    }

    @Test
    fun `invalid source url is dropped but valid video url is kept`() {
        val meal = loadFixture().toMealDetail()!!
        assertNull(meal.sourceUrl)
        assertEquals("https://www.youtube.com/watch?v=IO0issT0Rmc", meal.videoUrl)
    }

    @Test
    fun `only well-formed http and https urls pass validation`() {
        assertNull(validHttpUrlOrNull("javascript:alert(1)"))
        assertNull(validHttpUrlOrNull("ftp://example.com/file"))
        assertNull(validHttpUrlOrNull("https://exa mple.com"))
        assertNull(validHttpUrlOrNull("   "))
        assertEquals("https://example.com/a", validHttpUrlOrNull(" https://example.com/a "))
    }

    @Test
    fun `record without id or name is rejected`() {
        assertNull(mapOf<String, String?>("strMeal" to "No id").toMealDetail())
        assertNull(mapOf<String, String?>("idMeal" to "1", "strMeal" to "  ").toMealDetail())
    }
}
