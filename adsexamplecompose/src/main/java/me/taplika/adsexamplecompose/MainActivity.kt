package me.taplika.adsexamplecompose

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.lifecycleScope
import com.google.fleekads.attribution.DummyAttribution
import com.google.fleekads.core.AdListenerWithRevenue
import com.google.fleekads.core.BasicNativeAdLoader
import com.google.fleekads.formats.BasicNativePlacement
import com.google.fleekads.formats.FullScreenAds
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdOptions.ADCHOICES_TOP_LEFT
import kotlinx.coroutines.launch
import me.taplika.adsexamplecompose.ui.theme.FleekTheme


const val nativeAID = "ca-app-pub-3940256099942544/2247696110"


private var placement: BasicNativePlacement = BasicNativePlacement(

    BasicNativeAdLoader(

        nativeAID,
        nativeAdOptions = NativeAdOptions.Builder()
            .setAdChoicesPlacement(ADCHOICES_TOP_LEFT)
            .setVideoOptions(VideoOptions.Builder().setStartMuted(false).build())
            .build()
    )


)

var fullscreenNativeAds: FullScreenAds = FullScreenAds(
    placement = placement,
    layout = com.google.fleekads.R.layout.fullscreen_view
)


suspend fun loadAndShowFullscreenNativeAds(context: FragmentActivity) {
    // Preload.
    val presenter = fullscreenNativeAds.preload(
        fragmentActivity = context,
        listener = AdListenerWithRevenue(
            adLoader = placement,
            ilrdFactory = DummyAttribution()
        )
    )
    // Then show.
    presenter?.show(R.id.fullscreen_ads_placeholder) {
        // This code will run after ad impression handler triggered.
        Toast.makeText(context, "Impression delivered.", Toast.LENGTH_LONG).show()
    }

    if (presenter == null) {
        Toast.makeText(context, "No fullscreen ads loaded.", Toast.LENGTH_LONG).show()
    }
}

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val req = RequestConfiguration.Builder().setTestDeviceIds(listOf("D76DD54D3FB1E2AC52DC83A63D67C22E"))
        MobileAds.setRequestConfiguration(req.build())
        MobileAds.initialize(this)

        setContent {
            FleekTheme {
                Box(Modifier.fillMaxSize()) {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        Column(modifier = Modifier.fillMaxSize().padding(40.dp)) {
                            Greeting(
                                name = "Android",
                                modifier = Modifier.padding(innerPadding)
                            )
                            Button(onClick = {
                                lifecycleScope.launch {
                                    loadAndShowFullscreenNativeAds(this@MainActivity)
                                }
                            }) {
                                Text("Show")
                            }
                        }
                    }
                    FullscreenAdsPlaceholder(
                        modifier = Modifier
                            .matchParentSize()
                            .zIndex(1.0f)
                    )
                }
            }
        }
    }
}


@Composable
fun FullscreenAdsPlaceholder(
    modifier: Modifier = Modifier,
    fragmentId: Int = R.id.fullscreen_ads_placeholder
) {
    AndroidView(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(bottom = 12.dp)
            .zIndex(1000f),
        factory = { context ->
            FragmentContainerView(context).apply {
                id = fragmentId

                elevation = 10_000f
                translationZ = 10_000f
            }
        }
    )
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    FleekTheme {
        Greeting("Android")
    }
}