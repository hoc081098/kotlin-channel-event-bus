package com.hoc081098.channeleventbus.sample.android.ui.home.detail

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hoc081098.channeleventbus.ChannelEventBus
import com.hoc081098.channeleventbus.sample.android.common.HasSingleEventFlow
import com.hoc081098.channeleventbus.sample.android.common.SingleEventChannel
import com.hoc081098.channeleventbus.sample.android.ui.home.DetailResultToHomeEvent
import com.hoc081098.channeleventbus.sample.android.utils.NonBlankString.Companion.toNonBlankString
import com.hoc081098.channeleventbus.sample.android.utils.launchNowIn
import com.hoc081098.flowext.flowFromSuspend
import com.hoc081098.kmp.viewmodel.safe.NonNullSavedStateHandleKey
import com.hoc081098.kmp.viewmodel.safe.safe
import com.hoc081098.kmp.viewmodel.safe.string
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import timber.log.Timber

@Immutable
sealed interface DetailSingleEvent {
  data object Complete : DetailSingleEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class DetailVM(
  private val channelEventBus: ChannelEventBus,
  private val singleEventChannel: SingleEventChannel<DetailSingleEvent>,
  private val savedStateHandle: SavedStateHandle,
) : ViewModel(singleEventChannel),
  HasSingleEventFlow<DetailSingleEvent> by singleEventChannel {
  private val sendResultFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

  internal val textStateFlow: StateFlow<String> = savedStateHandle.safe.getStateFlow(TextKey)

  init {
    fun process(): Flow<Unit> = flowFromSuspend {
      delay(@Suppress("MagicNumber") 500) // simulate a long-running task

      textStateFlow.value
        .toNonBlankString()
        .map(::DetailResultToHomeEvent)
        .onSuccess(channelEventBus::send)
        .onSuccess { singleEventChannel.sendEvent(DetailSingleEvent.Complete) }
        .onFailure { Timber.e(it, "Error while sending result to home") }
    }

    sendResultFlow
      .flatMapLatest { process() }
      .launchNowIn(viewModelScope)
  }

  internal fun onTextChanged(text: String) {
    savedStateHandle.safe[TextKey] = text
  }

  internal fun sendResultToHome() {
    viewModelScope.launch { sendResultFlow.emit(Unit) }
  }

  private companion object {
    private val TextKey = NonNullSavedStateHandleKey.string("text", "")
  }
}
