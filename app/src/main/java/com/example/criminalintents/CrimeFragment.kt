package com.example.criminalintents

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import java.util.*
import androidx.lifecycle.Observer
import java.io.File

private const val DIALOG_DATE = "DialogDate"
private const val DIALOG_PHOTO = "DialogPhoto"
private const val REQUEST_CODE_DATE = 0
private const val REQUEST_CODE_CONTACT = 1
private const val REQUEST_CODE_PHOTO = 2
private const val DATE_FORMAT = "EEE, MMM, dd"
private const val FILE_PROVIDER_AUTH_STRING = "com.example.criminalintents.fileprovider"

class CrimeFragment : Fragment(), DatePickerFragment.Callbacks {
    private var mCrime: Crime? = null
    private lateinit var photoFile: File
    private lateinit var photoUri: Uri

    /* Fields */
    private lateinit var mEditTextTitle: EditText
    private lateinit var mButtonDate: Button
    private lateinit var mCheckBoxSolved: CheckBox
    private lateinit var mButtonReport: Button
    private lateinit var mButtonChooseSuspect: Button
    private lateinit var mImageViewPhoto: ImageView
    private lateinit var mImageButtonAddPhoto: ImageButton

    private val crimeDetailViewModel by lazy {
        ViewModelProviders.of(this).get(CrimeDetailViewModel::class.java)
    }

    companion object {
        private const val ARG_CRIME_ID: String = "crime_id"

        fun newInstance(crimeId: UUID): CrimeFragment {
            val args = Bundle()
            args.putSerializable(ARG_CRIME_ID, crimeId)
            return CrimeFragment().apply { arguments = args }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val crimeId = arguments?.getSerializable(ARG_CRIME_ID) as UUID
        crimeDetailViewModel.loadCrime(crimeId)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view: View = inflater.inflate(R.layout.fragment_crime, container, false)

        mButtonDate = view.findViewById(R.id.button_crime_date)
        mEditTextTitle = view.findViewById(R.id.edit_text_crime_title)
        mCheckBoxSolved = view.findViewById(R.id.check_box_crime_solved)
        mButtonReport = view.findViewById(R.id.btn_send_report)
        mButtonChooseSuspect = view.findViewById(R.id.btn_choose_suspects)
        mImageButtonAddPhoto = view.findViewById(R.id.btn_add_photo)
        mImageViewPhoto = view.findViewById(R.id.img_crime_photo)

        crimeDetailViewModel.crimeLiveData.observe(
            viewLifecycleOwner,
            Observer { crime ->
                crime?.let {
                    this.mCrime = crime
                    photoFile = crimeDetailViewModel.getPhotoFile(crime)
                    photoUri = FileProvider.getUriForFile(
                        requireActivity(),
                        FILE_PROVIDER_AUTH_STRING,
                        photoFile
                    )
                    updateUI()
                }

            }
        )
        return view
    }

    override fun onStart() {
        super.onStart()

        mImageButtonAddPhoto.apply {
            // check if camera exists on the current device
            val packageManager: PackageManager = requireActivity().packageManager
            val captureImageIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val resolvedActivity: ResolveInfo? = packageManager.resolveActivity(
                captureImageIntent,
                PackageManager.MATCH_DEFAULT_ONLY
            )
            if (resolvedActivity == null) {
                isEnabled = false   // block button if not exists
            }

            setOnClickListener {
                captureImageIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)

                // get all activities to take a photo
                val cameraActivities: List<ResolveInfo> = packageManager.queryIntentActivities(
                    captureImageIntent,
                    PackageManager.MATCH_DEFAULT_ONLY
                )

                for (cameraActivity in cameraActivities) {
                    // grant permission to write file by @photoUri for all activities, which are appropriate
                    requireActivity().grantUriPermission(
                        cameraActivity.activityInfo.packageName,
                        photoUri,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                }
                startActivityForResult(captureImageIntent, REQUEST_CODE_PHOTO)
            }
        }

        mButtonDate.setOnClickListener {
            DatePickerFragment.newInstance(mCrime?.date!!).apply {
                setTargetFragment(this@CrimeFragment, REQUEST_CODE_DATE)
                show(this@CrimeFragment.requireFragmentManager(), DIALOG_DATE)
            }
        }

        mButtonReport.setOnClickListener {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, getCrimeReport())
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.crime_report_subject))
            }.also {
                val chooser = Intent.createChooser(it, getString(R.string.send_report))
                startActivity(chooser)  // chooser gives an opportunity to select one app to process the intent
            }
        }

        mButtonChooseSuspect.apply {
            val pickContactIntent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
            setOnClickListener {
                startActivityForResult(pickContactIntent, REQUEST_CODE_CONTACT)
            }

            // check if activity exists and the intent can be processed
            val packageManager: PackageManager = requireActivity().packageManager
            val resolvedActivity: ResolveInfo? =
                packageManager.resolveActivity(pickContactIntent,
                    PackageManager.MATCH_DEFAULT_ONLY)
            if (resolvedActivity == null) {
                isEnabled = false   // block button
            }
        }

        mImageViewPhoto.apply {
            setOnClickListener {
                PhotoDetailFragment.newInstance(photoFile).apply {
                    show(this@CrimeFragment.requireFragmentManager(), DIALOG_PHOTO)
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        crimeDetailViewModel.saveCrime(mCrime!!)
    }

    override fun onDetach() {
        super.onDetach()

        // close permission to write by uri
        requireActivity().revokeUriPermission(photoUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when {
            resultCode != Activity.RESULT_OK -> return
            requestCode == REQUEST_CODE_CONTACT && data != null -> {
                val contactUri: Uri = data.data!!
                val queryFields = arrayOf(ContactsContract.Contacts.DISPLAY_NAME)
                val cursor = requireActivity().contentResolver
                    .query(contactUri, queryFields, null, null, null)
                cursor?.use {
                    if (it.count == 0) {
                        return
                    }
                    it.moveToFirst()
                    val suspect = it.getString(0)
                    mCrime?.suspect = suspect
                    crimeDetailViewModel.saveCrime(mCrime!!)
                    mButtonChooseSuspect.text = suspect
                }
            }

            requestCode == REQUEST_CODE_PHOTO -> {
                // close permission to write by uri
                requireActivity().revokeUriPermission(photoUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

                updatePhotoView()
            }
        }
    }

    private fun updateUI() {
        if (mCrime?.suspect?.isNotEmpty()!!) {
            mButtonChooseSuspect.text = mCrime?.suspect
        }
        mButtonDate.text = mCrime?.date.toString()
        mEditTextTitle.setText(mCrime?.title ?: "")
        mEditTextTitle.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                mCrime?.title = s.toString()
            }

            override fun afterTextChanged(s: Editable?) {

            }
        })

        mCheckBoxSolved.apply {
            isChecked = mCrime?.solved!!
            jumpDrawablesToCurrentState()
        }
        mCheckBoxSolved.setOnCheckedChangeListener { _, isChecked ->
            mCrime?.solved = isChecked
        }
        updatePhotoView()
    }

    private fun updatePhotoView() {
        if (photoFile.exists()) {
            val bitmap = getScaledBitmap(photoFile.path, requireActivity())
            mImageViewPhoto.setImageBitmap(bitmap)
        } else {
            mImageViewPhoto.setImageDrawable(null)
        }
    }

    private fun getCrimeReport(): String {
        val solvedString = if (mCrime?.solved!!) {
            getString(R.string.crime_report_solved)
        } else {
            getString(R.string.crime_report_unsolved)
        }

        val dateString = DateFormat.format(DATE_FORMAT, mCrime?.date).toString()
        var suspect = if (mCrime?.suspect?.isBlank()!!) {
            getString(R.string.crime_report_suspect, mCrime?.suspect)
        } else {
            getString(R.string.crime_report_no_suspect)
        }
        return getString(R.string.crime_report,
                    mCrime?.title, dateString, solvedString, suspect)
    }

    override fun onDateSelected(date: Date) {
        mCrime?.date = date
        updateUI()
    }
}