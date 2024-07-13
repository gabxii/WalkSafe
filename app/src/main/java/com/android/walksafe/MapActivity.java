package com.android.walksafe;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
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


    private ProgressBar progressBar;
    private ImageView arrow;

    // Declare count variables
    private int crimeCount = 0;
    private int cctvCount = 0;
    private int policeCount = 0;
    private int streetlightCount = 0;



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

        // Initialize Safety Index
        safetyIndex = new SafetyIndex(this, gMap);

        // Find views
        bottomSheet = findViewById(R.id.bottomSheetLinearLayout);
        bottomSheetLayout = findViewById(R.id.metricsLinearLayout);

        FrameLayout progressBarButton = findViewById(R.id.progressBarButton);
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
        overallsafetyProgressBar = findViewById(R.id.overallsafetyProgressBar);
        overallsafetyTextView = findViewById(R.id.overallsafetyCountTextView);
        routeNameTextView = bottomSheet.findViewById(R.id.routeNameTextView);
        routeTimeTextView = bottomSheet.findViewById(R.id.routeTimeTextView);
        routeDistanceTextView = bottomSheet.findViewById(R.id.routeDistanceTextView);
        arrow = findViewById(R.id.arrow);

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

        // Set click listener for progress bar
        progressBarButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToMetrics();
            }
        });

        // Set click listener for arrow
        arrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToMetrics();
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


    private void navigateToMetrics() {
        Intent intent = new Intent(MapActivity.this, MetricsActivity.class);
        intent.putExtra("crimeCount", crimeCount); // Replace with your actual data
        intent.putExtra("cctvCount", cctvCount);
        intent.putExtra("policeCount", policeCount);
        intent.putExtra("streetlightCount", streetlightCount);
        startActivity(intent);


        // Example: Passing route data as an extra
        if (route != null && !route.isEmpty()) {
            intent.putParcelableArrayListExtra("route", (ArrayList<LatLng>) route);
        }

        startActivity(intent);
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
    public void onRouteObtained(List<List<LatLng>> routes, String routeName, String routeTime, String routeDistance) {
        if (gMap != null && !routes.isEmpty()) {
            // Clear any existing polylines and metrics data
            clearMap();

            // Iterate through each route
            for (int i = 0; i < routes.size(); i++) {
                List<LatLng> route = routes.get(i);

                // Add polyline for each route segment
                Polyline polyline = gMap.addPolyline(new PolylineOptions()
                        .addAll(route)
                        .width(12)
                        .color(Color.GRAY)
                        .clickable(true));
                polyline.setTag("Route " + (i + 1));
                polylines.add(polyline); // Add polyline to list

                // Fetch data for each route segment
                fetchCrimeData(route, i);
                fetchStreetlightData(route, i);
                fetchPoliceData(route, i);
                fetchCCTVData(route, i);

                // Update route details in the bottom sheet
                routeNameTextView.setText(routeName);
                routeTimeTextView.setText(routeTime);
                routeDistanceTextView.setText(routeDistance);
            }

            // Show the bottom sheet and update UI
            showBottomSheet(true);
        } else {
            // Handle case where route or route details are null or empty
            Log.e(TAG, "Route or route details are null or empty");
            Toast.makeText(this, "Failed to obtain route or route details", Toast.LENGTH_SHORT).show();
        }
    }









//    @Override
//    public void onPolylineClick(Polyline polyline) {
//        if (polyline != null && polyline.getTag() != null) {
//            polyline.setColor(Color.parseColor("#1A73E8"));
//            zoomToPolyline(polyline);
//            Toast.makeText(this, polyline.getTag().toString(), Toast.LENGTH_SHORT).show();
//
//            // Clear previous data and fetch new data for the clicked polyline
//            clearMap(); // Clear previous markers and polylines
//            clearMetrics(); // Clear previous metrics data
//
//            // Fetch data for the clicked polyline
//            fetchCrimeData(polyline.getPoints());
//            fetchStreetlightData(polyline.getPoints());
//            fetchPoliceData(polyline.getPoints());
//            fetchCCTVData(polyline.getPoints());
//
//            showBottomSheet(true);
//        } else {
//            showBottomSheet(false);
//        }
//    }

    private void fetchCrimeData(List<LatLng> route, int i) {
        crimeData.fetchCrimeData(route, new CrimeData.CrimeDataCallback() {
            @Override
            public void onCrimeDataReceived(int count) {
                Log.d(TAG, "Crime data received: " + count);
                crimeCount = count; // Update crimeCount variable
                updateMetricsActivityCrimeCount(count);
            }
        });
    }

    private void fetchCCTVData(List<LatLng> route, int i) {
        cctvData.fetchCCTVData(route, new CCTVData.CCTVDataCallback() {
            @Override
            public void onCCTVDataReceived(int count) {
                Log.d(TAG, "CCTV data received: " + count);
                cctvCount = count; // Update cctvCount variable
                updateMetricsActivityCCTVCount(count);
            }
        });
    }

    private void fetchPoliceData(List<LatLng> route, int i) {
        policeStationData.fetchPoliceStationData(route, new PoliceStationData.PoliceStationDataCallback() {
            @Override
            public void onPoliceStationDataReceived(int count) {
                Log.d(TAG, "Police station data received: " + count);
                policeCount = count; // Update policeCount variable
                updateMetricsActivityPoliceCount(count);
            }
        });
    }

    private void fetchStreetlightData(List<LatLng> route, int i) {
        streetlightData.fetchStreetlightData(route, new StreetlightData.StreetlightDataCallback() {
            @Override
            public void onStreetlightDataReceived(int count) {
                Log.d(TAG, "Streetlight data received: " + count);
                streetlightCount = count; // Update streetlightCount variable
                updateMetricsActivityStreetlightCount(count);
            }
        });
    }



    private void updateMetricsActivityCrimeCount(int count) {
        if (metricsActivity != null) {
            metricsActivity.updateCrimeCount(count);
        }
    }

    private void updateMetricsActivityCCTVCount(int count) {
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



    // Helper method to clear previous metrics data
    private void clearMetrics() {
        // Clear previous metric counts or update to zero
        metricsActivity.updateCrimeCount(0);
        metricsActivity.updateCCTVCount(0);
        metricsActivity.updatePoliceCount(0);
        metricsActivity.updateStreetlightCount(0);
    }

    public void updateBottomSheetOverallCount(int count) {
        TextView overallsafetyCountTextView = overallsafetyLayout.findViewById(R.id.overallsafetyCountTextView);
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
        overallsafetyProgressBar.setProgress(count);
    }

//    public void updateBottomSheetCCTVCount(int count) {
//        TextView cctvCountTextView = metricsLayout.findViewById(R.id.cctvCountTextView);
//        Log.d(TAG, "CCTV Count: " + count); // Log count for debugging
//        cctvCountTextView.setText(String.valueOf(count)); // Convert count to String
//
//        // Adjust color based on count
//        if (count < 10) {
//            setProgressBarColor(cctvProgressBar, R.color.dangerColor);
//        } else if (count < 15) {
//            setProgressBarColor(cctvProgressBar, R.color.mediumColor);
//        } else {
//            setProgressBarColor(cctvProgressBar, R.color.safeColor);
//        }
//
//        // Update progress bar value
//        cctvProgressBar.setProgress(count);
//    }
//
//    public void updateBottomSheetCrimeCount(int count) {
//        TextView crimeCountTextView = metricsLayout.findViewById(R.id.crimeCountTextView);
//        Log.d(TAG, "Crime Count: " + count); // Log count for debugging
//        crimeCountTextView.setText(String.valueOf(count)); // Convert count to String
//
//        // Adjust color based on count
//        if (count < 10) {
//            setProgressBarColor(crimeProgressBar, R.color.safeColor);
//        } else if (count < 20) {
//            setProgressBarColor(crimeProgressBar, R.color.mediumColor);
//        } else {
//            setProgressBarColor(crimeProgressBar, R.color.dangerColor);
//        }
//
//        // Update progress bar value
//        crimeProgressBar.setProgress(count);
//    }
//
//    public void updateBottomSheetPoliceStationCount(int count) {
//        TextView policeCountTextView = metricsLayout.findViewById(R.id.policeCountTextView);
//        Log.d(TAG, "Police Station Count: " + count); // Log count for debugging
//        policeCountTextView.setText(String.valueOf(count)); // Convert count to String
//
//        // Adjust color based on count
//        if (count < 10) {
//            setProgressBarColor(policeProgressBar, R.color.dangerColor);
//        } else if (count < 20) {
//            setProgressBarColor(policeProgressBar, R.color.safeColor);
//        } else {
//            setProgressBarColor(policeProgressBar, R.color.dangerColor);
//        }
//
//        // Update progress bar value
//        policeProgressBar.setProgress(count);
//    }
//
//    public void updateBottomSheetStreetlightCount(int count) {
//        TextView streetlightCountTextView = metricsLayout.findViewById(R.id.streetlightCountTextView);
//        Log.d(TAG, "Streetlight Count: " + count); // Log count for debugging
//        streetlightCountTextView.setText(String.valueOf(count)); // Convert count to String
//
//        // Adjust color based on count
//        if (count < 10) {
//            setProgressBarColor(streetlightProgressBar, R.color.dangerColor);
//        } else if (count < 20) {
//            setProgressBarColor(streetlightProgressBar, R.color.mediumColor);
//        } else {
//            setProgressBarColor(streetlightProgressBar, R.color.safeColor);
//        }
//
//        // Update progress bar value
//        streetlightProgressBar.setProgress(count);
//    }


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