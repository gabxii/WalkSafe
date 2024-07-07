package com.android.walksafe;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.AsyncTask;
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
                new FetchDataTask(this).execute(polylinePoints);
            } else {
                Log.e(TAG, "No polyline points received");
                Toast.makeText(this, "No route points received", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.e(TAG, "Intent is null");
            Toast.makeText(this, "Intent is null", Toast.LENGTH_SHORT).show();
        }
    }

    private static class FetchDataTask extends AsyncTask<ArrayList<LatLng>, Void, Void> {
        private final MetricsActivity activity;

        FetchDataTask(MetricsActivity activity) {
            this.activity = activity;
        }

        @Override
        protected Void doInBackground(ArrayList<LatLng>... params) {
            ArrayList<LatLng> route = params[0];
            activity.fetchCrimeData(route);
            activity.fetchCCTVData(route);
            activity.fetchStreetlightData(route);
            activity.fetchPoliceData(route);
            return null;
        }
    }

    private void fetchCrimeData(ArrayList<LatLng> route) {
        CrimeData crimeData = new CrimeData(getApplicationContext(), null);
        crimeData.fetchCrimeData(route, count -> runOnUiThread(() -> updateCrimeCount(count)));
    }

    private void fetchCCTVData(ArrayList<LatLng> route) {
        CCTVData cctvData = new CCTVData(getApplicationContext(), null);
        cctvData.fetchCCTVData(route, count -> runOnUiThread(() -> updateCCTVCount(count)));
    }

    private void fetchStreetlightData(ArrayList<LatLng> route) {
        StreetlightData streetlightData = new StreetlightData(getApplicationContext(), null);
        streetlightData.fetchStreetlightData(route, count -> runOnUiThread(() -> updateStreetlightCount(count)));
    }

    private void fetchPoliceData(ArrayList<LatLng> route) {
        PoliceStationData policeStationData = new PoliceStationData(getApplicationContext(), null);
        policeStationData.fetchPoliceStationData(route, count -> runOnUiThread(() -> updatePoliceCount(count)));
    }

    public void updateCrimeCount(int count) {
        crimeCountTextView.setText(String.valueOf(count));
        setCrimeProgressBarColor(count);
        crimeProgressBar.setProgress(count);
    }

    public void updateCCTVCount(int count) {
        cctvCountTextView.setText(String.valueOf(count));
        setCCTVProgressBarColor(count);
        cctvProgressBar.setProgress(count);
    }

    public void updateStreetlightCount(int count) {
        streetlightCountTextView.setText(String.valueOf(count));
        setStreetlightProgressBarColor(count);
        streetlightProgressBar.setProgress(count);
    }

    public void updatePoliceCount(int count) {
        policeCountTextView.setText(String.valueOf(count));
        setPoliceProgressBarColor(count);
        policeProgressBar.setProgress(count);
    }

    private void setCrimeProgressBarColor(int count) {
        if (count < 10) {
            setProgressBarColor(crimeProgressBar, R.color.safeColor);
        } else if (count < 20) {
            setProgressBarColor(crimeProgressBar, R.color.mediumColor);
        } else {
            setProgressBarColor(crimeProgressBar, R.color.dangerColor);
        }
    }

    private void setCCTVProgressBarColor(int count) {
        if (count < 5) {
            setProgressBarColor(cctvProgressBar, R.color.dangerColor);
        } else if (count < 15) {
            setProgressBarColor(cctvProgressBar, R.color.mediumColor);
        } else {
            setProgressBarColor(cctvProgressBar, R.color.safeColor);
        }
    }

    private void setStreetlightProgressBarColor(int count) {
        if (count < 5) {
            setProgressBarColor(streetlightProgressBar, R.color.dangerColor);
        } else if (count < 15) {
            setProgressBarColor(streetlightProgressBar, R.color.mediumColor);
        } else {
            setProgressBarColor(streetlightProgressBar, R.color.safeColor);
        }
    }

    private void setPoliceProgressBarColor(int count) {
        if (count < 1) {
            setProgressBarColor(policeProgressBar, R.color.dangerColor);
        } else if (count < 3) {
            setProgressBarColor(policeProgressBar, R.color.mediumColor);
        } else {
            setProgressBarColor(policeProgressBar, R.color.safeColor);
        }
    }

    private void setProgressBarColor(ProgressBar progressBar, int colorResId) {
        progressBar.setProgressTintList(ColorStateList.valueOf(ContextCompat.getColor(this, colorResId)));
    }
}
