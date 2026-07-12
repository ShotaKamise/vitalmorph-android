package app.vitalmorph.data

import app.vitalmorph.domain.FoodCatalogItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Open Food Facts(world.openfoodfacts.org、非営利のオープンデータ)から取得した食品候補。
 * 栄養値は100gあたり。記録前にユーザーが確認・修正できる。
 */
data class ExternalFood(
    val barcode: String,
    val name: String,
    val brand: String?,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val fatPer100g: Double,
    val carbsPer100g: Double,
    val vitaminCMgPer100g: Double = 0.0,
    val calciumMgPer100g: Double = 0.0,
    val ironMgPer100g: Double = 0.0,
) {
    val displayName: String
        get() = if (brand.isNullOrBlank()) name else "$name($brand)"

    /** カタログ項目(100g基準)へ変換する。 */
    fun toCatalogItem() = FoodCatalogItem(
        foodId = "off:$barcode",
        name = displayName.take(40),
        standardAmount = 100.0,
        amountUnit = "g",
        calories = caloriesPer100g,
        proteinGrams = proteinPer100g,
        fatGrams = fatPer100g,
        carbsGrams = carbsPer100g,
    )
}

/**
 * Open Food FactsレスポンスのJSON解析。ネットワークに依存しない純ロジック。
 */
object OpenFoodFactsParser {
    fun parseSearch(json: String): List<ExternalFood> {
        val root = JSONObject(json)
        val products = root.optJSONArray("products") ?: return emptyList()
        return (0 until products.length()).mapNotNull { index ->
            parseProductObject(products.optJSONObject(index) ?: return@mapNotNull null)
        }
    }

    fun parseProduct(json: String): ExternalFood? {
        val root = JSONObject(json)
        if (root.optInt("status", 0) != 1) return null
        return parseProductObject(root.optJSONObject("product") ?: return null)
    }

    private fun parseProductObject(product: JSONObject): ExternalFood? {
        val name = product.optString("product_name_ja").ifBlank { product.optString("product_name") }
        val code = product.optString("code")
        if (name.isBlank() || code.isBlank()) return null
        val nutriments = product.optJSONObject("nutriments") ?: JSONObject()
        val kcal = nutriments.optDouble("energy-kcal_100g", Double.NaN)
        if (kcal.isNaN()) return null
        // ビタミンCはg単位で返ることがあるため、1未満の値はgとしてmgへ換算する。
        fun mg(key: String): Double {
            val value = nutriments.optDouble(key, 0.0)
            return if (value in 0.0..1.0) value * 1_000 else value.coerceAtLeast(0.0)
        }
        return ExternalFood(
            barcode = code,
            name = name,
            brand = product.optString("brands").takeIf { it.isNotBlank() },
            caloriesPer100g = kcal.coerceAtLeast(0.0),
            proteinPer100g = nutriments.optDouble("proteins_100g", 0.0).coerceAtLeast(0.0),
            fatPer100g = nutriments.optDouble("fat_100g", 0.0).coerceAtLeast(0.0),
            carbsPer100g = nutriments.optDouble("carbohydrates_100g", 0.0).coerceAtLeast(0.0),
            vitaminCMgPer100g = mg("vitamin-c_100g"),
            calciumMgPer100g = mg("calcium_100g"),
            ironMgPer100g = mg("iron_100g"),
        )
    }
}

/**
 * Open Food Factsへの検索。送信するのは検索キーワードまたはバーコード番号のみで、
 * 健康データは一切送信しない。取得結果は端末内でのみ利用する(2026-07-12ユーザー承認)。
 */
class OpenFoodFactsRepository {
    companion object {
        private const val BASE = "https://world.openfoodfacts.org"
        private const val USER_AGENT = "VitaMorph-Android/0.9 (personal health game)"
        private const val TIMEOUT_MS = 8_000
    }

    suspend fun searchByName(query: String): Result<List<ExternalFood>> = withContext(Dispatchers.IO) {
        runCatching {
            val encoded = URLEncoder.encode(query.trim(), "UTF-8")
            val url = "$BASE/cgi/search.pl?search_terms=$encoded&search_simple=1&action=process&json=1" +
                "&page_size=10&fields=code,product_name,product_name_ja,brands,nutriments"
            OpenFoodFactsParser.parseSearch(fetch(url))
        }
    }

    suspend fun findByBarcode(barcode: String): Result<ExternalFood?> = withContext(Dispatchers.IO) {
        runCatching {
            val code = barcode.filter(Char::isDigit)
            require(code.length in 6..14) { "invalid barcode" }
            val url = "$BASE/api/v2/product/$code.json?fields=code,product_name,product_name_ja,brands,nutriments"
            OpenFoodFactsParser.parseProduct(fetch(url))
        }
    }

    private fun fetch(urlString: String): String {
        val connection = URL(urlString).openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.setRequestProperty("User-Agent", USER_AGENT)
            if (connection.responseCode !in 200..299) {
                error("Open Food Facts応答エラー: ${connection.responseCode}")
            }
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
