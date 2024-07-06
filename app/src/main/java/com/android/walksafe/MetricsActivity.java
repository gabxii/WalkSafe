package com.android.walksafe;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

public class MetricsActivity extends AppCompatActivity {

    private static final String TAG = "MetricsActivity";

    private ProgressBar crimeProgressBar, cctvProgressBar, streetlightProgressBar, policeProgressBar;
    private TextView crimeCountTextView, cctvCountTextView, streetlightCountTextView, policeCountTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.metrics_layout);

        // Initialize views
        crimeProgressBar = findViewById(R.id.crimeProgressBar);
        crimeCountTextView = findViewById(R.id.crimeCountTextView);
        cctvProgressBar = findViewById(R.id.cctvProgressBar);
        cctvCountTextView = findViewById(R.id.cctvCountTextView);
        streetlightProgressBar = findViewById(R.id.streetlightProgressBar);
        streetlightCountTextView = findViewById(R.id.streetlightCountTextView);
        policeProgressBar = findViewById(R.id.policeProgressBar);
        policeCountTextView = findViewById(R.id.policeCountTextView);

        // Retrieve data from intent
        Intent intent = getIntent();
        if (intent != null) {
            ArrayList<LatLng> polylinePoints = intent.getParcelableArrayListExtra("polylinePoints");
            if (polylinePoints != null) {
                // Fetch data for all metrics
                fetchCrimeData(polylinePoints);
                fetchCCTVData(polylinePoints);
                fetchStreetlightData(polylinePoints);
                fetchPoliceData(polylinePoints);
            } else {
                Log.e(TAG, "No polyline points received");
                Toast.makeText(this, "No route points received", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.e(TAG, "Intent is null");
            Toast.makeText(this, "Intent is null", Toast.LENGTH_SHORT).show();
        }
    }

    // Method to fetch crime data using CrimeData class
    private void fetchCrimeData(ArrayList<LatLng> route) {
        CrimeData crimeData = new CrimeData(getApplicationContext(), null); // Pass context and null map
        crimeData.fetchCrimeData(route, new CrimeData.CrimeDataCallback() {
            @Override
            public void onCrimeDataReceived(int count) {
                Log.d(TAG, "Crime Count: " + count);
                updateCrimeCount(count);
            }
        });
    }

    // Method to fetch CCTV data
    private void fetchCCTVData(ArrayList<LatLng> route) {
        CCTVData cctvData = new CCTVData(getApplicationContext(), null); // Pass context and null map
        cctvData.fetchCCTVData(route, new CCTVData.CCTVDataCallback() {
            @Override
            public void onCCTVDataReceived(int count) {
                Log.d(TAG, "CCTVCount: " + count);
                updateCCTVCount(count);
            }
        });
    }

    // Method to fetch streetlight data
    private void fetchStreetlightData(ArrayList<LatLng> route) {
        StreetlightData streetlightData = new StreetlightData(getApplicationContext(), null); // Pass context and null map
        streetlightData.fetchStreetlightData(route, new StreetlightData.StreetlightDataCallback() {
            @Override
            public void onStreetlightDataReceived(int count) {
                Log.d(TAG, "Streetlight Count: " + count);
                updateStreetlightCount(count);
            }
        });
    }

    // Method to fetch police data
    private void fetchPoliceData(ArrayList<LatLng> route) {
        PoliceStationData policeStationData = new PoliceStationData(getApplicationContext(), null); // Pass context and null map
        policeStationData.fetchPoliceStationData(route, new PoliceStationData.PoliceStationDataCallback() {
            @Override
            public void onPoliceStationDataReceived(int count) {
                Log.d(TAG, "Police Station Count: " + count);
                updatePoliceCount(count);
            }
        });
    }

    // Update crime count TextView and ProgressBar
    public void updateCrimeCount(int count) {
        crimeCountTextView.setText(String.valueOf(count)); // Convert count to String
        setCrimeProgressBarColor(count);
        crimeProgressBar.setProgress(count);
    }

    // Update CCTV count TextView and ProgressBar
    public void updateCCTVCount(int count) {
        cctvCountTextView.setText(String.valueOf(count)); // Convert count to String
        setCCTVProgressBarColor(count);
        cctvProgressBar.setProgress(count);
    }

    // Update streetlight count TextView and ProgressBar
    public void updateStreetlightCount(int count) {
        streetlightCountTextView.setText(String.valueOf(count)); // Convert count to String
        setStreetlightProgressBarColor(count);
        streetlightProgressBar.setProgress(count);
    }

    // Update police count TextView and ProgressBar
    public void updatePoliceCount(int count) {
        policeCountTextView.setText(String.valueOf(count)); // Convert count to String
        setPoliceProgressBarColor(count);
        policeProgressBar.setProgress(count);
    }

    // Helper method to set crime progress bar color dynamically based on count
    private void setCrimeProgressBarColor(int count) {
        if (count < 10) {
            setProgressBarColor(crimeProgressBar, R.color.safeColor);
        } else if (count < 20) {
            setProgressBarColor(crimeProgressBar, R.color.mediumColor);
        } else {
            setProgressBarColor(crimeProgressBar, R.color.dangerColor);
        }
    }

    // Helper method to set CCTV progress bar color dynamically based on count
    private void setCCTVProgressBarColor(int count) {
        // Example logic, replace with appropriate color logic for CCTV
        if (count < 5) {
            setProgressBarColor(cctvProgressBar, R.color.safeColor);
        } else if (count < 10) {
            setProgressBarColor(cctvProgressBar, R.color.mediumColor);
        } else {
            setProgressBarColor(cctvProgressBar, R.color.dangerColor);
        }
    }

    // Helper method to set streetlight progress bar color dynamically based on count
    private void setStreetlightProgressBarColor(int count) {
        // Example logic, replace with appropriate color logic for streetlights
        if (count < 20) {
            setProgressBarColor(streetlightProgressBar, R.color.safeColor);
        } else if (count < 40) {
            setProgressBarColor(streetlightProgressBar, R.color.mediumColor);
        } else {
            setProgressBarColor(streetlightProgressBar, R.color.dangerColor);
        }
    }

    // Helper method to set police progress bar color dynamically based on count
    private void setPoliceProgressBarColor(int count) {
        // Example logic, replace with appropriate color logic for police
        if (count < 2) {
            setProgressBarColor(policeProgressBar, R.color.safeColor);
        } else if (count < 5) {
            setProgressBarColor(policeProgressBar, R.color.mediumColor);
        } else {
            setProgressBarColor(policeProgressBar, R.color.dangerColor);
        }
    }

    // Helper method to set progress bar color dynamically based on colorRes
    private void setProgressBarColor(ProgressBar progressBar, int colorRes) {
        int color = ContextCompat.getColor(this, colorRes);
        progressBar.setProgressTintList(ColorStateList.valueOf(color));
    }
}
