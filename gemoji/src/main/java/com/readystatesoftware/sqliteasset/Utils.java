/*
 * Copyright (C) 2011 readyState Software Ltd
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

package com.readystatesoftware.sqliteasset;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

class Utils {

  private static final String TAG = SQLiteAssetHelper.class.getSimpleName();

  public static void writeExtractedFileToDisk(InputStream in, OutputStream outs) throws IOException {
    byte[] buffer = new byte[1024];
    int length;
    while ((length = in.read(buffer))>0){
      outs.write(buffer, 0, length);
    }
    outs.flush();
    outs.close();
    in.close();
  }

  public static ZipInputStream getFileFromZip(InputStream zipFileStream) throws IOException {
    ZipInputStream zis = new ZipInputStream(zipFileStream);
    ZipEntry ze;
    while ((ze = zis.getNextEntry()) != null) {
      Log.w(TAG, "extracting file: '" + ze.getName() + "'...");
      return zis;
    }
    return null;
  }
}
