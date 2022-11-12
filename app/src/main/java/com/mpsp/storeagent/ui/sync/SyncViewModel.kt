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
import com.mpsp.storeagent.models.MasterCategory
import com.mpsp.storeagent.models.Product
import com.mpsp.storeagent.models.Subcategory
import com.mpsp.storeagent.models.SyncParameters
import com.mpsp.storeagent.ui.uievents.SyncEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class SyncState(
    val syncResultEvent: SyncEvent = SyncEvent(),
    val isLoading: Boolean = false,
) : MavericksState

class SyncViewModel(initialState: SyncState) : MavericksViewModel<SyncState>(initialState) {
    val WITH_DELIMITER = "((?<=%1\$s)|(?=%1\$s))"

    private val database = FirebaseDatabase.getInstance()
    private val productParametersDatabaseRef = database.getReference("")

    private val localDatabase: AppDatabase = App.getInstance().getDatabase()

    init {

    }

    private fun setIsLoading(isLoading: Boolean) {
        setState {
            copy(isLoading = isLoading)
        }
    }

    private fun triggerSyncFinishedEvent(isSuccess: Boolean) {
        val id = java.util.UUID.randomUUID().toString()
        setState {
            copy(
                syncResultEvent = SyncEvent(
                    id = id,
                    isSuccess = isSuccess
                )
            )
        }
    }

    fun initiateSyncProcess() {
        viewModelScope.launch(Dispatchers.IO) {
            val valueListener = object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    setIsLoading(true)

                    val parameters = snapshot.getValue(SyncParameters::class.java)

                    if(parameters == null || parameters.productParams == null || parameters.masterCategoryParams == null) {
                        setIsLoading(false)
                        triggerSyncFinishedEvent(false)
                        return
                    }

                    val productTrainingPhrases = parameters.productParams.trainingPhrases
                    val productTrainingPhrasesWithQuantity = parameters.productParams.trainingPhrasesWithQuantity

                    if(productTrainingPhrases.isEmpty() || productTrainingPhrasesWithQuantity.isEmpty()) {
                        setIsLoading(false)
                        triggerSyncFinishedEvent(false)
                        return
                    }

                    viewModelScope.launch(Dispatchers.IO) {
                        //Creates product entities
                        val products = localDatabase.ProductDao().getProducts()
                        createProductEntities(products)

                        //creates product master category entities
                        val masterCategories = localDatabase.MasterCategoryDao().getMasterCategories()
                        createProductMasterCategories(masterCategories)

                        //creates product subcategory entities
                        val subcategories = localDatabase.SubcategoryDao().getSubcategories()
                        createProductSubcategories(subcategories)

                        //creates product training phrases for intent choose-product-from-product-type
                        //and choose-product-with-quantity
                        createProductTrainingPhrases(products, productTrainingPhrases, productTrainingPhrasesWithQuantity)

                        //creates master category training phrases for intent choose-product-type
                        createMasterCategoryTrainingPhrases(parameters.masterCategoryParams.trainingPhrases, masterCategories)

                        //creates master and subcategory training phrases for intent choose-product-type-and-subtype
                        val masterToSubcategories: MutableMap<MasterCategory, ArrayList<Subcategory>> = mutableMapOf()
                        val masterSubcategoriesRelation = localDatabase.MasterSubcategoryDao().getMasterSubcategories()
                        masterSubcategoriesRelation.forEach {
                            val masterCategory = localDatabase.MasterCategoryDao().getMasterCategory(it.masterCategoryID)
                            val subcategory = localDatabase.SubcategoryDao().getSubcategory(it.subcategoryID)

                            if(!masterToSubcategories.containsKey(masterCategory)) {
                                masterToSubcategories[masterCategory] = arrayListOf(subcategory)
                            } else {
                                masterToSubcategories[masterCategory]?.add(subcategory)
                            }
                        }
                        createMasterCategorySubcategoryTrainingPhrases(
                            parameters.masterAndSubcategoryParams.trainingPhrases,
                            masterToSubcategories
                        )

                        setIsLoading(false)
                        triggerSyncFinishedEvent(true)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    setIsLoading(false)
                    triggerSyncFinishedEvent(false)
                }

            }

            productParametersDatabaseRef.addListenerForSingleValueEvent(valueListener)
        }
    }

    private fun createProductTrainingPhrases(
        products: List<Product>,
        productTrainingPhrases: ArrayList<String>,
        productTrainingPhrasesWithQuantity: ArrayList<String>
    ) {
        val finalProductTrainingPhrases = arrayListOf<String>()
        val finalProductWithQuantityTrainingPhrases = arrayListOf<String>()

        products.forEach { product ->
            productTrainingPhrases.forEach { productTrainingPhrase ->
                val startingPoint = productTrainingPhrase.indexOfFirst { it == '@' }
                val endingPoint = productTrainingPhrase.indexOfLast { it == '@' }
                if(startingPoint < 0 || endingPoint < 0) {
                    setIsLoading(false)
                    triggerSyncFinishedEvent(false)
                    return
                }

                val processedTrainingPhrase = productTrainingPhrase.replaceRange(startingPoint + 1, endingPoint, product.name)
                finalProductTrainingPhrases.add(processedTrainingPhrase)
            }

            productTrainingPhrasesWithQuantity.forEach { productTrainingPhraseWithQuantity ->
                val startingPoint = productTrainingPhraseWithQuantity.indexOfFirst { it == '@' }
                val endingPoint = productTrainingPhraseWithQuantity.indexOfLast { it == '@' }
                if(startingPoint < 0 || endingPoint < 0) {
                    setIsLoading(false)
                    triggerSyncFinishedEvent(false)
                    return
                }

                val processedTrainingPhrase = productTrainingPhraseWithQuantity.replaceRange(startingPoint + 1, endingPoint, product.name)
                finalProductWithQuantityTrainingPhrases.add(processedTrainingPhrase)
            }
        }

        if(finalProductTrainingPhrases.isEmpty() || finalProductWithQuantityTrainingPhrases.isEmpty()) {
            setIsLoading(false)
            triggerSyncFinishedEvent(false)
            return
        }

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
            setIsLoading(false)
            triggerSyncFinishedEvent(false)
            return
        }
    }

    private fun createMasterCategoryTrainingPhrases(
        masterCategoryTrainingPhrases: List<String>,
        masterCategories: List<MasterCategory>
    ) {
        val finalMasterCategoryTrainingPhrases = arrayListOf<String>()

        masterCategories.forEach { masterCategory ->
            masterCategoryTrainingPhrases.forEach { trainingPhrase ->
                val startingPoint = trainingPhrase.indexOfFirst { it == '@' }
                val endingPoint = trainingPhrase.indexOfLast { it == '@' }
                if(startingPoint < 0 || endingPoint < 0) {
                    setIsLoading(false)
                    triggerSyncFinishedEvent(false)
                    return
                }

                val processedTrainingPhrase = trainingPhrase.replaceRange(startingPoint + 1, endingPoint, masterCategory.title)
                finalMasterCategoryTrainingPhrases.add(processedTrainingPhrase)
            }
        }

        if(finalMasterCategoryTrainingPhrases.isEmpty())
            return

        val intentClientSettings: IntentsSettings = IntentsSettings.newBuilder()
            .setCredentialsProvider(FixedCredentialsProvider.create(AppConstants.agentCredentials)).build()
        val intentClient: IntentsClient = IntentsClient.create(intentClientSettings)
        val parentAgent: AgentName = AgentName.of(AppConstants.projectId)

        val agentTrainingPhrasesMasterCat = createAgentMasterCategoryTrainingPhrases(finalMasterCategoryTrainingPhrases)

        try {
            val listIntentsRequest = ListIntentsRequest.newBuilder().setIntentView(IntentView.INTENT_VIEW_FULL).setParent(parentAgent.toString()).build()
            val response: ListIntentsResponse = intentClient.listIntentsCallable().call(listIntentsRequest)

            response.intentsList.forEach { intent ->
                if (intent.displayName.equals("choose-product-type")) {
                    agentTrainingPhrasesMasterCat.addAll(intent.trainingPhrasesList)

                    val intentBuilder = intentClient.getIntent(intent.name).toBuilder()
                    intentBuilder.addAllTrainingPhrases(agentTrainingPhrasesMasterCat)
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
        } catch (exception: Exception) {
            setIsLoading(false)
            triggerSyncFinishedEvent(false)
            return
        }
    }

    /*
    Updates the training phrases for the choose-product-type-subtype intent
    In this particular case, the training phrases are already broken into the proper parts.
     */
    private fun createMasterCategorySubcategoryTrainingPhrases(
        masterSubcategoryTrainingPhrases: ArrayList<ArrayList<String>>,
        masterToSubcategories: MutableMap<MasterCategory, ArrayList<Subcategory>>
    ) {
        val finalMasterSubcategoryTrainingPhrases = arrayListOf<Intent.TrainingPhrase>()
        val phraseParts: ArrayList<Intent.TrainingPhrase.Part> = arrayListOf()

        masterToSubcategories.forEach { entry ->
            val masterCategory = entry.key
            val subcategories = entry.value

            subcategories.forEach { subcategory ->
                masterSubcategoryTrainingPhrases.forEach { trainingPhrase ->
                    trainingPhrase.forEach { part ->
                        if(part.contains("@mastercategory@"))
                        {
                            phraseParts.add(
                                Intent.TrainingPhrase.Part.newBuilder()
                                    .setText(part.replace("@mastercategory@", masterCategory.title))
                                    .setEntityType("@product-type")
                                    .setAlias("product-type")
                                    .build()
                            )
                        } else if(part.contains("@subcategory@")) {
                            phraseParts.add(
                                Intent.TrainingPhrase.Part.newBuilder()
                                    .setText(part.replace("@subcategory@", subcategory.title))
                                    .setEntityType("@product-subtype")
                                    .setAlias("product-subtype")
                                    .build()
                            )
                        } else {
                            phraseParts.add(
                                Intent.TrainingPhrase.Part.newBuilder().setText(part).build()
                            )
                        }
                    }

                    finalMasterSubcategoryTrainingPhrases.add(
                        Intent.TrainingPhrase.newBuilder().addAllParts(
                            phraseParts
                        ).build()
                    )

                    phraseParts.clear()
                }
            }
        }

        if(finalMasterSubcategoryTrainingPhrases.isEmpty()) {
            setIsLoading(false)
            triggerSyncFinishedEvent(false)
            return
        }

        val intentClientSettings: IntentsSettings = IntentsSettings.newBuilder()
            .setCredentialsProvider(FixedCredentialsProvider.create(AppConstants.agentCredentials)).build()
        val intentClient: IntentsClient = IntentsClient.create(intentClientSettings)
        val parentAgent: AgentName = AgentName.of(AppConstants.projectId)

        try {
            val listIntentsRequest = ListIntentsRequest.newBuilder().setIntentView(IntentView.INTENT_VIEW_FULL).setParent(parentAgent.toString()).build()
            val response: ListIntentsResponse = intentClient.listIntentsCallable().call(listIntentsRequest)

            response.intentsList.forEach { intent ->
                if (intent.displayName.equals("choose-product-type-and-subtype")) {
                    finalMasterSubcategoryTrainingPhrases.addAll(intent.trainingPhrasesList)

                    val intentBuilder = intentClient.getIntent(intent.name).toBuilder()
                    intentBuilder.addAllTrainingPhrases(finalMasterSubcategoryTrainingPhrases)
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
        } catch (exception: Exception) {
            setIsLoading(false)
            triggerSyncFinishedEvent(false)
            return
        }
    }

    private fun createProductEntities(products: List<Product>) {
        val entityClient = EntityTypesClient.create(
            EntityTypesSettings.newBuilder().setCredentialsProvider(FixedCredentialsProvider.create(AppConstants.agentCredentials)).build()
        )
        val entityRequest = ListEntityTypesRequest.newBuilder().setParent(AgentName.of(AppConstants.projectId).toString()).build()

        val entitiesList = arrayListOf<EntityType.Entity>()
        //Create the Product Entities with their Synonyms
        products.forEach { product ->
            //This query is not suspending because this function will run on IO thread.
            val productAlias = App.getInstance().getDatabase().ProductAliasDao().getProductAlias(product.id)
            val aliases = productAlias.map { productAlias ->
                productAlias.alias
            }

            entitiesList.add(
                EntityType.Entity.newBuilder().setValue(product.name)
                    .addAllSynonyms(aliases).build()
            )
        }

        try {
            entityClient.listEntityTypesCallable().call(entityRequest).entityTypesList.forEach { entityType ->
                if(entityType.displayName.equals("product")) {
                    val entityTypeBuilder = entityClient.getEntityType(entityType.name).toBuilder()
                    val updatedEntityType = entityTypeBuilder.addAllEntities(
                        entitiesList
                    ).build()

                    val fieldMask = FieldMask.newBuilder().addPaths("entities").build()
                    val request = UpdateEntityTypeRequest.newBuilder().setEntityType(updatedEntityType).setUpdateMask(fieldMask).build()
                    val response = entityClient.updateEntityType(request)
                    response.toString()
                }
            }

            entityClient.shutdownNow()
        } catch (ex: Exception) {
            setIsLoading(false)
            triggerSyncFinishedEvent(false)
        }
    }

    private fun createAgentProductTrainingPhrases(
        finalProductTrainingPhrases: ArrayList<String>
    ): ArrayList<Intent.TrainingPhrase> {
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

    private fun createAgentMasterCategoryTrainingPhrases(
        masterCategoryTrainingPhrases: ArrayList<String>
    ): ArrayList<Intent.TrainingPhrase> {
        val agentTrainingPhrasesProduct = arrayListOf<Intent.TrainingPhrase>()
        masterCategoryTrainingPhrases.forEach { phrase ->
            val startingPoint = phrase.indexOfFirst { it == '@' }
            val endingPoint = phrase.indexOfLast { it == '@' }

            val partBeforeMasterCategory = phrase.substringBefore("@")
            val masterCategory = phrase.subSequence(startingPoint + 1, endingPoint).toString()
            val partAfterMasterCategory = phrase.substringAfterLast("@")

            val parts = arrayListOf(
                Intent.TrainingPhrase.Part.newBuilder().setText(partBeforeMasterCategory).build(),
                Intent.TrainingPhrase.Part.newBuilder().setText(masterCategory).setEntityType("@product-type").setAlias("product-type").build(),
                Intent.TrainingPhrase.Part.newBuilder().setText(partAfterMasterCategory).build(),
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

    private fun createProductMasterCategories(masterCategories: List<MasterCategory>) {
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
            setIsLoading(false)
            triggerSyncFinishedEvent(false)
            return
        }
    }

    private fun createProductSubcategories(subcategories: List<Subcategory>) {
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
            setIsLoading(false)
            triggerSyncFinishedEvent(false)
            return
        }
    }
}

//            App.getInstance().getDatabase().SubcategoryDao().insertSubcategory(
//                arrayListOf(
//                    Subcategory("1", "Playstation 5", "Field1", "Numeric1"),
//                    Subcategory("2", "Xbox Series X","Field1", "Numeric1"),
//                    Subcategory("3", "Xiaomi","Field1", "Numeric1"),
//                    Subcategory("4", "Samsung","Field1", "Numeric1"),
//                )
//            )
//            App.getInstance().getDatabase().MasterCategoryDao().insertMasterCategory(
//                arrayListOf(
//                    MasterCategory("1", "Consoles"),
//                    MasterCategory("2", "SmartPhones"),
//                )
//            )
//            App.getInstance().getDatabase().SubcategoryAliasDao().insertSubcategoryAlias(
//                arrayListOf(
//                    SubcategoryAlias(
//                        "1",
//                        "1",
//                        "PS5"
//                    ),
//                    SubcategoryAlias(
//                        "2",
//                        "2",
//                        "Xbox X"
//                    ),
//                    SubcategoryAlias(
//                        "3",
//                        "3",
//                        "Xiaomi Phone"
//                    ),
//                    SubcategoryAlias(
//                        "4",
//                        "4",
//                        "Samsung Phone"
//                    ),
//                )
//            )
//            App.getInstance().getDatabase().MasterCategoryAliasDao().insertMasterAlias(
//                arrayListOf(
//                    MasterCategoryAlias(
//                        "1",
//                        "1",
//                        "Gaming Consoles"
//                    ),
//                    MasterCategoryAlias(
//                        "2",
//                        "2",
//                        "Phones"
//                    )
//                )
//            )
//            App.getInstance().getDatabase().MasterSubcategoryDao().insertMasterSubcategory(
//                arrayListOf(
//                    MasterSubcategory("1", "1"),
//                    MasterSubcategory("1", "2"),
//                    MasterSubcategory("2", "3"),
//                    MasterSubcategory("2", "4"),
//                )
//            )
//            App.getInstance().getDatabase().ProductDao().insertProducts(
//                arrayListOf(
//                    Product(
//                        "1",
//                        "1",
//                        "1",
//                        "Playstation 5 No Disc Model"
//                    ),
//                    Product(
//                        "2",
//                        "1",
//                        "1",
//                        "Playstation 5 Disc Model"
//                    ),
//                    Product(
//                        "3",
//                        "1",
//                        "2",
//                        "Xbox Series X Disc Model"
//                    ),
//                    Product(
//                        "4",
//                        "1",
//                        "2",
//                        "Xbox Series X Disc Model"
//                    ),
//                    Product(
//                        "5",
//                        "2",
//                        "3",
//                        "Xiaomi Redmi 2"
//                    ),
//                    Product(
//                        "6",
//                        "2",
//                        "4",
//                        "Samsung Galaxy 3"
//                    )
//                )
//            )
//            App.getInstance().getDatabase().ProductAliasDao().insertProductAlias(
//                arrayListOf(
//                    ProductAlias("1", "1", "PS5"),
//                    ProductAlias("2", "2", "PS5"),
//                    ProductAlias("3", "3", "Xbox Series X"),
//                    ProductAlias("4", "4", "Xbox Series X"),
//                    ProductAlias("5", "5", "Xiaomi 2"),
//                    ProductAlias("6", "6", "Samsung 3")
//                )
//            )