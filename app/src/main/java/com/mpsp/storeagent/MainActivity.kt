package com.mpsp.storeagent

import android.Manifest
import android.app.PendingIntent.getActivity
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
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

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {


            ActivityCompat.requestPermissions(
                this,
                arrayOf<String>( Manifest.permission.RECORD_AUDIO ),
                100
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode == 100) {
            if(grantResults[0] == PackageManager.PERMISSION_DENIED) {
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Mandatory Permissions")
                builder.setMessage("Mandatory Permissions have been denied. Exiting Store Agent App.")

                builder.setOnDismissListener {
                    this.finish()
                }
                builder.show()
            }
        }
    }
}