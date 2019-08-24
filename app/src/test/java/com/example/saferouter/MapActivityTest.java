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

    }

    @Test
    public void generateCorrectPointJsonString(){
        Point point = Point.fromLngLat(144.966597416688, -37.741313666549);
        String jsonString = generateJsonStringForOneCoordinates(point);
        assertEquals("(144.966597416688,-37.741313666549)", jsonString);
    }
}