package com.mpsp.storeagent

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.dialogflow.v2.SessionsSettings

object AppConstants {
    //Executed during first initialization
    init {}

    var agentCredentials: GoogleCredentials? = null
    var projectId: String = ""
    var sessionSettings: SessionsSettings? = null
    val firstInitSuccessKey = "FIRST_INIT_SUCCESS"
}