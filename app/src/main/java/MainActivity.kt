package de.dpg.qr // Ersetze dies mit deinem tatsächlichen Package-Namen

import android.annotation.SuppressLint // Import for SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.qr.ui.theme.QrTheme // Make sure this theme name is correct
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.*

// --- QrViewModel Class ---
class QrViewModel : ViewModel() {

    private val _inputText = mutableStateOf("")
    val inputText: State<String> = _inputText

    private val _qrBitmap = mutableStateOf<Bitmap?>(null)
    val qrBitmap: State<Bitmap?> = _qrBitmap

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage

    private val _sessionRecentTexts = mutableStateOf<List<String>>(emptyList())
    val sessionRecentTexts: State<List<String>> = _sessionRecentTexts

    private val maxSessionRecentTexts = 5

    fun onInputTextChanged(newText: String) {
        _inputText.value = newText
        if (newText.isNotBlank()) {
            generateQrCodeAndUpdateState() // Live QR generation and update recents
        } else {
            _qrBitmap.value = null
            _errorMessage.value = null
        }
    }

    private fun generateQrCodeAndUpdateState() {
        val textToEncode = _inputText.value
        if (textToEncode.isNotBlank()) {
            try {
                _qrBitmap.value = generateQrBitmapInternal(textToEncode)
                _errorMessage.value = null
                addTextToRecents(textToEncode) // Add the current text (possibly appended) to recents
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "Fehler beim Generieren."
                _qrBitmap.value = null
            }
        }
    }

    private fun addTextToRecents(text: String) {
        if (text.isBlank()) return
        val updatedRecent = _sessionRecentTexts.value.toMutableList()
        updatedRecent.remove(text)
        updatedRecent.add(0, text)
        _sessionRecentTexts.value = updatedRecent.take(maxSessionRecentTexts)
    }

    fun onRecentTextSelected(selectedRecentText: String) {
        val currentTextInField = _inputText.value
        val newAppendedText = if (currentTextInField.isBlank()) {
            selectedRecentText
        } else {
            "$currentTextInField $selectedRecentText" // Append with a space
        }
        _inputText.value = newAppendedText

        if (_inputText.value.isNotBlank()) {
            generateQrCodeAndUpdateState() // Regenerate QR for the new appended text and update recents
        } else {
            _qrBitmap.value = null
            _errorMessage.value = null
        }
    }

    fun clearInputText() {
        _inputText.value = ""
        _qrBitmap.value = null
        _errorMessage.value = null
    }

    private fun generateQrBitmapInternal(text: String, width: Int = 512, height: Int = 512): Bitmap? {
        val qrCodeWriter = QRCodeWriter()
        val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java)
        hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
        hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.L
        val bitMatrix: BitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height, hints)
        val bmp = createBitmap(bitMatrix.width, bitMatrix.height, Bitmap.Config.RGB_565)
        for (x in 0 until bitMatrix.width) {
            for (y in 0 until bitMatrix.height) {
                bmp[x, y] = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
            }
        }
        return bmp
    }
}
// --- End of QrViewModel ---

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QrTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    QrCodeGeneratorScreen(qrViewModel = viewModel())
                }
            }
        }
    }
}

@Composable
fun QrCodeGeneratorScreen(modifier: Modifier = Modifier, qrViewModel: QrViewModel = viewModel()) {
    val textToEncode by qrViewModel.inputText
    val qrBitmap by qrViewModel.qrBitmap
    val errorMessage by qrViewModel.errorMessage
    val recentTexts by qrViewModel.sessionRecentTexts

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "DPG-QR Generator",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = textToEncode,
            onValueChange = { qrViewModel.onInputTextChanged(it) },
            label = { Text("Text für QR-Code eingeben") },
            modifier = Modifier.fillMaxWidth(),
            isError = errorMessage != null && textToEncode.isNotBlank()
        )

        // Optional: Button to clear the input field
        Button(
            onClick = { qrViewModel.clearInputText() },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Eingabe löschen") // "Clear Input"
        }

        errorMessage?.let {
            if (textToEncode.isNotBlank()) {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp)) // Adjusted spacer

        if (textToEncode.isNotBlank() && qrBitmap != null) {
            Image(
                bitmap = qrBitmap!!.asImageBitmap(),
                contentDescription = "Generierter QR-Code",
                modifier = Modifier
                    .size(256.dp)
            )
        } else if (textToEncode.isNotBlank() && errorMessage != null) {
            Box(
                modifier = Modifier
                    .size(256.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("QR-Code konnte nicht generiert werden.")
            }
        } else {
            Box(
                modifier = Modifier
                    .size(256.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(if (textToEncode.isBlank()) "Text Eingeben um QR-Code Anzuzeigen" else "")
            }
        }

        if (recentTexts.isNotEmpty()) {
            Text(
                text = "Kürzlich (diese Sitzung):",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 150.dp)
            ) {
                items(recentTexts) { text ->
                    Text(
                        text = text,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { qrViewModel.onRecentTextSelected(text) }
                            .padding(vertical = 8.dp)
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

// Add SuppressLint if you want to hide the "ViewModelConstructor" or similar warning for previews.
// Replace "ComposeViewModelInjection" with the actual lint ID if different.
@SuppressLint("ComposeViewModelInjection", "ViewModelConstructorInComposable") // THIS IS THE "FIX" FOR THE LINT WARNING
@Preview(showBackground = true, widthDp = 320, heightDp = 720)
@Composable
fun DefaultPreview() {
    QrTheme {
        QrCodeGeneratorScreen(qrViewModel = QrViewModel())
    }
}
