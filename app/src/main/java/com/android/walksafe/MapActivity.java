package com.android.walksafe;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
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
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
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
    private Dialog loadingDialog;

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

        // Show the disclaimer dialog
        showDisclaimerDialog();

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



        // Set up the start navigation button click listener
        Button startNavigationButton = findViewById(R.id.startNavigationButton);
        startNavigationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (clickedRouteIndex != -1) {
                    List<LatLng> route = routes.get(clickedRouteIndex);
                    if (route != null && route.size() >= 2) {
                        // Use the first and last points of the route for turn-by-turn navigation
                        LatLng origin = route.get(0);
                        LatLng destination = route.get(route.size() - 1);

                        // Create a PathDirections instance and start turn-by-turn navigation
                        new PathDirections(MapActivity.this, metricsActivity, cctvData, crimeData, policeStationData, streetlightData)
                                .startTurnByTurnNavigation(origin, destination);
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

    private void showDisclaimerDialog() {
        DisclaimerDialogFragment disclaimerDialog = new DisclaimerDialogFragment();
        disclaimerDialog.setOnDismissListener(() -> {
            // Handle any actions after the dialog is dismissed
            // For example, initialize other components or proceed with the map setup
        });
        disclaimerDialog.show(getSupportFragmentManager(), "DisclaimerDialog");
    }

    private void showLoading() {
        if (loadingDialog == null) {
            loadingDialog = new Dialog(this);
            loadingDialog.setContentView(R.layout.loading_dialog);
            loadingDialog.setCancelable(false); // Prevent dismiss on touch outside
        }
        loadingDialog.show();
    }

    private void hideLoading() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
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



//    // This method should be called when you want to start navigation
//    private void startNavigation(LatLng destination) {
//        // Example origin; use actual user location
//        Location origin = new Location("");
//        origin.setLatitude(gMap.getCameraPosition().target.latitude);
//        origin.setLongitude(gMap.getCameraPosition().target.longitude);
//
//        pathDirections.setGoogleMap(gMap);
//        pathDirections.setDestination(destination, "Destination Name");
//        pathDirections.setRequestAlternativeRoutes(true); // Set as needed
//        pathDirections.execute(origin);
//    }


    private void updateInstructionsList(List<String> instructions) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, instructions);
        ListView listView = findViewById(R.id.instructions_list_view); // Ensure you have a ListView in your layout
        listView.setAdapter(adapter);
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
        // Show the loading indicator
        showLoading();
        policeStationData.clearPoliceStationData();

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

        // Calculate the safety index and update the safety text for the selected route
        calculateSafetyIndex(routes.get(routeIndex), routeIndex);
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
        showLoading();
        safetyIndex.fetchSafetyMetrics(route, new SafetyIndex.SafetyIndexCallback() {
            @Override
            public void onSafetyIndexCalculated(double safetyIndexValue) {
                Log.d(TAG, "Safety index calculated: " + safetyIndexValue);

                // Ensure the route index is valid and corresponds to the clicked route
                if (routeIndex >= 0 && routeIndex < routesContainer.getChildCount()) {
                    // Find the view for this route container
                    View routeView = routesContainer.getChildAt(routeIndex);

                    // Find the ProgressBar and TextView for this route
                    ProgressBar overallsafetyProgressBar = routeView.findViewById(R.id.overallsafetyProgressBar);
                    TextView overallsafetyCountTextView = routeView.findViewById(R.id.overallsafetyCountTextView);
                    TextView safetyStatusTextView = findViewById(R.id.safetyStatusTextView); // Ensure this ID is correct

                    if (overallsafetyProgressBar != null && overallsafetyCountTextView != null && safetyStatusTextView != null) {
                        Log.d(TAG, "Updating safety index UI for route index: " + routeIndex);

                        // Update the safety index and progress bar
                        overallsafetyCountTextView.setText(String.format("%.2f", safetyIndexValue));
                        int progress = (int) Math.round(safetyIndexValue);
                        overallsafetyProgressBar.setProgress(progress);

                        // Set custom progress drawable and safety status based on safety index
                        int colorResId;
                        String statusText;

                        // Normalize safety index value to percentage
                        double percentage = (safetyIndexValue / 100.0) * 100.0;

                        if (percentage > 91) {
                            colorResId = R.color.safeColor90;
                            statusText = "Safe to Walk Alone";
                        } else if (percentage >= 81) {
                            colorResId = R.color.safeColor80;
                            statusText = "Safe to Walk Alone";
                        } else if (percentage >= 71) {
                            colorResId = R.color.safeColor70;
                            statusText = "Safe to Walk Alone";
                        } else if (percentage >= 61) {
                            colorResId = R.color.safeColor60;
                            statusText = "Safe to Walk Alone";
                        } else if (percentage >= 51) {
                            colorResId = R.color.mediumColor50;
                            statusText = "Consider a Walking Buddy";
                        } else if (percentage >= 41) {
                            colorResId = R.color.mediumColor40;
                            statusText = "Consider a Walking Buddy";
                        } else if (percentage >= 31) {
                            colorResId = R.color.mediumColor30;
                            statusText = "Consider a Walking Buddy";
                        } else if (percentage >= 21) {
                            colorResId = R.color.dangerColor20;
                            statusText = "Opt for Transportationn";
                        } else if (percentage >= 11) {
                            colorResId = R.color.dangerColor10;
                            statusText = "Opt for Transportation";
                        } else {
                            colorResId = R.color.dangerColor0;
                            statusText = "Opt for Transportation";
                        }

                        // Set the progress bar color
                        setProgressBarColor(overallsafetyProgressBar, colorResId);
                        // Update the safety status text
                        safetyStatusTextView.setText(statusText);
                    } else {
                        Log.e(TAG, "Failed to find one or more views for route index: " + routeIndex);
                    }
                } else {
                    Log.e(TAG, "Invalid route index: " + routeIndex);
                }

                hideLoading(); // Hide loading dialog after updating the UI
            }
        });

        return routeIndex;
    }


    private int fetchCrimeData(List<LatLng> route, int i) {
        crimeData.fetchCrimeData(route, new CrimeData.CrimeDataCallback() {
            @Override
            public void onCrimeDataReceived(int count, List<CrimeData.Crime> crimes) {
                Log.d(TAG, "Crime data received: " + count);
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
                Log.d(TAG, "Police Station data received: " + count);
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



    public void clearMap() {
        if (gMap != null) {
            gMap.clear();
            bottomSheet.setVisibility(View.GONE);
            bottomSheetLayout.removeAllViews();
        }
    }

    // Request User Location
    private void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        } else {
            locationPermissionGranted = true;
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
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }



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
                                    .newLatLngZoom(defaultLocation, 100));
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
        if (requestCode == PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION) {
            locationPermissionGranted = grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            updateLocationUI();
            getCurrentLocation(); // Retry fetching location after permission is granted
        }
    }

}