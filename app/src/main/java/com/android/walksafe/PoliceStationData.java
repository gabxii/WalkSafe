package com.android.walksafe;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class PoliceStationData {

    private GoogleMap gMap;
    private Context context;
    private static final float PS_RADIUS_METERS = 100; // Define the appropriate radius for police stations

    public PoliceStationData(Context context, GoogleMap gMap) {
        this.gMap = gMap;
        this.context = context;
    }

    public void fetchPoliceStationData(List<LatLng> route, PoliceStationDataCallback callback) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("16cguN-iGSPhESXETuT8LJvCehUEvRMd6nDcri3QiekQ");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int count = 0;

                for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                    String childKey = childSnapshot.getKey();
                    if (childKey.equals("PS_AZCKO") || childKey.equals("PS_LBK") || childKey.equals("PS_SGP")) {
                        for (DataSnapshot stationSnapshot : childSnapshot.getChildren()) {

                            Object latitudeObj = stationSnapshot.child("latitude").getValue();
                            Object longitudeObj = stationSnapshot.child("longitude").getValue();

                            if (latitudeObj instanceof Double && longitudeObj instanceof Double) {
                                double latitude = (double) latitudeObj;
                                double longitude = (double) longitudeObj;

                                LatLng location = new LatLng(latitude, longitude);

                                if (isNearRoute(location, route)) {
                                    count++;
                                }
                            } else {
                                Log.e("PoliceStationData", "Latitude or longitude is not of type Double");
                            }
                        }
                    }
                }

                callback.onPoliceStationDataReceived(count);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("PoliceStationData", "Error fetching police station data", databaseError.toException());
            }
        });
    }


    public interface PoliceStationDataCallback {
        void onPoliceStationDataReceived(int count);
    }

    // Method to check if a police station is near the route
    private boolean isNearRoute(LatLng location, List<LatLng> route) {
        // Iterate through each segment of the route
        for (int i = 0; i < route.size() - 1; i++) {
            LatLng startPoint = route.get(i);
            LatLng endPoint = route.get(i + 1);

            float distanceToSegment = calculateDistanceToSegment(location, startPoint, endPoint); // Calculate the distance from the police station location to the segment

            // Check if the distance is within the police station radius
            if (distanceToSegment < PS_RADIUS_METERS) {
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
            // If the line segment is just a point, return the distance to that point
            return distanceToStart;
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
