package com.jku.mms_projekt.viewModel

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import com.jku.mms_projekt.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.InputStream

class VideoViewModel(application: Application) : AndroidViewModel(application) {
    data class ViewState(
        val video: MediaItem? = null,
        val transcript: String = "",
        val summary: String = "",
        val createSummary: Boolean = true
    )

    private val sharedPref = application.getSharedPreferences(getContext().getString(R.string.preference_file_key), Context.MODE_PRIVATE)
    private val _viewState = MutableStateFlow(ViewState())
    val viewState = _viewState.asStateFlow()

    /**
     * @author Dominik Birngruber
     * @param uri
     * Gets the Media item from the given uri and saves the location in shared preferences
     */
    fun setMediaItem(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            getContext().contentResolver.let { contentResolver: ContentResolver ->
                //val readUriPermission: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                //contentResolver.takePersistableUriPermission(uri, readUriPermission)
                contentResolver.openInputStream(uri)?.use { inputStream: InputStream ->
                    _viewState.update { currentState: ViewState ->
                        val mediaItem = MediaItem.fromUri(uri)
                        with (sharedPref.edit()) {
                            putString(getContext().getString(R.string.video_key), "file://"+getContext().filesDir.path+"/video.mp4")
                            apply()
                        }
                        currentState.copy(video = mediaItem)
                    }
                }
            }
        }
    }

    fun setTranscript(transcript: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _viewState.update { currentState: ViewState ->
                with (sharedPref.edit()) {
                    putString(getContext().getString(R.string.transcript_key), transcript)
                    apply()
                }
                currentState.copy(transcript = transcript)
            }
        }
    }

    fun setSummary(summary: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _viewState.update { currentState: ViewState ->
                with (sharedPref.edit()) {
                    putString(getContext().getString(R.string.summary_key), summary)
                    apply()
                }
                currentState.copy(summary = summary)
            }
        }
    }

    fun setCreateSummary(createSummary: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            _viewState.update { currentState: ViewState ->
                with (sharedPref.edit()) {
                    putBoolean(getContext().getString(R.string.createSummary), createSummary)
                    apply()
                }
                currentState.copy(createSummary = createSummary)
            }
        }
    }

    /**
     * @author Dominik Birngruber
     * Resets all state attributes to their default value and resets the shared preferences
     */
    fun reset() {
        viewModelScope.launch(Dispatchers.IO) {
            _viewState.update { currentState: ViewState ->
                with (sharedPref.edit()) {
                    putString(getContext().getString(R.string.video_key), "")
                    putString(getContext().getString(R.string.transcript_key), "")
                    putString(getContext().getString(R.string.summary_key), "")
                    putString(getContext().getString(R.string.transcript_key_key), "")
                    putBoolean(getContext().getString(R.string.createSummary), false)
                    apply()
                }
                currentState.copy(transcript = "", video = null, summary = "", createSummary = true)
            }
        }
    }

    private fun getContext(): Context = getApplication<Application>().applicationContext
}