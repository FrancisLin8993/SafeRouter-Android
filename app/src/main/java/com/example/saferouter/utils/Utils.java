package com.example.saferouter.utils;

import com.google.gson.JsonObject;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.utils.PolylineUtils;
import com.mapbox.turf.TurfClassification;
import com.mapbox.turf.TurfConstants;
import com.mapbox.turf.TurfMeasurement;

import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.example.saferouter.utils.CommonConstants.DANGEROUS_LEVEL;

/**
 * Miscellaneous methods using in the program.
 */
public class Utils {

    /**
     * Generate a corresponding string format of a coordinate for safety level api request
     *
     * @param point
     * @return
     */
    public static String generateJsonStringForOneCoordinates(Point point) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("(");
        stringBuilder.append(point.longitude());
        stringBuilder.append(",");
        stringBuilder.append(point.latitude());
        stringBuilder.append(")");
        return stringBuilder.toString();
    }

    /**
     * Generate the json string for calling safety level api request
     *
     * @param points
     * @return
     */
    public static String generateCoordinatesJsonString(List<Point> points) {
        StringBuilder stringBuilder = new StringBuilder();
        //Does not need the last coordinate for calling safety level
        //points.remove(points.size() - 1);
        stringBuilder.append("{");
        stringBuilder.append("\"data\":");
        stringBuilder.append("\"[[");
        for (int i = 0; i <= points.size() - 1; i++) {
            stringBuilder.append(generateJsonStringForOneCoordinates(points.get(i)));
            if (i != points.size() - 1)
                stringBuilder.append(",");
        }
        stringBuilder.append("]]");
        stringBuilder.append("\"}");
        return stringBuilder.toString();
    }

    /**
     * Generate the json string for calling safety level api request
     *
     * @param points
     * @return
     */
    public static String generateCoordinatesJsonStringForRoutingAlgorithm(List<Point> points) {
        StringBuilder stringBuilder = new StringBuilder();
        //Does not need the last coordinate for calling safety level
        //points.remove(points.size() - 1);
        stringBuilder.append("{");
        stringBuilder.append("\"data\":");
        stringBuilder.append("\"[");
        for (int i = 0; i <= points.size() - 1; i++) {
            stringBuilder.append(generateJsonStringForOneCoordinates(points.get(i)));
            if (i != points.size() - 1)
                stringBuilder.append(",");
        }
        stringBuilder.append("]");
        stringBuilder.append("\"}");
        return stringBuilder.toString();
    }

    /**
     * Generate the json string for one route.
     *
     * @param points
     * @return
     */
    public static String generateJsonStringForOneRoute(List<Point> points) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[");
        for (int i = 0; i <= points.size() - 1; i++) {
            stringBuilder.append(generateJsonStringForOneCoordinates(points.get(i)));
            if (i != points.size() - 1)
                stringBuilder.append(",");
        }
        stringBuilder.append("]");
        return stringBuilder.toString();
    }

    /**
     * Generate the json string for multiple routes to make safety level request
     * @param pointsOfRouteList
     * @return
     */
    public static String generateJsonStringForMultipleRoutes(List<List<Point>> pointsOfRouteList) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{");
        stringBuilder.append("\"data\":");
        stringBuilder.append("\"[");

        for (int i = 0; i <= pointsOfRouteList.size() - 1; i++) {
            List<Point> pointListOfOneRoute = pointsOfRouteList.get(i);
            stringBuilder.append(generateJsonStringForOneRoute(pointListOfOneRoute));
            if (i != pointsOfRouteList.size() - 1)
                stringBuilder.append(",");
        }

        stringBuilder.append("]");
        stringBuilder.append("\"}");
        return stringBuilder.toString();
    }

    public static List<List<String>> parseNavigationRatingsFromResponse(String jsonResponse){
        List<List<String>> navigationRatingsListOfRoutes = new ArrayList<>();
        /*jsonResponse = StringUtils.removeStart(jsonResponse, "{\"navigation_ratings\":[[");
        String[] ratingsAndScores = jsonResponse.split("]],\"ratings\":");*/
        jsonResponse = StringUtils.substringBetween(jsonResponse, "{\"navigation_ratings\":[[", "]],\"ratings\":");
        if (jsonResponse.contains("],[")) {
            String[] stringsOfRoutes = jsonResponse.split("],\\[");
            for (int i = 0; i <= stringsOfRoutes.length - 1; i++) {
                StringUtils.appendIfMissing(stringsOfRoutes[i], "]");
                navigationRatingsListOfRoutes.add(parseResponseValuesOfOneRoute(stringsOfRoutes[i]));
            }

        } else {
            navigationRatingsListOfRoutes.add(parseResponseValuesOfOneRoute(jsonResponse));
        }
        return navigationRatingsListOfRoutes;
    }

    /**
     * Parse safety level of routes from response.
     *
     * @param jsonResponse
     * @return
     */
    public static List<List<String>> parseSafetyLevelFromResponse(String jsonResponse) {
        List<List<String>> safetyLevelListOfRoutes = new ArrayList<>();
        /*jsonResponse = StringUtils.removeStart(jsonResponse, "{\"navigation_ratings\":[[");
        String[] ratingsAndScores = jsonResponse.split("]],\"ratings\":\\[\\[");
        String[] safetyRatingsAndScores = ratingsAndScores[1].split("]],\"scores\":\\[");*/
        jsonResponse = StringUtils.substringBetween(jsonResponse, "\"ratings\":[[", "]],\"scores\"");
        //check if the response string contains safety levels of multiple routes
        if (jsonResponse.contains("],[")) {
            String[] stringsOfRoutes = jsonResponse.split("],\\[");
            for (int i = 0; i <= stringsOfRoutes.length - 1; i++) {
                StringUtils.appendIfMissing(stringsOfRoutes[i], "]");
                safetyLevelListOfRoutes.add(parseResponseValuesOfOneRoute(stringsOfRoutes[i]));
            }

        } else {
            safetyLevelListOfRoutes.add(parseResponseValuesOfOneRoute(jsonResponse));
        }
        return safetyLevelListOfRoutes;
    }

    public static List<List<String>> parseVoiceAlertMessageFromResponse(String jsonResponse) {
        List<List<String>> voiceAlertListOfRoutes = new ArrayList<>();
        jsonResponse = StringUtils.substringBetween(jsonResponse, "\"voice_alerts\":[[", "]]}");
        //check if the response string contains safety levels of multiple routes
        if (jsonResponse.contains("],[")) {
            String[] stringsOfRoutes = jsonResponse.split("],\\[");
            for (int i = 0; i <= stringsOfRoutes.length - 1; i++) {
                StringUtils.appendIfMissing(stringsOfRoutes[i], "]");
                voiceAlertListOfRoutes.add(parseResponseValuesOfOneRoute(stringsOfRoutes[i]));
            }

        } else {
            voiceAlertListOfRoutes.add(parseResponseValuesOfOneRoute(jsonResponse));
        }
        return voiceAlertListOfRoutes;
    }

    /**
     * Parse safety score of routes from the response.
     * @param jsonResponse
     * @return
     */
    public static List<String> parseSafetyScoreOfRoutesFromResponse(String jsonResponse) {
        List<String> safetyScoreOfRoutes = new ArrayList<>();
        jsonResponse = StringUtils.substringBetween(jsonResponse, ",\"scores\":[", "],\"voice_alerts\"");
        if (jsonResponse.contains(",")){
            String[] safetyScoreString = jsonResponse.split(",");
            safetyScoreOfRoutes.addAll(Arrays.asList(safetyScoreString));
        } else {
            safetyScoreOfRoutes.add(jsonResponse);
        }

        return safetyScoreOfRoutes;
    }

    /**
     * Convert safety level response of one route
     *
     * @param string
     * @return
     */
    public static List<String> parseResponseValuesOfOneRoute(String string) {
        List<String> responseValuesList = new ArrayList<>();
        String[] stringsOfLevels = string.split(",");
        for (int i = 0; i <= stringsOfLevels.length - 1; i++) {
            if (stringsOfLevels[i].contains("\"")){
                stringsOfLevels[i] = RegExUtils.removeAll(stringsOfLevels[i], "\"");
            }
            responseValuesList.add(stringsOfLevels[i]);
        }
        return responseValuesList;
    }

    /**
     * Get the distance between two points
     *
     * @param point1
     * @param point2
     * @return
     */
    public static double calculateDistanceBetweenTwoPoint(Point point1, Point point2) {
        return TurfMeasurement.distance(point1, point2, TurfConstants.UNIT_METRES);
    }


    /**
     * Retrieve coordinates from the geometry attributes of the response route.
     *
     * @param directionsRoute
     * @return
     */
    public static List<Point> getPointsOfRoutes(DirectionsRoute directionsRoute) {
        List<Point> points = new ArrayList<>();
        if (directionsRoute != null) {
            String encodedPolyline = directionsRoute.geometry();
            points = PolylineUtils.decode(encodedPolyline, 6);
        }
        return points;
    }

    /**
     * Retrieve the index of all the dangerous points from the route
     * @param safetyLevelString
     * @return
     */
    public static List<Integer> getDangerousPointIndexFromCurrentRoute(List<String> safetyLevelString) {
        List<Integer> dangerousPointIndexList = new ArrayList<>();
        for (int i = 0; i <= safetyLevelString.size() - 1; i++) {
            if (safetyLevelString.get(i).equals(DANGEROUS_LEVEL))
                dangerousPointIndexList.add(i);
        }
        return dangerousPointIndexList;
    }
}
