package com.jku.mms_projekt.request

import android.content.Context
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.transformer.TransformationRequest
import androidx.media3.transformer.TransformationResult
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.Transformer.Listener
import com.google.gson.Gson
import com.jku.mms_projekt.R
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import java.io.File
import okhttp3.Request
import okhttp3.RequestBody
import java.util.concurrent.Executors

/**
 * @author Dominik Birngruber
 * @param context
 * @param url
 * @param port
 * Simple helper class to make requests to the backend
 */
class Requests(val context: Context, val port: Int, val url: String) {
    private val executor = Executors.newSingleThreadExecutor()

    // region Save MediaItem
    /**
     * @author Dominik Birngruber
     * @param mediaItem
     * @param callback
     * Saves the given mediaItem to the data folder of the app
     */
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    fun saveMediaItem(mediaItem: MediaItem, callback: () -> Unit) {
        val request = TransformationRequest.Builder().build()

        val listener = TransformationListenerCallback(callback)

        val transformer = Transformer.Builder(context)
            .setTransformationRequest(request)
            .setRemoveVideo(false)
            .setRemoveAudio(false)
            .addListener(listener)
            .build()

        val videoFile = File(context.filesDir.path + "/video.mp4")
        if (videoFile.exists()) videoFile.delete()

        transformer.startTransformation(mediaItem, context.filesDir.path + "/video.mp4")
    }

    inner class TransformationListenerCallback(var callback: () -> Unit): Listener {
        @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
        override fun onTransformationCompleted(
            inputMediaItem: MediaItem,
            transformationResult: TransformationResult
        ) {
            Log.d("Listener", "Transformation finished in " + transformationResult.durationMs + "ms")
            callback()
            super.onTransformationCompleted(inputMediaItem, transformationResult)
        }
    }

    // endregion

    // region Send Transcription request
    /**
     * @author Dominik Birngruber
     * @param mediaItem the video to transcribe
     * @param model the used whisper model, can be tiny, base, small, medium or large
     * @param summarize whether to summarize the transcript or not
     * Converts the video to an MP3 and makes a request to the backend. Afterwards saved the returned key in the shared preferences
     */
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    fun sendTranscriptionRequest(mediaItem: MediaItem, model: String, summarize: Boolean) {
        val request = TransformationRequest.Builder()
            .setAudioMimeType(MimeTypes.AUDIO_AAC)
            .build()

        val listener = TransformationListener(model, summarize)

        val transformer = Transformer.Builder(context)
            .setTransformationRequest(request)
            .addListener(listener)
            .setRemoveVideo(true)
            .build()

        val audioFile = File(context.filesDir.path + "/transformed.mp3")
        if (audioFile.exists()) audioFile.delete()

        transformer.startTransformation(mediaItem, context.filesDir.path + "/transformed.mp3")
    }

    /**
     * @author Dominik Birngruber
     * @param model the used whisper model, can be tiny, base, small, medium or large
     * @param summarize whether to summarize the transcript or not
     * Listener to make the http request after transformation to mp3 is finished
     */
    private inner class TransformationListener(var model: String, var summarize: Boolean): Listener {
        @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
        override fun onTransformationCompleted(
            inputMediaItem: MediaItem,
            transformationResult: TransformationResult
        ) {
            Log.d(
                "Listener",
                "Transformation finished in " + transformationResult.durationMs + "ms"
            )

            executor.execute {
                val client = OkHttpClient()

                val body = MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "audio", "transformed.mp3",
                        RequestBody.create(
                            MediaType.parse("application/octet-stream"),
                            File(context.filesDir.path + "/transformed.mp3")
                        )
                    )
                    .build()

                val request = Request.Builder()
                    .url("http://$url:$port/transcribe?model=$model&summarize=$summarize")
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()

                val key = Gson().fromJson(response.body()!!.string(), Key::class.java)

                val sharedPref = context.applicationContext.getSharedPreferences(
                    context.getString(R.string.preference_file_key),
                    Context.MODE_PRIVATE
                )

                with(sharedPref.edit()) {
                    putString(context.getString(R.string.transcript_key_key), key.key)
                    apply()
                }
            }
            super.onTransformationCompleted(inputMediaItem, transformationResult)
        }
    }

    /**
     * @author Dominik Birngruber
     * Simple data class used for parsing the key from the request body
     */
    data class Key(var key: String)

    //endregion

    // region Get Transcript
    /**
     * @author Domink Birngruber
     * @param key The transcription key obtained by the transcription request
     * Sends a request to get the transcribed text.
     */
    fun getTranscript(key: String): Transcription {
        val future = executor.submit<Transcription> {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("http://$url:$port/checkTranscription?transcriptionkey=$key")
                .build()
            val response = client.newCall(request).execute()

            return@submit Gson().fromJson(response.body()!!.string(), Transcription::class.java)
        }

        return future.get()
    }
    // endregion

    /**
     * @author Dominik Birngruber
     * Data class to store results from the API
     */
    data class Transcription(var completed: Boolean, var transcript: String, var summary: String)
}