package catchup.app.data

import catchup.app.data.LumberYard.Entry
import com.google.common.truth.Truth.assertThat
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import org.junit.Before
import org.junit.Test

class DiskLumberYardTest {

  private val fs = FakeFileSystem()
  private val logsPath = "logs".toPath()
  private val flushInterval = 1000.milliseconds
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

  @Test
  fun smokeTest() = runTest {
    val lumberYard =
      DiskLumberYard(
        logsPath,
        scope = this,
        flushInterval = flushInterval,
        fs = fs,
        clock = testClock,
      )

    // 3 is DEBUG
    val entry =
      Entry(testClock.now().toLocalDateTime(TimeZone.UTC), 3, "tag", "message\nwith\nnewlines")
    lumberYard.addEntry(entry)

    assertThat(logFiles()).isEmpty()

    testScheduler.advanceTimeBy(flushInterval.plus(1.milliseconds))

    assertThat(logFiles()).hasSize(1)
    val logText = fs.read(logFiles().first()) { readUtf8() }.trim()
    assertThat(logText).contains("tag D message")
    lumberYard.closeAndJoin()

    val result = lumberYard.cleanUp()
    assertThat(result).isGreaterThan(0L)
    assertThat(logFiles()).isEmpty()
  }

  @Test
  fun closeAndJoinFlushes() = runTest {
    val lumberYard =
      DiskLumberYard(
        logsPath,
        scope = this,
        flushInterval = flushInterval,
        fs = fs,
        clock = testClock,
      )

    // 3 is DEBUG
    val entry =
      Entry(testClock.now().toLocalDateTime(TimeZone.UTC), 3, "tag", "message\nwith\nnewlines")
    lumberYard.addEntry(entry)

    assertThat(logFiles()).isEmpty()

    lumberYard.closeAndJoin()

    assertThat(logFiles()).hasSize(1)
    val logText = fs.read(logFiles().first()) { readUtf8() }.trim()
    assertThat(logText).contains("tag D message")
  }

  // TODO
  //  test file rotations
  //  test repeated flushes

  private fun logFiles() = fs.list(logsPath)
}
