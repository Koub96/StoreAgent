package com.mpsp.storeagent.ui.basket

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import com.airbnb.mvrx.MavericksView
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.compose.mavericksActivityViewModel
import com.airbnb.mvrx.fragmentViewModel
import com.mpsp.storeagent.R
class BasketFragment : Fragment(), MavericksView {

    val viewModel: BasketViewModel by fragmentViewModel(BasketViewModel::class)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                CreateBasketScreen()
            }
        }
    }

    @Composable
    private fun CreateBasketScreen() {

    }

    override fun invalidate() {}
}