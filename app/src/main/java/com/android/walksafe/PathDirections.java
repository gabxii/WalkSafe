package com.android.walksafe;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class PathDirections extends AsyncTask<Location, Void, List<List<LatLng>>> {

    private MapActivity mapActivity;
    private MetricsActivity metricsActivity;
    private GoogleMap gMap;
    private Context context;
    private LatLng destination;
    private String destinationName;
    private boolean requestAlternativeRoutes; // Flag to request alternative routes
    private List<Polyline> polylines = new ArrayList<>(); // List to store all polylines
    private Polyline primaryPolyline; // Store reference to the primary polyline
    private List<List<LatLng>> decodedPolylines;
    private final CrimeData crimeData;
    private final CCTVData cctvData;
    private final PoliceStationData policeStationData;
    private final StreetlightData streetlightData;
    private SafetyIndex safetyIndex;
    private List<String> routeName;
    private List<String> routeTime;
    private List<String> routeDistance;

    public PathDirections (MapActivity mapActivity, MetricsActivity metricsActivity, CCTVData cctvData, CrimeData crimeData, PoliceStationData policeStationData, StreetlightData streetlightData) {
        this.mapActivity = mapActivity;
        this.context = mapActivity.getApplicationContext();

        this.metricsActivity = metricsActivity;

        this.cctvData = cctvData;
        this.crimeData = crimeData;
        this.policeStationData = policeStationData;
        this.streetlightData = streetlightData;

        this.safetyIndex = safetyIndex;

        this.gMap = null;
    }

    public void setGoogleMap(GoogleMap googleMap) {
        this.gMap = googleMap;
    }

    public void setDestination(LatLng destination, String destinationName) {
        this.destination = destination;
        this.destinationName = destinationName;
    }

    public void setRequestAlternativeRoutes(boolean requestAlternativeRoutes) {
        this.requestAlternativeRoutes = requestAlternativeRoutes;
    }

    // Start turn-by-turn navigation
    public void startTurnByTurnNavigation(LatLng origin, LatLng destination) {
        if (context != null && origin != null && destination != null) {
            String uri = "google.navigation:q=" + destination.latitude + "," + destination.longitude + "&origin=" + origin.latitude + "," + origin.longitude;
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            intent.setPackage("com.google.android.apps.maps");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // Add this line to fix the issue
            context.startActivity(intent);
        } else {
            Log.e(TAG, "Invalid context, origin, or destination for navigation");
        }
    }


    @Override
    protected List<List<LatLng>> doInBackground(Location... locations) {
        if (locations == null || locations.length == 0 || locations[0] == null) {
            Log.e(TAG, "Location object is null");
            return null;
        }

        Location origin = locations[0];
        LatLng originLatLng = new LatLng(origin.getLatitude(), origin.getLongitude());

        List<List<LatLng>> decodedPolylines = new ArrayList<>();

        try {
            String apiKey = context.getString(R.string.api_key);
            String urlString = "https://maps.googleapis.com/maps/api/directions/json?" +
                    "origin=" + originLatLng.latitude + "," + originLatLng.longitude +
                    "&destination=" + destination.latitude + "," + destination.longitude +
                    "&mode=walking" +
                    "&key=" + apiKey;

            if (requestAlternativeRoutes) {
                urlString += "&alternatives=true";
            }

            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            InputStream inputStream = connection.getInputStream();
            Scanner scanner = new Scanner(inputStream);
            StringBuilder stringBuilder = new StringBuilder();

            while (scanner.hasNext()) {
                stringBuilder.append(scanner.nextLine());
            }

            JSONObject jsonObject = new JSONObject(stringBuilder.toString());
            JSONArray routesArray = jsonObject.getJSONArray("routes");

            decodedPolylines.clear(); // Clear existing data
            routeName = new ArrayList<>();
            routeTime = new ArrayList<>();
            routeDistance = new ArrayList<>();

            for (int i = 0; i < routesArray.length(); i++) {
                JSONObject route = routesArray.getJSONObject(i);
                JSONObject legs = route.getJSONArray("legs").getJSONObject(0);

                String currentRouteName = route.getString("summary");
                String currentRouteTime = legs.getJSONObject("duration").getString("text");
                String currentRouteDistance = legs.getJSONObject("distance").getString("text");

                routeName.add(currentRouteName);
                routeTime.add(currentRouteTime);
                routeDistance.add(currentRouteDistance);

                JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
                String points = overviewPolyline.getString("points");
                List<LatLng> decodedPolyline = decodePolyline(points);
                decodedPolylines.add(decodedPolyline);
            }
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Error fetching route data", e);
        }

        return decodedPolylines;
    }

    @Override
    protected void onPostExecute(List<List<LatLng>> decodedPolylines) {
        if (gMap == null) {
            Log.e(TAG, "GoogleMap object is null");
            return;
        }

        this.decodedPolylines = decodedPolylines; // Store decoded polylines for later use
        gMap.clear(); // Clear previous polylines from the map
        polylines.clear(); // Clear the list of polylines

        // Prepare lists to store route details
        List<String> routeNames = new ArrayList<>();
        List<String> routeTimes = new ArrayList<>();
        List<String> routeDistances = new ArrayList<>();

        // Loop through each decoded polyline
        for (int i = 0; i < decodedPolylines.size(); i++) {
            List<LatLng> decodedPolyline = decodedPolylines.get(i);

            // Fetch route details
            String currentRouteName = routeName.get(i);
            String currentRouteTime = routeTime.get(i);
            String currentRouteDistance = routeDistance.get(i);

            // Add details to lists
            routeNames.add(currentRouteName);
            routeTimes.add(currentRouteTime);
            routeDistances.add(currentRouteDistance);

            // Add polyline to GoogleMap
            PolylineOptions polylineOptions = new PolylineOptions();
            polylineOptions.addAll(decodedPolyline);

            // Customize polyline appearance based on index
            Polyline polyline;
            if (i == 0) {
                polylineOptions.color(Color.parseColor("#1A73E8")); // Main route color is blue
                polylineOptions.zIndex(1); // Set higher z-index for the primary polyline
                polyline = gMap.addPolyline(polylineOptions);
                primaryPolyline = polyline; // Store reference to the primary polyline
            } else {
                polylineOptions.color(Color.parseColor("#7A7878")); // Set other polylines color to grey
                polylineOptions.zIndex(0); // Set lower z-index for the alternative polylines
                polyline = gMap.addPolyline(polylineOptions);
            }

            polylines.add(polyline); // Add polyline to list

            // Add markers for origin and destination
            MarkerOptions originMarkerOptions = new MarkerOptions()
                    .position(decodedPolyline.get(0)) // First point of the polyline (origin)
                    .icon(BitmapDescriptorFactory.defaultMarker(210))
                    .title("Origin");
            MarkerOptions destinationMarkerOptions = new MarkerOptions()
                    .position(decodedPolyline.get(decodedPolyline.size() - 1)) // Last point of the polyline (destination)
                    .title(destinationName); // Use destinationName passed from autocompleteFragment
            gMap.addMarker(originMarkerOptions);
            gMap.addMarker(destinationMarkerOptions);

            // Zoom to fit all polylines
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (LatLng latLng : decodedPolyline) {
                builder.include(latLng);
            }

            LatLngBounds bounds = builder.build();
            int padding = 250; // Padding in pixels
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding);
            gMap.animateCamera(cameraUpdate);
        }

        // Pass route data back to MapActivity for UI updates
        if (mapActivity != null) {
            mapActivity.onRouteObtained(decodedPolylines, routeNames, routeTimes, routeDistances);
        }
    }

    // Method to start navigation using a route
    public void startTurnByTurnNavigation(List<LatLng> route) {
        if (route != null && !route.isEmpty()) {
            LatLng origin = route.get(0);
            LatLng destination = route.get(route.size() - 1);
            startTurnByTurnNavigation(origin, destination);
        } else {
            Log.e(TAG, "Invalid route for navigation");
        }
    }


    private List<LatLng> decodePolyline(String encoded) {
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
}
