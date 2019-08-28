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
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.Toast;

import com.mancj.materialsearchbar.MaterialSearchBar;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.location.LocationEngineRequest;
import com.mapbox.android.core.location.LocationEngineResult;
import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.MapboxDirections;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.RouteLeg;
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

import com.mapbox.mapboxsdk.style.sources.Source;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;

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
public class MapActivity extends AppCompatActivity implements OnMapReadyCallback, MapboxMap.OnMapClickListener, PermissionsListener {
    // variables for adding location layer
    private MapView mapView;
    private MapboxMap mapboxMap;
    // variables for adding location layer
    private PermissionsManager permissionsManager;
    private LocationComponent locationComponent;
    // variables for calculating and drawing a route
    private DirectionsRoute currentRoute;
    private String safetyLevelResponseString;
    private static final String TAG = "DirectionsActivity";
    private List<Point> pointsOfRoute;
    private MaterialSearchBar originSearchBar;
    private MaterialSearchBar destinationSearchBar;
    private int clickedSearchBarId;
    private MapboxDirections directionsRequestClient;
    private Button colourInfoButton;
    private Button clearAllButton;
    private CameraPosition currentCameraPosition;
    private Point originPoint;

    private int[] colorArr = new int[]{R.color.routeGreen, R.color.routeYellow, R.color.routeRed};
    private static final int REQUEST_CODE_AUTOCOMPLETE = 1;

    private LocationEngine locationEngine;
    private long DEFAULT_INTERVAL_IN_MILLISECONDS = 1000L;
    private long DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_IN_MILLISECONDS * 5;
    // Variables needed to listen to location updates
    private MainActivityLocationCallback callback = new MainActivityLocationCallback(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.access_token));
        setContentView(R.layout.activity_map);
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
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

                mapboxMap.addOnMapClickListener(MapActivity.this);

                currentCameraPosition = mapboxMap.getCameraPosition();
                originPoint = getCurrentLocation();

                //Initialise search bars and buttons
                originSearchBar = findViewById(R.id.origin_search_bar);
                originSearchBar.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        clickedSearchBarId = v.getId();
                        redirectToSearchScreen();
                    }
                });

                destinationSearchBar = findViewById(R.id.destination_search_bar);
                destinationSearchBar.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        clickedSearchBarId = v.getId();
                        redirectToSearchScreen();
                    }
                });

                colourInfoButton = findViewById(R.id.button_colour_info);
                colourInfoButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showColourInfoDialog();
                    }
                });

                clearAllButton = findViewById(R.id.button_clear);
                clearAllButton.setOnClickListener(new View.OnClickListener() {
                    /**
                     * Clear all displayed routes and move the camera back to user's location
                     * @param v
                     */
                    @Override
                    public void onClick(View v) {
                        removeLayersAndResource();
                        originSearchBar.setPlaceHolder(getString(R.string.origin_init_holder));
                        destinationSearchBar.setPlaceHolder(getString(R.string.destination_init_holder));
                        mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(currentCameraPosition));
                        originPoint = getCurrentLocation();
                    }
                });
            }
        });
    }

    private Point getCurrentLocation(){
        return Point.fromLngLat(locationComponent.getLastKnownLocation().getLongitude(),
                locationComponent.getLastKnownLocation().getLatitude());
    }

    /**
     * Redirect to search location screen
     */
    private void redirectToSearchScreen(){
        Intent intent = new PlaceAutocomplete.IntentBuilder()
                .accessToken(Mapbox.getAccessToken())
                .placeOptions(PlaceOptions.builder()
                        .backgroundColor(Color.WHITE)
                        .limit(5)
                        .country("AU")
                        .build(PlaceOptions.MODE_CARDS))
                .build(MapActivity.this);
        startActivityForResult(intent, REQUEST_CODE_AUTOCOMPLETE);
    }

    /**
     * Recenter the camera position and zoom level to display the whole route on the map.
     */
    private void recenterCameraAfterDisplayingRoute(){
        if (mapboxMap != null){
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
     * Receive location result and display the destination and route
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_AUTOCOMPLETE) {
            CarmenFeature selectedLocationCarmenFeature = PlaceAutocomplete.getPlace(data);
            Point destinationPoint;

            if (clickedSearchBarId == R.id.destination_search_bar) {
                destinationSearchBar.setPlaceHolder(selectedLocationCarmenFeature.placeName());

                destinationPoint = Point.fromLngLat(((Point)selectedLocationCarmenFeature.geometry()).longitude(), ((Point)selectedLocationCarmenFeature.geometry()).latitude());

                renderRouteOnMap(originPoint, destinationPoint);

            } else if (clickedSearchBarId == R.id.origin_search_bar) {
                originSearchBar.setPlaceHolder(selectedLocationCarmenFeature.placeName());
                originPoint = Point.fromLngLat(((Point)selectedLocationCarmenFeature.geometry()).longitude(), ((Point)selectedLocationCarmenFeature.geometry()).latitude());
            }
        }
    }


    private void renderRouteOnMap(Point originPoint, Point destination){
        GeoJsonSource destinationSource = mapboxMap.getStyle().getSourceAs("destination-source-id");
        if (destinationSource != null) {
            destinationSource.setGeoJson(Feature.fromGeometry(destination));
        }

        GeoJsonSource originSource = mapboxMap.getStyle().getSourceAs("origin-source-id");
        if (originSource != null && !originPoint.equals(getCurrentLocation())) {
            originSource.setGeoJson(Feature.fromGeometry(originPoint));
        }

        removeLayersAndResource();
        getSimplifiedRoute(originPoint, destination);
    }

    /**
     * Add the destination icon symbol
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

    @SuppressWarnings( {"MissingPermission"})
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
     * @param jsonString
     * @param callback
     */
    private void getSafetyLevel(String jsonString, Callback<ResponseBody> callback){

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
                Log.e(TAG, "No safety level found");
                return;
            }
            try {
                safetyLevelResponseString = response.body().string();
                List<String> safetyLevels = Utils.extractSafetyLevelFromResponseString(safetyLevelResponseString);
                drawRoutePolyLine(safetyLevels);
                recenterCameraAfterDisplayingRoute();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onFailure(Call<ResponseBody> call, Throwable t) {
            Log.e(TAG, "Error: " + t.getMessage());
        }
    };

    /**
     * Get a route with simplified mode of overview from mapbox directions api
     * @param origin
     * @param destination
     */
    private void getSimplifiedRoute(Point origin, Point destination){

        directionsRequestClient = MapboxDirections.builder()
                .origin(origin)
                .destination(destination)
                .overview(DirectionsCriteria.OVERVIEW_SIMPLIFIED)
                .profile(DirectionsCriteria.PROFILE_DRIVING)
                .accessToken(getString(R.string.access_token))
                .build();

        directionsRequestClient.enqueueCall(new Callback<DirectionsResponse>() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                Log.d(TAG, "Response code: " + response.code());
                if (response.body() == null) {
                    Log.e(TAG, "No routes found, make sure you set the right user and access token.");
                    return;
                } else if (response.body().routes().size() < 1) {
                    Log.e(TAG, "No routes found");
                    return;
                }

                currentRoute = response.body().routes().get(0);

                //Collect the coordinates on the route
                pointsOfRoute = getPointsOfRoutes(currentRoute);
                String coordinatesString = Utils.generateCoordinatesJsonString(pointsOfRoute);

                //Request for safety levels of different sections of the returned route.
                getSafetyLevel(coordinatesString,safetyLevelCallback);

            }

            @Override
            public void onFailure(Call<DirectionsResponse> call, Throwable t) {
                Log.e(TAG, "Error: " + t.getMessage());
            }
        });
    }

    /**
     * Draw the polyline section by section
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void drawRoutePolyLine(List<String> safetyLevelList){
        Map safetyLevelMap = Utils.getSafetyLevelColourMap();
        for (int i = 0; i <= pointsOfRoute.size() -2; i++){
            int colourOfSection = (int)safetyLevelMap.get(safetyLevelList.get(i));
            drawOneLegOfRoute(pointsOfRoute.get(i), pointsOfRoute.get(i + 1), colourOfSection);
            //int colorIndex = new Random().nextInt(colorArr.length);
            //drawOneLegOfRoute(pointsOfRoute.get(i), pointsOfRoute.get(i + 1), colorArr[colorIndex]);
        }
    }

    /**
     * Retrieve coordinates from the geometry attributes of the response route.
      * @param directionsRoute
     * @return
     */
    private List<Point> getPointsOfRoutes(DirectionsRoute directionsRoute){
        List<Point> points = new ArrayList<>();
        if (directionsRoute != null){
            String encodedPolyline = directionsRoute.geometry();
            points = PolylineUtils.decode(encodedPolyline, 6);
        }
        return points;
    }

    /**
     * Remove line layers and sources
     */
    private void removeLayersAndResource(){
        if (mapboxMap != null){
            mapboxMap.getStyle(style -> {
                String layerId;
                List<Layer> layers = style.getLayers();
                for (Layer layer: layers){
                    layerId = layer.getId();
                    if (layerId.contains("add-line-layer"))
                        style.removeLayer(layer.getId());
                }

                List<Source> sources = style.getSources();
                for (Source source: sources){
                    if (source.getId().contains("add-line-resource"))
                        style.removeSource(source.getId());
                }
            });
        }
    }

    /**
     * Draw one section of route between two points.
     * @param origin
     * @param destination
     * @param color
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void drawOneLegOfRoute(Point origin, Point destination, int color){
        List<Point> coordinates = new ArrayList<>();
        coordinates.add(origin);
        coordinates.add(destination);

        if (mapboxMap != null){
            mapboxMap.getStyle(style -> {

                LineString lineString = LineString.fromLngLats(coordinates);
                FeatureCollection featureCollection = FeatureCollection.fromFeatures(new Feature[]{Feature.fromGeometry(lineString)});
                String lineSourceID = "add-line-source-" + coordinates.toString() + UUID.randomUUID().toString();
                GeoJsonSource geoJsonSource = new GeoJsonSource(lineSourceID, featureCollection);

                style.addSource(geoJsonSource);
                String lineLayerID = "add-line-layer-" + coordinates.toString() + UUID.randomUUID().toString();
                LineLayer lineLayer = new LineLayer(lineLayerID, lineSourceID);
                style.addLayer(lineLayer.withProperties(
                        lineCap(Property.LINE_CAP_ROUND),
                        lineJoin(Property.LINE_JOIN_ROUND),
                        lineWidth(8f),
                        lineColor(getColor(color)))
                );
            });
        }

    }

    /**
     * Display the dialog of colour information.
     */
    private void showColourInfoDialog(){
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

    @SuppressWarnings( {"MissingPermission"})
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
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}
