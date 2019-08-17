#!/usr/bin/env python3

#
#  Copyright (c) 2017 Zac Sweers
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

import sqlite3
import json
import os

dbPath = 'gemoji.db'
jsonPath = 'gemoji.json'

if os.path.exists(dbPath):
    os.remove(dbPath)

connection = sqlite3.connect(dbPath)
cursor = connection.cursor()
cursor.execute('CREATE TABLE `gemoji` (`alias` TEXT NOT NULL, `emoji` TEXT, PRIMARY KEY(`alias`))')

file = open(jsonPath, 'r', -1, "utf8")
gemojis = json.load(file)

for gemoji in gemojis:
    emoji = gemoji['emoji'] if 'emoji' in gemoji else None
    aliases = gemoji['aliases'] if 'aliases' in gemoji else None

    if emoji is not None and aliases is not None:
        for alias in filter(None, aliases):
            cursor.execute("INSERT INTO gemoji VALUES ('%s', '%s')" % (alias, emoji))

connection.commit()
connection.close()

print("Longest alias")
longestAlias = max([alias for alias in [max(gemoji['aliases'], key=len) for gemoji in gemojis]], key=len)
print(longestAlias)
