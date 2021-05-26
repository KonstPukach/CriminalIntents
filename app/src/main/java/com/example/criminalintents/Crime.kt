package com.example.criminalintents

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity
data class Crime(
    @PrimaryKey val id: UUID = UUID.randomUUID(),       // special type to generate unique ID
    var date: Date = Date()
) {
    lateinit var title: String
    var solved: Boolean = false

    constructor(t: String, s: Boolean) : this() {
        title = t
        solved = s
    }
}