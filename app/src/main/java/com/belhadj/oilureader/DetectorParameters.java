package com.belhadj.oilureader;

public class DetectorParameters {

    public DetectorParameters()
    {
        SetDefaultParams();
    }
    public void SetDefaultParams()
    {
        adaptiveThreshWinSizeMin = 3;
        adaptiveThreshWinSizeMax = 23;
        adaptiveThreshWinSizeStep = 10;
        adaptiveThreshConstant = 7;
        binarizationThresh = 127;
        cannyThreshold = 0;
        cannyThresholdLinking = 0;

        minMarkerPerimeterRate = 0.03;
        maxMarkerPerimeterRate = 4.0;
        polygonalApproxAccuracyRate = 0.03;
        minCornerDistanceRate = 0.05;
        minDistanceToBorder = 3;
        minMarkerDistanceRate = 0.05;
        detectInvertedMarker = false;
    }


    public int adaptiveThreshWinSizeMin;
    public int adaptiveThreshWinSizeMax;
    public int adaptiveThreshWinSizeStep;
    public double adaptiveThreshConstant;
    public double minMarkerPerimeterRate;
    public double maxMarkerPerimeterRate;
    public double polygonalApproxAccuracyRate;
    public double minCornerDistanceRate;
    public int minDistanceToBorder;
    public double minMarkerDistanceRate;

    public boolean detectInvertedMarker;
    public double binarizationThresh;
    public double cannyThreshold;
    public double cannyThresholdLinking;
}
