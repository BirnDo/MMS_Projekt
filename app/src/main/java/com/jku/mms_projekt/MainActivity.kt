package com.jku.mms_projekt

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Add
import androidx.compose.material.icons.twotone.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.jku.mms_projekt.annotations.AllColorPreview
import com.jku.mms_projekt.request.Requests
import com.jku.mms_projekt.ui.theme.DynamicColor
import com.jku.mms_projekt.viewModel.VideoViewModel

/**
 * @author Dominik Birngruber
 */
class MainActivity : ComponentActivity() {
    private val videoViewModel: VideoViewModel by viewModels<VideoViewModel>()
    lateinit var pickMedia: ActivityResultLauncher<PickVisualMediaRequest>
    private var createSummary = true
    private var transcriptKey: String = ""
    private lateinit var request: Requests
    private lateinit var transcript: Requests.Transcription

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        request = Requests(context = this, url = "192.168.0.105", port = 5000)
        addPickMedia()
        readPreferences()
        makeRequestForTranscript()

        setContent {
            DynamicColor {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainUi()
                }
            }
        }
    }

    //region Helper functions

    /**
     * @author Dominik Birngruber
     * Reads all fields from the shared Preferences
     */
    private fun readPreferences() {
        val sharedPref = this.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)
        val transcript = sharedPref.getString(getString(R.string.transcript_key), "").orEmpty()
        val summary = sharedPref.getString(getString(R.string.summary_key), "").orEmpty()
        val videoUri = sharedPref.getString(getString(R.string.video_key), "").orEmpty()
        createSummary = sharedPref.getBoolean(getString(R.string.createSummary), true)
        transcriptKey = sharedPref.getString(getString(R.string.transcript_key_key), "").orEmpty()

        if(transcript != "") {
            videoViewModel.setTranscript(transcript)
        }

        if(summary != "") {
            videoViewModel.setSummary(summary)
        }

        if (videoUri != "") {
            videoViewModel.setMediaItem(Uri.parse(videoUri))
        }

        videoViewModel.setCreateSummary(createSummary)
    }

    /**
     * @author Dominik Birngruber
     * Sends a request to the server and sets the summary and transcript if transcription is completed
     */
    private fun makeRequestForTranscript() {
        if(transcriptKey != ""){
            val transcript = request.getTranscript(transcriptKey)
            if(transcript.completed) {
                videoViewModel.setTranscript(transcript.transcript)
                if (createSummary) videoViewModel.setSummary(transcript.summary)
            }
        }
    }

    /**
     * @author Dominik Birngruber
     * Registers for the new Photo Picker and sets callback functions
     */
    private fun addPickMedia() {
        pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                videoViewModel.setMediaItem(uri)
                request.saveMediaItem(MediaItem.fromUri(uri)) {
                    videoViewModel.setMediaItem(Uri.parse("file://"+this.filesDir.path+"/video.mp4"))
                }
                videoViewModel.setTranscript("")
                if (createSummary) {
                    videoViewModel.setSummary("")
                }
            }
        }
    }

    //endregion

    //region Composables

    /**
     * @author Dominik Birngruber
     * Button for choosing video and selecting whether a summary should be made
     */
    @Composable
    fun VideoSelection() {
        val summaryState = remember { mutableStateOf(true) }
        Column() {
            Button(
                onClick = {
                    videoViewModel.setCreateSummary(createSummary)
                    pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) },
                contentPadding = PaddingValues(
                    start = 20.dp,
                    top = 12.dp,
                    end = 20.dp,
                    bottom = 12.dp
                ),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(
                    Icons.TwoTone.Add,
                    contentDescription = "Choose Video",
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                Text(text = "Choose a video")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = summaryState.value,
                    onCheckedChange = { summaryState.value = it; createSummary = it })
                Spacer(modifier = Modifier.size(SwitchDefaults.IconSize))
                Text(text = "Create summary")
            }
        }
    }

    /**
     * @author Dominik Birngruber
     * Button for making a new request. Resets the viewModel to the initial state.
     */
    @Composable
    fun NewRequest() {
        Button(
            onClick = {
                videoViewModel.reset() }, contentPadding = PaddingValues(
                start = 20.dp,
                top = 12.dp,
                end = 20.dp,
                bottom = 12.dp
            ),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Icon(
                Icons.TwoTone.Refresh,
                contentDescription = "New request",
                modifier = Modifier.size(ButtonDefaults.IconSize)
            )
            Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
            Text(text = "New request")
        }
    }

    /**
     * @author Dominik Birngruber
     * @param video The Media item which will be played in the video player
     */
    @Composable
    fun VideoPlayer(video: MediaItem) {
        val context = LocalContext.current

        val exoPlayer = ExoPlayer.Builder(LocalContext.current)
            .build()
            .also { exoPlayer ->
                exoPlayer.setMediaItem(video)
                exoPlayer.prepare()
            }

        DisposableEffect(
            AndroidView(factory = {
                PlayerView(context).apply {
                    player = exoPlayer
                }
            }, modifier = Modifier.clip(MaterialTheme.shapes.medium))
        ) {
            onDispose { exoPlayer.release() }
        }
    }

    /**
     * @author Dominik Birngruber
     * @param text the text to show on the card
     * Shows a themed card with the given text
     */
    @Composable
    fun  TextCard(text: String) {
        Card(modifier = Modifier.fillMaxSize(0.98F)) {
            LazyColumn() {
                item {
                    Text(text = text, modifier = Modifier.padding(10.dp))
                }
            }
        }
    }

    /**
     * @author Dominik Birngruber
     * @param mediaItem the video to transcribe
     * Sends a request to the backend for the given video
     */
    @Composable
    private fun SendRequest(mediaItem: MediaItem) {
        Button(onClick = {
            readPreferences()
            request.sendTranscriptionRequest(mediaItem, "small", createSummary)
            Toast.makeText(this@MainActivity, "Starting transcription, please come back again later", Toast.LENGTH_SHORT).show()}) {
            Text(text = "Start request")
        }
    }

    /**
     * @author Dominik Birngruber
     * Sends a request to the backend to check if the transcription has finished
     */
    @Composable
    private fun CheckTranscript() {
        Button(onClick = {
            readPreferences()
            transcript = request.getTranscript(key = transcriptKey)
            if(transcript.completed) {
                videoViewModel.setTranscript(transcript.transcript)
                if (createSummary) {
                    videoViewModel.setSummary(transcript.summary)
                    Toast.makeText(this@MainActivity, "Swipe for summary", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this@MainActivity, "Transcription not finished, check again later", Toast.LENGTH_LONG).show()
            }
        }) {
            Text(text = "Check for transcript")
        }
    }

    @Composable
    private fun InitialView() {
        VideoSelection()
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun VideoView(
        video: MediaItem,
        showTranscript: Boolean,
        pager: MutableList<String>? = null
    ) {
        Column(
            modifier = Modifier.padding(5.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ){
            NewRequest()
            VideoPlayer(video)
            SendRequest(video)
            if(!showTranscript) {
                CheckTranscript()
            } else {
                if(pager != null) {
                    HorizontalPager(pageCount = pager.size) {
                        TextCard(text = pager[it])
                    }
                }
            }
        }
    }

    @Composable
    private fun MainUi() {
        val viewState = videoViewModel.viewState.collectAsState()
        var transcriptStart = "Transcript:"
        if(viewState.value.summary != "") {
            transcriptStart += " (Swipe for summary)\n\n"
        } else {
            transcriptStart += "\n\n"
        }
        val textPager = mutableListOf(transcriptStart + viewState.value.transcript)
        if (viewState.value.summary != "") {
            textPager.add("Summary: \n" + viewState.value.summary)
        }
        val video = viewState.value.video
        val showTranscript = viewState.value.transcript != ""

        DynamicColor {
            Column(
                modifier = Modifier.padding(5.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                if(video == null) {
                    InitialView()
                } else {
                    VideoView(video = video, showTranscript = showTranscript, pager = textPager)
                }
            }
        }
    }

    //endregion

    // region Previews
    @AllColorPreview
    @Composable
    private fun InitialPreview() {
        DynamicColor {
            Column(
                modifier = Modifier.padding(5.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                InitialView()
            }
        }
    }

    @AllColorPreview
    @Composable
    private fun VideoSelectedNotYetTranscribed() {
        DynamicColor {
            Column(
                modifier = Modifier.padding(5.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Column(
                    modifier = Modifier.padding(5.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ){
                    NewRequest()
                    Image(painter = painterResource(id = R.drawable.still_image), contentDescription = "Still image", modifier = Modifier.clip(MaterialTheme.shapes.medium))
                    SendRequest(MediaItem.EMPTY)
                    CheckTranscript()
                }
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @AllColorPreview
    @Composable
    private fun VideoSelectedAndTranscribedNoSummary() {
        val loremIpsum = LoremIpsum(1000).values.joinToString(" ")
        val pager = mutableListOf("Transcript:\n\n$loremIpsum")
        DynamicColor {
            Column(
                modifier = Modifier.padding(5.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Column(
                    modifier = Modifier.padding(5.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ){
                    NewRequest()
                    Image(painter = painterResource(id = R.drawable.still_image), contentDescription = "Still image", modifier = Modifier.clip(MaterialTheme.shapes.medium))
                    SendRequest(MediaItem.EMPTY)
                    HorizontalPager(pageCount = pager.size) {
                        TextCard(text = pager[it])
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @AllColorPreview
    @Composable
    private fun VideoSelectedAndTranscribedWithSummary() {
        val loremIpsum = LoremIpsum(1000).values.joinToString(" ")
        val pager = mutableListOf("Transcript: (Swipe for summary)\n\n$loremIpsum")
        pager.add("Summary:\n\n${loremIpsum.substring(0..100)}")
        DynamicColor {
            Column(
                modifier = Modifier.padding(5.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Column(
                    modifier = Modifier.padding(5.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ){
                    NewRequest()
                    Image(painter = painterResource(id = R.drawable.still_image), contentDescription = "Still image", modifier = Modifier.clip(MaterialTheme.shapes.medium))
                    SendRequest(MediaItem.EMPTY)
                    HorizontalPager(pageCount = pager.size) {
                        TextCard(text = pager[it])
                    }
                }
            }
        }
    }
    // endregion
}