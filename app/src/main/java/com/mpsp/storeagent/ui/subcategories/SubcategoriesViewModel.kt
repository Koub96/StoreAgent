package com.mpsp.storeagent.ui.subcategories

import android.os.Parcelable
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.PersistState
import com.mpsp.storeagent.App
import com.mpsp.storeagent.models.Subcategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

@Parcelize
data class SubcategoriesFragmentArgs(
    val masterCategoryId: String
) : Parcelable

data class SubcategoriesState(
    @PersistState val masterCategoryId: String = "",
    val subcategories: List<Subcategory> = emptyList()
) : MavericksState {
    constructor(args: SubcategoriesFragmentArgs) : this(args.masterCategoryId)
}

class SubcategoriesViewModel(initialState: SubcategoriesState) : MavericksViewModel<SubcategoriesState>(initialState) {
    val appDatabase = App.getInstance().getDatabase()

    init {
        fetchSubcategories()
    }

    private fun fetchSubcategories()  = withState { state ->
        val id = state.masterCategoryId
        viewModelScope.launch(Dispatchers.IO) {
            val masterSubcategories = appDatabase.MasterSubcategoryDao().getMasterSubcategories(id)
            val subcategories = arrayListOf<Subcategory>()
            masterSubcategories.forEach {
                val subcategory = appDatabase.SubcategoryDao().getSubcategory(it.subcategoryID)
                subcategories.add(subcategory)
            }

            setState {
                copy(
                    subcategories = subcategories.toList()
                )
            }
        }
    }
}