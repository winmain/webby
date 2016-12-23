package ;
/**
	The Date class provides a basic structure for date and time related
	information. Date instances can be created by

	- `new Date()` for a specific date,
	- `Date.now()` to obtain information about the current time,
	- `Date.fromTime()` with a given timestamp or
	- `Date.fromString()` by parsing from a String.

	There is some extra functions available in the `DateTools` class.

	In the context of haxe dates, a timestamp is defined as the number of
	milliseconds elapsed since 1st January 1970.
**/
@:native("Date")
extern class Date
{
  /**
		Creates a new date object from the given arguments.

		The behaviour of a Date instance is only consistent across platforms if
		the the arguments describe a valid date.

		- month: 0 to 11
		- day: 1 to 31
		- hour: 0 to 23
		- min: 0 to 59
		- sec: 0 to 59
	**/
  @:overload(function(value: Float): Void { })
  @:overload(function(dateString: String): Void { })
  @:overload(function(year: Int, month: Int, ?day: Int, ?hour: Int, ?min: Int, ?sec: Int, ?ms: Int): Void { })
  function new( ): Void;

  /**
		Returns the timestamp of the date. It might only have a per-second
		precision depending on the platforms.
	**/
  function getTime(): Float;

  /**
		Returns the hours of `this` Date (0-23 range).
	**/
  function getHours(): Int;

  /**
		Returns the minutes of `this` Date (0-59 range).
	**/
  function getMinutes(): Int;

  /**
		Returns the seconds of the `this` Date (0-59 range).
	**/
  function getSeconds(): Int;

  /**
		Returns the full year of `this` Date (4-digits).
	**/
  function getFullYear(): Int;

  /**
		Returns the month of `this` Date (0-11 range).
	**/
  function getMonth(): Int;

  /**
		Returns the day of `this` Date (1-31 range).
	**/
  function getDate(): Int;

  /**
		Returns the day of the week of `this` Date (0-6 range).
	**/
  function getDay(): Int;

  /**
		Returns a string representation of `this` Date, by using the
		standard format [YYYY-MM-DD HH:MM:SS]. See `DateTools.format` for
		other formating rules.
	**/
  function toString(): String;

  function getUTCFullYear(): Int;
  function getUTCMonth(): Int;
  function getUTCDate(): Int;
  function getUTCHours(): Int;
  function getUTCMinutes(): Int;
  function getUTCSeconds(): Int;

  // ------------------------------------ Static methods ---------------------------------

  /*
  Returns the numeric value corresponding to the current time - the number of milliseconds elapsed since 1 January 1970 00:00:00 UTC.
  Date.parse()
  Parses a string representation of a date and returns the number of milliseconds since 1 January, 1970, 00:00:00, UTC.
   */
  static function now(): Int;
}
