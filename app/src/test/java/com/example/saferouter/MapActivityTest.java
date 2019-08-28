package com.example.saferouter;

import com.mapbox.geojson.Point;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.example.saferouter.Utils.generateJsonStringForOneCoordinates;
import static org.junit.Assert.*;

public class MapActivityTest {

    @Test
    public void generateCorrectCoordinatesJsonString(){
        List<Point> points = new ArrayList<>();
        points.add(Point.fromLngLat(144.966597416688, -37.741313666549));
        String jsonString = Utils.generateCoordinatesJsonString(points);
        assertEquals("{\"data\":\"[[(144.966597416688,-37.741313666549)]]\"}", jsonString);
    }

    @Test
    public void generateCorrectPointJsonString(){
        Point point = Point.fromLngLat(144.966597416688, -37.741313666549);
        String jsonString = generateJsonStringForOneCoordinates(point);
        assertEquals("(144.966597416688,-37.741313666549)", jsonString);
    }

    @Test
    public void getCorrectSafetyLevels(){
        String safetyLevelResponse = "[[3.0,1.0]]";
        List<String> safetyLevelList = Utils.extractSafetyLevelFromResponseString(safetyLevelResponse);
        assertEquals("3.0", safetyLevelList.get(0));

    }
}