package com.example.saferouter.model;

public class RouteInfoItem {
    private String riskScore, routeNo, duration, distance, recommendation;

    public RouteInfoItem(String riskScore, String routeNo, String duration, String distance, String recommendation) {
        this.riskScore = riskScore;
        this.routeNo = routeNo;
        this.duration = duration;
        this.distance = distance;
        this.recommendation = recommendation;
    }

    public RouteInfoItem() {
    }

    public String getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(String riskScore) {
        this.riskScore = riskScore;
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

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }
}
