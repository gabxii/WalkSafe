package com.android.walksafe;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean locationPermissionGranted;
    private GoogleMap gMap;
    private View mapCardView;
    private static final String TAG = "Info";
    private FusedLocationProviderClient fusedLocationProviderClient;
    private final LatLng defaultLocation = new LatLng(0, 0); // Default to the equator
    private Location lastKnownLocation;

    private PathDirections pathDirections;
    private List<LatLng> route;
    private List<Polyline> polylines = new ArrayList<>();
    private List<LatLng> polylinePoints; // Define polylinePoints here
    private PlacesClient placesClient;

    private MetricsActivity metricsActivity;

    private CrimeData crimeData;
    private CCTVData cctvData;
    private PoliceStationData policeStationData;
    private StreetlightData streetlightData;
    private SafetyIndex safetyIndex;

    private View bottomSheet;

    private LinearLayout overallsafetyLayout;
    private LinearLayout bottomSheetLayout;
    private LinearLayout routesContainer;
    private LinearLayout routesLayout;
    private ScrollView routesScrollView;
    private BottomSheetBehavior bottomSheetBehavior;

    private ProgressBar crimeProgressBar;
    private TextView crimeCountTextView;
    private ProgressBar cctvProgressBar;
    private TextView cctvCountTextView;
    private ProgressBar policeProgressBar;
    private TextView policeCountTextView;
    private ProgressBar streetlightProgressBar;
    private TextView streetlightTextView;
    private ProgressBar overallsafetyProgressBar;
    private TextView overallsafetyTextView;
    private TextView routeNameTextView;
    private TextView routeTimeTextView;
    private TextView routeDistanceTextView;
    private ListView listView;

    private List<List<LatLng>> decodedPolylines;

    private ImageView arrow;

    // Declare count variables
    private int crimeCount = 0;
    private int cctvCount = 0;
    private int policeCount = 0;
    private int streetlightCount = 0;
    private String routeName = "";
    private List<String> routeTimes;
    private List<String> routeDistances;
    private List<String> routeNames;
    private List<List<LatLng>> routes;
    private int clickedRouteIndex = -1;

    private LocationCallback locationCallback; // Declare here

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_activity);

        initializePlaces(); // Initialize Places SDK and Autocomplete Fragment

        // Initialize map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Initialize PlacesClient
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getString(R.string.api_key));
        }
        placesClient = Places.createClient(this);

        mapCardView = findViewById(R.id.mapCardView);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this); // Initialize fused location provider
        polylines = new ArrayList<>(); // Initialize polylines list

        //Initialize Safety Metric Classes
        cctvData = new CCTVData(this, gMap);
        crimeData = new CrimeData(this, gMap);
        policeStationData = new PoliceStationData(this, gMap);
        streetlightData = new StreetlightData(this, gMap);

        //Initialize Safety Index
        safetyIndex = new SafetyIndex(this, gMap);

        // Find views
        bottomSheet = findViewById(R.id.bottomSheetLinearLayout);
        bottomSheetLayout = findViewById(R.id.metricsLinearLayout);
        routesContainer = findViewById(R.id.routesContainer);
        routesScrollView = findViewById(R.id.routesScrollView);
        routesLayout = findViewById(R.id.routes_Layout);
        overallsafetyLayout = findViewById(R.id.overallsafety_Layout);

        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        // Find views
        crimeProgressBar = findViewById(R.id.crimeProgressBar);
        crimeCountTextView = findViewById(R.id.crimeCountTextView);
        cctvProgressBar = findViewById(R.id.cctvProgressBar);
        cctvCountTextView = findViewById(R.id.cctvCountTextView);
        policeProgressBar = findViewById(R.id.policeProgressBar);
        policeCountTextView = findViewById(R.id.policeCountTextView);
        streetlightProgressBar = findViewById(R.id.streetlightProgressBar);
        streetlightTextView = findViewById(R.id.streetlightCountTextView);
        overallsafetyProgressBar = routesContainer.findViewById(R.id.overallsafetyProgressBar);
        overallsafetyTextView = routesContainer.findViewById(R.id.overallsafetyCountTextView);
        routeNameTextView = bottomSheet.findViewById(R.id.routeNameTextView);
        routeTimeTextView = bottomSheet.findViewById(R.id.routeTimeTextView);
        routeDistanceTextView = bottomSheet.findViewById(R.id.routeDistanceTextView);
        arrow = routesContainer.findViewById(R.id.arrow);

        ArrayList<LatLng> polylinePoints = new ArrayList<>();


        // Real-time Navigation
        Button startNavigationButton = findViewById(R.id.startNavigationButton);
        startNavigationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (clickedRouteIndex != -1) {
                    List<LatLng> route = routes.get(clickedRouteIndex);
                    if (route != null && !route.isEmpty()) {
                        // Pass the route to start real-time street view navigation
                        startRealTimeStreetViewNavigation(route);
                    } else {
                        Toast.makeText(MapActivity.this, "Route is not valid", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MapActivity.this, "Please select a route first", Toast.LENGTH_SHORT).show();
                }
            }
        });





        // Set bottom sheet callback
        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    moveBottomSheetMapUp();
                } else if (newState == BottomSheetBehavior.STATE_HIDDEN || newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    moveBottomSheetMapDown();
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                // Optionally, handle slide offset if needed
            }
        });
    }



    private void moveBottomSheetMapUp() {
        if (bottomSheet.getHeight() > 0) {
            CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) mapCardView.getLayoutParams();
            params.bottomMargin = bottomSheet.getHeight();
            mapCardView.setLayoutParams(params);
        } else {
            bottomSheet.post(new Runnable() {
                @Override
                public void run() {
                    moveBottomSheetMapUp(); // Retry after bottomSheet has been laid out
                }
            });
        }
    }

    private void moveBottomSheetMapDown() {
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) mapCardView.getLayoutParams();
        params.bottomMargin = 0;
        mapCardView.setLayoutParams(params);
    }



    private void initializePlaces() {
        String apiKey = getString(R.string.api_key);
        Places.initialize(getApplicationContext(), apiKey); // Initialize Places SDK
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment) // Initialize the AutocompleteSupportFragment.
                getSupportFragmentManager().findFragmentById(R.id.autoComplete);
        autocompleteFragment.setCountries("PH"); // Set countries to restrict autocomplete suggestions
        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG)); // Specify the types of place data to return
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                LatLng userDestination = place.getLatLng();
                clearMap(); // Clear previous markers and polylines
                PathDirections pathDirections = new PathDirections(MapActivity.this, metricsActivity, cctvData, crimeData, policeStationData, streetlightData); // Create an instance of DirectionsTask
                pathDirections.setGoogleMap(gMap);  // Set GoogleMap instance
                pathDirections.setDestination(userDestination, place.getName().toString()); // Pass destination name and coordinates
                pathDirections.execute(lastKnownLocation); // Execute task with current location
                requestDirections(lastKnownLocation, userDestination, place.getName().toString()); // Call requestDirections with current location and selected destination
                moveBottomSheetMapUp();
            }

            @Override
            public void onError(Status status) {
                Log.i(TAG, "An error occurred: " + status);
                Toast.makeText(MapActivity.this, "An error occurred: " + status, Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void requestDirections(Location origin, LatLng destination, String destinationName) {
        PathDirections pathDirections = new PathDirections(MapActivity.this, metricsActivity, cctvData, crimeData, policeStationData, streetlightData);
        pathDirections.setGoogleMap(gMap);
        pathDirections.setDestination(destination, destinationName);
        pathDirections.setRequestAlternativeRoutes(true); // Request alternative routes
        pathDirections.execute(origin);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        gMap = googleMap;

        getLocationPermission();
        updateLocationUI();
        getCurrentLocation();

        //Initialize Safety Metric Classes
        cctvData = new CCTVData(this, gMap);
        crimeData = new CrimeData(this, gMap);
        policeStationData = new PoliceStationData(this, gMap);
        streetlightData = new StreetlightData(this, gMap);
    }



    private void startRealTimeStreetViewNavigation(List<LatLng> route) {
        if (route != null && !route.isEmpty()) {
            // Use the starting point of the route for Street View
            LatLng startPoint = route.get(0);
            String uri = String.format("google.streetview:cbll=%f,%f", startPoint.latitude, startPoint.longitude);
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            intent.setPackage("com.google.android.apps.maps");

            // Start the intent to launch Google Maps in Street View
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(this, "Google Maps is not installed", Toast.LENGTH_SHORT).show();
            }

            // Also, show the route with directions on the map
            // Call this after starting Street View to ensure the map is ready
            showDirectionsOnMap(route);
        } else {
            Toast.makeText(this, "Invalid route for Street View", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDirectionsOnMap(List<LatLng> route) {
        String url = getDirectionsUrl(route);
        new FetchUrl().execute(url);
    }

    private String getDirectionsUrl(List<LatLng> route) {
        StringBuilder urlString = new StringBuilder("https://maps.googleapis.com/maps/api/directions/json?");
        urlString.append("origin=").append(route.get(0).latitude).append(",").append(route.get(0).longitude);
        urlString.append("&destination=").append(route.get(route.size() - 1).latitude).append(",").append(route.get(route.size() - 1).longitude);
        urlString.append("&waypoints=");
        for (int i = 1; i < route.size() - 1; i++) {
            urlString.append(route.get(i).latitude).append(",").append(route.get(i).longitude);
            if (i < route.size() - 2) {
                urlString.append("|");
            }
        }
        urlString.append("&key=").append(getString(R.string.api_key)); // Your API key
        return urlString.toString();
    }

    private class FetchUrl extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... url) {
            // Fetch data from URL
            String data = "";
            try {
                URL urlObj = new URL(url[0]);
                HttpURLConnection urlConnection = (HttpURLConnection) urlObj.openConnection();
                InputStream inputStream = urlConnection.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }
                bufferedReader.close();
                data = stringBuilder.toString();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            // Parse the result and display directions
            parseDirections(result);
        }
    }

    private void parseDirections(String jsonData) {
        // Parse the JSON response and draw the route on the map
        try {
            JSONObject jsonObject = new JSONObject(jsonData);
            JSONArray routes = jsonObject.getJSONArray("routes");
            if (routes.length() > 0) {
                JSONObject route = routes.getJSONObject(0);
                JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
                String points = overviewPolyline.getString("points");
                List<LatLng> decodedPath = decodePoly(points);
                gMap.addPolyline(new PolylineOptions().addAll(decodedPath).width(12).color(Color.BLUE));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<LatLng> decodePoly(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;
            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;
            LatLng p = new LatLng((((lat * 1E-5))), (((lng * 1E-5))));
            poly.add(p);
        }
        return poly;
    }



    // Show obtained Route
    public void onRouteObtained(List<List<LatLng>> routes, List<String> routeNames, List<String> routeTimes, List<String> routeDistances) {
        this.routes = routes; // Assign routes to class-level variable
        this.routeNames = routeNames; // Assign routeNames to class-level variable
        this.routeTimes = routeTimes; // Assign routeTimes to class-level variable
        this.routeDistances = routeDistances; // Assign routeDistances to class-level variable

        if (gMap != null && !routes.isEmpty() && routes.size() == routeNames.size()
                && routes.size() == routeTimes.size() && routes.size() == routeDistances.size()) {
            // Clear any existing polylines and metrics data
            clearMap();
            polylines.clear();

            // Clear any existing views in the container
            routesContainer.removeAllViews();

            // Iterate through each route
            for (int i = 0; i < routes.size(); i++) {
                List<LatLng> route = routes.get(i);

                // Add polyline for each route segment (assuming gMap is set correctly)
                Polyline polyline = gMap.addPolyline(new PolylineOptions()
                        .addAll(route)
                        .width(12)
                        .color(Color.GRAY) // Adjust colors as necessary
                        .clickable(true));
                polyline.setTag("Route " + (i + 1));
                polylines.add(polyline); // Add polyline to list

                // Inflate a new view for this route
                View routeView = getLayoutInflater().inflate(R.layout.routes_layout, routesContainer, false);

                // Set route details in the new view
                TextView routeNameTextView = routeView.findViewById(R.id.routeNameTextView);
                TextView routeTimeTextView = routeView.findViewById(R.id.routeTimeTextView);
                TextView routeDistanceTextView = routeView.findViewById(R.id.routeDistanceTextView);

                routeNameTextView.setText(routeNames.get(i));
                routeTimeTextView.setText(routeTimes.get(i));
                routeDistanceTextView.setText(routeDistances.get(i));

                // Find views in the inflated layout for this route
                ProgressBar progressBar = routeView.findViewById(R.id.overallsafetyProgressBar);
                ImageView arrow = routeView.findViewById(R.id.arrow);

                // Set tags for identification
                progressBar.setTag(i);
                arrow.setTag(i);

                // Set click listeners for progress bar and arrow
                progressBar.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int routeIndex = (int) v.getTag();
                        Log.d(TAG, "Progress bar clicked for route index: " + routeIndex);
                        onButtonClicked(routeIndex);
                    }
                });

                arrow.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int routeIndex = (int) v.getTag();
                        Log.d(TAG, "Arrow clicked for route index: " + routeIndex);
                        onButtonClicked(routeIndex);
                    }
                });

                // Set click listener for the entire route view container
                routeView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int routeIndex = (int) progressBar.getTag(); // Get route index from progress bar tag
                        Log.d(TAG, "Route container clicked for route index: " + routeIndex);
                        onRouteContainerClicked(routeIndex);
                    }
                });

                // Add the new route view to the container
                routesContainer.addView(routeView);

                // Calculate safety index asynchronously and update progress bar
                calculateSafetyIndex(route, i);
            }

            // Show the bottom sheet and update UI
            showBottomSheet(true);
        } else {
            // Handle case where route or route details are null or empty
            Log.e(TAG, "Route or route details are null or empty");
            Toast.makeText(this, "Failed to obtain route or route details", Toast.LENGTH_SHORT).show();
        }
    }



    // Method to handle route container click
    private void onRouteContainerClicked(int routeIndex) {
        // Change background color of clicked route container
        if (clickedRouteIndex != -1) {
            View previousClickedView = routesContainer.getChildAt(clickedRouteIndex);
            previousClickedView.setBackgroundColor(Color.TRANSPARENT); // Reset previous clicked view color

            // Reset previous polyline color and Z-index
            if (clickedRouteIndex < polylines.size()) {
                Polyline previousPolyline = polylines.get(clickedRouteIndex);
                previousPolyline.setColor(Color.GRAY); // Reset to default color
                previousPolyline.setZIndex(0); // Reset Z-index
            }
        }

        View clickedView = routesContainer.getChildAt(routeIndex);
        clickedView.setBackgroundColor(ContextCompat.getColor(this, R.color.secondary100));
        clickedRouteIndex = routeIndex; // Update clicked route index

        // Change polyline color and Z-index for the clicked route
        if (routeIndex < polylines.size()) {
            Polyline clickedPolyline = polylines.get(routeIndex);
            clickedPolyline.setColor(ContextCompat.getColor(this, R.color.polylineColor));
            clickedPolyline.setZIndex(1); // Bring to front by setting a higher Z-index

            // Calculate the bounds of the clicked polyline
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (LatLng point : clickedPolyline.getPoints()) {
                builder.include(point);
            }
            LatLngBounds bounds = builder.build();

            // Move the camera to the bounds of the polyline
            int padding = 30; // Adjust padding as needed
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding);
            gMap.animateCamera(cameraUpdate);
        }

        // Enable the start navigation button
        Button startNavigationButton = findViewById(R.id.startNavigationButton);
        startNavigationButton.setEnabled(true); // Enable the button
    }



    // Method to handle progress bar or arrow click
    private void onButtonClicked(int routeIndex) {

        // Fetch data based on the clicked route index
        String routeName = routeNames.get(routeIndex);
        int crimeCount = fetchCrimeData(routes.get(routeIndex), routeIndex);
        int cctvCount = fetchCCTVData(routes.get(routeIndex), routeIndex);
        int policeCount = fetchPoliceData(routes.get(routeIndex), routeIndex);
        int streetlightCount = fetchStreetlightData(routes.get(routeIndex), routeIndex);

        SafetyIndex safetyIndex = new SafetyIndex(crimeCount, cctvCount, policeCount, streetlightCount);
        int safetyIndexValue = calculateSafetyIndex(routes.get(routeIndex), routeIndex); // Ensure correct index

        // Navigate to MetricsActivity with the fetched data
        navigateToMetrics(routeIndex, routeName, crimeCount, cctvCount, policeCount, streetlightCount, safetyIndexValue);
    }


    // Method to navigate to metrics activity with selected route index and data
    private void navigateToMetrics(int routeIndex, String routeName, int crimeCount, int cctvCount, int policeCount, int streetlightCount, double safetyIndexValue){
        // Implement navigation to MetricsActivity passing routeIndex and data to show relevant metrics
        Intent intent = new Intent(this, MetricsActivity.class);
        intent.putExtra("routeName", routeName);
        intent.putExtra("crimeCount", crimeCount);
        intent.putExtra("cctvCount", cctvCount);
        intent.putExtra("policeCount", policeCount);
        intent.putExtra("streetlightCount", streetlightCount);
        intent.putExtra("safetyIndex", safetyIndexValue);

        // Assuming you calculate progress here based on safetyIndexValue or another logic
        int progress = (int) Math.round(safetyIndexValue); // Example calculation
        intent.putExtra("progress", progress);
        startActivity(intent);
    }


    private int calculateSafetyIndex(List<LatLng> route, int routeIndex) {
        // Fetch safety metrics asynchronously for the given route
        safetyIndex.fetchSafetyMetrics(route, new SafetyIndex.SafetyIndexCallback() {
            @Override
            public void onSafetyIndexCalculated(double safetyIndex) {
                // Update UI with safety index for this route
                ProgressBar overallsafetyProgressBar = routesContainer.getChildAt(routeIndex).findViewById(R.id.overallsafetyProgressBar);
                TextView overallsafetyCountTextView = routesContainer.getChildAt(routeIndex).findViewById(R.id.overallsafetyCountTextView);
                overallsafetyCountTextView.setText(String.valueOf(safetyIndex));
                int progress = (int) Math.round(safetyIndex);
                overallsafetyProgressBar.setProgress(progress);

                // Set custom progress drawable based on safety index
                if (progress > 91) {
                    setProgressBarColor(overallsafetyProgressBar, R.color.safeColor90);
                } else if (progress < 90 || progress > 81) {
                    setProgressBarColor(overallsafetyProgressBar, R.color.safeColor80);
                } else if (progress < 80 || progress > 71) {
                    setProgressBarColor(overallsafetyProgressBar, R.color.safeColor70);
                } else if (progress < 70 || progress > 61) {
                    setProgressBarColor(overallsafetyProgressBar, R.color.safeColor60);
                } else if (progress < 60 || progress > 51) {
                    setProgressBarColor(overallsafetyProgressBar, R.color.mediumColor50);
                } else if (progress < 50 || progress > 41) {
                    setProgressBarColor(overallsafetyProgressBar, R.color.mediumColor40);
                } else if (progress < 40 || progress > 31) {
                    setProgressBarColor(overallsafetyProgressBar, R.color.mediumColor30);
                } else if (progress < 30 || progress > 21) {
                    setProgressBarColor(overallsafetyProgressBar, R.color.dangerColor20);
                } else if (progress < 20 || progress > 11) {
                    setProgressBarColor(overallsafetyProgressBar, R.color.dangerColor10);
                } else {
                    setProgressBarColor(overallsafetyProgressBar, R.color.dangerColor0);
                }
            }
        });
        return routeIndex;
    }


    private int fetchCrimeData(List<LatLng> route, int i) {

        crimeData.fetchCrimeData(route, new CrimeData.CrimeDataCallback() {
            @Override
            public void onCrimeDataReceived( int count) {
                Log.d(TAG, "CCTV data received: " + count);
                crimeCount = count; // Update cctvCount variable
                updateMetricsActivityCrimeCount(count);
            }
        });
        return crimeCount;
    }


    private int fetchCCTVData(List<LatLng> route, int i) {
        cctvData.fetchCCTVData(route, new CCTVData.CCTVDataCallback() {
            @Override
            public void onCCTVDataReceived( int count) {
                Log.d(TAG, "CCTV data received: " + count);
                cctvCount = count; // Update cctvCount variable
                updateMetricsActivityCCTVCount( count);
            }
        });
        return cctvCount;
    }

    private int fetchPoliceData(List<LatLng> route, int i) {
        policeStationData.fetchPoliceStationData(route, new PoliceStationData.PoliceStationDataCallback() {
            @Override
            public void onPoliceStationDataReceived( int count) {
                Log.d(TAG, "Police station data received: " + count);
                policeCount = count; // Update policeCount variable
                updateMetricsActivityPoliceCount( count);
            }
        });
        return policeCount;
    }

    private int fetchStreetlightData(List<LatLng> route, int i) {
        streetlightData.fetchStreetlightData(route, new StreetlightData.StreetlightDataCallback() {
            @Override
            public void onStreetlightDataReceived(int count) {
                Log.d(TAG, "Streetlight data received: " + count);
                streetlightCount = count; // Update streetlightCount variable
                updateMetricsActivityStreetlightCount( count);
            }
        });
        return streetlightCount;
    }



    private void updateMetricsActivityCrimeCount(int count) {
        if (metricsActivity != null) {
            metricsActivity.updateCrimeCount(count);
        }
    }

    private void updateMetricsActivityCCTVCount( int count) {
        if (metricsActivity != null) {
            metricsActivity.updateCCTVCount(count);
        }
    }

    private void updateMetricsActivityStreetlightCount(int count) {
        if (metricsActivity != null) {
            metricsActivity.updateStreetlightCount(count);
        }
    }

    private void updateMetricsActivityPoliceCount(int count) {
        if (metricsActivity != null) {
            metricsActivity.updatePoliceCount(count);
        }
    }


    // Helper method to set progress bar color dynamically
    private void setProgressBarColor(ProgressBar progressBar, int colorRes) {
        int color = ContextCompat.getColor(this, colorRes);
        progressBar.setProgressTintList(ColorStateList.valueOf(color));
    }



    // Helper method to expand BottomSheet
    private void expandBottomSheet() {
        BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }



    // Helper method to show/hide bottom sheet
    public void showBottomSheet(boolean show) {
        if (show) {
            bottomSheet.setVisibility(View.VISIBLE);
            expandBottomSheet();

            // Check if templateLayout is not already added
            if (overallsafetyLayout.getParent() == null) {
                bottomSheetLayout.addView(overallsafetyLayout); // Add the template layout to the bottom sheet layout
            }
        } else {
            bottomSheet.setVisibility(View.GONE);
            bottomSheetLayout.removeView(overallsafetyLayout); // Remove the template layout if it exists
        }
    }


    // Helper method to zoom to clicked polyline
    private void zoomToPolyline(Polyline polyline) {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        List<LatLng> points = polyline.getPoints();
        for (LatLng point : points) {
            builder.include(point);
        }
        LatLngBounds bounds = builder.build();
        gMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
    }

    public void clearMap() {
        if (gMap != null) {
            gMap.clear();
            bottomSheet.setVisibility(View.GONE);
            bottomSheetLayout.removeAllViews();
        }
    }

    // Request User Location
    private void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    // Button for Current Location
    private void updateLocationUI() {
        if (gMap == null) {
            return;
        }
        try {
            if (locationPermissionGranted) {
                gMap.setMyLocationEnabled(true);
                gMap.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                gMap.setMyLocationEnabled(false);
                gMap.getUiSettings().setMyLocationButtonEnabled(false);
                lastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }


    // Zooms in for Current User Location
    private void getCurrentLocation() {
        try {
            if (locationPermissionGranted) {
                Task<Location> locationResult = fusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            lastKnownLocation = task.getResult();
                            gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(lastKnownLocation.getLatitude(),
                                            lastKnownLocation.getLongitude()), 15));
                        } else {
                            Log.d(TAG, "Current location is null. Using defaults.");
                            Log.e(TAG, "Exception: %s", task.getException());
                            gMap.moveCamera(CameraUpdateFactory
                                    .newLatLngZoom(defaultLocation, 15));
                            gMap.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage(), e);
        }
    }


    // User Location Approval
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        locationPermissionGranted = false;
        if (requestCode == PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationPermissionGranted = true;
            }
        }
        updateLocationUI();
        getCurrentLocation();
    }


}