/*
 * Copyright (C) 2019. Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package catchup.flowbinding

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import catchup.util.kotlin.safeOffer
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

fun Context.intentReceivers(intentFilter: IntentFilter): Flow<Intent> = callbackFlow {
  val receiver =
    object : BroadcastReceiver() {
      override fun onReceive(context: Context, intent: Intent) {
        safeOffer(intent)
      }
    }
  if (Build.VERSION.SDK_INT >= 33) {
    registerReceiver(receiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
  } else {
    @Suppress("UnspecifiedRegisterReceiverFlag") registerReceiver(receiver, intentFilter)
  }

  awaitClose { unregisterReceiver(receiver) }
}
