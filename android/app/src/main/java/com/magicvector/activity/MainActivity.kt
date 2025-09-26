package com.magicvector.activity

import android.os.Bundle
import com.core.baseutil.BaseAppCompatActivity
import com.magicvector.databinding.ActivityMainBinding

class MainActivity : BaseAppCompatActivity<ActivityMainBinding>(
    MainActivity::class
) {
    override fun initBinding(): ActivityMainBinding {
        return ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Example of a call to a native method
//        binding.tvHello.text = stringFromJNI()
    }

    override fun initView() {
        super.initView()

        setStatusBarColor(
            android.R.color.white
        )
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