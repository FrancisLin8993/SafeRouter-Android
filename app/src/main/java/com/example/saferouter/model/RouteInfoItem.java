package com.example.saferouter.model;

public class RouteInfoItem {
    private String safetyScore, routeNo, duration, distance;

    public RouteInfoItem(String safetyScore, String routeNo, String duration, String distance) {
        this.safetyScore = safetyScore;
        this.routeNo = routeNo;
        this.duration = duration;
        this.distance = distance;
    }

    public RouteInfoItem() {
    }

    public String getSafetyScore() {
        return safetyScore;
    }

    public void setSafetyScore(String safetyScore) {
        this.safetyScore = safetyScore;
    }

    public String getRouteNo() {
        return routeNo;
    }

    public void setRouteNo(String routeNo) {
        this.routeNo = routeNo;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getDistance() {
        return distance;
    }

    public void setDistance(String distance) {
        this.distance = distance;
    }
}
