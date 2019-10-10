package com.example.saferouter;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;

import java.io.IOException;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.Button;
import android.widget.Toast;

import com.example.saferouter.model.RouteInfoItem;
import com.example.saferouter.network.RoutingAlgorithmApiInterface;
import com.example.saferouter.network.SafetyLevelApiInterface;
import com.example.saferouter.utils.Utils;
import com.mancj.materialsearchbar.MaterialSearchBar;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.location.LocationEngineRequest;
import com.mapbox.android.core.location.LocationEngineResult;
import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.api.matching.v5.models.MapMatchingResponse;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.LocationComponentOptions;
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

import static com.example.saferouter.utils.CommonConstants.DEFAULT_INTERVAL_IN_MILLISECONDS;
import static com.example.saferouter.utils.CommonConstants.DEFAULT_MAX_WAIT_TIME;
import static com.example.saferouter.utils.CommonConstants.SAFETY_LEVEL_COLOURS;
import static com.example.saferouter.utils.CommonConstants.SAFETY_LEVEL_COLOUR_MAP;
import static com.example.saferouter.utils.Utils.calculateDistanceBetweenTwoPoint;
import static com.example.saferouter.utils.Utils.getDangerousPointIndexFromCurrentRoute;
import static com.mapbox.core.constants.Constants.PRECISION_6;
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
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;


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

import org.apache.commons.lang3.StringUtils;

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
    private List<DirectionsRoute> currentRouteList;
    private List<List<String>> safetyLevelsListOfRoutes;
    private List<List<String>> navigationRatingsListOfRoutes;
    private List<List<String>> voiceMessagesListOfRoutes;
    private static final String TAG = "MapActivity";
    private List<Point> pointsOfCurrentRoute;
    private List<List<Point>> pointsOfRouteList = new ArrayList<>();
    @BindView(R.id.origin_search_bar)
    MaterialSearchBar originSearchBar;
    @BindView(R.id.destination_search_bar)
    MaterialSearchBar destinationSearchBar;
    private int clickedSearchBarId;
    @BindView(R.id.startButton)
    Button startNavigationButton;
    @BindView(R.id.button_view_alternatives)
    Button viewAlternativesButton;
    @BindView(R.id.button_go_to_map)
    Button goToMapButton;
    @BindView(R.id.loading_progress)
    View progressBar;
    @BindView(R.id.about_page_fab)
    FloatingActionButton aboutPageButton;
    @BindView(R.id.recenter_location_fab)
    FloatingActionButton recenterLocationButton;
    private Point originPoint;
    private Point destinationPoint;
    //private LatLngBounds latLngBoundsMelbourne;
    private LocationEngine locationEngine;
    // Variables needed to listen to location updates
    private MainActivityLocationCallback locationCallback = new MainActivityLocationCallback(this);
    private List<Feature> greenFeatureList = new ArrayList<>();
    private List<Feature> yellowFeatureList = new ArrayList<>();
    private List<Feature> redFeatureList = new ArrayList<>();
    //Constants
    private static final int REQUEST_CODE_AUTOCOMPLETE = 1;

    private final LatLngBounds BBOX_MELBOURNE = new LatLngBounds.Builder()
            .include(new LatLng(-38.2250, 145.5498))
            .include(new LatLng(-37.5401, 144.5532))
            .build();
    private final Point MELBOURNE = Point.fromLngLat(144.9631, 37.8136);
    private static final int AUTO_COMPLETE_LIST_LIMIT = 5;
    private static final String PLACE_SEARCH_COUNTRY = "AU";
    private final String NO_ROUTES_ERROR_MESSAGE = "No routes found";
    private final String NO_SAFETY_LEVEL_ERROR_MESSAGE = "No safety level found";
    private static final String ROUTE_LAYER_ID = "route-layer-id";
    private static final String ROUTE_SOURCE_ID = "route-source-id";
    private static final int NO_ROUTE_SELECTED = -1;
    private static final String RECOMMENDATION_TAG = "SafeRouter Recommendation";

    private List<String> routeSafetyScoreStringList = new ArrayList<>();

    //Select Routes from list
    private DirectionsRoute selectedRoute;
    private int selectedRouteNo = NO_ROUTE_SELECTED;
    private int unselectedRouteNo1;
    private int unselectedRouteNo2;
    private int unselectedRouteNo3;

    //Recycler View
    private List<RouteInfoItem> routeInfoItemList = new ArrayList<>();
    @BindView(R.id.route_recycler_view)
    RecyclerView routeInfoRecyclerView;
    private RouteInfoItemAdapter routeInfoItemAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.access_token));
        setContentView(R.layout.activity_map);
        ButterKnife.bind(this);
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

                /*latLngBoundsMelbourne = new LatLngBounds.Builder()
                        .include(new LatLng(-38.2250, 145.5498))
                        .include(new LatLng(-37.5401, 144.5532))
                        .build();*/

                mapboxMap.addOnMapClickListener(MapActivity.this);
                //Limit the camera inside the latitude and longitude bounds of Greater Melbourne.
                mapboxMap.setLatLngBoundsForCameraTarget(BBOX_MELBOURNE);

                initRecyclerView();
                initSource(style);
            }
        });

    }

    /**
     * Initialize recycler view showing the route options.
     */
    private void initRecyclerView() {
        routeInfoItemAdapter = new RouteInfoItemAdapter(routeInfoItemList);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        routeInfoRecyclerView.setLayoutManager(layoutManager);
        routeInfoRecyclerView.setItemAnimator(new DefaultItemAnimator());
        routeInfoRecyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));
        routeInfoRecyclerView.setAdapter(routeInfoItemAdapter);
        routeInfoRecyclerView.addOnItemTouchListener(new RecyclerTouchListener(getApplicationContext(), routeInfoRecyclerView, new RecyclerTouchListener.ClickListner() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View view, int position) {

                RouteInfoItem item = routeInfoItemList.get(position);
                int routeNumber = Integer.parseInt(StringUtils.right(item.getRouteNo(), 1));
                selectedRouteNo = routeNumber - 1;
                selectedRoute = currentRouteList.get(selectedRouteNo);
                pointsOfCurrentRoute = Utils.getPointsOfRoutes(selectedRoute);

                if (currentRouteList.size() >= 2) {

                    if (selectedRouteNo == 0) {
                        unselectedRouteNo1 = 1;
                    }
                    if (selectedRouteNo == 1) {
                        unselectedRouteNo1 = 0;
                    }

                    if (currentRouteList.size() >= 3) {

                        if (selectedRouteNo == 0) {
                            unselectedRouteNo1 = 1;
                            unselectedRouteNo2 = 2;
                        }
                        if (selectedRouteNo == 1) {
                            unselectedRouteNo1 = 0;
                            unselectedRouteNo2 = 2;
                        }
                        if (selectedRouteNo == 2) {
                            unselectedRouteNo1 = 0;
                            unselectedRouteNo2 = 1;
                        }

                        if (currentRouteList.size() >= 4) {

                            if (selectedRouteNo == 0) {
                                unselectedRouteNo1 = 1;
                                unselectedRouteNo2 = 2;
                                unselectedRouteNo3 = 3;
                            }
                            if (selectedRouteNo == 1) {
                                unselectedRouteNo1 = 0;
                                unselectedRouteNo2 = 2;
                                unselectedRouteNo3 = 3;
                            }
                            if (selectedRouteNo == 2) {
                                unselectedRouteNo1 = 1;
                                unselectedRouteNo2 = 0;
                                unselectedRouteNo3 = 3;
                            }
                            if (selectedRouteNo == 3) {
                                unselectedRouteNo1 = 1;
                                unselectedRouteNo2 = 2;
                                unselectedRouteNo3 = 0;
                            }
                        }

                    }

                }

                onItemSelect(selectedRouteNo);
            }
        }));
    }

    /**
     * Show or hide the progress bar
     *
     * @param show
     */
    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        originSearchBar.setEnabled(!show);
        destinationSearchBar.setEnabled(!show);
        viewAlternativesButton.setEnabled(!show);
        routeInfoRecyclerView.setClickable(!show);
        startNavigationButton.setEnabled(!show);
        goToMapButton.setEnabled(!show);
    }

    /**
     * Show or hide the list of the route
     *
     * @param show
     */
    private void showRouteList(boolean show) {
        mapView.setVisibility(show ? View.GONE : View.VISIBLE);
        routeInfoRecyclerView.setVisibility(show ? View.VISIBLE : View.GONE);
        viewAlternativesButton.setVisibility(show ? View.GONE : View.VISIBLE);
        if (selectedRouteNo != NO_ROUTE_SELECTED) {
            goToMapButton.setVisibility(show ? View.VISIBLE : View.GONE);
        } else {
            goToMapButton.setVisibility(show ? View.GONE : View.VISIBLE);
        }
        startNavigationButton.setVisibility(show ? View.GONE : View.VISIBLE);
        if (show){
            findViewById(R.id.about_page_fab).setVisibility(View.GONE);
            findViewById(R.id.recenter_location_fab).setVisibility(View.GONE);
        } else {
            findViewById(R.id.about_page_fab).setVisibility(View.VISIBLE);
            findViewById(R.id.recenter_location_fab).setVisibility(View.VISIBLE);
        }

    }

    /**
     * Initialize data in each item in the recycler view.
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void initRouteItemData() {
        for (int i = 0; i <= currentRouteList.size() - 1; i++) {
            RouteInfoItem item = new RouteInfoItem();

            String routeNo = "Route No. " + (i + 1);
            item.setRouteNo(routeNo);

            double duration = currentRouteList.get(i).duration();
            BigDecimal durationInBigDecimal = new BigDecimal(duration);
            durationInBigDecimal = durationInBigDecimal.divide(new BigDecimal("60"), 0, RoundingMode.HALF_UP);
            String durationInString = String.valueOf(durationInBigDecimal) + " mins";
            item.setDuration(durationInString);

            double distance = currentRouteList.get(i).distance();
            BigDecimal distanceInBigDecimal = new BigDecimal(distance);
            distanceInBigDecimal = distanceInBigDecimal.divide(new BigDecimal("1000"), 1, RoundingMode.HALF_UP);
            String distanceInString = distanceInBigDecimal + " km";
            item.setDistance(distanceInString);

            String safetyScoreString = "Route Risk Score: " + routeSafetyScoreStringList.get(i);
            item.setRiskScore(safetyScoreString);

            routeInfoItemList.add(item);
        }

        routeInfoItemList.sort(Comparator.comparing(RouteInfoItem::getRiskScore));

        routeInfoItemList.get(0).setRecommendation(RECOMMENDATION_TAG);

        routeInfoItemAdapter.notifyDataSetChanged();
    }

    private void initSource(@NonNull Style loadedMapStyle) {
        loadedMapStyle.addSource(new GeoJsonSource(ROUTE_SOURCE_ID,
                FeatureCollection.fromFeatures(new Feature[]{})));
    }

    private void initLayers(@NonNull Style loadedMapStyle) {
        LineLayer routeLayer = new LineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID);

        // Add the LineLayer to the map. This layer will display the directions route.
        routeLayer.setProperties(
                lineCap(Property.LINE_CAP_ROUND),
                lineJoin(Property.LINE_JOIN_ROUND),
                lineWidth(5f),
                lineColor(Color.parseColor("#009688"))
        );
        loadedMapStyle.addLayer(routeLayer);
    }


    /**
     * Method of selecting an item on the list.
     *
     * @param selectedRouteNo
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void onItemSelect(int selectedRouteNo) {
        removeLayersAndResource();
        drawAlternativeRoute(unselectedRouteNo1, unselectedRouteNo2, unselectedRouteNo3);
        drawRoutePolyLine(safetyLevelsListOfRoutes.get(selectedRouteNo));
        recenterCameraAfterDisplayingRoute();
        showRouteList(false);
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

    @SuppressLint("MissingPermission")
    @OnClick(R.id.startButton)
    public void startNavigationButtonOnClick() {
        Intent intent = new Intent(this, MapNavigationActivity.class);
        intent.putExtra("navigationRoute", selectedRoute);
        intent.putExtra("originPoint", originPoint);
        intent.putExtra("destination", destinationPoint);
        intent.putExtra("safetyLevels", (Serializable) safetyLevelsListOfRoutes.get(selectedRouteNo));
        intent.putExtra("navigationRatings", (Serializable) navigationRatingsListOfRoutes.get(selectedRouteNo));
        intent.putExtra("voiceMessages", (Serializable) voiceMessagesListOfRoutes.get(selectedRouteNo));
        startActivity(intent);

    }

    @OnClick(R.id.about_page_fab)
    public void aboutPageButtonOnClick() {
        Intent intent = new Intent(this, AboutPageActivity.class);
        startActivity(intent);
    }

    @OnClick(R.id.recenter_location_fab)
    public void recenterLocationButtonOnClick(){
        originPoint = getCurrentLocation();
        LatLng originLatLng = new LatLng(originPoint.latitude(), originPoint.longitude());
        CameraPosition newCameraPosition = new CameraPosition.Builder().target(originLatLng).zoom(14).build();
        mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(newCameraPosition));
    }


    /*@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @OnClick(R.id.button_clear)
    public void clearAllButtonOnClick() {
        removeLayersAndResource();
        originSearchBar.setPlaceHolder(getString(R.string.origin_init_holder));
        destinationSearchBar.setPlaceHolder(getString(R.string.destination_init_holder));
        originPoint = getCurrentLocation();
        LatLng originLatLng = new LatLng(originPoint.latitude(), originPoint.longitude());
        CameraPosition newCameraPosition = new CameraPosition.Builder().target(originLatLng).build();
        mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(newCameraPosition));
        destinationPoint = null;
        hideMarker("origin-symbol-layer-id");
        hideMarker("destination-symbol-layer-id");
        startNavigationButton.setEnabled(false);
        //startNavigationButton.setBackgroundResource(R.color.mapboxGrayLight);
        startNavigationButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.mapboxGrayLight)));

        viewAlternativesButton.setVisibility(View.GONE);
        //clearAllButton.setVisibility(View.GONE);
        removeAllCurrentRouteInfo();
    }*/
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @OnClick(R.id.button_view_alternatives)
    public void goToListButtonOnClick() {
        showRouteList(true);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @OnClick(R.id.button_go_to_map)
    public void goToMapButtonOnClick() {
        showRouteList(false);
        startNavigationButton.setVisibility(selectedRouteNo != NO_ROUTE_SELECTED ? View.VISIBLE : View.GONE);
    }

    /**
     * Get user's current location
     *
     * @return
     */
    private Point getCurrentLocation() {
        if (locationComponent.getLastKnownLocation() == null) {
            return MELBOURNE;
        } else {
            return Point.fromLngLat(locationComponent.getLastKnownLocation().getLongitude(),
                    locationComponent.getLastKnownLocation().getLatitude());
        }

    }

    /**
     * Remove all the data in the route related object.
     */
    public void removeAllCurrentRouteInfo() {
        if (currentRouteList != null) {
            currentRouteList.clear();
        }

        if (pointsOfRouteList != null) {
            pointsOfRouteList.clear();
        }

        if (routeInfoItemList != null) {
            routeInfoItemList.clear();
        }

        if (routeSafetyScoreStringList != null) {
            routeSafetyScoreStringList.clear();
        }
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
            LatLng startPoint = new LatLng(originPoint.latitude(), originPoint.longitude());
            LatLng destination = new LatLng(destinationPoint.latitude(), destinationPoint.longitude());

            LatLngBounds latLngBounds = new LatLngBounds.Builder()
                    .include(startPoint)
                    .include(destination)
                    .build();

            mapboxMap.animateCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 200));
        }
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

                destinationPoint = Point.fromLngLat(((Point) Objects.requireNonNull(selectedLocationCarmenFeature.geometry())).longitude(), ((Point) Objects.requireNonNull(selectedLocationCarmenFeature.geometry())).latitude());

                if (originPoint == null)
                    originPoint = getCurrentLocation();

                if (destinationPoint.equals(originPoint))
                    Toast.makeText(MapActivity.this, "The destination and the starting point are the same.", Toast.LENGTH_LONG).show();
                else {
                    renderRouteOnMap(originPoint, destinationPoint);
                    routeInfoRecyclerView.setVisibility(View.GONE);
                    showProgress(true);
                }


            } else if (clickedSearchBarId == R.id.origin_search_bar) {
                originSearchBar.setPlaceHolder(selectedLocationCarmenFeature.placeName());
                originPoint = Point.fromLngLat(((Point) Objects.requireNonNull(selectedLocationCarmenFeature.geometry())).longitude(), ((Point) Objects.requireNonNull(selectedLocationCarmenFeature.geometry())).latitude());
                LatLng originLatLng = new LatLng(originPoint.latitude(), originPoint.longitude());
                CameraPosition newCameraPosition = new CameraPosition.Builder().target(originLatLng).build();
                mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(newCameraPosition));
                setOriginPointMarkerSsource(originPoint);

                if (destinationPoint != null && destinationPoint.equals(originPoint))
                    Toast.makeText(MapActivity.this, "The destination and the starting point are the same.", Toast.LENGTH_LONG).show();
                else {
                    if (!originPoint.equals(getCurrentLocation()))
                        showMarker("origin-symbol-layer-id");
                    if (destinationPoint != null) {
                        renderRouteOnMap(originPoint, destinationPoint);
                        routeInfoRecyclerView.setVisibility(View.GONE);
                        showProgress(true);
                    }
                }

            }
        }
    }

    /**
     * Set the marker geojson source of the destination
     *
     * @param destination
     */
    private void setDestinationMarkerSource(Point destination) {
        GeoJsonSource destinationSource = Objects.requireNonNull(mapboxMap.getStyle()).getSourceAs("destination-source-id");
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
        GeoJsonSource originSource = Objects.requireNonNull(mapboxMap.getStyle()).getSourceAs("origin-source-id");
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
        Layer layer = Objects.requireNonNull(mapboxMap.getStyle()).getLayer(layerId);
        if (layer != null)
            layer.setProperties(visibility(Property.NONE));
    }

    /**
     * Show a marker on the map
     *
     * @param layerId
     */
    private void showMarker(String layerId) {
        Layer layer = Objects.requireNonNull(mapboxMap.getStyle()).getLayer(layerId);
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
        selectedRouteNo = NO_ROUTE_SELECTED;
        removeLayersAndResource();
        getRouteFromMapbox(originPoint, destination);
        //recenterCameraAfterDisplayingRoute();
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

        if (safetyLevelsListOfRoutes != null){
            List<Integer> dangerousPointIndexList = getDangerousPointIndexFromCurrentRoute(safetyLevelsListOfRoutes.get(selectedRouteNo));
            Point clickedPoint = Point.fromLngLat(point.getLongitude(), point.getLatitude());
            for (int i = 0; i <= dangerousPointIndexList.size() - 1; i++) {
                int dangerousPointIndex = dangerousPointIndexList.get(i);
                Point dangerousPoint = pointsOfCurrentRoute.get(dangerousPointIndex);

                double distanceBetweenClickedAndDangerousPoint = calculateDistanceBetweenTwoPoint(clickedPoint, dangerousPoint);

                if (dangerousPointIndex != pointsOfCurrentRoute.size() - 1){
                    Point nextPoint = pointsOfCurrentRoute.get(dangerousPointIndex + 1);
                    double distanceBetweenClickedAndNextPoint = calculateDistanceBetweenTwoPoint(clickedPoint, nextPoint);
                    double distanceBetweenTwoConsecutivePoints = calculateDistanceBetweenTwoPoint(dangerousPoint, nextPoint);
                    if (distanceBetweenClickedAndDangerousPoint + distanceBetweenClickedAndNextPoint - distanceBetweenTwoConsecutivePoints < 50) {
                        String message = voiceMessagesListOfRoutes.get(selectedRouteNo).get(dangerousPointIndex);
                        Toast.makeText(MapActivity.this, message, Toast.LENGTH_LONG).show();
                    }
                }
            }
        }
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
        @TargetApi(Build.VERSION_CODES.N)
        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
            Log.d(TAG, "Safety Level Response code: " + response.code());
            if (response.body() == null) {
                Log.e(TAG, NO_SAFETY_LEVEL_ERROR_MESSAGE);
                Toast.makeText(MapActivity.this, NO_SAFETY_LEVEL_ERROR_MESSAGE, Toast.LENGTH_LONG).show();
                showProgress(false);
                return;
            }
            try {

                String safetyLevelResponseString = response.body().string();
                safetyLevelsListOfRoutes = Utils.parseSafetyLevelFromResponse(safetyLevelResponseString);
                navigationRatingsListOfRoutes = Utils.parseNavigationRatingsFromResponse(safetyLevelResponseString);
                routeSafetyScoreStringList = Utils.parseSafetyScoreOfRoutesFromResponse(safetyLevelResponseString);
                voiceMessagesListOfRoutes = Utils.parseVoiceAlertMessageFromResponse(safetyLevelResponseString);

                initRouteItemData();

                showProgress(false);
                showRouteList(true);

                recenterCameraAfterDisplayingRoute();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onFailure(Call<ResponseBody> call, Throwable t) {
            Log.e(TAG, "Error: " + t.getMessage());
            Toast.makeText(MapActivity.this, "No safety levels found", Toast.LENGTH_LONG).show();
            showProgress(false);
        }
    };

    /**
     * Getting our own routing algorithm
     */

    private void getRoutingAlgorithm(String jsonString, Callback<ResponseBody> callback) {

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(RoutingAlgorithmApiInterface.RoutingAlgorithm_URL)
                .build();

        RoutingAlgorithmApiInterface routingAlgorithmApiInterface = retrofit.create(RoutingAlgorithmApiInterface.class);

        RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonString);
        Call<ResponseBody> responseBodyCall = routingAlgorithmApiInterface.getOptimizedRoute(body);

        responseBodyCall.enqueue(callback);
    }

    Callback<ResponseBody> OptimizedRouteCallback = new Callback<ResponseBody>() {
        @TargetApi(Build.VERSION_CODES.N)
        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
            Log.d(TAG, "routing algorithm Response code: " + response.code());
            if (response.body() == null) {
                Log.e(TAG, NO_SAFETY_LEVEL_ERROR_MESSAGE);
                Toast.makeText(MapActivity.this, NO_SAFETY_LEVEL_ERROR_MESSAGE, Toast.LENGTH_LONG).show();
                return;
            }
            try {
                MapMatchingResponse matchingResponse = MapMatchingResponse.fromJson(response.body().string());
                DirectionsRoute optimizedRoute = matchingResponse.matchings().get(0).toDirectionRoute();
                currentRouteList.add(optimizedRoute);

                //Collect the coordinates on the route
                for (int i = 0; i <= currentRouteList.size() - 1; i++) {
                    pointsOfRouteList.add(Utils.getPointsOfRoutes(currentRouteList.get(i)));
                }

                pointsOfCurrentRoute = Utils.getPointsOfRoutes(currentRouteList.get(0));
                String CoordinateStringOfRoutes = Utils.generateJsonStringForMultipleRoutes(pointsOfRouteList);

                if (currentRouteList.size() >= 2) {

                    unselectedRouteNo1 = 1;

                    if (currentRouteList.size() >= 3) {

                        unselectedRouteNo2 = 2;

                        if (currentRouteList.size() >= 4) {

                            unselectedRouteNo3 = 3;

                        }
                    }
                }


                //Request for safety levels of different sections of the returned route.
                getSafetyLevel(CoordinateStringOfRoutes, safetyLevelCallback);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onFailure(Call<ResponseBody> call, Throwable t) {
            Log.e(TAG, "Error: " + t.getMessage());
            //Toast.makeText(MapActivity.this, "No Routing Algorithm found", Toast.LENGTH_LONG).show();
            for (int i = 0; i <= currentRouteList.size() - 1; i++) {
                pointsOfRouteList.add(Utils.getPointsOfRoutes(currentRouteList.get(i)));
            }
            String CoordinateStringOfRoutes = Utils.generateJsonStringForMultipleRoutes(pointsOfRouteList);
            getSafetyLevel(CoordinateStringOfRoutes, safetyLevelCallback);
        }
    };

    /**
     * Draw unselected route options on the map
     *
     * @param unselectedRouteNo1
     * @param unselectedRouteNo2
     */
    private void drawAlternativeRoute(int unselectedRouteNo1, int unselectedRouteNo2, int unselectedRouteNo3) {


        if (currentRouteList.size() >= 2) {

            if (mapboxMap != null) {
                mapboxMap.getStyle(style -> {


                    FeatureCollection featureCollection = FeatureCollection.fromFeatures(new Feature[]{Feature.fromGeometry(LineString.fromPolyline(Objects.requireNonNull(currentRouteList.get(unselectedRouteNo1).geometry()), PRECISION_6))});
                    String lineSourceID = "add-line-source-" + currentRouteList.toString() + UUID.randomUUID().toString();
                    GeoJsonSource geoJsonSource = new GeoJsonSource(lineSourceID, featureCollection);

                    style.addSource(geoJsonSource);
                    String lineLayerID = "add-line-layer-" + currentRouteList.toString() + UUID.randomUUID().toString();
                    LineLayer lineLayer = new LineLayer(lineLayerID, lineSourceID);
                    style.addLayer(lineLayer.withProperties(
                            lineCap(Property.LINE_CAP_ROUND),
                            lineJoin(Property.LINE_JOIN_ROUND),
                            lineWidth(8f),
                            lineColor("#696b6a")
                    ));
                });
            }

            if (currentRouteList.size() >= 3) {

                if (mapboxMap != null) {
                    mapboxMap.getStyle(style -> {


                        FeatureCollection featureCollection = FeatureCollection.fromFeatures(new Feature[]{Feature.fromGeometry(LineString.fromPolyline(Objects.requireNonNull(currentRouteList.get(unselectedRouteNo2).geometry()), PRECISION_6))});
                        String lineSourceID = "add-line-source-" + currentRouteList.toString() + UUID.randomUUID().toString();
                        GeoJsonSource geoJsonSource = new GeoJsonSource(lineSourceID, featureCollection);

                        style.addSource(geoJsonSource);
                        String lineLayerID = "add-line-layer-" + currentRouteList.toString() + UUID.randomUUID().toString();
                        LineLayer lineLayer = new LineLayer(lineLayerID, lineSourceID);
                        style.addLayer(lineLayer.withProperties(
                                lineCap(Property.LINE_CAP_ROUND),
                                lineJoin(Property.LINE_JOIN_ROUND),
                                lineWidth(8f),
                                lineColor("#696b6a")
                        ));
                    });
                }

                if (currentRouteList.size() >= 4) {

                    if (mapboxMap != null) {
                        mapboxMap.getStyle(style -> {


                            FeatureCollection featureCollection = FeatureCollection.fromFeatures(new Feature[]{Feature.fromGeometry(LineString.fromPolyline(Objects.requireNonNull(currentRouteList.get(unselectedRouteNo3).geometry()), PRECISION_6))});
                            String lineSourceID = "add-line-source-" + currentRouteList.toString() + UUID.randomUUID().toString();
                            GeoJsonSource geoJsonSource = new GeoJsonSource(lineSourceID, featureCollection);

                            style.addSource(geoJsonSource);
                            String lineLayerID = "add-line-layer-" + currentRouteList.toString() + UUID.randomUUID().toString();
                            LineLayer lineLayer = new LineLayer(lineLayerID, lineSourceID);
                            style.addLayer(lineLayer.withProperties(
                                    lineCap(Property.LINE_CAP_ROUND),
                                    lineJoin(Property.LINE_JOIN_ROUND),
                                    lineWidth(8f),
                                    lineColor("#696b6a")
                            ));
                        });
                    }
                }

            }

        }
    }

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
                .alternatives(true)
                .voiceUnits(DirectionsCriteria.METRIC)
                .build()
                .getRoute(new Callback<DirectionsResponse>() {
                    @Override
                    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                        Log.d(TAG, "Mapbox Directions Response code: " + response.code());
                        if (response.body() == null) {
                            Log.e(TAG, NO_ROUTES_ERROR_MESSAGE);
                            Toast.makeText(MapActivity.this, NO_ROUTES_ERROR_MESSAGE, Toast.LENGTH_LONG).show();
                            showProgress(false);
                            return;
                        } else if (response.body().routes().size() < 1) {
                            Log.e(TAG, NO_ROUTES_ERROR_MESSAGE);
                            Toast.makeText(MapActivity.this, NO_ROUTES_ERROR_MESSAGE, Toast.LENGTH_LONG).show();
                            showProgress(false);
                            return;
                        }

                        removeAllCurrentRouteInfo();

                        currentRouteList = response.body().routes();

                        List<Point> OriginDesPoints = new ArrayList<>();
                        OriginDesPoints.add(origin);
                        OriginDesPoints.add(destination);
                        String CoordinateStringOfRoutingAlgorithm = Utils.generateCoordinatesJsonStringForRoutingAlgorithm(OriginDesPoints);
                        getRoutingAlgorithm(CoordinateStringOfRoutingAlgorithm, OptimizedRouteCallback);

                    }

                    @Override
                    public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
                        Log.e(TAG, "Error: " + throwable.getMessage());
                        showProgress(false);
                    }


                });
    }

    /**
     * Draw the polyline section by section
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void drawRoutePolyLine(List<String> safetyLevelList) {
        for (int i = 0; i <= pointsOfCurrentRoute.size() - 2; i++) {
            int colourOfSection = SAFETY_LEVEL_COLOUR_MAP.get(safetyLevelList.get(i));
            addLegSourceOfRoute(pointsOfCurrentRoute.get(i), pointsOfCurrentRoute.get(i + 1), colourOfSection);
        }
        for (int i = 0; i <= SAFETY_LEVEL_COLOURS.length - 1; i++) {
            addRouteLayer(SAFETY_LEVEL_COLOURS[i]);
        }
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
        return Objects.requireNonNull(mapboxMap.getStyle()).getSource(lineSourceID) != null;
    }

    /**
     * Display the dialog of colour information.
     */
    private void showDisclaimerDialog() {
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

            //Create and customize the LocationComponent's options
            LocationComponentOptions customLocationComponentOptions = LocationComponentOptions.builder(this)
                    .build();

            // Get an instance of the component
            locationComponent = mapboxMap.getLocationComponent();

            LocationComponentActivationOptions locationComponentActivationOptions =
                    LocationComponentActivationOptions.builder(this, loadedMapStyle)
                            .locationComponentOptions(customLocationComponentOptions)
                            .build();

            // Activate with options
            locationComponent.activateLocationComponent(locationComponentActivationOptions);
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

        locationEngine.requestLocationUpdates(request, locationCallback, getMainLooper());
        locationEngine.getLastLocation(locationCallback);
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
    public void onBackPressed() {
        if (mapView.getVisibility() == View.VISIBLE) {
            finishAffinity();
        } else {
            showRouteList(false);
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
        if (locationEngine != null) {
            locationEngine.removeLocationUpdates(locationCallback);
        }
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
