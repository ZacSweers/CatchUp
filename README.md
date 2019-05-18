<p>
<img src="https://github.com/ZacSweers/CatchUp/blob/master/app/src/main/play/listings/en-US/feature-graphic/feature.png?raw=true"/>
</p>

CatchUp
=======

An app for catching up on things.

https://medium.com/@sweers/catching-up-on-catchup-introduction-7581c099f4bc

## Motivations

There's a lot of services I like reading up on throughout the day. Most of these services have
dedicated apps for consuming them, but often times I just want to skim the front page and only deep
dive occasionally. Enter CatchUp: a high level presentation of the "front page" of several services
in short form, and intelligent deeplinking into dedicated apps if you want to go further.

CatchUp is not an all-purpose client for each of these services, just the concierge for at-a-glance
details and router for getting on your way. It does not support login for any service, it does not
support customization/filtering of their feed. CatchUp is dumb, and you should use one of the many
great dedicated apps for this if you want more integration features.

CatchUp is also very much a testing ground for things I personally dive into, from architecture,
libraries, patterns, API quirks, and more. It's been a very fun project to spike test new things.

## Features

- Multiple services
- Hacker News
- Reddit
- Medium
- Product Hunt
- Slashdot
- Designer News
- Dribbble
- GitHub
- Infinite scrolling on supported services
- Pleasant, simple, consistent UI for across services
- Night mode
- Smart deeplinking into dedicated apps

## Technologies

- Kotlin
- RxJava 2/AutoDispose
- Debugging tooling as a first class citizen in the debug build
- Leak Canary, Chuck, Scalpel, debug drawer, Stetho, bug reporting, the works
- AndroidX/Jetpack
- Dagger 2
- One of the more interesting parts of CatchUp is that its service architecture is a Dagger-powered plugin system
- Room (Arch components)
- AutoValue + extensions
- Firebase
- Glide
- Apollo GraphQL
- Standard Square buffet of Okio/OkHttp 3/Retrofit 2/Moshi
- ThreetenABP
- Inspector

There's a lot of neat/interesting little tidbits in the CatchUp source code that I plan to write a
mini blog series about. Each service has its own nuances that make them unique to work with in code.

## Testing

While this is a personal pet project, extensive tests can be found [here](https://youtu.be/oHg5SJYRHA0).

## Influences

This app owes a lot of its inspiration, implementation details, and general inner workings to the
work of others. Particularly:
- [Nick Butcher](https://twitter.com/@crafty) and his [Plaid](https://github.com/nickbutcher/plaid) app
- [Jake Wharton](https://twitter.com/@jakewharton) and his [u2020](https://github.com/jakewharton/u2020) demo app

## Download

CatchUp is in open alpha, but master tends to be rather far ahead of what's on the Play Store.

<a href='https://play.google.com/store/apps/details?id=io.sweers.catchup'>
	<img alt='Get it on Google Play'
		src='https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png'
		height="116" width="300"/>
</a>

## Development

If you'd like to build CatchUp locally, you _should_ be able to just clone and build with no issues.

CatchUp tends to keep up with Android Studio canaries, so you may have to use a canary version.
Check the Android Gradle Plugin `deps.android.gradlePlugin` dependency in `gradle/dependencies.kt`.

If you want to build with working services, some require API keys. See the
[wiki](https://github.com/ZacSweers/CatchUp/wiki/Authentication-information) for more details on
which services require keys.

Bug fixes are always welcome. Tests are too if you're into that kinda thing, but I'm not actively
trying to make this project a shining icon of TDD. For new features or otherwise significant work,
please discuss in an issue first.

Note that by default, I have a Timber tree that crashes the app in the event of an error in debug
(fix me now!). This may be problematic if you don't have services authenticated (especially Firebase
and its ever shifting requirements), so you can disable this behavior via setting the `catchup.crashOnTimberError`
property in the root `gradle.properties` file to `false`.

For apollo-android's code generation: if you want to use a local installation of the `apollo-codegen`
node module you'll need to make sure `0.19.` is installed and linked (`npm install -g apollo-codegen@0.19.1`). Otherwise,
the gradle plugin should gracefully fallback to downloading it on demand.

License
-------

	Copyright (C) 2017 Zac Sweers

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

	http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
