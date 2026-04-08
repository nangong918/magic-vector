package com.magicvector.activity.test

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.magicvector.R
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController

class VADMainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vad_main)

        val navView: BottomNavigationView = findViewById(R.id.nav_view_vad)
        val navController = findNavController(R.id.fragment_vad_activity_main)

        navView.setupWithNavController(navController)
    }
}