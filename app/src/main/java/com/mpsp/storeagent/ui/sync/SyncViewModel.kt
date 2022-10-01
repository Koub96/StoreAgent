package com.mpsp.storeagent.ui.sync

import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import com.mpsp.storeagent.App
import com.mpsp.storeagent.models.Product
import com.squareup.okhttp.Dispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class SyncState(val title: String = "") : MavericksState

class SyncViewModel(initialState: SyncState) : MavericksViewModel<SyncState>(initialState) {
    private val database = FirebaseDatabase.getInstance()
    private val productParametersDatabaseRef = database.getReference("productParams")

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
                }

                override fun onCancelled(error: DatabaseError) {
                    //TODO Inform the UI
                }

            }

            productParametersDatabaseRef.addListenerForSingleValueEvent(valueListener)
        }
    }
}