package com.mpsp.storeagent

import android.app.Service
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.auth.oauth2.GoogleCredentials
import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.cloud.dialogflow.v2.SessionsSettings

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val stream = this.resources.openRawResource(R.raw.newcredentials)
        val credentials: GoogleCredentials = GoogleCredentials.fromStream(stream).createScoped("https://www.googleapis.com/auth/cloud-platform")
        val projectId = (credentials as ServiceAccountCredentials).projectId
        val settingsBuilder = SessionsSettings.newBuilder()
        val sessionSettings = settingsBuilder.setCredentialsProvider(FixedCredentialsProvider.create(credentials)).build()

        AppConstants.agentCredentials = credentials
        AppConstants.projectId = projectId
        AppConstants.sessionSettings = sessionSettings
    }
}