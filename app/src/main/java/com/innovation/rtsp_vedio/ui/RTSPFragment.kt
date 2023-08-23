package com.innovation.rtsp_vedio.ui

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.innovation.rtsp_vedio.ObjectDetectorHelper
import com.innovation.rtsp_vedio.databinding.FragmentRtspBinding
import org.tensorflow.lite.task.vision.detector.Detection
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import wseemann.media.FFmpegMediaMetadataRetriever
import java.util.LinkedList
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class RTSPFragment : Fragment() , ObjectDetectorHelper.DetectorListener{
    private var _binding: FragmentRtspBinding? = null
    private val binding get() = _binding!!

    private var libVLC: LibVLC? = null
    private lateinit var mediaPlayer: MediaPlayer

    private lateinit var objectDetectorHelper: ObjectDetectorHelper
    val url = "rtsp://admin:12345678a@45.9.229.134:6454/Streaming/Channels/101"

    private val saveIntervalSeconds = 1L
    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadScheduledExecutor()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRtspBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
    }

    private fun initViews() {
        libVLC = LibVLC(requireContext())
        mediaPlayer = MediaPlayer(libVLC)

        objectDetectorHelper = ObjectDetectorHelper(
            context = requireContext(),
            objectDetectorListener = this
        )

        initBottomSheetControls()

        startCapturingFrames()
    }

    private fun startCapturingFrames() {
        executor.scheduleWithFixedDelay({
            captureFrame()
        }, 0, saveIntervalSeconds, TimeUnit.SECONDS)
    }

    private fun captureFrame() {
        val retriever = FFmpegMediaMetadataRetriever()
        try {
            retriever.setDataSource(url)
            val frame = retriever.getFrameAtTime(-1)
            saveFrameToFile(frame)
        } catch (e: Exception) {
            handler.post {
                Toast.makeText(requireContext(), "Error capturing frame: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } finally {
            retriever.release()
        }
    }

    private fun saveFrameToFile(frame: Bitmap) {
        objectDetectorHelper.detect(frame, 0)
//        val file = File(getExternalFilesDir(null), "${System.currentTimeMillis() % 1000}screenshot2.png")
//        try {
//            FileOutputStream(file).use { output ->
//                frame.compress(Bitmap.CompressFormat.PNG, 100, output)
//            }
//            handler.post {
//                Toast.makeText(this, "Frame saved", Toast.LENGTH_SHORT).show()
//            }
//        } catch (e: IOException) {
//            handler.post {
//                Toast.makeText(this, "Error saving frame: ${e.message}", Toast.LENGTH_SHORT).show()
//            }
//        }
    }

    override fun onStart() {
        super.onStart()
        mediaPlayer.attachViews(binding.videoView, null, false, false)
        val media = Media(libVLC, Uri.parse(url))
        media.setHWDecoderEnabled(true, false)
        media.addOption(":network-caching=600")
        mediaPlayer.media = media
        media.release()
        mediaPlayer.play()
    }

    override fun onStop() {
        super.onStop()
        mediaPlayer.stop()
        mediaPlayer.detachViews()
    }

    private fun initBottomSheetControls() {
        binding.bottomSheetLayout.thresholdMinus.setOnClickListener {
            if (objectDetectorHelper.threshold >= 0.1) {
                objectDetectorHelper.threshold -= 0.1f
                updateControlsUi()
            }
        }

        binding.bottomSheetLayout.thresholdPlus.setOnClickListener {
            if (objectDetectorHelper.threshold <= 0.8) {
                objectDetectorHelper.threshold += 0.1f
                updateControlsUi()
            }
        }

        binding.bottomSheetLayout.maxResultsMinus.setOnClickListener {
            if (objectDetectorHelper.maxResults > 1) {
                objectDetectorHelper.maxResults--
                updateControlsUi()
            }
        }

        binding.bottomSheetLayout.maxResultsPlus.setOnClickListener {
            if (objectDetectorHelper.maxResults < 5) {
                objectDetectorHelper.maxResults++
                updateControlsUi()
            }
        }

        binding.bottomSheetLayout.threadsMinus.setOnClickListener {
            if (objectDetectorHelper.numThreads > 1) {
                objectDetectorHelper.numThreads--
                updateControlsUi()
            }
        }

        binding.bottomSheetLayout.threadsPlus.setOnClickListener {
            if (objectDetectorHelper.numThreads < 4) {
                objectDetectorHelper.numThreads++
                updateControlsUi()
            }
        }

        binding.bottomSheetLayout.spinnerDelegate.setSelection(0, false)
        binding.bottomSheetLayout.spinnerDelegate.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                    objectDetectorHelper.currentDelegate = p2
                    updateControlsUi()
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {

                }
            }

        binding.bottomSheetLayout.spinnerModel.setSelection(0, false)
        binding.bottomSheetLayout.spinnerModel.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                    objectDetectorHelper.currentModel = p2
                    updateControlsUi()
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {

                }
            }
    }

    private fun updateControlsUi() {
        binding.bottomSheetLayout.maxResultsValue.text =
            objectDetectorHelper.maxResults.toString()
        binding.bottomSheetLayout.thresholdValue.text =
            String.format("%.2f", objectDetectorHelper.threshold)
        binding.bottomSheetLayout.threadsValue.text =
            objectDetectorHelper.numThreads.toString()

        objectDetectorHelper.clearObjectDetector()
        binding.overlay.clear()
    }

    override fun onError(error: String) {
        requireActivity().runOnUiThread {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResults(
        results: MutableList<Detection>?,
        inferenceTime: Long,
        imageHeight: Int,
        imageWidth: Int
    ) {
        Log.d("dfasfsfasfsfd", "onResults: ${results}")
        requireActivity().runOnUiThread {

            binding.bottomSheetLayout.inferenceTimeVal.text =
                String.format("%d ms", inferenceTime)

            binding.overlay.setResults(
                results ?: LinkedList<Detection>(),
                imageHeight,
                imageWidth
            )

            binding.overlay.invalidate()
        }
    }

    override fun onDestroy() {
        mediaPlayer.release()
        libVLC!!.release()
        super.onDestroy()
    }
}