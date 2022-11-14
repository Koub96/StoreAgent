package com.mpsp.storeagent.ui.subcategories

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import com.airbnb.mvrx.MavericksView
import com.airbnb.mvrx.fragmentViewModel
import com.mpsp.storeagent.ui.dashboard.DashboardViewModel

class SubcategoriesFragment : Fragment(), MavericksView {
    private val viewModel: SubcategoriesViewModel by fragmentViewModel(SubcategoriesViewModel::class)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                CreateMasterCategoryScreen()
            }
        }
    }

    @Composable
    private fun CreateMasterCategoryScreen() {

    }

    override fun invalidate() {}
}