package com.example.voicetoggle

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

/**
 * Wraps Android's built-in SpeechRecognizer to provide a simple callback-based API.
 *
 * Usage:
 *   val mgr = VoiceRecognitionManager(context)
 *   mgr.startListening(
 *       onResult = { text -> /* handle recognised text */ },
 *       onError  = { msg  -> /* handle error */ },
 *       onReady  = {        /* mic is open   */ }
 *   )
 *
 * Always call destroy() when the host Activity is destroyed.
 */
class VoiceRecognitionManager(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null

    /** True if voice recognition is supported on this device. */
    val isAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(context)

    /**
     * Starts a single-shot voice recognition session.
     * The recogniser listens until silence is detected, then fires [onResult].
     */
    fun startListening(
        onResult: (String) -> Unit,
        onError: (String) -> Unit,
        onReady: () -> Unit = {},
        onBeginSpeech: () -> Unit = {},
        onEndSpeech: () -> Unit = {}
    ) {
        // Tear down any previous session
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {

            override fun onReadyForSpeech(params: Bundle?) = onReady()
            override fun onBeginningOfSpeech() = onBeginSpeech()
            override fun onEndOfSpeech() = onEndSpeech()

            override fun onResults(results: Bundle?) {
                val matches = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val best = matches?.firstOrNull()
                if (best != null) {
                    onResult(best)
                } else {
                    onError("No speech detected")
                }
            }

            override fun onError(errorCode: Int) {
                val message = when (errorCode) {
                    SpeechRecognizer.ERROR_AUDIO             -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT            -> "Client error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Missing RECORD_AUDIO permission"
                    SpeechRecognizer.ERROR_NETWORK           -> "Network error – check your connection"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT   -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH          -> "No match found – try again"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY   -> "Recogniser busy"
                    SpeechRecognizer.ERROR_SERVER            -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT    -> "No speech input detected"
                    else                                     -> "Unknown error ($errorCode)"
                }
                onError(message)
            }

            // Unused but required by the interface
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Say a voice command…")
        }

        speechRecognizer?.startListening(intent)
    }

    /** Stops any active listening session. */
    fun stopListening() {
        speechRecognizer?.stopListening()
    }

    /** Release resources. Must be called from Activity.onDestroy(). */
    fun destroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}
