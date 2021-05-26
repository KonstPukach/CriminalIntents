package com.example.criminalintents

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import java.util.*
import androidx.lifecycle.Observer

class CrimeFragment : Fragment() {
    private var mCrime: Crime? = null

    /* Fields */
    private lateinit var mEditTextTitle: EditText
    private lateinit var mButtonDate: Button
    private lateinit var mCheckBoxSolved: CheckBox

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
        crimeDetailViewModel.crimeLiveData.observe(
            viewLifecycleOwner,
            Observer { crime ->
                crime?.let {
                    this.mCrime = crime
                    initElements(view)
                }

            }
        )
        return view
    }

    override fun onStop() {
        super.onStop()
        crimeDetailViewModel.saveCrime(mCrime!!)
    }

    private fun initElements(viewParent: View) {
        mEditTextTitle = viewParent.findViewById(R.id.edit_text_crime_title)
        mEditTextTitle.setText(mCrime?.title)
        mEditTextTitle.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                mCrime?.title = s.toString()
            }

            override fun afterTextChanged(s: Editable?) {

            }
        })

        mButtonDate = (viewParent.findViewById(R.id.button_crime_date) as Button).apply {
            text = mCrime?.date.toString()
            isEnabled = false
        }

        mCheckBoxSolved = viewParent.findViewById(R.id.check_box_crime_solved)
        mCheckBoxSolved.apply {
            isChecked = mCrime?.solved!!
            jumpDrawablesToCurrentState()
        }
        mCheckBoxSolved.setOnCheckedChangeListener { _, isChecked ->
            mCrime?.solved = isChecked
        }
    }
}