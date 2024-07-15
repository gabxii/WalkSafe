package com.android.walksafe;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MetricsActivity extends AppCompatActivity {

    private static final String TAG = "MetricsActivity";

    private ProgressBar crimeProgressBar;
    private TextView crimeCountTextView;
    private ProgressBar cctvProgressBar;
    private TextView cctvCountTextView;
    private ProgressBar policeProgressBar;
    private TextView policeCountTextView;
    private ProgressBar streetlightProgressBar;
    private TextView streetlightCountTextView;
    private TextView metricsRouteTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.metrics_layout);

        // Initialize views
        crimeProgressBar = findViewById(R.id.crimeProgressBar);
        crimeCountTextView = findViewById(R.id.crimeCountTextView);
        cctvProgressBar = findViewById(R.id.cctvProgressBar);
        cctvCountTextView = findViewById(R.id.cctvCountTextView);
        policeProgressBar = findViewById(R.id.policeProgressBar);
        policeCountTextView = findViewById(R.id.policeCountTextView);
        streetlightProgressBar = findViewById(R.id.streetlightProgressBar);
        streetlightCountTextView = findViewById(R.id.streetlightCountTextView);
        metricsRouteTitle = findViewById(R.id.metricsRouteTitle);

        // Initialize back button
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Finish this activity to return to previous activity (likely MapActivity)
            }
        });



        // Retrieve intent extras
        Intent intent = getIntent();
        if (intent != null) {
            // Retrieve route index
            int routeIndex = intent.getIntExtra("routeIndex", -1); // -1 is default or invalid index

            // Retrieve metrics counts for the selected route
            String routeName = intent.getStringExtra("routeName");
            int crimeCount = intent.getIntExtra("crimeCount", 0);
            int cctvCount = intent.getIntExtra("cctvCount", 0);
            int policeCount = intent.getIntExtra("policeCount", 0);
            int streetlightCount = intent.getIntExtra("streetlightCount", 0);

            // Update UI with retrieved counts
            updateRouteTitle(routeName);
            updateCrimeCount(crimeCount);
            updateCCTVCount(cctvCount);
            updatePoliceCount(policeCount);
            updateStreetlightCount(streetlightCount);

        } else {
            Log.e(TAG, "Intent is null");
            Toast.makeText(this, "Intent is null", Toast.LENGTH_SHORT).show();
            finish(); // Close activity if intent is null
        }
    }

    public void updateRouteTitle(String routeName) {
        metricsRouteTitle.setText(routeName);
    }

    private void clearMetrics() {
        // Clear previous metric counts or update to zero
        updateCrimeCount(0);
        updateCCTVCount(0);
        updatePoliceCount(0);
        updateStreetlightCount(0);
    }

    // Update crime count UI
    public void updateCrimeCount(int count) {
        crimeCountTextView.setText(String.valueOf(count));
        crimeProgressBar.setProgress(count);
    }

    // Update CCTV count UI
    public void updateCCTVCount(int count) {
        cctvCountTextView.setText(String.valueOf(count));
        cctvProgressBar.setProgress(count);
    }

    // Update police count UI
    public void updatePoliceCount(int count) {
        policeCountTextView.setText(String.valueOf(count));
        policeProgressBar.setProgress(count);
    }

    // Update streetlight count UI
    public void updateStreetlightCount(int count) {
        streetlightCountTextView.setText(String.valueOf(count));
        streetlightProgressBar.setProgress(count);
    }
}
