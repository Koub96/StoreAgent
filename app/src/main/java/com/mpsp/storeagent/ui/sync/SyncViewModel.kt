package com.mpsp.storeagent.ui.sync

import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.cloud.dialogflow.v2.*
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.protobuf.FieldMask
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
                                val startingPoint = productTrainingPhrase.indexOfFirst { it == '@' }
                                val endingPoint = productTrainingPhrase.indexOfLast { it == '@' }
                                if(startingPoint < 0 || endingPoint < 0)
                                    //TODO Inform the UI
                                    return@launch

                                val processedTrainingPhrase = productTrainingPhrase.replaceRange(startingPoint + 1, endingPoint, product.name)
                                finalProductTrainingPhrases.add(processedTrainingPhrase)
                            }

                            productTrainingPhrasesWithQuantity.forEach { productTrainingPhraseWithQuantity ->
                                val startingPoint = productTrainingPhraseWithQuantity.indexOfFirst { it == '@' }
                                val endingPoint = productTrainingPhraseWithQuantity.indexOfLast { it == '@' }
                                if(startingPoint < 0 || endingPoint < 0)
                                //TODO Inform the UI
                                    return@launch

                                val processedTrainingPhrase = productTrainingPhraseWithQuantity.replaceRange(startingPoint + 1, endingPoint, product.name)
                                finalProductWithQuantityTrainingPhrases.add(processedTrainingPhrase)
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

                        val agentTrainingPhrasesProduct = arrayListOf<Intent.TrainingPhrase>()
                        finalProductTrainingPhrases.forEach { phrase ->
                            val startingPoint = phrase.indexOfFirst { it == '@' }
                            val endingPoint = phrase.indexOfLast { it == '@' }

                            val partBeforeProduct = phrase.substringBefore("@")
                            val product = phrase.subSequence(startingPoint + 1, endingPoint).toString()
                            val partAfterProduct = phrase.substringAfterLast("@")

                            val parts = arrayListOf(
                                Intent.TrainingPhrase.Part.newBuilder().setText(partBeforeProduct).build(),
                                Intent.TrainingPhrase.Part.newBuilder().setText(product).setEntityType("@product:product").build(),
                                Intent.TrainingPhrase.Part.newBuilder().setText(partAfterProduct).build(),
                            )

                            agentTrainingPhrasesProduct.add(
                                Intent.TrainingPhrase.newBuilder().addAllParts(
                                    parts
                                ).build()
                            )
                        }

                        val agentTrainingPhrasesProductWithQuantity = arrayListOf<Intent.TrainingPhrase>()
                        finalProductWithQuantityTrainingPhrases.forEach { phrase ->
                            val productStartingPoint = phrase.indexOfFirst { it == '@' }
                            val productEndingPoint = phrase.indexOfLast { it == '@' }

                            val partBeforeProduct = phrase.substringBefore("@")
                            val product = phrase.subSequence(productStartingPoint + 1, productEndingPoint).toString()
                            val partAfterProduct = phrase.substringAfterLast("@")

                            var parts = arrayListOf<Intent.TrainingPhrase.Part>()

                            if(partBeforeProduct.contains("#"))
                            {
                                val quantityStartingPoint = partBeforeProduct.indexOfFirst { it == '#' }
                                val quantityEndingPoint = partBeforeProduct.indexOfLast { it == '#' }
                                val phraseBeforeQuantity = partBeforeProduct.substringBefore("#")
                                val quantity = partBeforeProduct.subSequence(quantityStartingPoint + 1, quantityEndingPoint).toString()
                                val phraseAfterQuantity = partBeforeProduct.substringAfterLast("#")

                                parts = arrayListOf(
                                    Intent.TrainingPhrase.Part.newBuilder() .setText(phraseBeforeQuantity).build(),
                                    Intent.TrainingPhrase.Part.newBuilder().setText(quantity).setEntityType("@sys.number").build(),
                                    Intent.TrainingPhrase.Part.newBuilder().setText(phraseAfterQuantity).build(),
                                    Intent.TrainingPhrase.Part.newBuilder().setText(product).setEntityType("@product:product").build(),
                                    Intent.TrainingPhrase.Part.newBuilder().setText(partAfterProduct).build(),
                                )

                            } else if (partAfterProduct.contains("#")) {
                                val quantityStartingPoint = partAfterProduct.indexOfFirst { it == '#' }
                                val quantityEndingPoint = partAfterProduct.indexOfLast { it == '#' }
                                val phraseBeforeQuantity = partAfterProduct.substringBefore("#")
                                val quantity = partAfterProduct.subSequence(quantityStartingPoint + 1, quantityEndingPoint).toString()
                                val phraseAfterQuantity = partAfterProduct.substringAfterLast("#")

                                parts = arrayListOf(
                                    Intent.TrainingPhrase.Part.newBuilder().setText(partBeforeProduct).build(),
                                    Intent.TrainingPhrase.Part.newBuilder().setText(product).setEntityType("@product:product").build(),
                                    Intent.TrainingPhrase.Part.newBuilder().setText(phraseBeforeQuantity).build(),
                                    Intent.TrainingPhrase.Part.newBuilder().setText(quantity).setEntityType("@sys.number").build(),
                                    Intent.TrainingPhrase.Part.newBuilder().setText(phraseAfterQuantity).build(),
                                )
                            }

                            agentTrainingPhrasesProductWithQuantity.add(
                                Intent.TrainingPhrase.newBuilder().addAllParts(
                                    parts
                                ).build()
                            )
                        }

                        //Communication with Agent
                        try {
                            val listIntentsRequest = ListIntentsRequest.newBuilder().setIntentView(IntentView.INTENT_VIEW_FULL).setParent(parentAgent.toString()).build()
                            val response: ListIntentsResponse = intentClient.listIntentsCallable().call(listIntentsRequest)
                            response.intentsList.forEach { intent ->
                                if (intent.displayName.equals("choose-product-from-product-type")) {
                                    agentTrainingPhrasesProduct.addAll(intent.trainingPhrasesList)

                                    val intentBuilder = intentClient.getIntent(intent.name).toBuilder()
                                    intentBuilder.addAllTrainingPhrases(agentTrainingPhrasesProduct)
                                    val intent: Intent = intentBuilder.build()

                                    val fieldMask = FieldMask.newBuilder().addPaths("training_phrases").build()

                                    val request = UpdateIntentRequest.newBuilder()
                                        .setIntent(intent)
                                        .setUpdateMask(fieldMask)
                                        .build()

                                    // Make API request to update intent using fieldmask
                                    val response: Intent = intentClient.updateIntent(request)
                                }

                                if (intent.displayName.equals("choose-product-with-quantity")) {
                                    agentTrainingPhrasesProductWithQuantity.addAll(intent.trainingPhrasesList)

                                    val intentBuilder = intentClient.getIntent(intent.name).toBuilder()
                                    intentBuilder.addAllTrainingPhrases(agentTrainingPhrasesProductWithQuantity)
                                    val intent: Intent = intentBuilder.build()

                                    val fieldMask = FieldMask.newBuilder().addPaths("training_phrases").build()

                                    val request = UpdateIntentRequest.newBuilder()
                                        .setIntent(intent)
                                        .setUpdateMask(fieldMask)
                                        .build()

                                    // Make API request to update intent using fieldmask
                                    val response: Intent = intentClient.updateIntent(request)
                                }
                            }
                        } catch (ex: Exception) {
                            ex.toString()
                            //TODO Inform UI - Update of training phrases failed.
                            return@launch
                        }
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