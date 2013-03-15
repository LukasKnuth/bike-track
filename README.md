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

I'm not yet sure if the application will ever make it to Google Play. Therefore, you'll want to download the `.apk`-file from the download section of this project, copy it to your device and install it manually.

If you ever pirated an application, you'll probably know how to do that ;)

### Build it

At the moment, you'll need to have the *Android SDK*, *Platform 4.1* and the *Google APIs* for that platform, to successfully build the source yourself.

Also, you'll want to clone the git sub-modules before building the application.

Build scripts and other helpers *may* follow.

## License

You may use the application free of any charge, if you like.

## Contribution

If you'd like to contribute to the project, just fork it here on GitHub, do your work and send me a pull request. For ideas on what you could add, see the [ToDo section](#todo).

If you encounter any **problems or bugs**, please also *open a ticket* on the [Issue-Tracker](https://github.com/LukasKnuth/bike-track/issues).

## ToDo

* Add charts for all collected data (AchartEngine, JavaDoc for ChartFactory).
* Add measuring altitude (see http://stackoverflow.com/questions/6141390)
* Make the TrackerService bind-able and use the messages to get the GPS-state.
* Change Font to Roboto (http://developer.android.com/design/style/typography.html)
* Add Photo-taking capabilities (Tour pictures, with GEO-Tags, etc)
* New Tour-Activity and Main-Activity Listview layout (with the images above and some data about the tour)
* Also remove the need to give a name/date and find out the start-location (City) and the current date
