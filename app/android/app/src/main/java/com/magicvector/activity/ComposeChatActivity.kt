package com.magicvector.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.magicvector.ui.theme.MagicVectorTheme
import com.magicvector.ui.view.activity.ChatToolbar

class ComposeChatActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MagicVectorTheme {
                ChatScreen(onBackClick = {})
            }
        }
    }
}

@Composable
private fun ChatScreen(
    agentName: String = "",
    onBackClick: () -> Unit,
) {
    val context = LocalContext.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            ChatToolbar (
                title = agentName,
                onBackClick = onBackClick,
            )
        }
    ) { innerPadding ->
        Column (
            modifier = Modifier.padding(innerPadding)
                .fillMaxSize()
        ) {

        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun ChatScreenPreview() {
    MagicVectorTheme {
        ChatScreen(
            agentName = "鸦羽天下第一!!",
            onBackClick = {}
        )
    }
}