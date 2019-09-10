package com.example.saferouter;

import android.location.Location;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.mapbox.api.directions.v5.models.BannerInstructions;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.services.android.navigation.ui.v5.NavigationView;
import com.mapbox.services.android.navigation.ui.v5.NavigationViewOptions;
import com.mapbox.services.android.navigation.ui.v5.OnNavigationReadyCallback;
import com.mapbox.services.android.navigation.ui.v5.listeners.BannerInstructionsListener;
import com.mapbox.services.android.navigation.ui.v5.listeners.InstructionListListener;
import com.mapbox.services.android.navigation.ui.v5.listeners.NavigationListener;
import com.mapbox.services.android.navigation.ui.v5.listeners.SpeechAnnouncementListener;
import com.mapbox.services.android.navigation.ui.v5.voice.SpeechAnnouncement;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MapNavigationActivity extends AppCompatActivity implements OnNavigationReadyCallback,
        NavigationListener, ProgressChangeListener, InstructionListListener, SpeechAnnouncementListener,
        BannerInstructionsListener {

    private static Point originPoint;
    private static Point destination;
    private static final int INITIAL_ZOOM = 16;
    private DirectionsRoute directionsRoute;

    @BindView(R.id.navigation_view)
    NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.access_token));
        setContentView(R.layout.activity_map_navigation);
        ButterKnife.bind(this);
        //navigationView = findViewById(R.id.navigationView);
        originPoint = (Point) getIntent().getExtras().get("originPoint");
        destination = (Point) getIntent().getExtras().get("destination");
        directionsRoute = (DirectionsRoute) getIntent().getExtras().get("navigationRoute");
        CameraPosition initialPosition = new CameraPosition.Builder()
                .target(new LatLng(originPoint.latitude(), destination.longitude()))
                .zoom(INITIAL_ZOOM)
                .build();
        navigationView.onCreate(savedInstanceState);
        navigationView.initialize(this, initialPosition);
    }

    @Override
    protected void onStart() {
        super.onStart();
        navigationView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        navigationView.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        navigationView.onDestroy();

    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        navigationView.onLowMemory();
    }

    @Override
    protected void onPause() {
        super.onPause();
        navigationView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        navigationView.onResume();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        navigationView.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        navigationView.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    private void startNavigation() {
        NavigationViewOptions.Builder options =
                NavigationViewOptions.builder()
                        .navigationListener(this)
                        .directionsRoute(directionsRoute)
                        .shouldSimulateRoute(true)
                        .progressChangeListener(this)
                        .instructionListListener(this)
                        .speechAnnouncementListener(this)
                        .bannerInstructionsListener(this);

        navigationView.startNavigation(options.build());
    }

    @Override
    public void onNavigationReady(boolean isRunning) {
        if (directionsRoute != null) {
            startNavigation();
        }
    }

    @Override
    public BannerInstructions willDisplay(BannerInstructions instructions) {
        return instructions;
    }

    @Override
    public void onInstructionListVisibilityChanged(boolean visible) {

    }

    @Override
    public void onCancelNavigation() {
        finish();
    }

    @Override
    public void onNavigationFinished() {

    }

    @Override
    public void onNavigationRunning() {

    }

    @Override
    public SpeechAnnouncement willVoice(SpeechAnnouncement announcement) {
        return announcement;
    }

    @Override
    public void onProgressChange(Location location, RouteProgress routeProgress) {

    }


}
