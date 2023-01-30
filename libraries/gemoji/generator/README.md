gemoji generator
================

This subproject contains a JSON file of all the emoji supported by GitHub and a Kotlin CLI to generate a `.db` file of aliases.

This is used by catchup to pre-package the gemoji database in the app ahead of time and use it for replacing aliases with emoji.

## Usage

1. Update `gemoji.json` to the latest version from https://github.com/github/gemoji.
2. Add a new `.sqm` file to `src/main/sqdelight/migrations` to increment the DB version.
3. Run `./generate_gemoji.sh` from the root project to generate `gemoji.db`.
4. Check in the updated files.
