package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.WhatsAppGreen
import com.example.util.ContactLauncher

@Composable
fun DirectContactBar(
    phone: String,
    whatsAppNumber: String,
    clientName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = { ContactLauncher.dialPhone(context, phone) },
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .testTag("contact_call_button"),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Call,
                contentDescription = "Llamar a $clientName",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Llamar", maxLines = 1)
        }

        Button(
            onClick = { ContactLauncher.openWhatsApp(context, whatsAppNumber, clientName) },
            modifier = Modifier
                .weight(1.15f)
                .heightIn(min = 48.dp)
                .testTag("contact_whatsapp_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = WhatsAppGreen,
                contentColor = Color.White
            ),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Chat,
                contentDescription = "Abrir WhatsApp con $clientName",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("WhatsApp", maxLines = 1)
        }

        FilledTonalButton(
            onClick = { ContactLauncher.sendSms(context, phone, clientName) },
            modifier = Modifier
                .weight(0.9f)
                .heightIn(min = 48.dp)
                .testTag("contact_sms_button"),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Sms,
                contentDescription = "Enviar SMS a $clientName",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("SMS", maxLines = 1)
        }
    }
}
