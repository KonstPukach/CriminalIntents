package com.example.criminalintents

import android.app.Application
import android.util.Log
import java.io.File

class CriminalIntentApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CrimeRepository.initialize(this)    // Initializing database repository
    }
}