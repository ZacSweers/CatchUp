/*
 * Copyright (c) 2017 Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sweers.catchup.data.gemoji

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

internal const val GEMOJI_TABLE_NAME = "gemoji"
internal const val GEMOJI_ALIAS_COLUMN_NAME = "alias"
internal const val GEMOJI_EMOJI_COLUMN_NAME = "emoji"

@Entity(tableName = GEMOJI_TABLE_NAME)
data class Gemoji(
    @PrimaryKey
    @ColumnInfo(name = GEMOJI_ALIAS_COLUMN_NAME)
    var alias: String,
    @ColumnInfo(name = GEMOJI_EMOJI_COLUMN_NAME)
    var emoji: String
)
