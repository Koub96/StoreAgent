package com.mpsp.storeagent

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.dialogflow.v2.SessionsSettings

object AppConstants {
    init {}

    var agentCredentials: GoogleCredentials? = null
    var projectId: String = ""
    var sessionSettings: SessionsSettings? = null
    const val firstInitSuccessKey = "FIRST_INIT_SUCCESS"
    const val agentLanguageCode = "en-US"
}