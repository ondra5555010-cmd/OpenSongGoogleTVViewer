package com.example.opensonggoogletvviewer.viewmodel

import androidx.lifecycle.ViewModel
import com.example.opensonggoogletvviewer.data.OpenSongRepository
import com.example.opensonggoogletvviewer.model.ConnectionState
import com.example.opensonggoogletvviewer.model.CurrentSlide
import kotlinx.coroutines.flow.StateFlow

class SlideViewModel(
    private val repo: OpenSongRepository
) : ViewModel() {

    val slide: StateFlow<CurrentSlide> = repo.slide
    val connection: StateFlow<ConnectionState> = repo.connection

    fun start() = repo.start()
    fun stop() = repo.stop()

    fun nextSlide() = repo.nextSlide()
    fun previousSlide() = repo.previousSlide()

    override fun onCleared() {
        repo.stop()
        super.onCleared()
    }
}