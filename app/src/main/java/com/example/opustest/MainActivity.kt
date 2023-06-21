package com.example.opustest

import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.opustest.R
import com.theeasiestway.opus.Constants
import com.theeasiestway.opus.Opus
import com.theeasiestway.opusapp.mic.ControllerAudio
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

//
// Created by Loboda Alexey on 21.05.2020.
//

class MainActivity : AppCompatActivity() {

    private val TAG = "OpusActivity"
    private val audioPermission = android.Manifest.permission.RECORD_AUDIO
    private val readPermission = android.Manifest.permission.READ_EXTERNAL_STORAGE
    private val writePermission = android.Manifest.permission.WRITE_EXTERNAL_STORAGE

    private lateinit var vSampleRateSeek: SeekBar
    private lateinit var vSampleRate: TextView
    private lateinit var vPlay: Button
    private lateinit var vStop: Button
    private lateinit var vBytes: RadioButton
    private lateinit var vShorts: RadioButton
    private lateinit var vMono: RadioButton
    private lateinit var vStereo: RadioButton
    private lateinit var vConvert: CheckBox

    private val codec = Opus()
    private val APPLICATION = Constants.Application.audio()
    private var CHUNK_SIZE = 0
    private lateinit var SAMPLE_RATE: Constants.SampleRate
    private lateinit var CHANNELS: Constants.Channels
    private lateinit var DEF_FRAME_SIZE: Constants.FrameSize
    private lateinit var FRAME_SIZE_SHORT: Constants.FrameSize
    private lateinit var FRAME_SIZE_BYTE: Constants.FrameSize

    private var runLoop = false
    private var needToConvert = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        vSampleRateSeek = findViewById(R.id.vSampleRateSeek)
        vSampleRate = findViewById(R.id.vSampleRate)

        vPlay = findViewById(R.id.vPlay)
        vStop = findViewById(R.id.vStop)

        vBytes = findViewById(R.id.vHandleBytes)
        vShorts = findViewById(R.id.vHandleShorts)
        vMono = findViewById(R.id.vMono)
        vStereo = findViewById(R.id.vStereo)
        vConvert = findViewById(R.id.vConvert)

        vSampleRateSeek.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                SAMPLE_RATE = getSampleRate(progress)
                val lableText = "${SAMPLE_RATE.v} Hz"
                vSampleRate.text = lableText
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        vSampleRateSeek.progress = 0

        vPlay.setOnClickListener {
            vPlay.visibility = View.GONE
            vStop.visibility = View.VISIBLE
            // requestPermissions()

            startLoop()
        }

        vStop.setOnClickListener {
            stopRecording()
        }

        vConvert.setOnCheckedChangeListener { _, isChecked -> needToConvert = isChecked }
    }

    private fun recalculateCodecValues() {
        DEF_FRAME_SIZE = getDefaultFrameSize(SAMPLE_RATE.v)
        CHANNELS = if (vMono.isChecked) Constants.Channels.mono() else Constants.Channels.stereo()
        /** "CHUNK_SIZE = DEF_FRAME_SIZE.v * CHANNELS.v * 2" it's formula from opus.h "frame_size*channels*sizeof(opus_int16)" */
        CHUNK_SIZE = DEF_FRAME_SIZE.v * CHANNELS.v * 2                                              // bytes or shorts in a frame
        FRAME_SIZE_SHORT = Constants.FrameSize.fromValue(CHUNK_SIZE / CHANNELS.v)            // samples per channel
        FRAME_SIZE_BYTE = Constants.FrameSize.fromValue(CHUNK_SIZE / 2 / CHANNELS.v)         // samples per channel
    }

    private fun getSampleRate(v: Int): Constants.SampleRate {
        return when(v) {
            0 -> Constants.SampleRate._8000()
            1 -> Constants.SampleRate._12000()
            2 -> Constants.SampleRate._16000()
            3 -> Constants.SampleRate._24000()
            4 -> Constants.SampleRate._48000()
            else -> throw IllegalArgumentException()
        }
    }

    private fun getDefaultFrameSize(v: Int): Constants.FrameSize {
        return when (v) {
            8000 -> Constants.FrameSize._160()
            12000 -> Constants.FrameSize._240()
            16000 -> Constants.FrameSize._160()
            24000 -> Constants.FrameSize._240()
            48000 -> Constants.FrameSize._120()
            else -> throw IllegalArgumentException()
        }
    }

    private fun stopRecording() {
        vStop.visibility = View.GONE
        vPlay.visibility = View.VISIBLE
        stopLoop()
        ControllerAudio.stopRecord()
        ControllerAudio.stopTrack()
        vSampleRateSeek.isEnabled = true
        vBytes.isEnabled = true
        vShorts.isEnabled = true
        vMono.isEnabled = true
        vStereo.isEnabled = true
    }

    private fun startLoop() {
        stopLoop()

        vSampleRateSeek.isEnabled = false
        vBytes.isEnabled = false
        vShorts.isEnabled = false
        vMono.isEnabled = false
        vStereo.isEnabled = false

        val handleShorts = vShorts.isChecked
        recalculateCodecValues()

        codec.encoderInit(SAMPLE_RATE, CHANNELS, APPLICATION)
        codec.decoderInit(SAMPLE_RATE, CHANNELS)

        ControllerAudio.initRecorder(SAMPLE_RATE.v, CHUNK_SIZE, CHANNELS.v == 1)
        ControllerAudio.initTrack(SAMPLE_RATE.v, CHANNELS.v == 1)
        ControllerAudio.startRecord()
        runLoop = true
        Thread {
            test()
            /*
            while (runLoop) { if (handleShorts) handleShorts() else handleBytes() }
            if (!runLoop) {
                codec.encoderRelease()
                codec.decoderRelease()
            }
             */
        }.start()
    }

    private fun test() {

        try {

/*
            val rate = 48000
            val bufferSize = AudioTrack.getMinBufferSize(
                48000,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            );

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            val audioFormat = AudioFormat.Builder()
                .setSampleRate(rate.toInt())
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
            */

            println("--- INICIO ----------------------------------------------------")

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

            for (i in 0..20000) {

                socket.receive(packet)
                println("UDP " + packet.length.toString() + packet.data.contentToString())

                if (packet.length < 10){
                    continue
                }

                val encodedAudio = packet.data.slice(4 until packet.length).toByteArray()


                val decoded =
                    codec.decode(
                        bytes = encodedAudio,
                        frameSize = Constants.FrameSize._1920()
                    )

                if (decoded != null) {
                    println("UDP D" + decoded.contentToString())

                    if (needToConvert) {
                        val converted = codec.convert(decoded) ?: return
                        Log.d(TAG, "converted: ${decoded.size} bytes into ${converted.size} shorts")
                        ControllerAudio.write(converted)
                    } else ControllerAudio.write(decoded)
                } else{
                    println("decoded = null")
                }


                //val decoded = codec.decode(encodedAudio, FRAME_SIZE_BYTE) ?: return
                //println("UDP D" + decoded.contentToString())


            }

            socket.close()
            println("--- FIN ----------------------------------------------------")


        } catch (e: Exception) {
            println(e.message)
        }

    }

    private fun stopLoop() {
        runLoop = false
    }

    private fun handleShorts() {
        val frame = ControllerAudio.getFrameShort() ?: return
        val encoded = codec.encode(frame, FRAME_SIZE_SHORT) ?: return
        Log.d(TAG, "encoded: ${frame.size} shorts of ${if (CHANNELS.v == 1) "MONO" else "STEREO"} audio into ${encoded.size} shorts")
        val decoded = codec.decode(encoded, FRAME_SIZE_SHORT) ?: return
        Log.d(TAG, "decoded: ${decoded.size} shorts")

        if (needToConvert) {
            val converted = codec.convert(decoded) ?: return
            Log.d(TAG, "converted: ${decoded.size} shorts into ${converted.size} bytes")
            ControllerAudio.write(converted)
        } else ControllerAudio.write(decoded)
        Log.d(TAG, "===========================================")
    }

    private fun handleBytes() {
        val frame = ControllerAudio.getFrame() ?: return
        val encoded = codec.encode(frame, FRAME_SIZE_BYTE) ?: return
        Log.d(TAG, "encoded: ${frame.size} bytes of ${if (CHANNELS.v == 1) "MONO" else "STEREO"} audio into ${encoded.size} bytes")
        val decoded = codec.decode(encoded, FRAME_SIZE_BYTE) ?: return
        Log.d(TAG, "decoded: ${decoded.size} bytes")

        if (needToConvert) {
            val converted = codec.convert(decoded) ?: return
            Log.d(TAG, "converted: ${decoded.size} bytes into ${converted.size} shorts")
            ControllerAudio.write(converted)
        } else ControllerAudio.write(decoded)
        Log.d(TAG, "===========================================")
    }

    private fun requestPermissions() {
        if (checkSelfPermission(audioPermission) != PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(readPermission) != PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(writePermission) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(audioPermission, readPermission, writePermission), 123)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (permissions[0] == audioPermission &&
            permissions[1] == readPermission &&
            permissions[2] == writePermission &&
            requestCode == 123) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                grantResults[1] == PackageManager.PERMISSION_GRANTED &&
                grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                startLoop()
            }
            else {
                Toast.makeText(this, "App doesn't have enough permissions to continue", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        stopRecording()
    }
}
