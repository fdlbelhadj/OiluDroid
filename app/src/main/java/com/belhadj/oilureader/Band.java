package com.belhadj.oilureader;

public class Band {
    private double size;
    private Tri_histogramVH.BandType type;
    private int BaseLarge;

    public String bins; //{ get; set; }
    public int Start;//{ get; private set; }
    public int End;//{ get; private set; }

    public Band(int BaseLarge, int Start, int End, double size, Tri_histogramVH.BandType type) {
        this.Start = Start;
        this.End = End;
        this.size = size;
        this.type = type;
        this.BaseLarge = BaseLarge;
        bins = "";
    }

    public Tri_histogramVH.BandType getBandType() {
        return type;
    }

    int getBandWidth() {
        return End - Start;
    }

    boolean isWhiteBand() {
        return type == Tri_histogramVH.BandType.WhiteBand;
    }

    boolean isBlackBand() {
        return type == Tri_histogramVH.BandType.BlackBand;
    }

    double getBandSize() {
        return size;
    }

    int getBaseLarge() {
        return BaseLarge;
    }

    int getMinSize() {
        int s = 0;
        int bw = End - Start;
        if (type != Tri_histogramVH.BandType.WhiteBand)
            s = (BaseLarge - 2 * bw) * bw * 255; //    *bw / bw

        return s;
    }

}
