package catchup.app.data

import com.google.common.truth.Truth.assertThat
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import org.junit.Before
import org.junit.Test

class LumberYardTest {

  private val fs = FakeFileSystem()
  private val logsPath = "logs".toPath()
  private val flushInterval = 1000L.milliseconds
  private val testClock =
    object : Clock {
      override fun now(): Instant {
        return Instant.DISTANT_PAST
      }
    }

  @Before
  fun setup() {
    fs.createDirectory(logsPath)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun smokeTest() = runTest {
    val lumberYard =
      LumberYard(
        logsPath,
        scope = this,
        flushInterval = flushInterval,
        fs = fs,
        clock = testClock,
      )

    // 3 is DEBUG
    val entry =
      LumberYard.Entry(
        testClock.now().toLocalDateTime(TimeZone.UTC),
        3,
        "tag",
        "message\nwith\nnewlines",
      )
    lumberYard.addEntry(entry)

    assertThat(logFiles()).isEmpty()

    println("testScheduler.currentTime: ${testScheduler.currentTime}")
    testScheduler.advanceTimeBy(flushInterval.plus(1.seconds))
    println("testScheduler.currentTime: ${testScheduler.currentTime}")

    assertThat(logFiles()).hasSize(1)
    val logText = fs.read(logFiles().first()) { readUtf8() }.trim()
    assertThat(logText).contains("tag D message")

    val result = lumberYard.cleanUp()
    assertThat(result).isGreaterThan(0L)
    assertThat(logFiles()).isEmpty()
  }

  @Test
  fun closeAndJoinFlushes() = runTest {
    val lumberYard =
      LumberYard(
        logsPath,
        scope = this,
        flushInterval = flushInterval,
        fs = fs,
        clock = testClock,
      )

    // 3 is DEBUG
    val entry =
      LumberYard.Entry(
        testClock.now().toLocalDateTime(TimeZone.UTC),
        3,
        "tag",
        "message\nwith\nnewlines",
      )
    lumberYard.addEntry(entry)

    assertThat(logFiles()).isEmpty()

    lumberYard.closeAndJoin()

    assertThat(logFiles()).hasSize(1)
    val logText = fs.read(logFiles().first()) { readUtf8() }.trim()
    assertThat(logText).contains("tag D message")
  }

  private fun logFiles() = fs.list(logsPath)
}
