package com.example.hotwheelscollector.data.cloud

object SyncStatusManager {

    private var currentState =
        SyncState.SYNCED

    private val listeners =
        mutableListOf<(SyncState) -> Unit>()

    fun getState(): SyncState {

        return currentState
    }

    fun setState(state: SyncState) {

        currentState = state

        listeners.forEach {
            it(state)
        }
    }

    fun observe(
        listener: (SyncState) -> Unit
    ) {

        listeners.add(listener)

        listener(currentState)
    }

    fun removeObserver(
        listener: (SyncState) -> Unit
    ) {

        listeners.remove(listener)
    }
}