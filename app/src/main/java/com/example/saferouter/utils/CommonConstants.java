package com.example.saferouter.utils;

import com.example.saferouter.R;

import java.util.HashMap;
import java.util.Map;

public class CommonConstants {
    private CommonConstants() {
    }

    public static final String SAFE_LEVEL = "1.0";
    public static final String MEDIUM_LEVEL = "2.0";
    public static final String DANGEROUS_LEVEL = "3.0";

    public static final int[] SAFETY_LEVEL_COLOURS = new int[]{R.color.routeGreen, R.color.routeYellow, R.color.routeRed};

    public static final Map<String, Integer> SAFETY_LEVEL_COLOUR_MAP = new HashMap<String, Integer>() {
        {
            put(SAFE_LEVEL, R.color.routeGreen);
            put(MEDIUM_LEVEL, R.color.routeYellow);
            put(DANGEROUS_LEVEL, R.color.routeRed);
        }
    };
    public static final String VOICE_ALERT_MESSAGE = "Dangerous section ahead. Please drive safely.";
    public static final String COMPONENT_NAVIGATION_INSTRUCTION_CACHE = "component-navigation-instruction-cache";
    public static final long TEN_MEGABYTE_CACHE_SIZE = 10 * 1024 * 1024;
    public static final int DISTANCE_TO_VOICE_ALERT_POINT = 500;
    public static final long DEFAULT_INTERVAL_IN_MILLISECONDS = 1000L;
    public static final long DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_IN_MILLISECONDS * 5;

}
