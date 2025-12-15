package com.demo.cpp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.demo.cpp.databinding.ActivityMainBinding

class StartActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Example of a call to a native method
        binding.sampleText.text = stringFromJNI()
    }

    /**
     * A native method that is implemented by the 'cpp' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    companion object {
        // Used to load the 'cpp' library on application startup.
        init {
            System.loadLibrary("cpp")
        }
    }
}