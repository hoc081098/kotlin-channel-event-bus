package com.hoc081098.channeleventbus

import com.hoc081098.flowext.interval
import java.util.concurrent.Executors
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalCoroutinesApi::class)
class ChannelEventBusJvmTest {
  @Test
  fun flatMapLatest_Works(): Unit = runBlocking(Dispatchers.IO) {
    val bus = ChannelEventBus(ChannelEventBusLogger.stdout())
    val flow = interval(initialDelay = Duration.ZERO, period = 100.milliseconds)
      .flowOn(Executors.newScheduledThreadPool(2).asCoroutineDispatcher())
      .take(10)
      .flatMapLatest { bus.receiveAsFlow(TestEventInt) }
      .take(100)

    launch {
      repeat(100) {
        delay(33)
        launch { bus.send(TestEventInt(it)) }
      }
    }

    launch {
      assertContentEquals(
        expected = (0..<100).map { TestEventInt(it) },
        actual = flow.toList().sortedBy { it.payload },
      )
    }
  }
}
