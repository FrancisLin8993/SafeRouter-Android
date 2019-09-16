package com.example.saferouter.utils;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.utils.PolylineUtils;

import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

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

    /**
     * Parse safety level of routes from response.
     *
     * @param jsonResponse
     * @return
     */
    public static List<List<String>> parseSafetyLevelFromResponse(String jsonResponse) {
        List<List<String>> safetyLevelListOfRoutes = new ArrayList<>();
        jsonResponse = StringUtils.removeStart(jsonResponse, "{\"ratings\":[[");
        String[] ratingsAndScores = jsonResponse.split(",\"scores\":");
        //check if the response string contains safety levels of multiple routes
        if (ratingsAndScores[0].contains("],[")) {
            String[] stringsOfRoutes = ratingsAndScores[0].split("],\\[");
            for (int i = 0; i <= stringsOfRoutes.length - 1; i++) {
                StringUtils.appendIfMissing(stringsOfRoutes[i], "]");
                safetyLevelListOfRoutes.add(parseSafetyLevelOfOneRoute(stringsOfRoutes[i]));
            }

        } else {
            safetyLevelListOfRoutes.add(parseSafetyLevelOfOneRoute(jsonResponse));
        }
        return safetyLevelListOfRoutes;

    }

    /**
     * Parse safety score of routes from the response.
     * @param jsonResponse
     * @return
     */
    public static List<String> parseSafetyScoreOfRoutesFromResponse(String jsonResponse) {
        List<String> safetyScoreOfRoutes = new ArrayList<>();
        jsonResponse = StringUtils.removeStart(jsonResponse, "{\"ratings\":[[");
        String[] ratingsAndScores = jsonResponse.split(",\"scores\":\\[");
        String[] safetyscores = ratingsAndScores[1].split("]");
        if (safetyscores[0].contains(",")){
            String[] safetyScoreString = safetyscores[0].split(",");
            for (int i = 0; i <= safetyScoreString.length - 1; i++){
                safetyScoreOfRoutes.add(safetyScoreString[i]);
            }
        } else {
            safetyScoreOfRoutes.add(safetyscores[0]);
        }

        return safetyScoreOfRoutes;
    }

    /**
     * Convert safety level response of one route
     *
     * @param string
     * @return
     */
    public static List<String> parseSafetyLevelOfOneRoute(String string) {
        List<String> safetyLevelList = new ArrayList<>();
        String[] stringsOfLevels = string.split(",");
        for (int i = 0; i <= stringsOfLevels.length - 1; i++) {
            safetyLevelList.add(stringsOfLevels[i]);
        }
        return safetyLevelList;
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

    public static List<Double> convertStringListToBigDecimal(List<String> stringList) {
        List<Double> safetyScoreListInDouble = new ArrayList<>();
        for (int i = 0; i <= stringList.size() - 2; i++) {
            double safetyScore = Double.parseDouble(stringList.get(i));
            safetyScoreListInDouble.add(safetyScore);
        }
        return safetyScoreListInDouble;
    }

    public static BigDecimal calculateSafetyScore(List<Double> safetyScoreListInDouble) {
        double sum = 0.0;
        for (Double safetyScore : safetyScoreListInDouble) {
            sum += safetyScore;
        }

        double safetyScore = sum / (safetyScoreListInDouble.size() - 1);

        return new BigDecimal(safetyScore).setScale(0, RoundingMode.HALF_UP);
    }
}
