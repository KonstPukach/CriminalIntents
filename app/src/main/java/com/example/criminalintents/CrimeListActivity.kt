package com.example.criminalintents

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import java.util.*

private const val TAG = "CrimeListActivity"

class CrimeListActivity : SingleFragmentActivity(), CrimeListFragment.Callbacks {
    override fun createFragment(): Fragment {
        return CrimeListFragment()
    }

    override fun onCrimeSelected(crimeId: UUID) {
        val fragment = CrimeFragment.newInstance(crimeId)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}