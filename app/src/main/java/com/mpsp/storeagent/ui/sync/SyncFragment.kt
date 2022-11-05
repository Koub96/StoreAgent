package com.mpsp.storeagent.ui.sync

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.airbnb.mvrx.MavericksView
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksViewModel
import com.airbnb.mvrx.fragmentViewModel
import com.mpsp.storeagent.AppConstants
import com.mpsp.storeagent.R


class SyncFragment : Fragment(), MavericksView {

    private val viewModel: SyncViewModel by fragmentViewModel(SyncViewModel::class)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupViewModelSubscriptions()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                CreateSyncScreen()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if(getInitializationSuccessful())
            findNavController().navigate(R.id.sync_to_dashboard)
    }

    private fun setupViewModelSubscriptions() {
        viewModel.onEach(SyncState::syncResultEvent ,uniqueOnly()) { event ->
            if(event.id.isNullOrEmpty())
                return@onEach

            val message = if(event.isSuccess)
            {
                getString(R.string.syncSuccess)
            } else {
                getString(R.string.syncFail)
            }

            android.app.AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.sync))
                .setMessage(message)
                .setPositiveButton(android.R.string.yes) { dialog, which -> }
                .setOnDismissListener {
                    setInitializationSuccessful(event.isSuccess)
                    if(event.isSuccess) {
                        findNavController().navigate(R.id.sync_to_dashboard)
                    }
                }
                .setIcon(android.R.drawable.ic_dialog_info)
                .show()
        }
    }

    @Composable
    private fun CreateSyncScreen() {
        val viewModel: SyncViewModel = mavericksViewModel()

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(modifier = Modifier.wrapContentSize(Alignment.Center)) {
                Row(modifier = Modifier.wrapContentSize()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Image(
                        imageVector = ImageVector.vectorResource(id = R.drawable.agent_face),
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = getString(R.string.syncDescription),
                        modifier = Modifier.align(Alignment.CenterVertically),
                        style = TextStyle(
                            fontStyle = FontStyle.Normal,
                            fontWeight = FontWeight.Medium,
                            textDecoration = TextDecoration.Underline,
                            fontSize = 18.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    elevation = ButtonDefaults.elevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 8.dp,
                        disabledElevation = 0.dp
                    ),
                    onClick = { viewModel.initiateSyncProcess() },
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(0.dp),
                    contentPadding = PaddingValues(15.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.White),
                    border = BorderStroke(1.dp, Color.Blue)
                ) {
                    Box(modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.Refresh,
                            modifier = Modifier
                                .size(20.dp)
                                .align(Alignment.CenterStart),
                            contentDescription = "",
                            tint = Color.Unspecified
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = getString(R.string.syncButtonDescription),
                            color = Color.Black,
                            textAlign = TextAlign.Center,
                            style = TextStyle(fontSize = 18.sp)
                        )
                    }
                }
            }

            CreateProgressDialog()
        }
    }

    @Composable
    private fun CreateProgressDialog() {
        val viewModel: SyncViewModel = mavericksViewModel()
        val loading = viewModel.collectAsState(SyncState::isLoading)

        if(loading.value) {
            Dialog(
                onDismissRequest = {}
            ) {
                Surface(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(16)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(start = 56.dp, end = 56.dp, top = 32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        CircularProgressIndicator(
                            modifier = Modifier
                                .width(intrinsicSize = IntrinsicSize.Min)
                                .height(intrinsicSize = IntrinsicSize.Min)
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Text(
                            modifier = Modifier
                                .padding(bottom = 32.dp),
                            text = getString(R.string.syncInProgress),
                            style = TextStyle(
                                color = Color.Black,
                                fontSize = 16.sp
                            )
                        )
                    }
                }
            }
        }
    }

    private fun setInitializationSuccessful(success: Boolean) {
        val sharedPreference =  requireActivity().getPreferences(
            Context.MODE_PRIVATE
        )
        val editor = sharedPreference.edit()
        editor.putBoolean(AppConstants.firstInitSuccessKey, success)
        editor.apply()
    }

    private fun getInitializationSuccessful(): Boolean {
        val sharedPreference =  requireActivity().getPreferences(
            Context.MODE_PRIVATE
        )

        return sharedPreference.getBoolean(AppConstants.firstInitSuccessKey, false)
    }

    override fun invalidate() { }
}