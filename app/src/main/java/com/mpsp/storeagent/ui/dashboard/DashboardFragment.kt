package com.mpsp.storeagent.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.mpsp.storeagent.R
import java.util.*


class DashboardFragment : Fragment(), RecognitionListener {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                CreateDashboardScreen()
            }
        }
    }

    @Composable
    private fun CreateDashboardScreen() {
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        initiateSpeechToText()
                    },
                    content = {
                        Icon(
                            painter = painterResource(id = R.drawable.mic),
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                )
            },
            content = {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 128.dp),
                    modifier = Modifier.padding(it)
                ) {
                    items(5) { index ->
                        Text(text = "Item: $index")
                    }
                }
            }
        )
    }

    private fun initiateSpeechToText() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        intent.putExtra(
            RecognizerIntent.EXTRA_CALLING_PACKAGE,
            requireContext().applicationInfo.packageName
        )
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 2)
        intent.putExtra(
            RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS,
            5000L
        )
        val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext())
        speechRecognizer.setRecognitionListener(this)
        speechRecognizer.startListening(intent)
    }

    override fun onReadyForSpeech(params: Bundle?) {

    }

    override fun onBeginningOfSpeech() {

    }

    override fun onRmsChanged(rmsdB: Float) {

    }

    override fun onBufferReceived(buffer: ByteArray?) {

    }

    override fun onEndOfSpeech() {
        this.toString()
    }

    override fun onError(error: Int) {
        error.toString()
    }

    override fun onResults(results: Bundle?) {
        results.toString()
        val res = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        res.toString()
    }

    override fun onPartialResults(partialResults: Bundle?) {

    }

    override fun onEvent(eventType: Int, params: Bundle?) {

    }
}