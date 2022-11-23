package com.mpsp.storeagent.ui.products

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.airbnb.mvrx.MavericksView
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksViewModel
import com.airbnb.mvrx.fragmentViewModel
import com.mpsp.storeagent.R
import com.mpsp.storeagent.agent.intenthandlers.agentNavigation
import com.mpsp.storeagent.ui.basket.SharedBasketViewModel
import com.mpsp.storeagent.ui.subcategories.SubcategoriesState
import com.mpsp.storeagent.ui.subcategories.SubcategoriesViewModel
import java.util.*

class ProductsFragment : Fragment(), MavericksView, RecognitionListener, TextToSpeech.OnInitListener {
    private val sharedBasketViewModel: SharedBasketViewModel by activityViewModel(SharedBasketViewModel::class)
    private val viewModel: ProductsViewModel by fragmentViewModel(ProductsViewModel::class)
    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupViewModelSubscriptions()
        setupTextToSpeech()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                CreateProductsScreen()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
    }

    private fun setupViewModelSubscriptions() {
        viewModel.onEach(ProductsState::agentResponseEvent ,uniqueOnly()) { event ->
            if(textToSpeech == null)
                return@onEach

            textToSpeech?.speak(event.response, TextToSpeech.QUEUE_ADD, null)
        }
        viewModel.onEach(ProductsState::actionNavigationEvent ,uniqueOnly()) { event ->
            this.agentNavigation(event.action.navigationEvent, event.action.entityMapping)
        }
        viewModel.onEach(ProductsState::addToBasketEvent ,uniqueOnly()) { event ->
            sharedBasketViewModel.addProductToBasket(event.productId, event.quantity)
        }
    }

    @Composable
    private fun CreateProductsScreen() {
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
                    ProductsGridList()
                    CreateSpeechDialog()
                }
            }
        )
    }

    @Composable
    private fun ProductsGridList() {
        val viewModel: ProductsViewModel = mavericksViewModel()
        val products = viewModel.collectAsState(ProductsState::products)

        val horizontalGradientBrush = Brush.verticalGradient(
            colors = listOf(
                Color.Blue,
                Color.White
            ),
            startY = 278.0f,
            endY = 0.0f
        )

        LazyVerticalGrid(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            columns = GridCells.Adaptive(127.dp)
        ) {
            items(products.value) { item ->
                Card(
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .padding(top = 16.dp, start = 8.dp, end = 8.dp)
                        .width(80.dp)
                        .height(90.dp)
                ) {
                    Box(
                        modifier = Modifier.background(brush = horizontalGradientBrush),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = item.name, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun CreateSpeechDialog() {
        val viewModel: ProductsViewModel = mavericksViewModel()
        val showSpeechDialog = viewModel.collectAsState(ProductsState::showSpeechDialog)

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

    private fun setupTextToSpeech() {
        textToSpeech = TextToSpeech(requireContext(), this)
        textToSpeech?.language = Locale.US
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

    override fun invalidate() {}

    override fun onReadyForSpeech(params: Bundle?) {
        viewModel.showSpeechDialog()
    }

    override fun onBeginningOfSpeech() {
    }

    override fun onRmsChanged(rmsdB: Float) {
    }

    override fun onBufferReceived(buffer: ByteArray?) {
    }

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

    override fun onPartialResults(partialResults: Bundle?) {
    }

    override fun onEvent(eventType: Int, params: Bundle?) {
    }

    override fun onInit(status: Int) {
        status.toString()
    }
}