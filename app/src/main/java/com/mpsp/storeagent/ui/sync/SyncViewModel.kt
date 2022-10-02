package com.mpsp.storeagent.ui.sync

import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.cloud.dialogflow.v2.AgentName
import com.google.cloud.dialogflow.v2.Intent
import com.google.cloud.dialogflow.v2.IntentsClient
import com.google.cloud.dialogflow.v2.IntentsSettings
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.mpsp.storeagent.App
import com.mpsp.storeagent.AppConstants
import com.mpsp.storeagent.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class SyncState(val title: String = "") : MavericksState

class SyncViewModel(initialState: SyncState) : MavericksViewModel<SyncState>(initialState) {
    val WITH_DELIMITER = "((?<=%1\$s)|(?=%1\$s))"

    private val database = FirebaseDatabase.getInstance()
    private val productParametersDatabaseRef = database.getReference("productParams")

    private val localDatabase: AppDatabase = App.getInstance().getDatabase()

    init {

    }

    fun initiateSyncProcess() {
        viewModelScope.launch(Dispatchers.IO) {
//            App.getInstance().getDatabase().ProductDao().insertProducts(
//                arrayListOf(
//                    Product(1),
//                    Product(2)
//                )
//            )

            val valueListener = object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val trainingPhrases = snapshot.value as HashMap<String, ArrayList<String>>
                    val productTrainingPhrases = trainingPhrases["trainingPhrases"]
                    val productTrainingPhrasesWithQuantity = trainingPhrases["trainingPhrasesWithQuantity"]

                    if(productTrainingPhrases.isNullOrEmpty() || productTrainingPhrasesWithQuantity.isNullOrEmpty())
                        //TODO Inform the UI
                        return

                    //TODO Iterate over all training phrases and produce the final training phrases with the product names.
                    viewModelScope.launch(Dispatchers.IO) {
                        val finalProductTrainingPhrases = arrayListOf<String>()
                        val finalProductWithQuantityTrainingPhrases = arrayListOf<String>()

                        val products = localDatabase.ProductDao().getProducts()
                        products.forEach { product ->
                            productTrainingPhrases.forEach { productTrainingPhrase ->
                                val finalTrainingPhrase = productTrainingPhrase.replace("@product", product.name)
                                finalProductTrainingPhrases.add(finalTrainingPhrase)
                            }

                            productTrainingPhrasesWithQuantity.forEach { productTrainingPhraseWithQuantity ->
                                val finalTrainingPhraseWithQuantity = productTrainingPhraseWithQuantity.replace("@product", product.name)
                                finalProductWithQuantityTrainingPhrases.add(finalTrainingPhraseWithQuantity)
                            }
                        }

                        if(productTrainingPhrases.isNullOrEmpty() || productTrainingPhrasesWithQuantity.isNullOrEmpty())
                            //TODO Inform the UI
                            return@launch

                        //TODO First set the Entities to the Agent

                        val intentClientSettings: IntentsSettings = IntentsSettings.newBuilder()
                            .setCredentialsProvider(FixedCredentialsProvider.create(AppConstants.agentCredentials)).build()
                        val intentClient: IntentsClient = IntentsClient.create(intentClientSettings)
                        val parentAgent: AgentName = AgentName.of(AppConstants.projectId)

                        val agentTrainingPhrasesProduct = emptyList<Intent.TrainingPhrase>()

                        val trainingPhraseParts: ArrayList<Intent.TrainingPhrase.Part> = arrayListOf(
                            Intent.TrainingPhrase.Part.newBuilder().setText("New training phrase 2 ").build(),
                            Intent.TrainingPhrase.Part.newBuilder().setText("Playstation 4").setEntityType("@product:product").build()
                        )
                        val trainingPhrases: ArrayList<Intent.TrainingPhrase> = arrayListOf<Intent.TrainingPhrase>()
                        trainingPhrases.add(
                            Intent.TrainingPhrase.newBuilder()
                                .addAllParts(
                                    trainingPhraseParts
                                ).build()
                        )
                    }

                }

                override fun onCancelled(error: DatabaseError) {
                    //TODO Inform the UI
                }

            }

            productParametersDatabaseRef.addListenerForSingleValueEvent(valueListener)
        }
    }
}