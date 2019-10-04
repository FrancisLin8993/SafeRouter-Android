package com.example.saferouter;

import android.annotation.SuppressLint;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.saferouter.model.NavigationDangerousInfoItem;
import com.example.saferouter.utils.Utils;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.location.LocationEngineRequest;
import com.mapbox.android.core.location.LocationEngineResult;
import com.mapbox.api.directions.v5.models.BannerInstructions;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.LegAnnotation;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.RouteLeg;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.services.android.navigation.ui.v5.NavigationView;
import com.mapbox.services.android.navigation.ui.v5.NavigationViewOptions;
import com.mapbox.services.android.navigation.ui.v5.OnNavigationReadyCallback;
import com.mapbox.services.android.navigation.ui.v5.SoundButton;
import com.mapbox.services.android.navigation.ui.v5.listeners.BannerInstructionsListener;
import com.mapbox.services.android.navigation.ui.v5.listeners.InstructionListListener;
import com.mapbox.services.android.navigation.ui.v5.listeners.NavigationListener;
import com.mapbox.services.android.navigation.ui.v5.listeners.RouteListener;
import com.mapbox.services.android.navigation.ui.v5.listeners.SpeechAnnouncementListener;
import com.mapbox.services.android.navigation.ui.v5.voice.NavigationSpeechPlayer;
import com.mapbox.services.android.navigation.ui.v5.voice.SpeechAnnouncement;
import com.mapbox.services.android.navigation.ui.v5.voice.SpeechPlayerProvider;
import com.mapbox.services.android.navigation.ui.v5.voice.VoiceInstructionLoader;
import com.mapbox.services.android.navigation.v5.instruction.Instruction;
import com.mapbox.services.android.navigation.v5.milestone.Milestone;
import com.mapbox.services.android.navigation.v5.milestone.MilestoneEventListener;
import com.mapbox.services.android.navigation.v5.milestone.StepMilestone;
import com.mapbox.services.android.navigation.v5.milestone.Trigger;
import com.mapbox.services.android.navigation.v5.milestone.TriggerProperty;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.turf.TurfConstants;
import com.mapbox.turf.TurfMeasurement;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import okhttp3.Cache;

import static com.example.saferouter.utils.CommonConstants.COMPONENT_NAVIGATION_INSTRUCTION_CACHE;
import static com.example.saferouter.utils.CommonConstants.DANGEROUS_LEVEL;
import static com.example.saferouter.utils.CommonConstants.DEFAULT_INTERVAL_IN_MILLISECONDS;
import static com.example.saferouter.utils.CommonConstants.DEFAULT_MAX_WAIT_TIME;
import static com.example.saferouter.utils.CommonConstants.TEN_MEGABYTE_CACHE_SIZE;
import static com.example.saferouter.utils.Utils.getDangerousPointIndexFromCurrentRoute;

public class MapNavigationActivity extends AppCompatActivity implements OnNavigationReadyCallback,
        NavigationListener, ProgressChangeListener, InstructionListListener, SpeechAnnouncementListener,
        BannerInstructionsListener, MilestoneEventListener, RouteListener {

    private static Point originPoint;
    private static Point destination;
    private static final int INITIAL_ZOOM = 16;
    private DirectionsRoute directionsRoute;
    private List<Point> pointsOfRoute;
    private List<String> safetyLevelsList;
    private List<String> navigationRatingsList;
    private List<String> voiceMessagesList;
    private List<Milestone> milestoneList = new ArrayList<>();
    private List<NavigationDangerousInfoItem> navigationDangerousInfoItemList = new ArrayList<>();
    private NavigationSpeechPlayer speechPlayer;
    private LocationEngine locationEngine;
    private NavigationActivityLocationCallback locationCallback = new NavigationActivityLocationCallback(this);

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
        pointsOfRoute = Utils.getPointsOfRoutes(directionsRoute);
        safetyLevelsList = (ArrayList<String>) getIntent().getExtras().get("safetyLevels");
        navigationRatingsList = (ArrayList<String>) getIntent().getExtras().get("navigationRatings");
        navigationRatingsList.remove(0);
        voiceMessagesList = (ArrayList<String>) getIntent().getExtras().get("voiceMessages");
        modifyRouteCongestionInfo();
        addAllMilestone();
        initializeSpeechPlayer();
        //initLocationEngine();
        CameraPosition initialPosition = new CameraPosition.Builder()
                .target(new LatLng(originPoint.latitude(), destination.longitude()))
                .zoom(INITIAL_ZOOM)
                .build();
        navigationView.onCreate(savedInstanceState);
        navigationView.initialize(this, initialPosition);

    }

    private void modifyRouteCongestionInfo() {
        RouteLeg routeLeg = directionsRoute.legs().get(0);
        LegAnnotation legAnnotation = routeLeg.annotation();
        LegAnnotation newLegAnnotation = legAnnotation.toBuilder().congestion(navigationRatingsList).build();
        RouteLeg newRouteLeg = routeLeg.toBuilder().annotation(newLegAnnotation).build();
        List<RouteLeg> newLegs = new ArrayList<>();
        newLegs.add(newRouteLeg);
        directionsRoute = directionsRoute.toBuilder().legs(newLegs).build();
    }

    @SuppressLint("MissingPermission")
    private void initLocationEngine() {
        locationEngine = LocationEngineProvider.getBestLocationEngine(this);

        LocationEngineRequest request = new LocationEngineRequest.Builder(DEFAULT_INTERVAL_IN_MILLISECONDS)
                .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                .setMaxWaitTime(DEFAULT_MAX_WAIT_TIME).build();

        locationEngine.requestLocationUpdates(request, locationCallback, getMainLooper());
        locationEngine.getLastLocation(locationCallback);
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
                        .bannerInstructionsListener(this)
                        .routeListener(this)
                        .locationEngine(locationEngine)
                        .milestones(milestoneList)
                        .milestoneEventListener(this)
                        .speechPlayer(speechPlayer);

        navigationView.startNavigation(options.build());
    }


    /**
     * Build a custom milestone for voice alert.
     *
     * @return
     */
    private Milestone buildOneMilestone() {
        return new StepMilestone.Builder().setTrigger(
                Trigger.eq(TriggerProperty.NEW_STEP, TriggerProperty.TRUE)
        ).build();
    }

    private Milestone buildOneMilestone(int dangerousInfoItemListIndex, NavigationDangerousInfoItem item) {
        class dangerousInfoInstruction extends Instruction {

            @Override
            public String buildInstruction(RouteProgress routeProgress) {
                return item.getVoiceMessage();
            }
        }
        return new StepMilestone.Builder()
                .setIdentifier(dangerousInfoItemListIndex + 100)
                .setInstruction(new dangerousInfoInstruction())
                .setTrigger(
                        Trigger.eq(TriggerProperty.NEW_STEP, TriggerProperty.TRUE)
                ).build();
    }

    /*private Milestone buildOneMilestone(int dangerousInfoItemListIndex, NavigationDangerousInfoItem item) {
        return new RouteMilestone.Builder()
                .setIdentifier(dangerousInfoItemListIndex)
                .setInstruction(new Instruction() {
                    @Override
                    public String buildInstruction(RouteProgress routeProgress) {
                        return item.getVoiceMessage();
                    }
                })
                .setTrigger(
                        Trigger.all(
                                Trigger.eq(TriggerProperty.STEP_INDEX, item.getStepIndex()),
                                Trigger.gt(TriggerProperty.STEP_DISTANCE_TOTAL_METERS, 200),
                                Trigger.lte(TriggerProperty.STEP_DISTANCE_TRAVELED_METERS, item.getDistanceToStep()))
                ).build();
    }*/


    /**
     * @param safetyLevelString
     * @return
     */
    private void initAllDangerousInfoItem(List<String> safetyLevelString) {


        List<Integer> dangerousPointIndexList = getDangerousPointIndexFromCurrentRoute(safetyLevelString);

        List<Point> stepsPointList = getTheStepPointsFromCurrentRoute();

        for (int i = 0; i <= dangerousPointIndexList.size() - 1; i++) {
            Point dangerousPoint = pointsOfRoute.get(dangerousPointIndexList.get(i));
            int milestoneStepIndex = getStepIndexOfDangerousPoint(dangerousPoint, stepsPointList);
            double distanceBetweenStepIndexAndDangerousPoint = getDistanceBetweenDangerousPointAndStepPoint(milestoneStepIndex, dangerousPoint, stepsPointList);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(voiceMessagesList.get(dangerousPointIndexList.get(i)));
            stringBuilder.append("in ");
            stringBuilder.append((int) distanceBetweenStepIndexAndDangerousPoint);
            stringBuilder.append(" meters, ");
            String voiceMessage = stringBuilder.toString();
            NavigationDangerousInfoItem item = new NavigationDangerousInfoItem(milestoneStepIndex, distanceBetweenStepIndexAndDangerousPoint, voiceMessage);
            if ((navigationDangerousInfoItemList.size() == 0) || (navigationDangerousInfoItemList != null && item.getStepIndex() != navigationDangerousInfoItemList.get(navigationDangerousInfoItemList.size() - 1).getStepIndex())) {
                navigationDangerousInfoItemList.add(item);
            }
        }


    }


    /**
     * Get the nearest step point from the dangerous point.
     *
     * @param dangerousPoint
     * @param stepPoints
     * @return
     *//*
    private Point getTheNearestStepPointOfDangerousPoint(Point dangerousPoint, List<Point> stepPoints) {
        return TurfClassification.nearestPoint(dangerousPoint, stepPoints);
    }*/

    /**
     * Get the distance between two points
     *
     * @param dangerousPoint
     * @param stepPoint
     * @return
     */
    private double calculateDistanceBetweenTwoPoint(Point dangerousPoint, Point stepPoint) {
        return TurfMeasurement.distance(dangerousPoint, stepPoint, TurfConstants.UNIT_METRES);
    }

    private int getStepIndexOfDangerousPoint(Point dangerousPoint, List<Point> stepsPointList) {
        int stepIndex = 0;
        List<LegStep> legSteps = directionsRoute.legs().get(0).steps();

        for (int i = 0; i <= stepsPointList.size() - 2; i++) {
            double distanceToCurrentStepIndex = calculateDistanceBetweenTwoPoint(dangerousPoint, stepsPointList.get(i));
            double distanceToNextStepIndex = calculateDistanceBetweenTwoPoint(dangerousPoint, stepsPointList.get(i + 1));
            if (distanceToCurrentStepIndex + distanceToNextStepIndex - legSteps.get(i).distance() < 1) {
                stepIndex = i;
            }
        }

        return stepIndex;
    }

    private double getDistanceBetweenDangerousPointAndStepPoint(int stepIndex, Point dangerousPoint, List<Point> stepsPointList) {
        return calculateDistanceBetweenTwoPoint(dangerousPoint, stepsPointList.get(stepIndex));
    }

    /**
     * Get all the point index of dangerous points from the current route
     *
     * @param
     * @return
     *//*
    private List<Integer> getDangerousPointIndexFromCurrentRoute(List<String> safetyLevelString) {
        List<Integer> dangerousPointIndexList = new ArrayList<>();
        for (int i = 0; i <= safetyLevelString.size() - 1; i++) {
            if (safetyLevelString.get(i).equals(DANGEROUS_LEVEL))
                dangerousPointIndexList.add(i);
        }
        return dangerousPointIndexList;
    }*/

    /**
     * Get all the starting points of steps in the route
     *
     * @return
     */
    private List<Point> getTheStepPointsFromCurrentRoute() {
        List<Point> stepPoints = new ArrayList<>();

        List<LegStep> legSteps = directionsRoute.legs().get(0).steps();

        for (LegStep step : legSteps) {
            stepPoints.add(step.maneuver().location());
        }

        return stepPoints;
    }


    private void addAllMilestone() {
        initAllDangerousInfoItem(safetyLevelsList);
        for (int i = 0; i <= navigationDangerousInfoItemList.size() - 1; i++) {
            milestoneList.add(buildOneMilestone(i, navigationDangerousInfoItemList.get(i)));
        }
    }

    private void initializeSpeechPlayer() {
        String english = Locale.US.getLanguage();
        Cache cache = new Cache(new File(getApplication().getCacheDir(), COMPONENT_NAVIGATION_INSTRUCTION_CACHE),
                TEN_MEGABYTE_CACHE_SIZE);
        VoiceInstructionLoader voiceInstructionLoader = new VoiceInstructionLoader(getApplication(),
                Mapbox.getAccessToken(), cache);
        SpeechPlayerProvider speechPlayerProvider = new SpeechPlayerProvider(getApplication(), english, true,
                voiceInstructionLoader);
        speechPlayer = new NavigationSpeechPlayer(speechPlayerProvider);
    }

    private void playAnnouncement(Milestone milestone, String instruction) {
        if (milestone instanceof StepMilestone) {
            SpeechAnnouncement announcement = SpeechAnnouncement.builder()
                    .announcement(instruction)
                    .build();
            speechPlayer.play(announcement);
            System.out.println(instruction + " announcement played");
        }
    }

    @Override
    public void onNavigationReady(boolean isRunning) {
        navigationView.findViewById(R.id.feedbackFab).setVisibility(View.GONE);
        if (directionsRoute != null) {
            startNavigation();
        }
    }

    private static class NavigationActivityLocationCallback
            implements LocationEngineCallback<LocationEngineResult> {

        private final WeakReference<MapNavigationActivity> activityWeakReference;

        NavigationActivityLocationCallback(MapNavigationActivity activity) {
            this.activityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void onSuccess(LocationEngineResult result) {
            MapNavigationActivity activity = activityWeakReference.get();

            if (activity != null) {
                Location location = result.getLastLocation();

                if (location == null) {
                    return;
                }


            }
        }
        @Override
        public void onFailure(@NonNull Exception exception) {
            Log.d("LocationChangeActivity", exception.getLocalizedMessage());
            MapNavigationActivity activity = activityWeakReference.get();
            if (activity != null) {
                Toast.makeText(activity, exception.getLocalizedMessage(),
                        Toast.LENGTH_SHORT).show();
            }
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


    @Override
    public void onMilestoneEvent(RouteProgress routeProgress, String instruction, Milestone milestone) {
        if (milestone.getIdentifier() >= 100) {
            if (navigationDangerousInfoItemList.get(milestone.getIdentifier() - 100).getStepIndex() == routeProgress.currentLegProgress().stepIndex()) {
                playAnnouncement(milestone, instruction);
            }
        }
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
        // Ensure proper shutdown of the SpeechPlayer
        if (speechPlayer != null) {
            speechPlayer.onDestroy();
        }

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

    @Override
    public boolean allowRerouteFrom(Point offRoutePoint) {
        return false;
    }

    @Override
    public void onOffRoute(Point offRoutePoint) {

    }

    @Override
    public void onRerouteAlong(DirectionsRoute directionsRoute) {

    }

    @Override
    public void onFailedReroute(String errorMessage) {

    }

    @Override
    public void onArrival() {

    }
}
