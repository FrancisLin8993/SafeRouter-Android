package com.example.saferouter;

import com.example.saferouter.utils.Utils;
import com.mapbox.geojson.Point;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.example.saferouter.utils.Utils.generateJsonStringForOneCoordinates;
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
    public void generateCorrectRouteJsonString(){
        List<Point> points = new ArrayList<>();
        Point point1 = Point.fromLngLat(144.966597416688, -37.741313666549);
        Point point2 = Point.fromLngLat(144.934456, -37.751234);
        points.add(point1);
        points.add(point2);
        String jsonString = Utils.generateJsonStringForOneRoute(points);
        assertEquals("[(144.966597416688,-37.741313666549),(144.934456,-37.751234)]", jsonString);
    }

    @Test
    public void generateCorrectJsonStringForMultipleRoutes(){
        List<Point> pointsList1 = new ArrayList<>();
        Point point1 = Point.fromLngLat(144.966597416688, -37.741313666549);
        Point point2 = Point.fromLngLat(144.934456, -37.751234);
        pointsList1.add(point1);
        pointsList1.add(point2);

        List<Point> pointsList2 = new ArrayList<>();
        Point point3 = Point.fromLngLat(144.966597416688, -37.741313666549);
        Point point4 = Point.fromLngLat(144.934456, -37.751234);
        pointsList2.add(point3);
        pointsList2.add(point4);

        List<List<Point>> pointsOfRouteList = new ArrayList<>();
        pointsOfRouteList.add(pointsList1);
        pointsOfRouteList.add(pointsList2);

        String jsonString = Utils.generateJsonStringForMultipleRoutes(pointsOfRouteList);
        assertEquals("{\"data\":\"[[(144.966597416688,-37.741313666549),(144.934456,-37.751234)],[(144.966597416688,-37.741313666549),(144.934456,-37.751234)]]\"}", jsonString);

    }

    @Test
    public void getCorrectSafetyLevelsForMultipleRoutes(){
        String safetyLevelResponse = "{\"ratings\":[[3.0,1.0],[3.0,1.0]]";
        List<List<String>> safetyLevelListOfRoutes = Utils.parseSafetyLevelFromResponse(safetyLevelResponse);
        assertEquals("3.0", safetyLevelListOfRoutes.get(0).get(0));
        assertEquals("1.0", safetyLevelListOfRoutes.get(0).get(1));
        assertEquals("3.0", safetyLevelListOfRoutes.get(1).get(0));
        assertEquals("1.0", safetyLevelListOfRoutes.get(1).get(1));
    }

    @Test
    public void getCorrectSafetyLevels(){
        String safetyLevelResponse = "[[3.0,1.0]]";
        List<String> safetyLevelList = Utils.extractSafetyLevelFromResponseString(safetyLevelResponse);
        assertEquals("3.0", safetyLevelList.get(0));

    }
}