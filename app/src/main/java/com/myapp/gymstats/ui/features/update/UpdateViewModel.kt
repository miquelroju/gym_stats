package com.myapp.gymstats.ui.features.update

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myapp.gymstats.BuildConfig
import com.myapp.gymstats.data.remote.UpdateChecker
import com.myapp.gymstats.data.remote.UpdateInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UpdateViewModel @Inject constructor() : ViewModel() {
    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo = _updateInfo.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading = _isDownloading.asStateFlow()

    fun checkForUpdate() {
        viewModelScope.launch {
            _updateInfo.value = UpdateChecker.checkForUpdate(BuildConfig.VERSION_CODE)
        }
    }

    fun downloadAndInstall(context: Context, url: String) {
        viewModelScope.launch {
            _isDownloading.value = true
            UpdateChecker.downloadAndInstall(context, url)
            _isDownloading.value = false
        }
    }

    fun dismiss() { _updateInfo.value = null }
}