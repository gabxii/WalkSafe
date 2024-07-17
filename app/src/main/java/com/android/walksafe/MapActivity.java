package com.android.walksafe;

import static java.security.AccessController.getContext;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
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
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.security.AccessController;
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


        // Example of setting click listener on a button
        Button startNavigationButton = findViewById(R.id.startNavigationButton);
        if (startNavigationButton != null) {
            startNavigationButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Handle button click event
                }
            });
        } else {
            Log.e(TAG, "startNavigationButton is null. Check your layout or ID.");
        }


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
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) findViewById(R.id.mapCardView).getLayoutParams();
        params.bottomMargin = 0;
        findViewById(R.id.mapCardView).setLayoutParams(params);

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


    // Show obtained Route
    // Define class-level fields


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

        // Optionally, perform other actions like navigating to another screen
    }




    // Method to handle progress bar or arrow click
    private void onButtonClicked(int routeIndex) {
        // Fetch data based on the clicked route index
        // Example: Fetch crime, CCTV, police, streetlight data for the selected routeIndex
        String routeName = routeNames.get(routeIndex);
        int crimeCount = fetchCrimeData(routes.get(routeIndex), routeIndex);
        int cctvCount = fetchCCTVData(routes.get(routeIndex), routeIndex);
        int policeCount = fetchPoliceData(routes.get(routeIndex), routeIndex);
        int streetlightCount = fetchStreetlightData(routes.get(routeIndex), routeIndex);

        // Navigate to MetricsActivity with the fetched data
        navigateToMetrics(routeIndex, routeName, crimeCount, cctvCount, policeCount, streetlightCount);
    }


    // Method to navigate to metrics activity with selected route index and data
    private void navigateToMetrics(int routeIndex, String routeName, int crimeCount, int cctvCount, int policeCount, int streetlightCount) {
        // Implement navigation to MetricsActivity passing routeIndex and data to show relevant metrics
        Intent intent = new Intent(this, MetricsActivity.class);
        intent.putExtra("routeName", routeName);
        intent.putExtra("crimeCount", crimeCount);
        intent.putExtra("cctvCount", cctvCount);
        intent.putExtra("policeCount", policeCount);
        intent.putExtra("streetlightCount", streetlightCount);
        startActivity(intent);
    }


    private void calculateSafetyIndex(List<LatLng> route, int routeIndex) {
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
                if (progress < 10) {
                    setProgressBarColor(overallsafetyProgressBar, R.color.dangerColor);
                } else if (progress < 15) {
                    setProgressBarColor(overallsafetyProgressBar, R.color.mediumColor);
                } else {
                    setProgressBarColor(overallsafetyProgressBar, R.color.safeColor);
                }
            }
        });
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


    public void updateBottomSheetOverallCount(double count) {
        TextView overallsafetyCountTextView = routesLayout.findViewById(R.id.overallsafetyCountTextView);
        Log.d(TAG, "CCTV Count: " + count); // Log count for debugging
        overallsafetyCountTextView.setText(String.valueOf(count)); // Convert count to String

        // Adjust color based on count
        if (count < 10) {
            setProgressBarColor(overallsafetyProgressBar, R.color.dangerColor);
        } else if (count < 15) {
            setProgressBarColor(overallsafetyProgressBar, R.color.mediumColor);
        } else {
            setProgressBarColor(overallsafetyProgressBar, R.color.safeColor);
        }

        // Update progress bar value
        overallsafetyProgressBar.setProgress((int) count);
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
        if (ActivityCompat.checkSelfPermission(this.getApplicationContext(),
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
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    // Zooms in for Current User Location
    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Task<Location> task = fusedLocationProviderClient.getLastLocation();

        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    lastKnownLocation = location; // Store the user's last known location
                    LatLng latlng = new LatLng(location.getLatitude(), location.getLongitude());

                    Log.d("CurrentLocation", "Latitude: " + location.getLatitude() + ", Longitude: " + location.getLongitude());
                    MarkerOptions markerOptions = new MarkerOptions()
                            .position(latlng)
                            .icon(BitmapDescriptorFactory.defaultMarker(210))
                            .title("Current Location");

                    gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latlng, 20));
                    gMap.addMarker(markerOptions).showInfoWindow();
                }
            }
        });
    }

    // User Location Approval
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        locationPermissionGranted = false;
        if (requestCode == PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationPermissionGranted = true;
            }
        }
        updateLocationUI();
    }


}