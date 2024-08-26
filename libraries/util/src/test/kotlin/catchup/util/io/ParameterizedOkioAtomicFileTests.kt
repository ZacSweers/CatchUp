/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package catchup.util.io

import catchup.util.io.ParameterizedOkioAtomicFileTests.WriteAction.ABORT
import catchup.util.io.ParameterizedOkioAtomicFileTests.WriteAction.FAIL
import catchup.util.io.ParameterizedOkioAtomicFileTests.WriteAction.FINISH
import catchup.util.io.ParameterizedOkioAtomicFileTests.WriteAction.READ_FINISH
import kotlin.test.fail
import okio.IOException
import okio.Path
import okio.buffer
import okio.fakefilesystem.FakeFileSystem
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ParameterizedOkioAtomicFileTests(
  @Suppress("UNUSED_PARAMETER") unusedTestName: String,
  parameters: Parameters,
) {

  enum class WriteAction {
    FINISH,
    FAIL,
    ABORT,
    READ_FINISH,
  }

  private val existingFileNames: Array<String>? = parameters.existingFileNames
  private val writeAction: WriteAction? = parameters.writeAction
  private val expectedBytes: ByteArray? = parameters.expectedBytes
  private val fs = FakeFileSystem()
  private val directory = fs.workingDirectory
  private val baseFile = directory / BASE_NAME
  private val newFile = directory / NEW_NAME

  @After
  fun tearDown() {
    fs.checkNoOpenFiles()
  }

  @Test
  fun testAtomicFile() {
    if (existingFileNames != null) {
      for (fileName in existingFileNames) {
        when (fileName) {
          BASE_NAME -> writeBytes(baseFile, BASE_BYTES)
          NEW_NAME -> writeBytes(newFile, EXISTING_NEW_BYTES)
          BASE_NAME_DIRECTORY -> {
            fs.createDirectory(baseFile)
            assertTrue(fs.exists(baseFile))
          }
          else -> fail(fileName)
        }
      }
    }

    val atomicFile = AtomicFile(baseFile, fs, setPosixPermissions = false)
    if (writeAction != null) {
      atomicFile.startWrite().let { handle ->
        handle.sink().buffer().let { sink ->
          sink.write(NEW_BYTES)
          sink.flush()
          sink.close()
          when (writeAction) {
            FINISH -> {
              atomicFile.finishWrite(handle)
            }
            FAIL -> {
              atomicFile.failWrite(handle)
            }
            ABORT -> {
              // Close for the sake of the FS open files check
              handle.close()
            }
            READ_FINISH -> {
              // We are only using this action when there is no base file.
              assertThrows(IOException::class.java) { atomicFile.readByteArray() }
              atomicFile.finishWrite(handle)
            }
          }
        }
      }
    }

    if (expectedBytes != null) {
      assertArrayEquals(expectedBytes, atomicFile.readByteArray())
    } else {
      assertThrows(IOException::class.java) { atomicFile.readByteArray() }
    }
  }

  private fun writeBytes(file: Path, bytes: ByteArray) {
    fs.sink(file).buffer().use { sink -> sink.write(bytes) }
  }

  fun <T : Throwable> assertThrows(expectedType: Class<T>, runnable: () -> Unit): T {
    try {
      runnable()
    } catch (t: Throwable) {
      if (!expectedType.isInstance(t)) {
        sneakyThrow<RuntimeException>(t)
      }
      @Suppress("UNCHECKED_CAST")
      return t as T
    }
    throw AssertionError(String.format("Expected %s wasn't thrown", expectedType.simpleName))
  }

  private fun <T : RuntimeException> sneakyThrow(throwable: Throwable) {
    @Suppress("UNCHECKED_CAST") throw throwable as T
  }

  companion object {
    private val BASE_NAME = "base"
    private val NEW_NAME = "$BASE_NAME.new"

    // The string isn't actually used, but we just need a different identifier.
    private val BASE_NAME_DIRECTORY = "$BASE_NAME.dir"
    private val UTF_8 = Charsets.UTF_8
    private val BASE_BYTES = "base".toByteArray(UTF_8)
    private val EXISTING_NEW_BYTES = "unnew".toByteArray(UTF_8)
    private val NEW_BYTES = "new".toByteArray(UTF_8)

    @JvmStatic
    @Parameterized.Parameters(name = "{0}")
    fun data(): List<Array<Any>> {
      return listOf(
        arrayOf("none + none = none", Parameters(null, null, null)),
        arrayOf("none + finish = new", Parameters(null, FINISH, NEW_BYTES)),
        arrayOf("none + fail = none", Parameters(null, FAIL, null)),
        arrayOf("none + abort = none", Parameters(null, ABORT, null)),
        arrayOf("base + none = base", Parameters(arrayOf(BASE_NAME), null, BASE_BYTES)),
        arrayOf("base + finish = new", Parameters(arrayOf(BASE_NAME), FINISH, NEW_BYTES)),
        arrayOf("base + fail = base", Parameters(arrayOf(BASE_NAME), FAIL, BASE_BYTES)),
        arrayOf("base + abort = base", Parameters(arrayOf(BASE_NAME), ABORT, BASE_BYTES)),
        arrayOf("new + none = none", Parameters(arrayOf(NEW_NAME), null, null)),
        arrayOf("new + finish = new", Parameters(arrayOf(NEW_NAME), FINISH, NEW_BYTES)),
        arrayOf("new + fail = none", Parameters(arrayOf(NEW_NAME), FAIL, null)),
        arrayOf("new + abort = none", Parameters(arrayOf(NEW_NAME), ABORT, null)),
        arrayOf(
          "base & new + none = base",
          Parameters(arrayOf(BASE_NAME, NEW_NAME), null, BASE_BYTES),
        ),
        arrayOf(
          "base & new + finish = new",
          Parameters(arrayOf(BASE_NAME, NEW_NAME), FINISH, NEW_BYTES),
        ),
        arrayOf(
          "base & new + fail = base",
          Parameters(arrayOf(BASE_NAME, NEW_NAME), FAIL, BASE_BYTES),
        ),
        arrayOf(
          "base & new + abort = base",
          Parameters(arrayOf(BASE_NAME, NEW_NAME), ABORT, BASE_BYTES),
        ),
        arrayOf("base.dir + none = none", Parameters(arrayOf(BASE_NAME_DIRECTORY), null, null)),
        arrayOf(
          "base.dir + finish = new",
          Parameters(arrayOf(BASE_NAME_DIRECTORY), FINISH, NEW_BYTES),
        ),
        arrayOf("base.dir + fail = none", Parameters(arrayOf(BASE_NAME_DIRECTORY), FAIL, null)),
        arrayOf("base.dir + abort = none", Parameters(arrayOf(BASE_NAME_DIRECTORY), ABORT, null)),
        arrayOf(
          "base.dir & new + none = none",
          Parameters(arrayOf(BASE_NAME_DIRECTORY, NEW_NAME), null, null),
        ),
        arrayOf(
          "base.dir & new + finish = new",
          Parameters(arrayOf(BASE_NAME_DIRECTORY, NEW_NAME), FINISH, NEW_BYTES),
        ),
        arrayOf(
          "base.dir & new + fail = none",
          Parameters(arrayOf(BASE_NAME_DIRECTORY, NEW_NAME), FAIL, null),
        ),
        arrayOf(
          "base.dir & new + abort = none",
          Parameters(arrayOf(BASE_NAME_DIRECTORY, NEW_NAME), ABORT, null),
        ),
        arrayOf("none + read & finish = new", Parameters(null, READ_FINISH, NEW_BYTES)),
      )
    }
  }

  // JUnit on API 17 somehow turns null parameters into the string "null". Wrapping the parameters
  // inside a class solves this problem.
  class Parameters
  internal constructor(
    var existingFileNames: Array<String>?,
    var writeAction: WriteAction?,
    var expectedBytes: ByteArray?,
  )
}
