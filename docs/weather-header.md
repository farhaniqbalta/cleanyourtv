# Weather on Home

The optional weather header shows the current temperature and the next four upcoming hours below
the clock. It is off in the launcher defaults and enabled by the `minimalism_han` theme. It uses
the launcher's embedded weather code; installing the separate Weather add-on is not required.
The current line puts temperature and condition before the short city name. The forecast uses a
compact four-column strip, monochrome vector icons and the launcher's locale-aware font family.

Open **Launcher settings**, turn **Show weather on Home** on, and choose **Weather city**. Search
for a city and select a match. The header stays hidden until a city is selected and a response is
available. You can turn weather off in the same settings screen without changing your city.

The explicit on-TV choice takes precedence over `weather_enabled` in a theme and remains in place
when a theme is pushed or reset. Choose **Weather: follow theme** in Launcher settings to remove
that override and use the active theme's weather default. Ordinary theme reset does not remove it.

Weather uses Open-Meteo without an API key. The city search sends the search text; forecasts send
the selected coordinates. No weather location permission is requested. Requests run while Home is
visible, nominally every 30 minutes, or after five minutes following a failure. A recent cached
reading can appear offline for up to six hours; past hourly forecasts are never presented as
upcoming. The launcher now includes Android's Internet permission even when weather is off.

The code is adapted from `addons/weather/.../Forecast.java` for geocoding, HTTP timeouts and WMO
weather codes. Hourly selection, caching, display and lifecycle scheduling are specific to the
embedded header. The add-on remains a separate application. Weather text is provided in English
and Indonesian; other launcher languages currently fall back to English for these labels.

## Tested on

The owner installed the weather launcher and confirmed it works on a Skyworth SWTV-22AE-FHD,
Android 14, firmware UKNV.260211.001. The [TV screenshot](switches/weather_enabled.jpg) is
cropped to exclude the city. The launcher release build, lint and unit tests passed.
