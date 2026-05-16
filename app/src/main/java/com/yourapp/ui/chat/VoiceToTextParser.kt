package com.yourapp.ui.chat

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.pm.PackageManager
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

@Singleton
class VoiceToTextParser @Inject constructor(
    private val app: Application
) : RecognitionListener {

    private val _state = MutableStateFlow(VoiceToTextParserState())
    val state = _state.asStateFlow()

    private val recognizer = SpeechRecognizer.createSpeechRecognizer(app)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var audioRecord: AudioRecord? = null
    private var amplitudeJob: Job? = null

    fun startListening(languageCode: String = "en-US") {
        if (!SpeechRecognizer.isRecognitionAvailable(app)) {
            _state.update { it.copy(error = "Speech recognition is not available") }
            return
        }
        if (ContextCompat.checkSelfPermission(app, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            _state.update { it.copy(error = "Microphone permission is required") }
            return
        }

        _state.update { VoiceToTextParserState(isSpeaking = true) }
        startAmplitudeMonitoring()

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
        }

        recognizer.setRecognitionListener(this)
        recognizer.startListening(intent)
    }

    fun stopListening() {
        _state.update { it.copy(isSpeaking = false, amplitude = 0f) }
        stopAmplitudeMonitoring()
        recognizer.stopListening()
    }

    override fun onReadyForSpeech(params: Bundle?) {
        _state.update { it.copy(error = null) }
    }

    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {
        _state.update { it.copy(isSpeaking = false, amplitude = 0f) }
        stopAmplitudeMonitoring()
    }

    override fun onError(error: Int) {
        _state.update { it.copy(error = "Error: $error", isSpeaking = false, amplitude = 0f) }
        stopAmplitudeMonitoring()
    }

    override fun onResults(results: Bundle?) {
        results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.getOrNull(0)?.let { text ->
            _state.update { it.copy(spokenText = text) }
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {}
    override fun onEvent(eventType: Int, params: Bundle?) {}

    @SuppressLint("MissingPermission")
    private fun startAmplitudeMonitoring() {
        stopAmplitudeMonitoring(resetAmplitude = false)
        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            _state.update { it.copy(error = "Unable to read microphone amplitude") }
            return
        }

        val bufferSize = minBufferSize.coerceAtLeast(MIN_BUFFER_SIZE)
        val record = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize,
        )
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            _state.update { it.copy(error = "Unable to initialize microphone amplitude") }
            return
        }

        audioRecord = record
        amplitudeJob = scope.launch {
            val buffer = ShortArray(bufferSize / BYTES_PER_SHORT)
            try {
                record.startRecording()
                while (isActive) {
                    val read = record.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        var sumSquares = 0.0
                        for (index in 0 until read) {
                            val sample = buffer[index].toDouble()
                            sumSquares += sample * sample
                        }
                        val rms = sqrt(sumSquares / read) / Short.MAX_VALUE
                        _state.update {
                            it.copy(amplitude = (rms * AMPLITUDE_GAIN).toFloat().coerceIn(0f, 1f))
                        }
                    }
                }
            } catch (_: IllegalStateException) {
                _state.update { it.copy(error = "Unable to read microphone amplitude") }
            } finally {
                runCatching {
                    if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                        record.stop()
                    }
                }
                record.release()
                if (audioRecord === record) {
                    audioRecord = null
                }
            }
        }
    }

    private fun stopAmplitudeMonitoring(resetAmplitude: Boolean = true) {
        val record = audioRecord
        amplitudeJob?.cancel()
        amplitudeJob = null
        if (record?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            runCatching { record.stop() }
        }
        if (resetAmplitude) {
            _state.update { it.copy(amplitude = 0f) }
        }
    }

    private companion object {
        private const val SAMPLE_RATE = 16_000
        private const val MIN_BUFFER_SIZE = 2_048
        private const val BYTES_PER_SHORT = 2
        private const val AMPLITUDE_GAIN = 8f
    }
}

data class VoiceToTextParserState(
    val spokenText: String = "",
    val isSpeaking: Boolean = false,
    val amplitude: Float = 0f,
    val error: String? = null
)
