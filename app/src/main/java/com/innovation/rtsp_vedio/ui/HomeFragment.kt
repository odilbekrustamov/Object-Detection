package com.innovation.rtsp_vedio.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.innovation.rtsp_vedio.R
import com.innovation.rtsp_vedio.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
    }

    private fun initViews() {
        binding.btnEnter.setOnClickListener {
            val rtspUrl = binding.edittetxt.text.toString()
            if (rtspUrl.isNotEmpty()){
                val bundle = Bundle()
                bundle.putString("RTSPURL",rtspUrl)
                findNavController().navigate(R.id.action_homeFragment_to_RTSPFragment, bundle)
            }
        }
    }
}