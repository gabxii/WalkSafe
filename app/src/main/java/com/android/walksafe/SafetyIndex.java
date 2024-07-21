package com.android.walksafe;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import java.util.List;

public class SafetyIndex {
    private Context context;
    private GoogleMap googleMap;
    private CrimeData crimeData;
    private CCTVData cctvData;
    private PoliceStationData policeStationData;
    private StreetlightData streetlightData;

    private int crimeCount = 0;
    private int cctvCount = 0;
    private int policeCount = 0;
    private int streetlightCount = 0;


    // Maximum counts for normalization (adjust these values based on your data)
    private static final int MAX_CRIME_COUNT = 411;
    private static final int MAX_CCTV_COUNT = 45;
    private static final int MAX_POLICE_COUNT = 7;
    private static final int MAX_STREETLIGHT_COUNT = 354;

    // Weights for each metric
    private static final double WEIGHT_CRIME = 0.4;
    private static final double WEIGHT_CCTV = 0.2;
    private static final double WEIGHT_POLICE = 0.3;
    private static final double WEIGHT_STREETLIGHT = 0.1;



    public SafetyIndex(Context context, GoogleMap googleMap) {
        this.context = context;
        this.googleMap = googleMap;
        crimeData = new CrimeData(context, googleMap);
        cctvData = new CCTVData(context, googleMap);
        policeStationData = new PoliceStationData(context, googleMap);
        streetlightData = new StreetlightData(context, googleMap);
    }

    public void fetchSafetyMetrics(List<LatLng> polylinePoints, SafetyIndexCallback callback) {
        // Initialize counts
        crimeCount = -1;
        cctvCount = -1;
        policeCount = -1;
        streetlightCount = -1;

        // Fetch safety metrics asynchronously
        crimeData.fetchCrimeData(polylinePoints, new CrimeData.CrimeDataCallback() {
            @Override
            public void onCrimeDataReceived(int count) {
                crimeCount = count;
                calculateSafetyIndex(callback);
            }
        });

        cctvData.fetchCCTVData(polylinePoints, new CCTVData.CCTVDataCallback() {
            @Override
            public void onCCTVDataReceived(int count) {
                cctvCount = count;
                calculateSafetyIndex(callback);
            }
        });

        policeStationData.fetchPoliceStationData(polylinePoints, new PoliceStationData.PoliceStationDataCallback() {
            @Override
            public void onPoliceStationDataReceived(int count) {
                policeCount = count;
                calculateSafetyIndex(callback);
            }
        });

        streetlightData.fetchStreetlightData(polylinePoints, new StreetlightData.StreetlightDataCallback() {
            @Override
            public void onStreetlightDataReceived(int count) {
                streetlightCount = count;
                calculateSafetyIndex(callback);
            }
        });
    }


    private void calculateSafetyIndex(SafetyIndexCallback callback) {
        // Debug log
        Log.d("SafetyIndex", "CrimeCount: " + crimeCount);
        Log.d("SafetyIndex", "CCTVCount: " + cctvCount);
        Log.d("SafetyIndex", "PoliceCount: " + policeCount);
        Log.d("SafetyIndex", "StreetlightCount: " + streetlightCount);

        // Check if all data counts are available
        if (crimeCount >= 0 && cctvCount >= 0 && policeCount >= 0 && streetlightCount >= 0) {
            // Normalize counts
            double normalizedCrimeCount = (double) crimeCount / MAX_CRIME_COUNT;
            double normalizedCCTVCount = (double) cctvCount / MAX_CCTV_COUNT;
            double normalizedPoliceCount = (double) policeCount / MAX_POLICE_COUNT;
            double normalizedStreetlightCount = (double) streetlightCount / MAX_STREETLIGHT_COUNT;

            // Calculate safety index
            double safetyIndex = 100 * (1 - (WEIGHT_CRIME * normalizedCrimeCount +
                    WEIGHT_CCTV * normalizedCCTVCount +
                    WEIGHT_POLICE * normalizedPoliceCount +
                    WEIGHT_STREETLIGHT * normalizedStreetlightCount));

            // Format the safety index to 2 decimal places
            String formattedSafetyIndex = String.format("%.2f", safetyIndex);
            double roundedSafetyIndex = Double.parseDouble(formattedSafetyIndex);

            callback.onSafetyIndexCalculated(roundedSafetyIndex);
        }
    }


    // Callback interface for passing safety index value
    public interface SafetyIndexCallback {
        void onSafetyIndexCalculated(double safetyIndex);
    }

}
