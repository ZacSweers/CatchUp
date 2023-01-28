gemoji
======

This subproject contains a JSON file of all the emoji supported by GitHub and a pythong script to generate a `.db` file of aliases.

This is used by catchup to pre-package the gemoji database in the app ahead of time and use it for replacing aliases with emoji.

## Usage

1. Update `gemoji.json` to the latest version from https://github.com/github/gemoji.
2. Run `python gemoji.py` to generate `gemoji.db`.
3. Copy `gemoji.db` to `libraries/gemoji/src/main/assets/databases/gemoji.db` to update the one packaged in the project.