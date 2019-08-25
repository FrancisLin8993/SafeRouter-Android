package com.example.saferouter;

import com.mapbox.geojson.Point;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Miscellaneous methods using in the program.
 */
public class Utils {

    /**
     * The mapping between safety level numbers and colours
     * @return
     */
    public static Map<String, Integer> getSafetyLevelColourMap() {
        final Map<String, Integer> safetyLevelColourMap  = new HashMap<String, Integer>(){{
            put("1.0", R.color.routeGreen);
            put("2.0", R.color.routeYellow);
            put("3.0", R.color.routeRed);
        }};
        return safetyLevelColourMap;
    }

    /**
     * Generate a corresponding string format of a coordinate for safety level api request
     * @param point
     * @return
     */
    public static String generateJsonStringForOneCoordinates(Point point){
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
     * @param points
     * @return
     */
    public static String generateCoordinatesJsonString(List<Point> points){
        StringBuilder stringBuilder = new StringBuilder();
        //Does not need the last coordinate for calling safety level
        //points.remove(points.size() - 1);
        stringBuilder.append("{");
        stringBuilder.append("\"data\":");
        stringBuilder.append("\"[[");
        for (int i = 0; i <= points.size() - 1; i++){
            stringBuilder.append(generateJsonStringForOneCoordinates(points.get(i)));
            if (i != points.size() - 1)
                stringBuilder.append(",");
        }
        stringBuilder.append("]]");
        stringBuilder.append("\"}");
        return stringBuilder.toString();
    }

    /**
     * Convert the safety level api response to a list of safety level numbers
     * @param jsonResponse
     * @return
     */
    public static List<String> extractSafetyLevelFromResponseString(String jsonResponse){
        List<String> safetyLevelList = new ArrayList<>();
        jsonResponse = StringUtils.removeStart(jsonResponse, "[[");
        jsonResponse = StringUtils.removeEnd(jsonResponse, "]]\\n");
        String[] stringsOfLevels = jsonResponse.split(",");
        for (int i = 0; i <= stringsOfLevels.length - 1; i++){
            safetyLevelList.add(stringsOfLevels[i]);
        }
        return safetyLevelList;
    }
}
