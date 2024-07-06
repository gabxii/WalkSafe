package com.android.walksafe;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.location.Location;
import android.util.Log;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class CCTVData {
    private GoogleMap gMap;
    private Context context;
    private static final float CCTV_RADIUS_METERS = 30;
    private ProgressBar cctvProgressBar;

    public CCTVData(Context context, GoogleMap gMap) {
        this.gMap = gMap;
        this.context = context;
    }

    public void fetchCCTVData(List<LatLng> route, CCTVDataCallback callback) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("1rx9PxI0mStDuQ6-QXz2UGd9WKoUgKLrpO9Pb4fetBG8");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int count = 0;

                // Iterate over each child node (CCTV_AZCKO, CCTV_LBK, CCTV_SGP)
                for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                    String childKey = childSnapshot.getKey();

                    // Iterate over each nested node (1, 2, 3, ...)
                    for (DataSnapshot cctvSnapshot : childSnapshot.getChildren()) {
                        Log.d("CCTVData", "Processing node: " + cctvSnapshot.getKey());

                        // Log the entire snapshot to debug the structure
                        Log.d("CCTVData", "Snapshot: " + cctvSnapshot.toString());

                        // Retrieve CCTV count and location from database
                        Object cctvCountObj = cctvSnapshot.child("cctvCount").getValue();
                        Object latitudeObj = cctvSnapshot.child("latitude").getValue();
                        Object longitudeObj = cctvSnapshot.child("longitude").getValue();

                        // Convert and validate data types
                        Integer cctvCount = null;
                        Double latitude = null;
                        Double longitude = null;

                        if (cctvCountObj instanceof Long) {
                            cctvCount = ((Long) cctvCountObj).intValue();
                        } else if (cctvCountObj instanceof Integer) {
                            cctvCount = (Integer) cctvCountObj;
                        } else {
                            Log.e("CCTVData", "Invalid type for cctvCount: " + cctvCountObj);
                        }

                        if (latitudeObj instanceof Double) {
                            latitude = (Double) latitudeObj;
                        } else if (latitudeObj instanceof String) {
                            try {
                                latitude = Double.parseDouble((String) latitudeObj);
                            } catch (NumberFormatException e) {
                                Log.e("CCTVData", "Invalid type for latitude: " + latitudeObj);
                            }
                        }

                        if (longitudeObj instanceof Double) {
                            longitude = (Double) longitudeObj;
                        } else if (longitudeObj instanceof String) {
                            try {
                                longitude = Double.parseDouble((String) longitudeObj);
                            } catch (NumberFormatException e) {
                                Log.e("CCTVData", "Invalid type for longitude: " + longitudeObj);
                            }
                        }

                        Log.d("CCTVData", "cctvCount: " + cctvCount + ", latitude: " + latitude + ", longitude: " + longitude);

                        if (cctvCount != null && latitude != null && longitude != null) {
                            LatLng location = new LatLng(latitude, longitude);

                            // Check if CCTV is near the route
                            if (isNearRoute(location, route)) {
                                count += cctvCount; // Accumulate the count of CCTV cameras
                            }
                        } else {
                            Log.e("CCTVData", "cctvCount, latitude, or longitude is null or not of expected type for node: " + cctvSnapshot.getKey() +
                                    " cctvCount: " + cctvCount +
                                    ", latitude: " + latitude +
                                    ", longitude: " + longitude);
                        }
                    }
                }

                // Pass the total count of CCTV cameras to the callback
                callback.onCCTVDataReceived(count);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("CCTVData", "Error fetching CCTV data: " + databaseError.getMessage());
                // Handle database error gracefully
                callback.onCCTVDataReceived(0); // or another appropriate action
            }
        });
    }

    public interface CCTVDataCallback {
        void onCCTVDataReceived(int count);
    }


    // Method to check if a CCTV is near the route
    private boolean isNearRoute(LatLng location, List<LatLng> route) {
        // Iterate through each segment of the route
        for (int i = 0; i < route.size() - 1; i++) {
            LatLng startPoint = route.get(i);
            LatLng endPoint = route.get(i + 1);

            float distanceToSegment = calculateDistanceToSegment(location, startPoint, endPoint); // Calculate the distance from the CCTV location to the segment

            // Check if the distance is within the CCTV radius
            if (distanceToSegment < CCTV_RADIUS_METERS) {
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
