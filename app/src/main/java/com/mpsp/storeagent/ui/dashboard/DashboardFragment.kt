package com.mpsp.storeagent.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.airbnb.mvrx.MavericksView
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksViewModel
import com.airbnb.mvrx.fragmentViewModel
import com.mpsp.storeagent.R
import java.util.*


class DashboardFragment : Fragment(), MavericksView, RecognitionListener, TextToSpeech.OnInitListener {
    private val viewModel: DashboardViewModel by fragmentViewModel(DashboardViewModel::class)
    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupTextToSpeech()
        setupViewModelSubscriptions()
    }

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

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
    }

    private fun setupViewModelSubscriptions() {
        viewModel.onEach(DashboardState::agentResponseEvent ,uniqueOnly()) { event ->
            if(textToSpeech == null)
                return@onEach

            textToSpeech?.speak(event.response, TextToSpeech.QUEUE_ADD, null)
        }
        viewModel.onEach(DashboardState::actionNavigationEvent ,uniqueOnly()) { event ->
            android.app.AlertDialog.Builder(requireContext())
                .setTitle("DEBUG")
                .setMessage("Will do naivgation with id " + event.entityID + " and action " + event.action.name)
                .setPositiveButton(android.R.string.yes) { dialog, which -> }
                .setOnDismissListener {
                }
                .show()
        }
    }

    private fun setupTextToSpeech() {
        textToSpeech = TextToSpeech(requireContext(), this)
        textToSpeech?.language = Locale.US
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(it),
                    contentAlignment = Alignment.TopStart
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 128.dp),
                    ) {
                        items(5) { index ->
                            Text(text = "Item: $index")
                        }
                    }

                    CreateSpeechDialog()
                }
            }
        )
    }

    @Composable
    private fun CreateSpeechDialog() {
        val viewModel: DashboardViewModel = mavericksViewModel()
        val showSpeechDialog = viewModel.collectAsState(DashboardState::showSpeechDialog)

        if(showSpeechDialog.value) {
            Dialog(
                onDismissRequest = {
                    speechRecognizer?.cancel()
                    viewModel.hideSpeechDialog()
                }
            ) {
                Surface(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(16)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(start = 56.dp, end = 56.dp, top = 32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        CircularProgressIndicator(
                            modifier = Modifier
                                .width(intrinsicSize = IntrinsicSize.Min)
                                .height(intrinsicSize = IntrinsicSize.Min)
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Text(
                            modifier = Modifier
                                .padding(bottom = 32.dp),
                            text = getString(R.string.agentSpeechMessage),
                            style = TextStyle(
                                color = Color.Black,
                                fontSize = 16.sp
                            )
                        )
                    }
                }
            }
        }
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
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext())
        speechRecognizer!!.setRecognitionListener(this)
        speechRecognizer!!.startListening(intent)
    }

    override fun onReadyForSpeech(params: Bundle?) {
        viewModel.showSpeechDialog()
    }

    override fun onBeginningOfSpeech() {}

    override fun onRmsChanged(rmsdB: Float) {}

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        viewModel.hideSpeechDialog()
    }

    override fun onError(error: Int) {
        //TODO Error Handling
        viewModel.hideSpeechDialog()
    }

    override fun onResults(results: Bundle?) {
        val res = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if(!res.isNullOrEmpty())
            viewModel.processSpeech(res[0])
    }

    override fun onPartialResults(partialResults: Bundle?) {}

    override fun onEvent(eventType: Int, params: Bundle?) {}

    override fun invalidate() {}

    override fun onInit(status: Int) {
        status.toString()
    }
}