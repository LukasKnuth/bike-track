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

* Add charts for all collected data (AchartEngine, JavaDoc for ChartFactory).
* Cache the calculated statistics in a new DB-table.
* Make the TrackerService bind-able and use the messages to get the GPS-state.
* Change Font to Roboto (http://developer.android.com/design/style/typography.html)
* Add Photo-taking capabilities (Tour pictures, with GEO-Tags, etc)
* New Tour-Activity and Main-Activity Listview layout (with the images above and some data about the tour)
* On first application-launch, check the device-locale and guess measure-system