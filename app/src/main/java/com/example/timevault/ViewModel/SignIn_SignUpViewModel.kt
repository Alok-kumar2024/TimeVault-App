package com.example.timevault.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

enum class TabSelected { SignIn, SignUp }

class SignViewModel : ViewModel() {
    private val _SelectedTab = MutableLiveData<TabSelected>(TabSelected.SignIn)

    val selectedTab : LiveData<TabSelected> get() = _SelectedTab

    fun setTab (Tab : TabSelected)
    {
        _SelectedTab.value = Tab
    }



}