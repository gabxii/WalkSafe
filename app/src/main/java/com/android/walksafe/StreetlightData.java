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

public class StreetlightData {
    private Context context;
    private DatabaseReference databaseReference;
    private static final float SL_RADIUS_METERS = 30;

    public StreetlightData(Context context, GoogleMap gMap) {
        this.context = context;
        databaseReference = FirebaseDatabase.getInstance().getReference().child("13m6mDT-oDjzdmgSsm3mHVtD__sHwuM5yfO7OC8qsaz4");
    }

    public void fetchStreetlightData(List<LatLng> route, StreetlightDataCallback callback) {
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int count = 0;

                // Iterate over each child node (SL_AZCKO, SL_LBK, SL_SGP)
                for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                    String childKey = childSnapshot.getKey();

                    if (childKey.equals("SL_AZCKO") || childKey.equals("SL_LBK") || childKey.equals("SL_SGP")) {
                        for (DataSnapshot streetlightSnapshot : childSnapshot.getChildren()) {

                            // Iterate over each streetlight entry under the current child node
                            Object latitudeObj = streetlightSnapshot.child("latitude").getValue();
                            Object longitudeObj = streetlightSnapshot.child("longitude").getValue();

                            if (latitudeObj instanceof Double && longitudeObj instanceof Double) {
                                double latitude = (double) latitudeObj;
                                double longitude = (double) longitudeObj;

                                LatLng location = new LatLng(latitude, longitude);

                                // Check if streetlight is near the route
                                if (isNearRoute(location, route)) {
                                    count++;
                                }
                            } else {
                                Log.e("StreetlightData", "Latitude or longitude is not of type Double");
                            }
                        }
                    }
                }

                // Pass the count of streetlight locations to the callback
                callback.onStreetlightDataReceived(count);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("StreetlightData", "Error fetching streetlight data", databaseError.toException());
                // Handle error appropriately
            }
        });
    }

    public interface StreetlightDataCallback {
        void onStreetlightDataReceived(int count);
    }

    // Method to check if a streetlight is near the route
    private boolean isNearRoute(LatLng location, List<LatLng> route) {
        // Iterate through each segment of the route
        for (int i = 0; i < route.size() - 1; i++) {
            LatLng startPoint = route.get(i);
            LatLng endPoint = route.get(i + 1);

            float distanceToSegment = calculateDistanceToSegment(location, startPoint, endPoint);

            // Check if the distance is within the streetlight radius
            if (distanceToSegment < SL_RADIUS_METERS) {
                return true;
            }
        }
        return false;
    }

    // Method to calculate the distance from a point to a line segment
    private float calculateDistanceToSegment(LatLng point, LatLng start, LatLng end) {
        // Convert LatLng points to Location objects for distance calculation
        if (point == null || start == null || end == null) {
            return -1; // Handle null parameters appropriately
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

        float distanceToStart = location.distanceTo(startLocation);
        float distanceToEnd = location.distanceTo(endLocation);
        float length = startLocation.distanceTo(endLocation);

        if (length == 0) {
            return distanceToStart;
        }

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
