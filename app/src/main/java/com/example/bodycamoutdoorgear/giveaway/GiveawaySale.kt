package com.example.bodycamoutdoorgear.giveaway

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bodycamoutdoorgear.i18n.tr
import java.util.Currency
import java.util.Locale

/** The two kinds of card the camera can make. */
enum class CardMode { GIVEAWAY, SALE }

/** What the card is drawn as: a giveaway (yellow, FREE stamp) or a sale (blue, price tag). */
sealed class CardStyle {
    object Giveaway : CardStyle()

    /** [price] is the tag's text; null means the default "Contact me for price". */
    data class Sale(val price: String?) : CardStyle()
}

/**
 * A currency the seller can pick. Sales are local, so the card shows just the symbol
 * ("Rp 150.000", "$25"); the code is only in the picker, where "$" alone is ambiguous.
 */
data class SaleCurrency(val code: String, val symbol: String, val dotGrouping: Boolean = false) {
    companion object {
        val all = listOf(
            SaleCurrency("IDR", "Rp", true), SaleCurrency("USD", "$"), SaleCurrency("EUR", "€", true),
            SaleCurrency("GBP", "£"), SaleCurrency("JPY", "¥"), SaleCurrency("KRW", "₩"), SaleCurrency("CNY", "¥"),
            SaleCurrency("TWD", "NT$"), SaleCurrency("HKD", "$"), SaleCurrency("SGD", "$"), SaleCurrency("MYR", "RM"),
            SaleCurrency("THB", "฿"), SaleCurrency("VND", "₫", true), SaleCurrency("PHP", "₱"), SaleCurrency("INR", "₹"),
            SaleCurrency("AUD", "$"), SaleCurrency("NZD", "$"), SaleCurrency("CAD", "$"), SaleCurrency("MXN", "$"),
            SaleCurrency("BRL", "R$", true), SaleCurrency("AED", "AED"), SaleCurrency("ZAR", "R"), SaleCurrency("CHF", "CHF"),
        )

        fun with(code: String): SaleCurrency = all.firstOrNull { it.code == code } ?: all.first { it.code == "USD" }

        private fun prefs(context: Context) = context.getSharedPreferences("giveaway", Context.MODE_PRIVATE)

        /** The last currency used, or the one for the phone's region, or dollars. */
        fun savedCode(context: Context): String {
            prefs(context).getString("sale_currency", null)?.let { saved -> if (all.any { it.code == saved }) return saved }
            val regional = try { Currency.getInstance(Locale.getDefault()).currencyCode } catch (_: Exception) { null }
            return if (regional != null && all.any { it.code == regional }) regional else "USD"
        }

        fun remember(context: Context, code: String) = prefs(context).edit().putString("sale_currency", code).apply()
    }
}

object SalePrice {
    /** The longest amount that can be typed. */
    const val MAX_LENGTH = 12

    /** What the price field may hold: digits and one decimal point (a decimal comma is accepted). */
    fun sanitized(text: String): String {
        val out = StringBuilder()
        var hasPoint = false
        for (ch in text) {
            if (ch in '0'..'9') out.append(ch)
            else if ((ch == '.' || ch == ',') && !hasPoint && out.isNotEmpty()) { out.append('.'); hasPoint = true }
            if (out.length >= MAX_LENGTH) break
        }
        return out.toString()
    }

    /**
     * The text for the price tag, such as "Rp 150.000" or "$25.50". Null when nothing (or
     * zero) was entered, which makes the tag say "Contact me for price".
     */
    fun format(amount: String, currencyCode: String): String? {
        val clean = sanitized(amount)
        if (clean.isEmpty()) return null
        val parts = clean.split('.', limit = 2)
        val whole = parts[0].ifEmpty { "0" }
        var fraction = if (parts.size > 1) parts[1].take(2) else ""
        if (fraction.all { it == '0' }) fraction = ""        // "25.00" shows as "25"
        if (fraction.length == 1) fraction += "0"            // "25.5" is read as 25.50
        if (whole.all { it == '0' } && fraction.isEmpty()) return null
        val currency = SaleCurrency.with(currencyCode)
        val group = if (currency.dotGrouping) "." else ","
        val decimal = if (currency.dotGrouping) "," else "."
        val trimmed = whole.trimStart('0').ifEmpty { "0" }
        val grouped = trimmed.reversed().chunked(3).joinToString(group).reversed()
        val text = if (fraction.isNotEmpty()) grouped + decimal + fraction else grouped
        // Letter symbols ("Rp", "RM") get a space; sign symbols ("$", "€") do not.
        val spaced = currency.symbol.last().isLetter()
        return currency.symbol + (if (spaced) " " else "") + text
    }
}

/** The one-time "before you sell something" agreement, kept apart from the giveaway one. */
object SaleAgreement {
    const val CURRENT_VERSION = 1

    private fun prefs(context: Context) = context.getSharedPreferences("giveaway", Context.MODE_PRIVATE)

    fun needed(context: Context): Boolean = prefs(context).getInt("sale_agreement_version", 0) < CURRENT_VERSION
    fun accept(context: Context) = prefs(context).edit().putInt("sale_agreement_version", CURRENT_VERSION).apply()
}

private val FieldGrey = Color(0xFF2E2E2E)

/** Currency picker and amount, shown on the sale card's screen. */
@Composable
fun SalePriceField(amount: String, onAmount: (String) -> Unit, currencyCode: String, onCurrency: (String) -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    val current = SaleCurrency.with(currencyCode)
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFF1A1A1A)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                tr("Price"), color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp,
                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, modifier = Modifier.width(56.dp),
            )
            Box {
                Text(
                    "${current.symbol} ${current.code}  ▾", color = Color.White, fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(FieldGrey)
                        .clickable { menuOpen = true }.padding(10.dp),
                )
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    SaleCurrency.all.forEach { currency ->
                        DropdownMenuItem(
                            text = { Text("${currency.symbol}  ${currency.code}", fontFamily = FontFamily.Monospace) },
                            onClick = { onCurrency(currency.code); menuOpen = false },
                        )
                    }
                }
            }
            BasicTextField(
                value = amount,
                onValueChange = { onAmount(SalePrice.sanitized(it)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = TextStyle(color = Color.White, fontSize = 14.sp, fontFamily = FontFamily.Monospace),
                cursorBrush = SolidColor(Color(0xFFCFE3FF)),
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(6.dp)).background(FieldGrey).padding(10.dp),
            )
        }
        Text(
            tr("Leave the price empty to ask buyers to contact you."), color = Color.White.copy(alpha = 0.5f),
            fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(start = 66.dp),
        )
    }
}
