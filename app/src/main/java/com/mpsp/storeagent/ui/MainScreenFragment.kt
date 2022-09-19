package com.mpsp.storeagent.ui

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.auth.oauth2.GoogleCredentials
import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.cloud.dialogflow.v2.*
import com.mpsp.storeagent.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class MainScreenFragment : Fragment() {
    private var sessionsClient: SessionsClient? = null
    private var sessionName: SessionName? = null
    private val uuid = UUID.randomUUID().toString()
    private var projectId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return ComposeView(requireContext()).apply {
            setContent {
                createMainScreen()
            }
        }
    }

    @Composable
    private fun createMainScreen() {
        var text by remember {
            mutableStateOf("hi")
        }

        Column {
            OutlinedButton(
                onClick = { communicateWithAgent(text) }
            ) {

            }

            Text(text = text)
        }
    }

    private fun communicateWithAgent(text: String) {
        try {
            val stream = this.resources.openRawResource(R.raw.newcredentials)
            val credentials: GoogleCredentials = GoogleCredentials.fromStream(stream)
                .createScoped("https://www.googleapis.com/auth/cloud-platform")
            projectId = (credentials as ServiceAccountCredentials).projectId
            val settingsBuilder: SessionsSettings.Builder = SessionsSettings.newBuilder()
            val sessionsSettings: SessionsSettings = settingsBuilder.setCredentialsProvider(
                FixedCredentialsProvider.create(credentials)
            ).build()
            sessionsClient = SessionsClient.create(sessionsSettings)
            sessionName = SessionName.of(projectId, uuid)

            GlobalScope.launch {
                val stream = requireContext().resources.openRawResource(R.raw.newcredentials)
                val credentials: GoogleCredentials = GoogleCredentials.fromStream(stream)
                    .createScoped("https://www.googleapis.com/auth/cloud-platform")
                projectId = (credentials as ServiceAccountCredentials).projectId
                val settingsBuilder: SessionsSettings.Builder = SessionsSettings.newBuilder()
                val sessionsSettings: SessionsSettings = settingsBuilder.setCredentialsProvider(
                    FixedCredentialsProvider.create(credentials)
                ).build()

                withContext(Dispatchers.IO) {
                    val input = QueryInput.newBuilder()
                        .setText(TextInput.newBuilder().setText("hi").setLanguageCode("en-US")).build()

                    val detectIntentRequest = DetectIntentRequest.newBuilder()
                        .setSession(sessionName.toString())
                        .setQueryInput(input)
                        .build()
                    val result = sessionsClient?.detectIntent(detectIntentRequest)
                    if (result != null) {
                        result.toString()
                    }
                }
            }
        } catch (e: Exception) {
        }
    }
}