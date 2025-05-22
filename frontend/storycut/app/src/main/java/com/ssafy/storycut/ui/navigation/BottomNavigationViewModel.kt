package com.ssafy.storycut.ui.navigation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class BottomNavigationViewModel @Inject constructor() : ViewModel() {
    private val _isBottomNavVisible = MutableLiveData(true)
    val isBottomNavVisible: LiveData<Boolean> = _isBottomNavVisible

    fun setBottomNavVisibility(visible: Boolean) {
        _isBottomNavVisible.value = visible
    }
} 