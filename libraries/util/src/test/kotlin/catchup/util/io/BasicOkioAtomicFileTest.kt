package catchup.util.io

import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import okio.IOException
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class BasicOkioAtomicFileTest {

  private val fs = FakeFileSystem()
  private val filePath = "file".toPath()
  private val file = AtomicFile(filePath, fs, setPosixPermissions = false)

  @After
  fun tearDown() {
    fs.checkNoOpenFiles()
  }

  @Test
  fun tryWriteSuccess() {
    file.tryWrite { it.write(byteArrayOf(0, 1, 2)) }
    val bytes = file.readByteArray()
    assertArrayEquals(byteArrayOf(0, 1, 2), bytes)
  }

  @Test
  fun tryWriteSuccess_append() {
    // Write some initial bytes
    fs.write(filePath) { write(byteArrayOf(0, 1, 2)) }

    file.tryWrite(append = true) { it.write(byteArrayOf(0, 1, 2)) }
    val bytes = file.readByteArray()
    assertArrayEquals(byteArrayOf(0, 1, 2, 0, 1, 2), bytes)
  }

  @Test
  fun tryWriteSuccess_append_noExisting() {
    file.tryWrite(append = true) { it.write(byteArrayOf(0, 1, 2)) }
    val bytes = file.readByteArray()
    assertArrayEquals(byteArrayOf(0, 1, 2), bytes)
  }

  @Test
  fun tryWriteFail() {
    file.tryWrite { sink -> sink.write(byteArrayOf(0, 1, 2)) }

    val failure = IOException("Broken!")
    val exception =
      assertFailsWith<IOException> {
        file.tryWrite {
          it.write(byteArrayOf(3, 4, 5))
          throw failure
        }
      }
    assertThat(exception).isSameInstanceAs(failure)

    val bytes = file.readByteArray()
    assertArrayEquals(byteArrayOf(0, 1, 2), bytes)
  }

  @Test
  fun writeBytes() {
    file.writeBytes(byteArrayOf(0, 1, 2))

    val bytes = file.readByteArray()
    assertArrayEquals(byteArrayOf(0, 1, 2), bytes)
  }

  @Test
  fun writeText() {
    file.writeText("Hey")

    val bytes = file.readByteArray()
    assertArrayEquals(byteArrayOf(72, 101, 121), bytes)
  }

  @Test
  fun writeTextCharset() {
    file.writeText("Hey", charset = Charsets.UTF_16LE)

    val bytes = file.readByteArray()
    assertArrayEquals(byteArrayOf(72, 0, 101, 0, 121, 0), bytes)
  }

  @Test
  fun readBytes() {
    file.tryWrite { sink -> sink.write(byteArrayOf(0, 1, 2)) }

    assertArrayEquals(byteArrayOf(0, 1, 2), file.readByteArray())
  }

  @Test
  fun readText() {
    file.tryWrite { sink -> sink.write(byteArrayOf(72, 101, 121)) }

    assertEquals("Hey", file.readText())
  }

  @Test
  fun readTextCharset() {
    file.tryWrite { sink -> sink.write(byteArrayOf(72, 0, 101, 0, 121, 0)) }

    assertEquals("Hey", file.readText(charset = Charsets.UTF_16LE))
  }
}
