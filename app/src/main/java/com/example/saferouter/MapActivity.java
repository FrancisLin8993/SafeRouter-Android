package com.example.saferouter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.Toast;

import com.example.saferouter.network.SafetyLevelApiInterface;
import com.example.saferouter.utils.Utils;
import com.mancj.materialsearchbar.MaterialSearchBar;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.location.LocationEngineRequest;
import com.mapbox.android.core.location.LocationEngineResult;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.utils.PolylineUtils;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.modes.CameraMode;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.plugins.places.autocomplete.PlaceAutocomplete;
import com.mapbox.mapboxsdk.plugins.places.autocomplete.model.PlaceOptions;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineCap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineJoin;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineWidth;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.visibility;

import com.mapbox.mapboxsdk.style.sources.Source;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions;
import com.mapbox.services.android.navigation.v5.location.replay.ReplayRouteLocationEngine;
import com.mapbox.services.android.navigation.v5.milestone.Milestone;
import com.mapbox.services.android.navigation.v5.milestone.MilestoneEventListener;
import com.mapbox.services.android.navigation.v5.milestone.RouteMilestone;
import com.mapbox.services.android.navigation.v5.milestone.Trigger;
import com.mapbox.services.android.navigation.v5.milestone.TriggerProperty;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions;
import com.mapbox.services.android.navigation.v5.navigation.NavigationEventListener;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.turf.TurfClassification;
import com.mapbox.turf.TurfConstants;
import com.mapbox.turf.TurfMeasurement;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

import android.util.Log;

import android.view.View;

/**
 * Activity of the Mapbox related features.
 */
public class MapActivity extends AppCompatActivity implements OnMapReadyCallback, MapboxMap.OnMapClickListener, PermissionsListener, NavigationEventListener, ProgressChangeListener, MilestoneEventListener {
    // variables for adding location layer
    private MapView mapView;
    private MapboxMap mapboxMap;
    // variables for adding location layer
    private PermissionsManager permissionsManager;
    private LocationComponent locationComponent;
    // variables for calculating and drawing a route
    private DirectionsRoute currentRoute;
    private String safetyLevelResponseString;
    private static final String TAG = "MapActivity";
    private List<Point> pointsOfRoute;
    @BindView(R.id.origin_search_bar)
    MaterialSearchBar originSearchBar;
    @BindView(R.id.destination_search_bar)
    MaterialSearchBar destinationSearchBar;
    private int clickedSearchBarId;
    @BindView(R.id.button_colour_info)
    Button colourInfoButton;
    @BindView(R.id.button_clear)
    Button clearAllButton;
    @BindView(R.id.startButton)
    Button startNavigationButton;
    private CameraPosition currentCameraPosition;
    private Point originPoint;
    private Point destinationPoint;
    //private LatLngBounds latLngBoundsMelbourne;
    private MapboxNavigation navigation;
    ReplayRouteLocationEngine replayEngine;
    private LocationEngine locationEngine;
    // Variables needed to listen to location updates
    private MainActivityLocationCallback callback = new MainActivityLocationCallback(this);
    private List<Feature> greenFeatureList = new ArrayList<>();
    private List<Feature> yellowFeatureList = new ArrayList<>();
    private List<Feature> redFeatureList = new ArrayList<>();
    //Constants
    private final int[] SAFETY_LEVEL_COLOURS = new int[]{R.color.routeGreen, R.color.routeYellow, R.color.routeRed};
    private static final int REQUEST_CODE_AUTOCOMPLETE = 1;
    private final long DEFAULT_INTERVAL_IN_MILLISECONDS = 1000L;
    private final long DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_IN_MILLISECONDS * 5;
    private final String SAFE_LEVEL = "1.0";
    private final String MEDIUM_LEVEL = "2.0";
    private final String DANGEROUS_LEVEL = "3.0";
    private final LatLngBounds BBOX_MELBOURNE = new LatLngBounds.Builder()
            .include(new LatLng(-38.2250, 145.5498))
            .include(new LatLng(-37.5401, 144.5532))
            .build();
    private final int AUTO_COMPLETE_LIST_LIMIT = 5;
    private final String PLACE_SEARCH_COUNTRY = "AU";
    private final int DISTANCE_TO_VOICE_ALERT_POINT = 500;
    private final String NO_ROUTES_ERROR_MESSAGE = "No routes found";
    private final String NO_SAFETY_LEVEL_ERROR_MESSAGE = "No safety level found";
    private final Map<String, Integer> SAFETY_LEVEL_COLOUR_MAP = new HashMap<String, Integer>() {
        {
            put(SAFE_LEVEL, R.color.routeGreen);
            put(MEDIUM_LEVEL, R.color.routeYellow);
            put(DANGEROUS_LEVEL, R.color.routeRed);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.access_token));
        setContentView(R.layout.activity_map);
        ButterKnife.bind(this);
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        MapboxNavigationOptions options = MapboxNavigationOptions.builder()
                .isDebugLoggingEnabled(true)
                .build();
        navigation = new MapboxNavigation(this, Mapbox.getAccessToken(), options);
    }

    @Override
    public void onMapReady(@NonNull final MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;
        mapboxMap.setStyle(getString(R.string.navigation_guidance_day), new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                enableLocationComponent(style);

                addDestinationIconSymbolLayer(style);
                addOriginIconSymbolLayer(style);

                /*latLngBoundsMelbourne = new LatLngBounds.Builder()
                        .include(new LatLng(-38.2250, 145.5498))
                        .include(new LatLng(-37.5401, 144.5532))
                        .build();*/

                mapboxMap.addOnMapClickListener(MapActivity.this);
                //Limit the camera inside the latitude and longitude bounds of Greater Melbourne.
                mapboxMap.setLatLngBoundsForCameraTarget(BBOX_MELBOURNE);

                currentCameraPosition = mapboxMap.getCameraPosition();
                originPoint = getCurrentLocation();
            }
        });
    }

    @OnClick(R.id.origin_search_bar)
    public void originSearchBarOnClick(View v) {
        clickedSearchBarId = v.getId();
        redirectToSearchScreen();
    }

    @OnClick(R.id.destination_search_bar)
    public void destinationSearchBarOnClick(View v) {
        clickedSearchBarId = v.getId();
        redirectToSearchScreen();
    }

    @OnClick(R.id.button_colour_info)
    public void colorButtonOnClick() {
        showColourInfoDialog();
    }

    @OnClick(R.id.startButton)
    public void startNavigationButtonOnClick() {

        navigation.addNavigationEventListener(this);
        navigation.addProgressChangeListener(this);
        navigation.addMilestoneEventListener(this);

        replayEngine = new ReplayRouteLocationEngine();
        replayEngine.assign(currentRoute);
        navigation.setLocationEngine(replayEngine);
        navigation.startNavigation(currentRoute);

        boolean simulateRoute = true;
        NavigationLauncherOptions options = NavigationLauncherOptions.builder()
                .directionsRoute(currentRoute)
                .shouldSimulateRoute(simulateRoute)
                .build();
        // Call this method with Context from within an Activity
        NavigationLauncher.startNavigation(MapActivity.this, options);


    }

    /**
     * Clear all displayed routes and move the camera back to user's location
     */
    @OnClick(R.id.button_clear)
    public void clearButtonOnClick() {
        removeLayersAndResource();
        originSearchBar.setPlaceHolder(getString(R.string.origin_init_holder));
        destinationSearchBar.setPlaceHolder(getString(R.string.destination_init_holder));
        mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(currentCameraPosition));
        originPoint = getCurrentLocation();
        destinationPoint = null;
        hideMarker("origin-symbol-layer-id");
        hideMarker("destination-symbol-layer-id");
        startNavigationButton.setEnabled(false);
        startNavigationButton.setBackgroundResource(R.color.mapboxGrayLight);
    }

    /**
     * Get user's current location
     *
     * @return
     */
    private Point getCurrentLocation() {
        return Point.fromLngLat(locationComponent.getLastKnownLocation().getLongitude(),
                locationComponent.getLastKnownLocation().getLatitude());
    }

    /**
     * Redirect to search location screen
     */
    private void redirectToSearchScreen() {
        Intent intent = new PlaceAutocomplete.IntentBuilder()
                .accessToken(Mapbox.getAccessToken())
                .placeOptions(PlaceOptions.builder()
                        .backgroundColor(Color.WHITE)
                        .limit(AUTO_COMPLETE_LIST_LIMIT)
                        .country(PLACE_SEARCH_COUNTRY)
                        .bbox(144.5532, -38.2250, 145.5498, -37.5401)
                        .build(PlaceOptions.MODE_CARDS))
                .build(MapActivity.this);
        startActivityForResult(intent, REQUEST_CODE_AUTOCOMPLETE);
    }

    /**
     * Recenter the camera position and zoom level to display the whole route on the map.
     */
    private void recenterCameraAfterDisplayingRoute() {
        if (mapboxMap != null) {
            LatLng startPoint = new LatLng(pointsOfRoute.get(0).latitude(), pointsOfRoute.get(0).longitude());
            LatLng destination = new LatLng(pointsOfRoute.get(pointsOfRoute.size() - 1).latitude(), pointsOfRoute.get(pointsOfRoute.size() - 1).longitude());

            LatLngBounds latLngBounds = new LatLngBounds.Builder()
                    .include(startPoint)
                    .include(destination)
                    .build();

            mapboxMap.animateCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 200));
        }
    }

    /**
     * Build a custom milstone for voice alert.
     * @param stepIndex
     * @return
     */
    private Milestone buildOneMilestone(int stepIndex) {
        return new RouteMilestone.Builder()
                .setIdentifier(stepIndex)
                .setTrigger(
                        Trigger.all(
                                Trigger.eq(TriggerProperty.STEP_INDEX, stepIndex - 1),
                                Trigger.lte(TriggerProperty.STEP_DISTANCE_REMAINING_METERS, DISTANCE_TO_VOICE_ALERT_POINT)
                        )
                ).build();
    }


    /**
     * Add all milestones in navigation
     * @param milestoneStepIndexList
     */
    private void addAllMilestone(List<Integer> milestoneStepIndexList){
        for (Integer stepIndex : milestoneStepIndexList){
            navigation.addMilestone(buildOneMilestone(stepIndex));
        }
    }

    /**
     *
     * @param safetyLevelString
     * @return
     */
    private List<Integer> getMilstoneStepIndex(List<String> safetyLevelString) {
        List<Integer> milestoneStepIndexList = new ArrayList<>();

        List<Integer> dangerousPointIndexList = getDangerousPointIndexFromCurrentRoute(safetyLevelString);

        List<Point> stepsPointList = getTheStepPointsFromCurrentRoute();

        for (Integer integer : dangerousPointIndexList) {
            Point dangerousPoint = pointsOfRoute.get(integer);
            Point nearestStepPoint = getTheNearestStepPointOfDangerousPoint(dangerousPoint, stepsPointList);
            Map<Integer, Double> stepIndexAndDistanceMap = getStepIndexWhereDangerousPointIn(dangerousPoint, nearestStepPoint, stepsPointList);
            int milesonteStepIndex = evaluateStepIndex(dangerousPoint, stepIndexAndDistanceMap);

            milestoneStepIndexList.add(milesonteStepIndex);

        }

        return milestoneStepIndexList;
    }

    /**
     * Get the nearest step point from the dangerous point.
     *
     * @param dangerousPoint
     * @param stepPoints
     * @return
     */
    private Point getTheNearestStepPointOfDangerousPoint(Point dangerousPoint, List<Point> stepPoints) {
        return TurfClassification.nearestPoint(dangerousPoint, stepPoints);
    }

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

    /**
     * Get the nearest step index and the distance from a dangerous point.
     *
     * @param dangerousPoint
     * @param stepPoints
     * @return
     */
    private Map<Integer, Double> getStepIndexWhereDangerousPointIn(Point dangerousPoint, Point nearestStepPoint, List<Point> stepPoints) {
        Map<Integer, Double> stepIndexAndDistanceMap = new HashMap<>();

        double distance = calculateDistanceBetweenTwoPoint(dangerousPoint, nearestStepPoint);
        int stepIndex = stepPoints.indexOf(nearestStepPoint);

        stepIndexAndDistanceMap.put(stepIndex, distance);
        return stepIndexAndDistanceMap;
    }

    /**
     * evaluate whether a dangerous point is in the given step. If true, yes. If not, in the previous step.
     *
     * @param dangerousPoint
     * @param stepIndexAndDistanceMap
     * @return
     */
    private int evaluateStepIndex(Point dangerousPoint, Map stepIndexAndDistanceMap) {

        int stepIndex = (int) stepIndexAndDistanceMap.keySet().toArray()[0];

        double distanceofNextStep = calculateDistanceBetweenTwoPoint(dangerousPoint, pointsOfRoute.get(stepIndex + 1));

        LegStep step = currentRoute.legs().get(0).steps().get(stepIndex);

        if (distanceofNextStep + (double) stepIndexAndDistanceMap.get(stepIndex) == step.distance())
            return stepIndex;
        else
            return stepIndex - 1;
    }


    /**
     * Get all the point index of dangerous points from the current route
     *
     * @param safetyLevelString
     * @return
     */
    private List<Integer> getDangerousPointIndexFromCurrentRoute(List<String> safetyLevelString) {
        List<Integer> dangerousPointIndexList = new ArrayList<>();
        for (String string : safetyLevelString) {
            if (string.equals("3.0"))
                dangerousPointIndexList.add(safetyLevelString.indexOf(string));
        }
        return dangerousPointIndexList;
    }

    /**
     * Get all the starting points of steps in the route
     *
     * @return
     */
    private List<Point> getTheStepPointsFromCurrentRoute() {
        List<Point> stepPoints = new ArrayList<>();

        List<LegStep> legSteps = currentRoute.legs().get(0).steps();

        for (LegStep step : legSteps) {
            stepPoints.add(step.maneuver().location());
        }

        return stepPoints;
    }


    /**
     * Receive location result and display the destination and route
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_AUTOCOMPLETE) {
            CarmenFeature selectedLocationCarmenFeature = PlaceAutocomplete.getPlace(data);

            if (clickedSearchBarId == R.id.destination_search_bar) {
                destinationSearchBar.setPlaceHolder(selectedLocationCarmenFeature.placeName());

                destinationPoint = Point.fromLngLat(((Point) selectedLocationCarmenFeature.geometry()).longitude(), ((Point) selectedLocationCarmenFeature.geometry()).latitude());

                if (destinationPoint.equals(originPoint))
                    Toast.makeText(MapActivity.this, "The destination and the starting point are the same.", Toast.LENGTH_LONG).show();
                else
                    renderRouteOnMap(originPoint, destinationPoint);

            } else if (clickedSearchBarId == R.id.origin_search_bar) {
                originSearchBar.setPlaceHolder(selectedLocationCarmenFeature.placeName());
                originPoint = Point.fromLngLat(((Point) selectedLocationCarmenFeature.geometry()).longitude(), ((Point) selectedLocationCarmenFeature.geometry()).latitude());
                LatLng originLatLng = new LatLng(originPoint.latitude(), originPoint.longitude());
                CameraPosition newCameraPosition = new CameraPosition.Builder().target(originLatLng).build();
                mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(newCameraPosition));
                setOriginPointMarkerSsource(originPoint);
                if (!originPoint.equals(getCurrentLocation()))
                    showMarker("origin-symbol-layer-id");
                if (destinationPoint != null)
                    renderRouteOnMap(originPoint, destinationPoint);
            }
        }
    }

    /**
     * Set the marker geojson source of the destination
     *
     * @param destination
     */
    private void setDestinationMarkerSource(Point destination) {
        GeoJsonSource destinationSource = mapboxMap.getStyle().getSourceAs("destination-source-id");
        if (destinationSource != null) {
            destinationSource.setGeoJson(Feature.fromGeometry(destination));
        }
    }

    /**
     * Set the marker geojson source of the origin point
     *
     * @param originPoint
     */
    private void setOriginPointMarkerSsource(Point originPoint) {
        GeoJsonSource originSource = mapboxMap.getStyle().getSourceAs("origin-source-id");
        if (originSource != null && !originPoint.equals(getCurrentLocation())) {
            originSource.setGeoJson(Feature.fromGeometry(originPoint));
        }

    }

    /**
     * Hide a marker
     *
     * @param layerId
     */
    private void hideMarker(String layerId) {
        Layer layer = mapboxMap.getStyle().getLayer(layerId);
        if (layer != null)
            layer.setProperties(visibility(Property.NONE));
    }

    /**
     * Show a marker on the map
     *
     * @param layerId
     */
    private void showMarker(String layerId) {
        Layer layer = mapboxMap.getStyle().getLayer(layerId);
        if (layer != null)
            layer.setProperties(visibility(Property.VISIBLE));
    }


    /**
     * Render the multi-coloured polyline of the route on the map
     *
     * @param originPoint
     * @param destination
     */
    private void renderRouteOnMap(Point originPoint, Point destination) {

        setDestinationMarkerSource(destination);
        setOriginPointMarkerSsource(originPoint);

        removeLayersAndResource();
        getRouteFromMapbox(originPoint, destination);
        showMarker("destination-symbol-layer-id");
    }

    /**
     * Add the destination icon symbol
     *
     * @param loadedMapStyle
     */
    private void addDestinationIconSymbolLayer(@NonNull Style loadedMapStyle) {
        loadedMapStyle.addImage("destination-icon-id",
                BitmapFactory.decodeResource(this.getResources(), R.drawable.mapbox_marker_icon_default));
        GeoJsonSource geoJsonSource = new GeoJsonSource("destination-source-id");
        loadedMapStyle.addSource(geoJsonSource);
        SymbolLayer destinationSymbolLayer = new SymbolLayer("destination-symbol-layer-id", "destination-source-id");
        destinationSymbolLayer.withProperties(
                iconImage("destination-icon-id"),
                iconAllowOverlap(true),
                iconIgnorePlacement(true)
        );
        loadedMapStyle.addLayer(destinationSymbolLayer);
    }

    /**
     * Add the origin point icon symbol
     *
     * @param loadedMapStyle
     */
    private void addOriginIconSymbolLayer(@NonNull Style loadedMapStyle) {
        loadedMapStyle.addImage("origin-icon-id",
                BitmapFactory.decodeResource(this.getResources(), R.drawable.mapbox_marker_icon_default));
        GeoJsonSource geoJsonSource = new GeoJsonSource("origin-source-id");
        loadedMapStyle.addSource(geoJsonSource);
        SymbolLayer originSymbolLayer = new SymbolLayer("origin-symbol-layer-id", "origin-source-id");
        originSymbolLayer.withProperties(
                iconImage("origin-icon-id"),
                iconAllowOverlap(true),
                iconIgnorePlacement(true)
        );
        loadedMapStyle.addLayer(originSymbolLayer);
    }

    @SuppressWarnings({"MissingPermission"})
    @Override
    public boolean onMapClick(@NonNull LatLng point) {

        /*Point destinationPoint = Point.fromLngLat(point.getLongitude(), point.getLatitude());
        Point originPoint = Point.fromLngLat(locationComponent.getLastKnownLocation().getLongitude(),
                locationComponent.getLastKnownLocation().getLatitude());

        GeoJsonSource source = mapboxMap.getStyle().getSourceAs("destination-source-id");
        if (source != null) {
            source.setGeoJson(Feature.fromGeometry(destinationPoint));
        }

        getRoute(originPoint, destinationPoint);

        button.setEnabled(true);
        button.setBackgroundResource(R.color.mapboxBlue);*/
        return true;
    }

    /**
     * Request for safety level numbers from external api.
     *
     * @param jsonString
     * @param callback
     */
    private void getSafetyLevel(String jsonString, Callback<ResponseBody> callback) {

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(SafetyLevelApiInterface.BASE_URL)
                .build();

        SafetyLevelApiInterface service = retrofit.create(SafetyLevelApiInterface.class);

        RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonString);
        Call<ResponseBody> responseBodyCall = service.getSafetyLevel(body);

        responseBodyCall.enqueue(callback);
    }

    Callback<ResponseBody> safetyLevelCallback = new Callback<ResponseBody>() {
        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
            Log.d(TAG, "Response code: " + response.code());
            if (response.body() == null) {
                Log.e(TAG, NO_SAFETY_LEVEL_ERROR_MESSAGE);
                Toast.makeText(MapActivity.this, NO_SAFETY_LEVEL_ERROR_MESSAGE, Toast.LENGTH_LONG).show();
                return;
            }
            try {
                safetyLevelResponseString = response.body().string();
                List<String> safetyLevels = Utils.extractSafetyLevelFromResponseString(safetyLevelResponseString);
                drawRoutePolyLine(safetyLevels);
                recenterCameraAfterDisplayingRoute();
                addAllMilestone(getMilstoneStepIndex(safetyLevels));
                startNavigationButton.setEnabled(true);
                startNavigationButton.setBackgroundResource(R.color.mapboxBlue);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onFailure(Call<ResponseBody> call, Throwable t) {
            Log.e(TAG, "Error: " + t.getMessage());
            Toast.makeText(MapActivity.this, "No safety levels found", Toast.LENGTH_LONG).show();

        }
    };

    /**
     * Get a route with simplified mode of overview from mapbox directions api
     *
     * @param origin
     * @param destination
     */
    private void getRouteFromMapbox(Point origin, Point destination) {
        NavigationRoute.builder(this)
                .accessToken(Mapbox.getAccessToken())
                .origin(origin)
                .destination(destination)
                .build()
                .getRoute(new Callback<DirectionsResponse>() {
                    @Override
                    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                        Log.d(TAG, "Response code: " + response.code());
                        if (response.body() == null) {
                            Log.e(TAG, NO_ROUTES_ERROR_MESSAGE);
                            Toast.makeText(MapActivity.this, NO_ROUTES_ERROR_MESSAGE, Toast.LENGTH_LONG).show();
                            return;
                        } else if (response.body().routes().size() < 1) {
                            Log.e(TAG, NO_ROUTES_ERROR_MESSAGE);
                            Toast.makeText(MapActivity.this, NO_ROUTES_ERROR_MESSAGE, Toast.LENGTH_LONG).show();
                            return;
                        }

                        currentRoute = response.body().routes().get(0);

                        //Collect the coordinates on the route
                        pointsOfRoute = getPointsOfRoutes(currentRoute);
                        String coordinatesString = Utils.generateCoordinatesJsonString(pointsOfRoute);

                        //Request for safety levels of different sections of the returned route.
                        getSafetyLevel(coordinatesString, safetyLevelCallback);

                    }

                    @Override
                    public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
                        Log.e(TAG, "Error: " + throwable.getMessage());
                    }


                });
    }

    /**
     * Draw the polyline section by section
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void drawRoutePolyLine(List<String> safetyLevelList) {
        Map safetyLevelMap = SAFETY_LEVEL_COLOUR_MAP;
        for (int i = 0; i <= pointsOfRoute.size() - 2; i++) {
            int colourOfSection = (int) safetyLevelMap.get(safetyLevelList.get(i));
            addLegSourceOfRoute(pointsOfRoute.get(i), pointsOfRoute.get(i + 1), colourOfSection);
        }
        for (int i = 0; i <= SAFETY_LEVEL_COLOURS.length - 1; i++) {
            addRouteLayer(SAFETY_LEVEL_COLOURS[i]);
        }
    }

    /**
     * Retrieve coordinates from the geometry attributes of the response route.
     *
     * @param directionsRoute
     * @return
     */
    private List<Point> getPointsOfRoutes(DirectionsRoute directionsRoute) {
        List<Point> points = new ArrayList<>();
        if (directionsRoute != null) {
            String encodedPolyline = directionsRoute.geometry();
            points = PolylineUtils.decode(encodedPolyline, 6);
        }
        return points;
    }

    /**
     * Remove line layers and sources
     */
    private void removeLayersAndResource() {
        if (mapboxMap != null) {
            mapboxMap.getStyle(style -> {
                String layerId;
                List<Layer> layers = style.getLayers();
                for (Layer layer : layers) {
                    layerId = layer.getId();
                    if (layerId.contains("line-layer"))
                        style.removeLayer(layer.getId());
                }

                List<Source> sources = style.getSources();
                for (Source source : sources) {
                    if (source.getId().contains("line-source"))
                        style.removeSource(source.getId());
                }
            });
            removeAllFeatures();
        }
    }

    /**
     * Remove all the features of the multi coloured route
     */
    private void removeAllFeatures() {
        greenFeatureList.clear();
        yellowFeatureList.clear();
        redFeatureList.clear();
    }

    /**
     * Add a layer of one of the colours of the route
     *
     * @param colour
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void addRouteLayer(int colour) {
        if (mapboxMap != null) {
            mapboxMap.getStyle(style -> {

                String lineSourceID = getLineSourceIdFromColour(colour);
                String lineLayerID = "line-layer-" + UUID.randomUUID().toString();
                LineLayer lineLayer = new LineLayer(lineLayerID, lineSourceID);
                style.addLayer(lineLayer.withProperties(
                        lineCap(Property.LINE_CAP_ROUND),
                        lineJoin(Property.LINE_JOIN_ROUND),
                        lineWidth(8f),
                        lineColor(getColor(colour)))
                );
            });
        }
    }

    /**
     * Add geojson source of a step of a route
     *
     * @param origin
     * @param destination
     * @param colour
     */
    private void addLegSourceOfRoute(Point origin, Point destination, int colour) {
        List<Point> coordinates = new ArrayList<>();
        coordinates.add(origin);
        coordinates.add(destination);

        if (mapboxMap != null) {
            mapboxMap.getStyle(style -> {

                LineString lineString = LineString.fromLngLats(coordinates);
                String lineSourceID = getLineSourceIdFromColour(colour);
                List<Feature> featureList = getFeatureListFromColour(colour);
                FeatureCollection featureCollection;
                featureList.add(Feature.fromGeometry(lineString));
                featureCollection = FeatureCollection.fromFeatures(featureList);

                if (!isLineSourceExist(lineSourceID)) {
                    GeoJsonSource geoJsonSource = new GeoJsonSource(lineSourceID, featureCollection);
                    style.addSource(geoJsonSource);
                } else {
                    GeoJsonSource geoJsonSource = (GeoJsonSource) style.getSource(lineSourceID);
                    geoJsonSource.setGeoJson(featureCollection);
                }

            });
        }
    }

    /**
     * Get the corresponding line source id from the colour
     *
     * @param colour
     * @return
     */
    private String getLineSourceIdFromColour(int colour) {
        String lineSourceId = "";
        if (colour == R.color.routeGreen)
            lineSourceId = "green-line-source";
        else if (colour == R.color.routeYellow)
            lineSourceId = "yellow-line-source";
        else if (colour == R.color.routeRed)
            lineSourceId = "red-line-source";
        return lineSourceId;
    }

    /**
     * Get the corresponding feature list from the colour
     *
     * @param colour
     * @return
     */
    private List<Feature> getFeatureListFromColour(int colour) {
        List<Feature> featureList = null;
        if (colour == R.color.routeGreen)
            featureList = greenFeatureList;
        else if (colour == R.color.routeYellow)
            featureList = yellowFeatureList;
        else if (colour == R.color.routeRed)
            featureList = redFeatureList;
        return featureList;
    }

    /**
     * Evaluate whether a line source exists
     *
     * @param lineSourceID
     * @return
     */
    private boolean isLineSourceExist(String lineSourceID) {
        return mapboxMap.getStyle().getSource(lineSourceID) != null;
    }

    /**
     * Display the dialog of colour information.
     */
    private void showColourInfoDialog() {
        Dialog dialog = new Dialog(MapActivity.this);
        dialog.setContentView(R.layout.dialog_view);
        Button dialogButton = dialog.findViewById(R.id.buttonOk);
        dialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    @SuppressWarnings({"MissingPermission"})
    private void enableLocationComponent(@NonNull Style loadedMapStyle) {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            // Activate the MapboxMap LocationComponent to show user location
            // Adding in LocationComponentOptions is also an optional parameter
            locationComponent = mapboxMap.getLocationComponent();
            locationComponent.activateLocationComponent(this, loadedMapStyle);
            locationComponent.setLocationComponentEnabled(true);
            // Set the component's camera mode
            locationComponent.setCameraMode(CameraMode.TRACKING);
            locationComponent.setRenderMode(RenderMode.COMPASS);
            initLocationEngine();
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    @SuppressLint("MissingPermission")
    private void initLocationEngine() {
        locationEngine = LocationEngineProvider.getBestLocationEngine(this);

        LocationEngineRequest request = new LocationEngineRequest.Builder(DEFAULT_INTERVAL_IN_MILLISECONDS)
                .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                .setMaxWaitTime(DEFAULT_MAX_WAIT_TIME).build();

        locationEngine.requestLocationUpdates(request, callback, getMainLooper());
        locationEngine.getLastLocation(callback);
    }

    @Override
    public void onRunning(boolean running) {

    }

    @Override
    public void onProgressChange(Location location, RouteProgress routeProgress) {

    }

    @Override
    public void onMilestoneEvent(RouteProgress routeProgress, String instruction, Milestone milestone) {

    }

    private static class MainActivityLocationCallback
            implements LocationEngineCallback<LocationEngineResult> {

        private final WeakReference<MapActivity> activityWeakReference;

        MainActivityLocationCallback(MapActivity activity) {
            this.activityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void onSuccess(LocationEngineResult result) {
            MapActivity activity = activityWeakReference.get();

            if (activity != null) {
                Location location = result.getLastLocation();

                if (location == null) {
                    return;
                }

                // Pass the new location to the Maps SDK's LocationComponent
                if (activity.mapboxMap != null && result.getLastLocation() != null) {
                    activity.mapboxMap.getLocationComponent().forceLocationUpdate(result.getLastLocation());
                }
            }
        }


        @Override
        public void onFailure(@NonNull Exception exception) {
            Log.d("LocationChangeActivity", exception.getLocalizedMessage());
            MapActivity activity = activityWeakReference.get();
            if (activity != null) {
                Toast.makeText(activity, exception.getLocalizedMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(this, R.string.user_location_permission_explanation, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            enableLocationComponent(mapboxMap.getStyle());
        } else {
            Toast.makeText(this, R.string.user_location_permission_not_granted, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        navigation.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}
