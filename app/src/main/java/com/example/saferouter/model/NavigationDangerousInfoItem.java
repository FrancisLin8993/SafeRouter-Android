package com.example.saferouter.model;

/**
 * A model class for dangerous information used in navigation view.
 */
public class NavigationDangerousInfoItem {
    private int stepIndex;
    private double distanceToStep;
    private String voiceMessage;

    public NavigationDangerousInfoItem() {
    }

    public NavigationDangerousInfoItem(int stepIndex, double distanceToStep, String voiceMessage) {
        this.stepIndex = stepIndex;
        this.distanceToStep = distanceToStep;
        this.voiceMessage = voiceMessage;
    }

    public int getStepIndex() {
        return stepIndex;
    }

    public void setStepIndex(int stepIndex) {
        this.stepIndex = stepIndex;
    }

    public double getDistanceToStep() {
        return distanceToStep;
    }

    public void setDistanceToStep(double distanceToStep) {
        this.distanceToStep = distanceToStep;
    }

    public String getVoiceMessage() {
        return voiceMessage;
    }

    public void setVoiceMessage(String voiceMessage) {
        this.voiceMessage = voiceMessage;
    }
}
