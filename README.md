# Bike Track

"Bike Track" is an Android application, created for collecting geographical data on your bike tour with your Android mobile-phone or tablet. You can create multiple tours to organize the collected data and check out your statistics from the last ride.

"Bike Track" also offers a wide range of statistical features, including pretty charts and standard arithmetic functions (like the average speed or the total distance).

When the app is finished, the following capabilities will be implemented:

* Tracking your bike-ride using GPS
* Collecting [altitude](http://en.wikipedia.org/wiki/Altitude)-data using your devices [Barometer](http://en.wikipedia.org/wiki/Barometer) (more accurate) or the GPS module (less accurate)
* Measuring the speed with which you where moving at what point of the tour
* Showing all the above in a statistical fraction (with pretty charts)
* Review your tour on Google Maps (on the device)
* Export your tour-data for sharing- and backup-purposes

**Caution** The application **is in active development state** at this very moment. I can and won't guarantee that *any* of the above features are implemented. Only time knows how far I will get.

## Get it

See [Build it](#build-it)

### Build it

Before you can build the application with the provided gradle-scripts, you'll need to create the following two files:

* **Prerequisites** You have (created) a certificate to sign the app with.
* `signing.properties`: Defines what certificate to use and how to open it. See `signing_example.properties`.
* `apikeys.properties`: Defines all necessary API keys. Currently, only a Google Maps API Key is required, which you must generate with the certificate used above. See the [Google Maps Android Docs](https://developers.google.com/maps/documentation/android-api/signup) for more information.

## License

    Copyright 2012 Lukas Knuth

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

## Contribution

If you'd like to contribute to the project, just fork it here on GitHub, do your work and send me a pull request. For ideas on what you could add, see the [ToDo section](#todo).

If you encounter any **problems or bugs**, please also *open a ticket* on the [Issue-Tracker](https://github.com/LukasKnuth/bike-track/issues).

## ToDo

**Prioritized:**

* Add charts for all collected data (MPAndroidChart).
* Make the TrackerService bind-able and use the messages to get the GPS-state.
* Add capability to specify "goals" for a tour and the ability to track these goals during the tour (TTS to notify the user that he has/is about to archive one).
* When finishing a tour, do a reverse-golookup to find out where the tour started and ended/if it was a round-tripp (http://developer.android.com/reference/android/location/Geocoder.html)
* The Map-view needs some love:
    * Update the Google Play integration to the newest version
    * When opening the activity, zoom out so that the entire tour is visible and centered
    * Use a different color for the track
    * Don't reload the entire thing on config-changes
* The service needs to be more robust:
    * If GPS is disabled during a tour, notify the user (TTS? See below)
    * Show a persistent notification for the duration of the tour to easily get back
    * Handle device incapability's transparently (like no speed via GPS, use barometer for altitude, etz)
* Materialize the entire design
* On first application-launch, check the device-locale and guess measure-system
* Cache the calculated statistics in a new DB-table.
* Add Photo-taking capabilities (Tour pictures, with GEO-Tags, etc)
* Add a server-component which displays the apps data on a remote browser
    * Shows the tour-map
    * Allows export/import of tours
    * Shows the photos taken along the ride (see above)
