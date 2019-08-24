package com.example.saferouter;

import com.mapbox.geojson.Point;

import java.util.List;

public class Utils {
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
     * Generate the json string for calling safety level api
     * @param points
     * @return
     */
    public static String generateCoordinatesJsonString(List<Point> points){
        StringBuilder stringBuilder = new StringBuilder();
        //Does not need the last coordinate for calling safety level
        points.remove(points.size() - 1);
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
}
