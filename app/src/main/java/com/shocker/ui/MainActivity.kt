package com.shocker.ui

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.shocker.hideapk.databinding.ActivityMainBinding
import com.shocker.hideapk.hide.HideAPK
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var activity: Activity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        activity=this
        GlobalScope.launch {
            HideAPK.hide(activity,"randomName", applicationInfo.sourceDir)
        }
    }
}