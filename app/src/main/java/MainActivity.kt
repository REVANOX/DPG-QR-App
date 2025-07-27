package de.dpg.qr // Ersetze dies mit deinem tatsächlichen Package-Namen

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.qr.ui.theme.QrTheme // Stelle sicher, dass der Theme-Name korrekt ist
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.EnumMap
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Der Name des Themes wird aus deinem Projekt generiert (z.B. AppNameTheme)
            // Du findest ihn in der Datei ui/theme/Theme.kt
            QrTheme { // Passe dies an deinen Theme-Namen an
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    QrCodeGeneratorScreen()
                }
            }
        }
    }
}

@Composable
fun QrCodeGeneratorScreen(modifier: Modifier = Modifier) {
    var textToEncode by remember { mutableStateOf("") }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) } // Behalten für Fehler

    // LaunchedEffect, um QR-Code bei Textänderung zu generieren
    LaunchedEffect(textToEncode) {
        if (textToEncode.isNotBlank()) {
            try {
                // Hier könnten wir einen Debounce-Mechanismus einbauen,
                // z.B. kotlinx.coroutines.flow.debounce
                // Fürs Erste generieren wir direkt:
                qrBitmap = generateQrCode(textToEncode)
                errorMessage = null
            } catch (e: Exception) {
                e.printStackTrace()
                errorMessage = "Fehler beim Generieren." // Kürzere Fehlermeldung
                qrBitmap = null
            }
        } else {
            qrBitmap = null // Textfeld ist leer, also keinen QR-Code anzeigen
            errorMessage = null // Auch keine Fehlermeldung
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "DPG-QR Generator", // Angepasster Titel
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = textToEncode,
            onValueChange = {
                textToEncode = it
                // Die Generierungslogik ist jetzt im LaunchedEffect
            },
            label = { Text("Text für QR-Code eingeben") },
            modifier = Modifier.fillMaxWidth(),
            isError = errorMessage != null && textToEncode.isNotBlank() // Fehler nur anzeigen, wenn Text da ist
        )

        errorMessage?.let {
            if (textToEncode.isNotBlank()) { // Fehler nur anzeigen, wenn versucht wurde zu generieren
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Der Button wird nicht mehr benötigt für die Live-Generierung
        // Spacer(modifier = Modifier.height(16.dp))
        // Button( ... ) { ... }

        Spacer(modifier = Modifier.height(32.dp))

        if (textToEncode.isNotBlank() && qrBitmap != null) {
            Image(
                bitmap = qrBitmap!!.asImageBitmap(), // Sicherer Aufruf, da qrBitmap nicht null sein sollte, wenn textToEncode nicht leer ist und kein Fehler auftrat
                contentDescription = "Generierter QR-Code",
                modifier = Modifier
                    .size(256.dp)
                    .padding(top = 16.dp)
            )
        } else if (textToEncode.isNotBlank() && errorMessage != null) {
            // Optional: Platzhalter oder Fehlermeldung im Bildbereich anzeigen
            Box(
                modifier = Modifier
                    .size(256.dp)
                    .padding(top = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("QR-Code konnte nicht generiert werden.")
            }
        } else {
            // Platzhalter, wenn kein Text eingegeben wurde
            Box(
                modifier = Modifier
                    .size(256.dp)
                    .padding(top = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Hier QR-Code eingeben, um ihn anzuzeigen")
            }
        }
    }
}

fun generateQrCode(text: String, width: Int = 512, height: Int = 512): Bitmap? {
    if (text.isBlank()) return null

    val qrCodeWriter = QRCodeWriter()
    // Optional: Füge Hints hinzu, z.B. für Fehlerkorrektur oder Zeichensatz
    val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java)
    hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
    hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.L // L = Low (ca. 7% Korrektur)

    try {
        val bitMatrix: BitMatrix = qrCodeWriter.encode(
            text,
            BarcodeFormat.QR_CODE,
            width,
            height,
            hints // Füge die Hints hier hinzu
        )
        val bmp = createBitmap(bitMatrix.width, bitMatrix.height, Bitmap.Config.RGB_565)
        for (x in 0 until bitMatrix.width) {
            for (y in 0 until bitMatrix.height) {
                bmp[x, y] = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
            }
        }
        return bmp
    } catch (e: Exception) {
        // Wirf die Exception weiter oder handle sie spezifischer,
        // damit sie in QrCodeGeneratorScreen gefangen werden kann.
        throw e
    }
}

@Preview(showBackground = true, widthDp = 320, heightDp = 640)
@Composable
fun DefaultPreview() {
    QrTheme { // Passe dies an deinen Theme-Namen an
        val qrScreen = Unit
        qrScreen
    }
}

