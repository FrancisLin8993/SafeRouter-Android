package com.example.saferouter.utils;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.utils.PolylineUtils;

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
     * Convert the safety level api response to a list of safety level numbers
     *
     * @param jsonResponse
     * @return
     */
    public static List<String> extractSafetyLevelFromResponseString(String jsonResponse) {
        List<String> safetyLevelList = new ArrayList<>();
        jsonResponse = StringUtils.removeStart(jsonResponse, "[[");
        jsonResponse = StringUtils.removeEnd(jsonResponse, "]]\\n");
        String[] stringsOfLevels = jsonResponse.split(",");
        for (int i = 0; i <= stringsOfLevels.length - 1; i++) {
            safetyLevelList.add(stringsOfLevels[i]);
        }
        return safetyLevelList;
    }

    /**
     * Convert safety level response.
     *
     * @param jsonResponse
     * @return
     */
    public static List<List<String>> parseSafetyLevelFromResponse(String jsonResponse) {
        List<List<String>> safetyLevelListOfRoutes = new ArrayList<>();
        jsonResponse = StringUtils.removeStart(jsonResponse, "[[");
        jsonResponse = StringUtils.removeEnd(jsonResponse, "]]");
        //check if the response string contains safety levels of multiple routes
        if (jsonResponse.contains("],[")) {
            String[] stringsOfRoutes = jsonResponse.split("],\\[");
            //As the last value of safety level is not used, neglect the format of the last safety level string
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
        for (int i = 0; i <= stringList.size() - 2; i++){
            double safetyScore = Double.parseDouble(stringList.get(i));
            safetyScoreListInDouble.add(safetyScore);
        }
        return safetyScoreListInDouble;
    }

    public static BigDecimal calculateSafetyScore(List<Double> safetyScoreListInDouble){
        double sum = 0.0;
        for (Double safetyScore : safetyScoreListInDouble){
            sum += safetyScore;
        }

        double safetyScore = sum / (safetyScoreListInDouble.size() - 1);

        return new BigDecimal(safetyScore).setScale(0, RoundingMode.HALF_UP);
    }
}
