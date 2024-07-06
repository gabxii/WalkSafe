package com.android.walksafe;

import android.content.Context;
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

    private int crimeCount;
    private int cctvCount;
    private int policeCount;
    private int streetlightCount;

    public SafetyIndex(Context context, GoogleMap googleMap) {
        this.context = context;
        this.googleMap = googleMap;
        crimeData = new CrimeData(context, googleMap);
        cctvData = new CCTVData(context, googleMap);
        policeStationData = new PoliceStationData(context, googleMap);
        streetlightData = new StreetlightData(context, googleMap);
    }

    public void fetchSafetyMetrics(List<LatLng> polylinePoints, SafetyIndexCallback callback) {
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
        // Check if all data counts are available
        if (crimeCount >= 0 && cctvCount >= 0 && policeCount >= 0 && streetlightCount >= 0) {
            // Calculate safety index (Example calculation)
            // Adjust your formula based on your application's specific logic
            double safetyIndex = 100 - (crimeCount * 0.2 + cctvCount * 0.3 + policeCount * 0.1 + streetlightCount * 0.4);

            // Pass safety index to callback method
            callback.onSafetyIndexCalculated(safetyIndex);
        }
    }

    // Callback interface for passing safety index value
    public interface SafetyIndexCallback {
        void onSafetyIndexCalculated(double safetyIndex);
    }
}
