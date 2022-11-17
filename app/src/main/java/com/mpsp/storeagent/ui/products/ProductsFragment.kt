package com.mpsp.storeagent.ui.products

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import com.airbnb.mvrx.MavericksView
import com.mpsp.storeagent.R

class ProductsFragment : Fragment(), MavericksView {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                CreateProductsScreen()
            }
        }
    }

    @Composable
    private fun CreateProductsScreen() {

    }

    override fun invalidate() {}
}