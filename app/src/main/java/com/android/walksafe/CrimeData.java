package com.android.walksafe;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Log;
import android.location.Location;

import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CrimeData {
    // Weights for different crime categories (Example weights)
    private static final double WEIGHT_THEFT = 0.2;
    private static final double WEIGHT_PHYSICAL_INJURIES = 0.3;
    private static final double WEIGHT_ROBBERY = 0.4;
    private static final double WEIGHT_VAWC = 0.5;
    private static final double WEIGHT_CHILD_ABUSE = 0.6;
    private static final double WEIGHT_HOMICIDE = 0.8;
    private static final double WEIGHT_RAPE = 0.7;
    private static final float CRIME_RADIUS_METERS = 30;
    private static final float STROKE_WIDTH = 3f;
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


    // Method to retrieve crime data near a given location
    public void fetchCrimeData(List<LatLng> route, CrimeDataCallback callback) {
        db.collection("crime_incidents")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int crimeCount = 0;
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Double latitude = document.getDouble("location.latitude");
                        Double longitude = document.getDouble("location.longitude");
                        if (latitude != null && longitude != null) {
                            LatLng location = new LatLng(latitude, longitude);
                            if (isNearRoute(location, route)) {
                                crimeCount++;
                            }
                        }
                    }
                    callback.onCrimeDataReceived(crimeCount);
                })
                .addOnFailureListener(e -> {
                    Log.e("CrimeData", "Error fetching crime data", e);
                    callback.onCrimeDataReceived(0); // Handle failure by passing 0
                });
    }


    public interface CrimeDataCallback {
        void onCrimeDataReceived(int count);
    }


    // Calculate safety score based on crime data
    private double calculateSafetyScore(List<LatLng> route) {
        double totalWeightedCrimes = 0.0;

        for (Marker marker : crimeMarkers) {
            // Extract crime category from marker title
            String markerTitle = marker.getTitle();
            double weight = getWeightForCrimeCategory(markerTitle);
            totalWeightedCrimes += weight;
        }

        // Normalize the total weighted crimes
        double safetyScore = totalWeightedCrimes / getTotalWeightSum();

        return safetyScore;
    }


    // Assign weights based on crime category
    private double getWeightForCrimeCategory(String crimeCategory) {
        switch (crimeCategory.toUpperCase()) {
            case "THEFT":
                return WEIGHT_THEFT;
            case "PHYSICALINJURIES":
                return WEIGHT_PHYSICAL_INJURIES;
            case "ROBBERY":
                return WEIGHT_ROBBERY;
            case "VAWC":
                return WEIGHT_VAWC;
            case "CHILDABUSE":
                return WEIGHT_CHILD_ABUSE;
            case "HOMICIDE":
                return WEIGHT_HOMICIDE;
            case "RAPE":
                return WEIGHT_RAPE;
            default:
                return 0.0; // Default weight if crime category is unknown
        }
    }

    // Calculate total weight sum for normalization
    private double getTotalWeightSum() {
        // Sum of all weights
        return WEIGHT_THEFT + WEIGHT_PHYSICAL_INJURIES + WEIGHT_ROBBERY +
                WEIGHT_VAWC + WEIGHT_CHILD_ABUSE + WEIGHT_HOMICIDE + WEIGHT_RAPE;
    }


    public double calculateOverallCrimeWeight(List<LatLng> route) {
        double totalWeightedCrimes = 0.0;
        for (LatLng location : route) {
            // Example calculation based on crime categories
            // Adjust based on your actual weighting logic
            totalWeightedCrimes += WEIGHT_THEFT + WEIGHT_PHYSICAL_INJURIES + WEIGHT_ROBBERY;
        }
        return totalWeightedCrimes;
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
}
