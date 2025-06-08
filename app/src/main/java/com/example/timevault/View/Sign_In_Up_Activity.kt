package com.example.timevault.View

import android.content.res.Configuration
import android.graphics.Paint
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.timevault.Model.ThemeHelper
import com.example.timevault.R
import com.example.timevault.ViewModel.SignViewModel
import com.example.timevault.ViewModel.TabSelected
import com.example.timevault.databinding.ActivitySignInUpBinding

class Sign_In_Up_Activity : AppCompatActivity() {

    private lateinit var binding: ActivitySignInUpBinding

    private lateinit var viewModel : SignViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val sharetheme = getSharedPreferences("theme", MODE_PRIVATE)
        val savedTheme = sharetheme.getString("themeOption", ThemeHelper.SYSTEM) ?: ThemeHelper.SYSTEM
        ThemeHelper.applyTheme(savedTheme)

        binding = ActivitySignInUpBinding.inflate(layoutInflater)
        viewModel = ViewModelProvider(this)[SignViewModel::class.java]

        WindowCompat.setDecorFitsSystemWindows(window,false)
        WindowInsetsControllerCompat(window,window.decorView).isAppearanceLightStatusBars = false

        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (savedInstanceState == null)
        {
            switch(SignIn())
            highlight(binding.SignInLogin,binding.SignUp)
        }

        viewModel.selectedTab.observe(this)
        {tab ->

            when(tab)
            {
                TabSelected.SignIn -> {
                    switch(SignIn())
                    highlight(binding.SignInLogin,binding.SignUp)
                }
                TabSelected.SignUp ->
                {
                    switch(SignUp())
                    highlight(binding.SignUp,binding.SignInLogin)
                }
                else -> {
                    switch(SignIn())
                    highlight(binding.SignInLogin,binding.SignUp)
                }
            }

        }

        binding.SignUp.setOnClickListener {
//            switch(SignUp())
//
//            highlight(binding.SignUp,binding.SignInLogin)

            viewModel.setTab(TabSelected.SignUp)
        }

        binding.SignInLogin.setOnClickListener {

//            switch(SignIn())
//            highlight(binding.SignInLogin,binding.SignUp)

            viewModel.setTab(TabSelected.SignIn)
        }
    }

    fun switch(fragment : Fragment)
    {
        supportFragmentManager.beginTransaction()
            .replace(R.id.FragmentOfLogin,fragment).commit()

    }
   private fun highlight(selected : Button,other : Button)
    {
        selected.paintFlags = selected.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        selected.setTextColor(ContextCompat.getColor(this, R.color.white))

        other.paintFlags = other.paintFlags and Paint.UNDERLINE_TEXT_FLAG.inv()
        other.setTextColor(ContextCompat.getColor(this, R.color.purple_dark))
    }
}