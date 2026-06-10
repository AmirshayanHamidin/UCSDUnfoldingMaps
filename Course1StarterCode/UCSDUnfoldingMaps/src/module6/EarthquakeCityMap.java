package module6;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.data.Feature;
import de.fhpotsdam.unfolding.data.GeoJSONReader;
import de.fhpotsdam.unfolding.data.PointFeature;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.marker.AbstractShapeMarker;
import de.fhpotsdam.unfolding.marker.Marker;
import de.fhpotsdam.unfolding.marker.MultiMarker;
import de.fhpotsdam.unfolding.providers.Google;
import de.fhpotsdam.unfolding.providers.MBTilesMapProvider;
import de.fhpotsdam.unfolding.utils.MapUtils;
import parsing.ParseFeed;
import processing.core.PApplet;

/** EarthquakeCityMap
 * An application with an interactive map displaying earthquake data.
 * Author: UC San Diego Intermediate Software Development MOOC team
 * @author Your name here
 * Date: July 17, 2015
 * */
public class EarthquakeCityMap extends PApplet {
	
	// We will use member variables, instead of local variables, to store the data
	// that the setUp and draw methods will need to access (as well as other methods)
	// You will use many of these variables, but the only one you should need to add
	// code to modify is countryQuakes, where you will store the number of earthquakes
	// per country.
	
	// You can ignore this.  It's to get rid of eclipse warnings
	private static final long serialVersionUID = 1L;

	// How many of the strongest earthquakes to print to the console at startup
	private static final int NUM_TO_PRINT = 20;

	// IF YOU ARE WORKING OFFILINE, change the value of this variable to true
	private static final boolean offline = false;
	
	/** This is where to find the local tiles, for working without an Internet connection */
	public static String mbTilesString = "blankLight-1-3.mbtiles";
	
	

	//feed with magnitude 2.5+ Earthquakes
	private String earthquakesURL = "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/2.5_week.atom";
	
	// The files containing city names and info and country names and info
	private String cityFile = "city-data.json";
	private String countryFile = "countries.geo.json";
	
	// The map
	private UnfoldingMap map;
	
	// Markers for each city
	private List<Marker> cityMarkers;
	// Markers for each earthquake
	private List<Marker> quakeMarkers;

	// A List of country markers
	private List<Marker> countryMarkers;
	
	// NEW IN MODULE 5
	private CommonMarker lastSelected;
	private CommonMarker lastClicked;
	
	public void setup() {		
		// (1) Initializing canvas and map tiles
		// NOTE: OPENGL renderer needs JOGL natives that no longer work on modern Java.
		// Using the default Java2D renderer instead (no native libraries required).
		size(900, 700);
		if (offline) {
		    map = new UnfoldingMap(this, 200, 50, 650, 600, new MBTilesMapProvider(mbTilesString));
		    earthquakesURL = "2.5_week.atom";  // The same feed, but saved August 7, 2015
		}
		else {
			map = new UnfoldingMap(this, 200, 50, 650, 600, new Google.GoogleMapProvider());
			// IF YOU WANT TO TEST WITH A LOCAL FILE, uncomment the next line
		    //earthquakesURL = "2.5_week.atom";
		}
		MapUtils.createDefaultEventDispatcher(this, map);
		
		// FOR TESTING: Set earthquakesURL to be one of the testing files by uncommenting
		// one of the lines below.  This will work whether you are online or offline
		//earthquakesURL = "test1.atom";
		//earthquakesURL = "test2.atom";
		
		// Uncomment this line to take the quiz
		//earthquakesURL = "quiz2.atom";
		
		
		// (2) Reading in earthquake data and geometric properties
	    //     STEP 1: load country features and markers
		List<Feature> countries = GeoJSONReader.loadData(this, countryFile);
		countryMarkers = MapUtils.createSimpleMarkers(countries);
		
		//     STEP 2: read in city data
		List<Feature> cities = GeoJSONReader.loadData(this, cityFile);
		cityMarkers = new ArrayList<Marker>();
		for(Feature city : cities) {
		  cityMarkers.add(new CityMarker(city));
		}
	    
		//     STEP 3: read in earthquake RSS feed
	    List<PointFeature> earthquakes = ParseFeed.parseEarthquake(this, earthquakesURL);
	    quakeMarkers = new ArrayList<Marker>();
	    
	    for(PointFeature feature : earthquakes) {
		  //check if LandQuake
		  if(isLand(feature)) {
		    quakeMarkers.add(new LandQuakeMarker(feature));
		  }
		  // OceanQuakes
		  else {
		    quakeMarkers.add(new OceanQuakeMarker(feature));
		  }
	    }

	    // could be used for debugging
	    printQuakes();

	    // Sort the earthquakes (strongest first) and print the top ones.
	    sortAndPrint(NUM_TO_PRINT);
	 		
	    // (3) Add markers to map
	    //     NOTE: Country markers are not added to the map.  They are used
	    //           for their geometric properties
	    map.addMarkers(quakeMarkers);
	    map.addMarkers(cityMarkers);
	    
	    
	}  // End setup
	
	
	public void draw() {
		background(0);
		map.draw();
		addKey();
		
	}

	/** Launch the Processing sketch (opens the map window). */
	public static void main(String[] args) {
		PApplet.main(new String[] {"module6.EarthquakeCityMap"});
	}
	
	
	/** Sort the earthquake markers in descending order of magnitude and
	 * print out the titles of the top numToPrint of them.
	 *
	 * Copies the markers out of quakeMarkers with toArray(), sorts that array
	 * (which relies on EarthquakeMarker implementing Comparable, highest
	 * magnitude first), then prints up to numToPrint of them.  If numToPrint
	 * is larger than the number of earthquakes, it simply prints them all
	 * without crashing.
	 *
	 * @param numToPrint the maximum number of earthquakes to print
	 */
	private void sortAndPrint(int numToPrint)
	{
		// Copy the markers into an array we can sort.
		EarthquakeMarker[] markers = quakeMarkers.toArray(new EarthquakeMarker[0]);

		// Sort highest magnitude first (see EarthquakeMarker.compareTo).
		Arrays.sort(markers);

		// Don't run off the end of the array if numToPrint is too big.
		int limit = Math.min(numToPrint, markers.length);
		for (int i = 0; i < limit; i++) {
			System.out.println(markers[i]);
		}
	}
	
	/** Event handler that gets called automatically when the 
	 * mouse moves.
	 */
	@Override
	public void mouseMoved()
	{
		// clear the last selection
		if (lastSelected != null) {
			lastSelected.setSelected(false);
			lastSelected = null;
		
		}
		selectMarkerIfHover(quakeMarkers);
		selectMarkerIfHover(cityMarkers);
		//loop();
	}
	
	// If there is a marker selected 
	private void selectMarkerIfHover(List<Marker> markers)
	{
		// Abort if there's already a marker selected
		if (lastSelected != null) {
			return;
		}
		
		for (Marker m : markers) 
		{
			CommonMarker marker = (CommonMarker)m;
			if (marker.isInside(map,  mouseX, mouseY)) {
				lastSelected = marker;
				marker.setSelected(true);
				return;
			}
		}
	}
	
	/** The event handler for mouse clicks
	 * It will display an earthquake and its threat circle of cities
	 * Or if a city is clicked, it will display all the earthquakes 
	 * where the city is in the threat circle
	 */
	@Override
	public void mouseClicked()
	{
		if (lastClicked != null) {
			unhideMarkers();
			lastClicked = null;
		}
		else if (lastClicked == null) 
		{
			checkEarthquakesForClick();
			if (lastClicked == null) {
				checkCitiesForClick();
			}
		}
	}
	
	// Helper method that will check if a city marker was clicked on
	// and respond appropriately
	private void checkCitiesForClick()
	{
		if (lastClicked != null) return;
		// Loop over the earthquake markers to see if one of them is selected
		for (Marker marker : cityMarkers) {
			if (!marker.isHidden() && marker.isInside(map, mouseX, mouseY)) {
				lastClicked = (CommonMarker)marker;
				// Hide all the other earthquakes and hide
				for (Marker mhide : cityMarkers) {
					if (mhide != lastClicked) {
						mhide.setHidden(true);
					}
				}
				for (Marker mhide : quakeMarkers) {
					EarthquakeMarker quakeMarker = (EarthquakeMarker)mhide;
					if (quakeMarker.getDistanceTo(marker.getLocation()) 
							> quakeMarker.threatCircle()) {
						quakeMarker.setHidden(true);
					}
				}
				return;
			}
		}		
	}
	
	// Helper method that will check if an earthquake marker was clicked on
	// and respond appropriately
	private void checkEarthquakesForClick()
	{
		if (lastClicked != null) return;
		// Loop over the earthquake markers to see if one of them is selected
		for (Marker m : quakeMarkers) {
			EarthquakeMarker marker = (EarthquakeMarker)m;
			if (!marker.isHidden() && marker.isInside(map, mouseX, mouseY)) {
				lastClicked = marker;
				// Hide all the other earthquakes and hide
				for (Marker mhide : quakeMarkers) {
					if (mhide != lastClicked) {
						mhide.setHidden(true);
					}
				}
				for (Marker mhide : cityMarkers) {
					if (mhide.getDistanceTo(marker.getLocation()) 
							> marker.threatCircle()) {
						mhide.setHidden(true);
					}
				}
				return;
			}
		}
	}
	
	// loop over and unhide all markers
	private void unhideMarkers() {
		for(Marker marker : quakeMarkers) {
			marker.setHidden(false);
		}
			
		for(Marker marker : cityMarkers) {
			marker.setHidden(false);
		}
	}
	
	// helper method to draw key in GUI
	private void addKey() {
		// Remember you can use Processing's graphics methods here
		fill(255, 250, 240);

		int xbase = 25;
		int ybase = 50;

		// EXTENSION: the background rectangle is enlarged (taller) so that the
		// live statistics panel drawn below the legend never clips.
		rect(xbase, ybase, 160, 430);

		fill(0);
		textAlign(LEFT, CENTER);
		textSize(12);
		text("Earthquake Key", xbase+25, ybase+25);

		fill(150, 30, 30);
		int tri_xbase = xbase + 35;
		int tri_ybase = ybase + 50;
		triangle(tri_xbase, tri_ybase-CityMarker.TRI_SIZE, tri_xbase-CityMarker.TRI_SIZE,
				tri_ybase+CityMarker.TRI_SIZE, tri_xbase+CityMarker.TRI_SIZE,
				tri_ybase+CityMarker.TRI_SIZE);

		fill(0, 0, 0);
		textAlign(LEFT, CENTER);
		text("City Marker", tri_xbase + 15, tri_ybase);

		text("Land Quake", xbase+50, ybase+70);
		text("Ocean Quake", xbase+50, ybase+90);
		text("Size ~ Magnitude", xbase+25, ybase+110);

		fill(255, 255, 255);
		ellipse(xbase+35,
				ybase+70,
				10,
				10);
		rect(xbase+35-5, ybase+90-5, 10, 10);

		fill(color(255, 255, 0));
		ellipse(xbase+35, ybase+140, 12, 12);
		fill(color(0, 0, 255));
		ellipse(xbase+35, ybase+160, 12, 12);
		fill(color(255, 0, 0));
		ellipse(xbase+35, ybase+180, 12, 12);

		textAlign(LEFT, CENTER);
		fill(0, 0, 0);
		text("Shallow", xbase+50, ybase+140);
		text("Intermediate", xbase+50, ybase+160);
		text("Deep", xbase+50, ybase+180);

		text("Past hour", xbase+50, ybase+200);

		fill(255, 255, 255);
		int centerx = xbase+35;
		int centery = ybase+200;
		ellipse(centerx, centery, 12, 12);

		strokeWeight(2);
		line(centerx-8, centery-8, centerx+8, centery+8);
		line(centerx-8, centery+8, centerx+8, centery-8);

		// EXTENSION: live statistics panel.
		// Reset stroke so the text below is not affected by the strokeWeight(2)
		// used to draw the "Past hour" X above.
		strokeWeight(1);
		drawStatsPanel(xbase, ybase + 225);
	}

	/** EXTENSION helper: draw the live statistics panel inside the legend.
	 *
	 * When nothing is selected it shows global counts of earthquakes broken
	 * down by severity (major 6+, moderate, light, minor) plus a land/ocean
	 * tally.  When a city marker has been clicked it instead shows that city's
	 * threat report produced by {@link #cityStats(CityMarker)}.
	 *
	 * @param xbase left edge of the legend rectangle
	 * @param ytop  y coordinate at which to start drawing the panel
	 */
	private void drawStatsPanel(int xbase, int ytop)
	{
		fill(0);
		textAlign(LEFT, CENTER);

		// Divider line above the stats area.
		stroke(180);
		line(xbase + 10, ytop - 12, xbase + 150, ytop - 12);
		noStroke();

		String[] lines;
		String header;
		if (lastClicked != null && lastClicked instanceof CityMarker) {
			header = "City Threat Report";
			lines = cityStats((CityMarker) lastClicked);
		}
		else {
			header = "Live Stats (all quakes)";
			lines = globalStats();
		}

		textSize(11);
		fill(0);
		text(header, xbase + 10, ytop);

		textSize(10);
		int lineY = ytop + 18;
		for (String line : lines) {
			text(line, xbase + 12, lineY);
			lineY += 16;
		}
		// restore default-ish text size for anything drawn afterwards
		textSize(12);
	}

	/** Build the global earthquake statistics lines: counts by severity and
	 * a land vs. ocean tally over all earthquake markers.
	 *
	 * @return an array of display lines
	 */
	private String[] globalStats()
	{
		int major = 0, moderate = 0, light = 0, minor = 0;
		int land = 0, ocean = 0;

		if (quakeMarkers != null) {
			for (Marker m : quakeMarkers) {
				EarthquakeMarker quake = (EarthquakeMarker) m;
				float mag = quake.getMagnitude();
				if (mag >= 6.0f) {
					major++;
				}
				else if (mag >= EarthquakeMarker.THRESHOLD_MODERATE) {
					moderate++;
				}
				else if (mag >= EarthquakeMarker.THRESHOLD_LIGHT) {
					light++;
				}
				else {
					minor++;
				}

				if (quake.isOnLand()) {
					land++;
				}
				else {
					ocean++;
				}
			}
		}

		int total = major + moderate + light + minor;
		return new String[] {
				"Total quakes: " + total,
				"Major (6+):   " + major,
				"Moderate (5+):" + moderate,
				"Light (4+):   " + light,
				"Minor (<4):   " + minor,
				"Land: " + land + "   Ocean: " + ocean,
				"(click a city for detail)"
		};
	}

	/** EXTENSION helper: build a threat report for a single city.
	 *
	 * Counts the earthquakes whose threat circle reaches the city, computes
	 * their average magnitude, and identifies the strongest nearby quake
	 * (reporting its magnitude and age).
	 *
	 * @param city the clicked city marker
	 * @return an array of display lines describing the city's threat
	 */
	private String[] cityStats(CityMarker city)
	{
		int count = 0;
		float magSum = 0;
		EarthquakeMarker strongest = null;

		for (Marker m : quakeMarkers) {
			EarthquakeMarker quake = (EarthquakeMarker) m;
			// The city is threatened if it lies within this quake's threat circle.
			if (quake.getDistanceTo(city.getLocation()) <= quake.threatCircle()) {
				count++;
				magSum += quake.getMagnitude();
				if (strongest == null
						|| quake.getMagnitude() > strongest.getMagnitude()) {
					strongest = quake;
				}
			}
		}

		String cityName = city.getStringProperty("name");
		if (cityName == null) {
			cityName = "Selected city";
		}

		if (count == 0) {
			return new String[] {
					cityName,
					"No quakes in range",
					"(click again to reset)"
			};
		}

		float avg = magSum / count;
		String avgStr = String.format("%.1f", avg);
		String strongestStr = String.format("%.1f", strongest.getMagnitude());
		String age = strongest.getAge();
		if (age == null) {
			age = "unknown";
		}

		return new String[] {
				cityName,
				"Nearby quakes: " + count,
				"Avg magnitude: " + avgStr,
				"Strongest: M" + strongestStr,
				"  (" + age + ")",
				"(click again to reset)"
		};
	}

	
	
	// Checks whether this quake occurred on land.  If it did, it sets the 
	// "country" property of its PointFeature to the country where it occurred
	// and returns true.  Notice that the helper method isInCountry will
	// set this "country" property already.  Otherwise it returns false.
	private boolean isLand(PointFeature earthquake) {
		
		// IMPLEMENT THIS: loop over all countries to check if location is in any of them
		// If it is, add 1 to the entry in countryQuakes corresponding to this country.
		for (Marker country : countryMarkers) {
			if (isInCountry(earthquake, country)) {
				return true;
			}
		}
		
		// not inside any country
		return false;
	}
	
	// prints countries with number of earthquakes
	// You will want to loop through the country markers or country features
	// (either will work) and then for each country, loop through
	// the quakes to count how many occurred in that country.
	// Recall that the country markers have a "name" property, 
	// And LandQuakeMarkers have a "country" property set.
	private void printQuakes() {
		int totalWaterQuakes = quakeMarkers.size();
		for (Marker country : countryMarkers) {
			String countryName = country.getStringProperty("name");
			int numQuakes = 0;
			for (Marker marker : quakeMarkers)
			{
				EarthquakeMarker eqMarker = (EarthquakeMarker)marker;
				if (eqMarker.isOnLand()) {
					if (countryName.equals(eqMarker.getStringProperty("country"))) {
						numQuakes++;
					}
				}
			}
			if (numQuakes > 0) {
				totalWaterQuakes -= numQuakes;
				System.out.println(countryName + ": " + numQuakes);
			}
		}
		System.out.println("OCEAN QUAKES: " + totalWaterQuakes);
	}
	
	
	
	// helper method to test whether a given earthquake is in a given country
	// This will also add the country property to the properties of the earthquake feature if 
	// it's in one of the countries.
	// You should not have to modify this code
	private boolean isInCountry(PointFeature earthquake, Marker country) {
		// getting location of feature
		Location checkLoc = earthquake.getLocation();

		// some countries represented it as MultiMarker
		// looping over SimplePolygonMarkers which make them up to use isInsideByLoc
		if(country.getClass() == MultiMarker.class) {
				
			// looping over markers making up MultiMarker
			for(Marker marker : ((MultiMarker)country).getMarkers()) {
					
				// checking if inside
				if(((AbstractShapeMarker)marker).isInsideByLocation(checkLoc)) {
					earthquake.addProperty("country", country.getProperty("name"));
						
					// return if is inside one
					return true;
				}
			}
		}
			
		// check if inside country represented by SimplePolygonMarker
		else if(((AbstractShapeMarker)country).isInsideByLocation(checkLoc)) {
			earthquake.addProperty("country", country.getProperty("name"));
			
			return true;
		}
		return false;
	}

}
