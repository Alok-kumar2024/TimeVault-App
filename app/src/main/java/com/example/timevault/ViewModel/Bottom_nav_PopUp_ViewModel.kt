package com.example.timevault.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.timevault.View.MainActivity

//enum class Bottom{Home , Vault}

class BottomPopUp : ViewModel()
{
    private val _isPopupShown = MutableLiveData<Boolean>(false)
    private val _chosenBottom = MutableLiveData<Int>(MainActivity.Home_id)

    val isPopupShown : LiveData<Boolean> get() = _isPopupShown
    val chosenBottom : LiveData<Int> get() = _chosenBottom

    fun showPopup()
    {
        if(_isPopupShown.value == true) return
        _isPopupShown.value = true
    }

    fun hidePopup()
    {
        _isPopupShown.value = false
    }

    fun togglePopup()
    {
        _isPopupShown.value = !_isPopupShown.value!!
    }

    fun setBottom(bottom : Int)
    {
        if(bottom == MainActivity.Home_id || bottom == MainActivity.Vault_id)
        {
            _chosenBottom.value = bottom
        }
    }

}