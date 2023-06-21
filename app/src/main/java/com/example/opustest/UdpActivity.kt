package com.example.opustest

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
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
import java.lang.Thread.sleep
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class UDPActivity : ComponentActivity() {
    private val codec = Opus()
    private lateinit var SAMPLE_RATE: Constants.SampleRate
    private lateinit var CHANNELS: Constants.Channels

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OpusTestTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting("OPUS 4")
                }
            }
        }
        Thread(Runnable {
            sleep(500)
            test()
        }).start()
    }

    private fun test() {

        try {


            val sampleRate = 48000
            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            val audioFormat = AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()

            val audioTrack = AudioTrack(
                audioAttributes,
                audioFormat,
                bufferSize,
                AudioTrack.MODE_STREAM,
                0
            )

            audioTrack.play()

            codec.decoderInit(
                sampleRate = Constants.SampleRate._48000(),
                channels = Constants.Channels.mono()
            )

            println("--- INICIO ----------------------------------------------------")

            //var audios = Array(0){ByteArray(2048)}
            var audios = emptyArray<ByteArray>()

            val buffer = ByteArray(2048)
            var socket: DatagramSocket? = null

            val remoteHost = "172.31.120.230"//"127.0.0.1"
            val remotePort = 64739//64749

            socket = DatagramSocket()
            socket.broadcast = true
            val packet = DatagramPacket(buffer, buffer.size)

            val ping = byteArrayOf(99.toByte(), 0.toByte(), 0.toByte(), 0.toByte())
            val sendPacket =
                DatagramPacket(ping, ping.size, InetAddress.getByName(remoteHost), remotePort)
            socket.send(sendPacket)

            for (i in 0..99) {

                socket.receive(packet)

                if (packet.length < 10){
                    continue
                }

                val headerLen = packet.length - 40
                val header = packet.data.slice(0 until headerLen).toByteArray()
                val payload = packet.data.slice(headerLen until packet.length).toByteArray()

                println("UDP1 " + packet.data.slice(0 until packet.length).toByteArray().contentToString())
                println("UDP2 " + header.contentToString() + ":" + payload.contentToString())

                val decoded =
                    codec.decode(
                        bytes = payload,
                        frameSize = Constants.FrameSize._1920()
                    )

                if (decoded != null) {
                    //println("UDP D" + decoded.contentToString())
                    audioTrack.write(decoded, 0, decoded.size)
                    audios +=decoded
                } else {
                    println("UDP decoded = null!!")
                }
            }

            sleep(500)
            for (element in audios) {
                sleep(20)
                audioTrack.write(element, 0, element.size)
            }

            socket.close()
            println("--- FIN ----------------------------------------------------")


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