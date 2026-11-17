package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.R
import java.net.URLEncoder

object ContactLauncher {

    fun sanitizePhone(raw: String): String {
        val trimmed = raw.trim()
        val hasPlus = trimmed.startsWith("+")
        val digits = trimmed.filter { it.isDigit() }
        return if (hasPlus && digits.isNotEmpty()) "+$digits" else digits
    }

    fun sanitizeWhatsAppDigits(raw: String): String {
        return raw.filter { it.isDigit() }
    }

    fun dialPhone(context: Context, phone: String) {
        val cleaned = sanitizePhone(phone)
        if (cleaned.isEmpty()) return
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleaned")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        launchSafely(context, intent)
    }

    fun sendSms(context: Context, phone: String, clientName: String = "") {
        val cleaned = sanitizePhone(phone)
        if (cleaned.isEmpty()) return
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$cleaned")).apply {
            if (clientName.isNotBlank()) {
                putExtra("sms_body", "Hola $clientName, te saludo de la agencia de autos para dar seguimiento a tu cotización.")
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        launchSafely(context, intent)
    }

    fun openWhatsApp(context: Context, phoneOrWhatsApp: String, clientName: String = "") {
        val digits = sanitizeWhatsAppDigits(phoneOrWhatsApp)
        if (digits.isEmpty()) return
        val greeting = if (clientName.isNotBlank()) {
            URLEncoder.encode("Hola $clientName, te contacto de la agencia de autos para dar seguimiento.", "UTF-8")
        } else {
            ""
        }
        val url = if (greeting.isNotEmpty()) {
            "https://wa.me/$digits?text=$greeting"
        } else {
            "https://wa.me/$digits"
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        launchSafely(context, intent)
    }

    private fun launchSafely(context: Context, intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(
                context,
                context.getString(R.string.error_no_app_for_action),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
