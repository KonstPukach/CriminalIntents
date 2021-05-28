package com.example.criminalintents

import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import java.io.File

private const val ARG_URI = "photoUri"

class PhotoDetailFragment : DialogFragment() {
    companion object {
        fun newInstance(photoFile: File): PhotoDetailFragment {
            val bundle = Bundle().apply {
                putString(ARG_URI, photoFile.path)
            }
            return PhotoDetailFragment().apply {
                arguments = bundle
            }
        }
    }

    private lateinit var mImageView: ImageView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val view: View = inflater.inflate(R.layout.fragment_photo, container, false)

        mImageView = view.findViewById(R.id.img_photo_full)

        val path = arguments?.getString(ARG_URI)
        updatePhotoView(path)

        return view
    }

    private fun updatePhotoView(path: String?) {
        val file = File(path)
        if (!path.isNullOrEmpty() && file.exists()) {
            val bitmap = getScaledBitmap(path, requireActivity())
            mImageView.setImageBitmap(bitmap)
        } else {
            mImageView.setImageDrawable(null)
        }
    }
}