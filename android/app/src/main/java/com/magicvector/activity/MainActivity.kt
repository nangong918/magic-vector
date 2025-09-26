package com.magicvector.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.magicvector.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Example of a call to a native method
        binding.tvHello.text = stringFromJNI()
    }

    /**
     * A native method that is implemented by the 'magicvector' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    companion object {
        // Used to load the 'magicvector' library on application startup.
        init {
            System.loadLibrary("magicvector")
        }
    }
}