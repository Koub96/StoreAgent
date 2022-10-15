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
import com.mpsp.storeagent.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

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
//                    Product("1", "Playstation 2"),
//                    Product("2", "Gameboy SP")
//                )
//            )
//
//            App.getInstance().getDatabase().ProductAliasDao().insertProductAlias(
//                arrayListOf(
//                    ProductAlias("1", "1", "Alias 1"),
//                    ProductAlias("2", "1", "Alias 2"),
//                    ProductAlias("3", "2", "Alias 1"),
//                    ProductAlias("4", "2", "Alias 2")
//                )
//            )
//            App.getInstance().getDatabase().SubcategoryDao().insertSubcategory(
//                arrayListOf(
//                    Subcategory("1", "Subcategory 1", "Field1", "Numeric1"),
//                    Subcategory("2", "Subcategory 2","Field1", "Numeric1"),
//                    Subcategory("3", "Subcategory 3","Field1", "Numeric1"),
//                    Subcategory("4", "Subcategory 4","Field1", "Numeric1"),
//                )
//            )
//            App.getInstance().getDatabase().MasterCategoryDao().insertMasterCategory(
//                arrayListOf(
//                    MasterCategory("1", "Master Category 1"),
//                    MasterCategory("2", "Master Category 2"),
//                    MasterCategory("3", "Master Category 3"),
//                    MasterCategory("4", "Master Category 4")
//                )
//            )
//            App.getInstance().getDatabase().SubcategoryAliasDao().insertSubcategoryAlias(
//                arrayListOf(
//                    SubcategoryAlias("1", "1", "Alias subcategory 1"),
//                    SubcategoryAlias("2", "2", "Alias subcategory 2"),
//                    SubcategoryAlias("3", "3", "Alias subcategory 3"),
//                    SubcategoryAlias("4", "4", "Alias subcategory 4"),
//                )
//            )
//            App.getInstance().getDatabase().MasterCategoryAliasDao().insertMasterAlias(
//                arrayListOf(
//                    MasterCategoryAlias("1", "1", "Alias Master Category 1"),
//                    MasterCategoryAlias("2", "2", "Alias Master Category 2"),
//                    MasterCategoryAlias("3", "3", "Alias Master Category 3"),
//                    MasterCategoryAlias("4", "4", "Alias Master Category 4"),
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

                    viewModelScope.launch(Dispatchers.IO) {
                        val finalProductTrainingPhrases = arrayListOf<String>()
                        val finalProductWithQuantityTrainingPhrases = arrayListOf<String>()

                        val products = localDatabase.ProductDao().getProducts()
                        createProductEntitiesAndSynonyms(products)

                        val masterCategories = localDatabase.MasterCategoryDao().getMasterCategories()
                        createProductTypes(masterCategories)

                        val subcategories = localDatabase.SubcategoryDao().getSubcategories()
                        createProductSubtypes(subcategories)

                        //Create the training phrases
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

                        if(finalProductTrainingPhrases.isNullOrEmpty() || finalProductWithQuantityTrainingPhrases.isNullOrEmpty())
                            //TODO Inform the UI
                            return@launch

                        val intentClientSettings: IntentsSettings = IntentsSettings.newBuilder()
                            .setCredentialsProvider(FixedCredentialsProvider.create(AppConstants.agentCredentials)).build()
                        val intentClient: IntentsClient = IntentsClient.create(intentClientSettings)
                        val parentAgent: AgentName = AgentName.of(AppConstants.projectId)

                        val agentTrainingPhrasesProduct = createAgentProductTrainingPhrases(finalProductTrainingPhrases)
                        val agentTrainingPhrasesProductWithQuantity = createAgentProductTrainingPhrasesWithQuantity(finalProductWithQuantityTrainingPhrases)

                        //Communication with Agent to send product and product with quantity training phrases
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

    private fun createProductEntitiesAndSynonyms(products: List<Product>) {
        val entityClient = EntityTypesClient.create(
            EntityTypesSettings.newBuilder().setCredentialsProvider(FixedCredentialsProvider.create(AppConstants.agentCredentials)).build()
        )
        val entityRequest = ListEntityTypesRequest.newBuilder().setParent(AgentName.of(AppConstants.projectId).toString()).build()
        //Create the Product Entities with their Synonyms
        products.forEach { product ->
            //This query is not suspending because this function will run on IO thread.
            val productAlias = App.getInstance().getDatabase().ProductAliasDao().getProductAlias(product.id)
            val aliases = productAlias.map { productAlias ->
                productAlias.alias
            }

            try {
                entityClient.listEntityTypesCallable().call(entityRequest).entityTypesList.forEach { entityType ->
                    if(entityType.displayName.equals("product")) {
                        val entityTypeBuilder = entityClient.getEntityType(entityType.name).toBuilder()
                        val updatedEntityType = entityTypeBuilder.addAllEntities(
                            listOf<EntityType.Entity>(
                                EntityType.Entity.newBuilder().setValue(product.name)
                                    .addAllSynonyms(aliases).build()
                            )
                        ).build()

                        val fieldMask = FieldMask.newBuilder().addPaths("entities").build()
                        val request = UpdateEntityTypeRequest.newBuilder().setEntityType(updatedEntityType).setUpdateMask(fieldMask).build()
                        val response = entityClient.updateEntityType(request)
                        response.toString()
                    }
                }

                entityClient.shutdownNow()
            } catch (ex: Exception) {
                //TODO Inform the UI
            }
        }
    }

    private fun createAgentProductTrainingPhrases(finalProductTrainingPhrases: ArrayList<String>): ArrayList<Intent.TrainingPhrase> {
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

        return agentTrainingPhrasesProduct
    }

    private fun createAgentProductTrainingPhrasesWithQuantity(finalProductWithQuantityTrainingPhrases: ArrayList<String>): ArrayList<Intent.TrainingPhrase> {
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

        return agentTrainingPhrasesProductWithQuantity
    }

    private fun createProductTypes(masterCategories: List<MasterCategory>) {
        try {
            val entityClient = EntityTypesClient.create(
                EntityTypesSettings.newBuilder().setCredentialsProvider(FixedCredentialsProvider.create(AppConstants.agentCredentials)).build()
            )
            val entityRequest = ListEntityTypesRequest.newBuilder().setParent(AgentName.of(AppConstants.projectId).toString()).build()

            masterCategories.forEach { masterCategory ->
                val aliases = localDatabase.MasterCategoryAliasDao().getMasterCategoryAlias(masterCategory.id).map {
                    it.alias
                }

                entityClient.listEntityTypesCallable().call(entityRequest).entityTypesList.forEach { entityType ->
                    if(entityType.displayName.equals("product-type")) {
                        val entityTypeBuilder = entityClient.getEntityType(entityType.name).toBuilder()
                        val updatedEntityType = entityTypeBuilder.addAllEntities(
                            listOf<EntityType.Entity>(
                                EntityType.Entity.newBuilder().setValue(masterCategory.title)
                                .addAllSynonyms(aliases).build()
                            )
                        ).build()

                        val fieldMask = FieldMask.newBuilder().addPaths("entities").build()
                        val request = UpdateEntityTypeRequest.newBuilder().setEntityType(updatedEntityType).setUpdateMask(fieldMask).build()
                        val response = entityClient.updateEntityType(request)
                        response.toString()
                    }
                }
            }

            entityClient.shutdownNow()
        } catch (exception: Exception) {
            //TODO Inform the UI
            return
        }
    }

    private fun createProductSubtypes(subcategories: List<Subcategory>) {
        try {
            val entityClient = EntityTypesClient.create(
                EntityTypesSettings.newBuilder().setCredentialsProvider(FixedCredentialsProvider.create(AppConstants.agentCredentials)).build()
            )
            val entityRequest = ListEntityTypesRequest.newBuilder().setParent(AgentName.of(AppConstants.projectId).toString()).build()

            subcategories.forEach { subcategory ->
                val aliases = localDatabase.SubcategoryAliasDao().getSubcategoryAlias(subcategory.id).map {
                    it.alias
                }

                entityClient.listEntityTypesCallable().call(entityRequest).entityTypesList.forEach { entityType ->
                    if(entityType.displayName.equals("product-subtype")) {
                        val entityTypeBuilder = entityClient.getEntityType(entityType.name).toBuilder()
                        val updatedEntityType = entityTypeBuilder.addAllEntities(
                            listOf<EntityType.Entity>(
                                EntityType.Entity.newBuilder().setValue(subcategory.title)
                                .addAllSynonyms(aliases).build()
                            )
                        ).build()

                        val fieldMask = FieldMask.newBuilder().addPaths("entities").build()
                        val request = UpdateEntityTypeRequest.newBuilder().setEntityType(updatedEntityType).setUpdateMask(fieldMask).build()
                        val response = entityClient.updateEntityType(request)
                        response.toString()
                    }
                }
            }

            entityClient.shutdownNow()
        } catch (exception: Exception) {
            //TODO Inform the UI
            return
        }
    }
}