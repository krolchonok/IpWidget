package org.ushastoe.ipw

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.ushastoe.ipw.ui.theme.IpWTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val intent = Intent(this, MainIpWidget::class.java).apply {
            action = "com.ushastoe.ipwidget.ACTION_UPDATE"
        }
        sendBroadcast(intent)
        setContent {
            IpWTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    MainScreen(this@MainActivity)
                }
            }
        }
    }
    companion object {
        fun requestWidgetPinning(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, MainIpWidget::class.java)

            if (appWidgetManager.isRequestPinAppWidgetSupported) {
                appWidgetManager.requestPinAppWidget(componentName, null, null)
            } else {
                Toast.makeText(
                    context,
                    "Ваше устройство не поддерживает закрепление виджетов",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


}

@Composable
fun MainScreen(context: Context) {
    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(16.dp)) {
        Card(
            modifier = Modifier
                .padding(8.dp).fillMaxWidth(),
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer)
        ) {
            Box(modifier = Modifier.padding(16.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(text = "IpWidget",  style = MaterialTheme.typography.bodyLarge)
            }
        }



        Button(modifier = Modifier.fillMaxWidth().padding(8.dp), onClick = {
            MainActivity.requestWidgetPinning(context)
            }) {
                Text("Создать виджет")
                }
            }
        }




@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    val fakeContext = androidx.compose.ui.platform.LocalContext.current
    IpWTheme {
        MainScreen(fakeContext)
    }
}