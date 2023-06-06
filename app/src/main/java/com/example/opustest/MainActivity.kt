package com.example.opustest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.opustest.ui.theme.OpusTestTheme

import com.theeasiestway.opus.Constants
import com.theeasiestway.opus.Opus

class MainActivity : ComponentActivity() {
    private val codec = Opus()
    private lateinit var SAMPLE_RATE: Constants.SampleRate
    private lateinit var CHANNELS: Constants.Channels

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OpusTestTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Greeting("OPUS 4")
                }
            }
        }

        try {
            val encodedAudio = byteArrayOf(
                19.toByte(),
                65.toByte(),
                0.toByte(),
                6.toByte(),
                37.toByte(),
                235.toByte(),
                76.toByte(),
                78.toByte(),
                106.toByte(),
                236.toByte(),
                107.toByte(),
                177.toByte(),
                78.toByte(),
                132.toByte(),
                240.toByte(),
                127.toByte(),
                227.toByte(),
                152.toByte(),
                76.toByte(),
                156.toByte(),
                124.toByte(),
                65.toByte(),
                210.toByte(),
                72.toByte(),
                88.toByte(),
                119.toByte(),
                105.toByte(),
                7.toByte(),
                224.toByte(),
                166.toByte(),
                129.toByte(),
                255.toByte(),
                201.toByte(),
                146.toByte(),
                198.toByte(),
                253.toByte(),
                70.toByte(),
                206.toByte(),
                157.toByte(),
                160.toByte()
            )

            println("--------------------------------------------------------")

            codec.decoderInit(sampleRate = Constants.SampleRate._48000(),
                channels = Constants.Channels.mono())

            val decoded = codec.decode( bytes = encodedAudio, frameSize = Constants.FrameSize._1920())
            if (decoded != null) {
                println(decoded.contentToString())
            }

            println("--------------------------------------------------------")


        } catch (e: Exception) {
            println(e.message)
        }

    }
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
    OpusTestTheme {
        Greeting("Android")
    }
}