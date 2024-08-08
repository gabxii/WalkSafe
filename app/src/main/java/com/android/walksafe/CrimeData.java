package com.android.walksafe;

import android.content.Context;
import android.util.Log;
import android.location.Location;

import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class CrimeData {
    private static final float CRIME_RADIUS_METERS = 30;
    private FirebaseFirestore db;
    private GoogleMap gMap;
    private Context context; // For fetching resources
    private List<Marker> crimeMarkers;
    private List<Circle> crimeCircles;

    public CrimeData(Context context, GoogleMap gMap) {
        this.context = context;
        this.gMap = gMap;
        db = FirebaseFirestore.getInstance();
    }

    // Method to retrieve crime data near a given location and filter by barangays
    public void fetchCrimeData(List<LatLng> route, CrimeDataCallback callback) {
        // Define the list of barangays to filter
        List<String> barangays = new ArrayList<>();
        barangays.add("Session-Governor Pack Road");
        barangays.add("Legarda-Burnham-Kisad");
        barangays.add("AZCKO");

        db.collection("crime_incidents")
                .whereIn("Barangay", barangays) // Filter by barangays
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int crimeCount = 0;
                    List<Crime> crimes = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Double latitude = document.getDouble("location.latitude");
                        Double longitude = document.getDouble("location.longitude");
                        String barangay = document.getString("Barangay"); // Get the barangay from the document
                        if (latitude != null && longitude != null && barangay != null) {
                            LatLng location = new LatLng(latitude, longitude);
                            if (isNearRoute(location, route)) {
                                crimeCount++;
                                crimes.add(new Crime(location, barangay)); // Add crime to list
                            }
                        }
                    }
                    Log.d("CrimeData", "Crime Count: " + crimeCount);
                    callback.onCrimeDataReceived(crimeCount, crimes); // Pass crimes list to callback
                })
                .addOnFailureListener(e -> {
                    Log.e("CrimeData", "Error fetching crime data", e);
                    callback.onCrimeDataReceived(0, new ArrayList<>()); // Handle failure by passing 0 and empty list
                });
    }

    public interface CrimeDataCallback {
        void onCrimeDataReceived(int count, List<Crime> crimes); // Updated callback
    }

    // Method to check if a location is near the route
    private boolean isNearRoute(LatLng location, List<LatLng> route) {
        // Iterate through each segment of the route
        for (int i = 0; i < route.size() - 1; i++) {
            LatLng startPoint = route.get(i);
            LatLng endPoint = route.get(i + 1);

            float distanceToSegment = calculateDistanceToSegment(location, startPoint, endPoint); // Calculate the distance from the crime location to the segment

            // Check if the distance is within the crime radius
            if (distanceToSegment < CRIME_RADIUS_METERS) {
                return true;
            }
        }
        return false;
    }

    // Method to calculate the distance from a point to a line segment
    private float calculateDistanceToSegment(LatLng point, LatLng start, LatLng end) {
        // Convert LatLng points to Location objects for distance calculation
        if (point == null || start == null || end == null) {
            // Handle null parameters appropriately, such as returning a default value or throwing an exception
            return -1; // Example default return value
        }

        Location location = new Location("point");
        location.setLatitude(point.latitude);
        location.setLongitude(point.longitude);

        Location startLocation = new Location("start");
        startLocation.setLatitude(start.latitude);
        startLocation.setLongitude(start.longitude);

        Location endLocation = new Location("end");
        endLocation.setLatitude(end.latitude);
        endLocation.setLongitude(end.longitude);

        float distanceToStart = location.distanceTo(startLocation); // Distance to start point
        float distanceToEnd = location.distanceTo(endLocation); // Distance to end point
        float length = startLocation.distanceTo(endLocation); // Length of the line segment

        if (length == 0) {
            return distanceToStart; // If the line segment is just a point, return the distance to that point
        }

        // Calculate the distance to the line segment
        float dotProduct = (float) (((point.latitude - start.latitude) * (end.latitude - start.latitude)) +
                ((point.longitude - start.longitude) * (end.longitude - start.longitude)));
        float t = Math.max(0, Math.min(1, dotProduct / (length * length)));
        LatLng projection = new LatLng(
                start.latitude + t * (end.latitude - start.latitude),
                start.longitude + t * (end.longitude - start.longitude)
        );
        Location projectionLocation = new Location("projection");
        projectionLocation.setLatitude(projection.latitude);
        projectionLocation.setLongitude(projection.longitude);
        float distanceToLine = location.distanceTo(projectionLocation);

        return distanceToLine;
    }

    // Inner class to represent a Crime object
    public static class Crime {
        public LatLng location;
        public String barangay;

        public Crime(LatLng location, String barangay) {
            this.location = location;
            this.barangay = barangay;
        }
    }
}
