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
    private ProgressBar overallsafetyProgressBar;
    private TextView overallsafetyCountTextView;

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
            Bundle extras = intent.getExtras();
            if (extras != null) {
                int crimeCount = extras.getInt("crimeCount", 0);
                int cctvCount = extras.getInt("cctvCount", 0);
                int policeCount = extras.getInt("policeCount", 0);
                int streetlightCount = extras.getInt("streetlightCount", 0);

                // Update UI with retrieved counts
                updateCrimeCount(crimeCount);
                updateCCTVCount(cctvCount);
                updatePoliceCount(policeCount);
                updateStreetlightCount(streetlightCount);

            } else {
                Log.e(TAG, "Intent extras are null");
                Toast.makeText(this, "Failed to retrieve data", Toast.LENGTH_SHORT).show();
                finish(); // Close activity if data retrieval fails
            }
        } else {
            Log.e(TAG, "Intent is null");
            Toast.makeText(this, "Intent is null", Toast.LENGTH_SHORT).show();
            finish(); // Close activity if intent is null
        }
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

    // Update overall safety index UI
    public void updateOverallSafetyIndex(int index) {
        overallsafetyCountTextView.setText(String.valueOf(index));
        overallsafetyProgressBar.setProgress(index);

        // Adjust color based on safety index
        if (index < 10) {
            setProgressBarColor(overallsafetyProgressBar, R.color.dangerColor);
        } else if (index < 15) {
            setProgressBarColor(overallsafetyProgressBar, R.color.mediumColor);
        } else {
            setProgressBarColor(overallsafetyProgressBar, R.color.safeColor);
        }
    }

    // Helper method to set progress bar color dynamically
    private void setProgressBarColor(ProgressBar progressBar, int colorRes) {
        int color = getResources().getColor(colorRes);
        progressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(color));
    }
}