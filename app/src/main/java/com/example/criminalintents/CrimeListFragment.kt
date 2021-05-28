package com.example.criminalintents

import android.Manifest
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.*
import java.io.File
import java.lang.Exception
import java.util.*
import kotlin.concurrent.timerTask


private const val TAG = "CrimeListFragment"

class CrimeListFragment : Fragment() {
    interface Callbacks {
        fun onCrimeSelected(crimeId: UUID)
    }

    private var callbacks: Callbacks? = null

    private var mCrimeRecyclerView: RecyclerView? = null
    private var mAdapter: CrimeAdapter? = CrimeAdapter()

    private val crimeListViewModel: CrimeListViewModel by lazy {
        ViewModelProviders.of(this).get(CrimeListViewModel::class.java)
    }

    companion object {
        fun newInstance() : CrimeListFragment {
            return CrimeListFragment()
        }

        private val DIFF_CRIME = object : DiffUtil.ItemCallback<Crime>() {
            override fun areItemsTheSame(oldItem: Crime, newItem: Crime): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Crime, newItem: Crime): Boolean {
                return oldItem.equals(newItem)
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = context as Callbacks?
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view: View = inflater.inflate(R.layout.fragment_crime_list, container, false)
        mCrimeRecyclerView = view.findViewById(R.id.crime_recycler_view) as RecyclerView
        // set layoutManager object for recycler
        // LinearLayoutManager() manages elements on view where items have to be located
        mCrimeRecyclerView?.layoutManager = LinearLayoutManager(activity);
        mCrimeRecyclerView?.addItemDecoration(
            DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        crimeListViewModel.crimesListLiveData.observe(
            viewLifecycleOwner,     // synchronize LC of an observer and a fragment
            Observer { crimes ->    // gives reaction if crimes data is changed
                crimes?.let {
                    Log.i(TAG, "Got crimes ${crimes.size}")
                    updateUI(crimes)
                }
            }
        )
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_crime_list, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.new_crime -> {
                val crime = Crime("", false)
                crimeListViewModel.addCrime(crime)
                callbacks?.onCrimeSelected(crime.id)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onDetach() {
        super.onDetach()
        callbacks = null
    }

    private fun updateUI(crimes: List<Crime>) {
        mAdapter = CrimeAdapter()
        mCrimeRecyclerView?.adapter = mAdapter
        mAdapter?.submitList(crimes)
    }

    private inner class CrimeHolder(
        inflater: LayoutInflater,
        parent: ViewGroup
    ) : RecyclerView.ViewHolder(
        inflater.inflate(R.layout.list_item_crime, parent, false)
    ), View.OnClickListener {
        private val mTitleTextView = itemView.findViewById<TextView>(R.id.title_crime_item_text_view)
        private val mDateTextView = itemView.findViewById<TextView>(R.id.date_crime_item_text_view)

        private lateinit var mCrime: Crime

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(crime: Crime) {
            try {
                mCrime = crime
                mTitleTextView.text = crime.title
                mDateTextView.text = crime.date.toString()
            } catch (ex: Exception) {}

        }

        override fun onClick(v: View?) {
            callbacks?.onCrimeSelected(mCrime.id)
        }
    }


    /** ######## ADAPTER ########### */

    private inner class CrimeAdapter : ListAdapter<Crime, CrimeHolder>(DIFF_CRIME) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CrimeHolder {
            val layoutInflater = LayoutInflater.from(this@CrimeListFragment.activity)
            return CrimeHolder(layoutInflater, parent)

        }

        override fun onBindViewHolder(holder: CrimeHolder, position: Int) {
            val crime = getItem(position)
            holder.bind(crime)
        }
    }
}