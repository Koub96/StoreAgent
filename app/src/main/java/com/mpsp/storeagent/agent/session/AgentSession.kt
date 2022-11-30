package com.mpsp.storeagent.agent.session

import com.google.api.gax.core.FixedCredentialsProvider
import com.google.cloud.dialogflow.v2.SessionName
import com.google.cloud.dialogflow.v2.SessionsClient
import com.google.cloud.dialogflow.v2.SessionsSettings
import com.mpsp.storeagent.singletons.AppConstants
import java.util.*

//Session Client and Session Name must live through the whole process.
object AgentSession {
    private var sessionsClient: SessionsClient? = null
    private var sessionName: SessionName? = null

    fun getSessionClient(): SessionsClient? {
        if(sessionsClient == null) {
            val settingsBuilder: SessionsSettings.Builder = SessionsSettings.newBuilder()
            val sessionsSettings: SessionsSettings = settingsBuilder.setCredentialsProvider(
                FixedCredentialsProvider.create(AppConstants.agentCredentials)
            ).build()

            sessionsClient = SessionsClient.create(sessionsSettings)
        }

        return sessionsClient
    }

    fun getSessionName(): SessionName? {
        if(sessionName == null) {
            val uuid = UUID.randomUUID().toString()
            sessionName = SessionName.of(AppConstants.projectId, uuid)
        }
        return sessionName
    }

    fun terminateSessionClient() {
        if(sessionsClient == null) {
            sessionName = null
            return
        }

        sessionsClient?.shutdown()
        sessionName = null
        sessionsClient = null
    }
}