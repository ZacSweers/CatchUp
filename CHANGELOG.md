

## 0.3.0 (2018-03-18)
* **New:** You can now reorder services in settings
* **New:** You can now enable/disable services in settings. Note this is WIP and not polished yet.
* **Enhancement:** GitHub is now paginated when there are more results (thanks to [@charlesdurham](https://github.com/charlesdurham)!)
* **Enhancement:** All models have been moved to native kotlin data classes using a new Moshi code gen implementation. Not really a user facing feature, but I'm really proud of it :)
* **Enhancement:** APK size went on a diet! Fully enabled shrinking and APK splits, so now download size is ~4.9MB.
* **Fix:** Medium is now fixed via pointing to the new `/topic/popular` endpoint
* **Fix:** Building with a new version of D8 that should avoid some bizarre VM crashes on some Mediatek devices ಠ_ಠ

## 0.2.2 (2017-12-17)
* Fix pagination not working

## 0.2.1 (2017-12-16)
* Don't show changelog on first display
* Make changelog section sort by descending date

## 0.2.0 (2017-12-16)
* **New:** Changelog notifications in the app
* **New:** In-app hints implementation using [TapTargetView](https://github.com/keepsafe/TapTargetView)
* **New:** French translations (thanks to [@anicolas](https://github.com/anicolas)!)
* **New:** German translations (thanks to [@AlexAmin](https://github.com/AlexAmin)!)
* **New:** Built for Android 8.1
* **Enhancement:** GitHub emojis are rendered in titles now (thanks to [@charlesdurham](https://github.com/charlesdurham)!)
* **Enhancement:** List items use ConstrainLayout now, which should have better perf (thanks to [@R4md4c](https://github.com/R4md4c)!)
* **Fix:** Gifs in Dribbble should no longer accidentally show multiple times (thanks to [@charlesdurham](https://github.com/charlesdurham)!)
* **Fix:** Existing images shouldn't re-fade on refresh
* **Fix:** A small memory leak in image services

## Initial release
