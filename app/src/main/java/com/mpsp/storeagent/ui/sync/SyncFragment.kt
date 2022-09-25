package com.mpsp.storeagent.ui.sync

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.Text
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
import androidx.fragment.app.Fragment
import com.mpsp.storeagent.R

class SyncFragment : Fragment() {

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

    @Composable
    private fun CreateSyncScreen() {
        Column(modifier = Modifier.wrapContentSize(Alignment.Center)) {
            Row(modifier = Modifier.wrapContentSize()) {
                Spacer(modifier = Modifier.width(4.dp))
                Image(imageVector = ImageVector.vectorResource(id = R.drawable.agent_face), contentDescription = null)
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
                onClick = {  },
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
    }
}