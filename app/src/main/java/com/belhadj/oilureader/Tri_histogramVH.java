package com.belhadj.oilureader;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;


public class Tri_histogramVH {
    //TODO : include these in params
    //TODO : include these in params
    public static int MAX_BANDS_NUMBER_IN_TRIANGLE = 10;
    public static int MAX_BANDS_WIDTH_DIFF = 2;
    public static int MAX_BANDS_SIZE_DIFF = MAX_BANDS_WIDTH_DIFF * 255;
    public static float MIN_BAND_SIZE_FACTOR = 0.550F;
    public static int BLACK_VALUE = 255;    // inverted
    public static int WHITE_VALUE = 0;      //
    public static float EXPECTED_BAR_SUM_FACTOR = 2.0f/6;

    Vector<Float> cumulHoriz, cumulVert;
    public byte[] Mhh_Bars;

    List<com.belhadj.oilureader.Band> H_Bands, V_Bands; // list of zones on the current band that might contain oilu code data
    Mat src;

    //int BlackBandWidth = -1;
    private float WhiteBandWidth = -1;
    private float BlackBandWidth = -1;
    private final int srcWidth;
    private final boolean debug;

    public Tri_histogramVH(Mat src, boolean debug) {
        this.src = src;
        srcWidth = src.width();
        this.debug = debug;
    }

    public void CalculateDirectionalHisto(Direction direct) {
        if (src.empty()) return;

        if (direct != Direction.V_Direction) {
            Mat cum = new Mat();
            Core.reduce(src, cum, 1, Core.REDUCE_SUM, CvType.CV_32F);
            cumulHoriz = new Vector<>();

            Converters.Mat_to_vector_float(cum, cumulHoriz);
        }

        if (direct != Direction.H_Direction) {
            Mat cum = new Mat();
            Core.reduce(src, cum, 0, Core.REDUCE_SUM, CvType.CV_32F);
            cumulVert = new Vector<>();
            Core.transpose(cum,cum);
            Converters.Mat_to_vector_float(cum, cumulVert);
        }
    }

    public void GetHorizontalBands() {
        Vector<Float> v = cumulHoriz;
        if (v.size() == 0) return;

        H_Bands = new ArrayList<>();
        Mhh_Bars = new byte[v.size()];
        for (int k = 0; k < v.size(); k++) Mhh_Bars[k] = 1;

        int baseLarge = srcWidth;
        int i = 0;
        int indexNewBand = i;
        double sum = 0;

        BandType prevBarType = getBarType(v.get(i), 0);
        if (prevBarType == BandType.WhiteBand ) Mhh_Bars[i] = 0;//MatExtension.SetRowToValue(src, i, WHITE_VALUE);

        for (i = 1; i < v.size(); i++)
        {
            BandType curBarType = getBarType(v.get(i), i);
            if (curBarType == BandType.WhiteBand) Mhh_Bars[i] = 0;//MatExtension.SetRowToValue(src, i, WHITE_VALUE);

            if (curBarType != prevBarType)
            {
                H_Bands.add(new com.belhadj.oilureader.Band(baseLarge, indexNewBand, i, sum, prevBarType ));

                baseLarge -= 2 * (i - indexNewBand);
                prevBarType = curBarType;
                indexNewBand = i;
                sum = 0;
            }
            else sum += v.get(i);

        }
        // the last band ends at the border
        if (indexNewBand != v.size())
            H_Bands.add(new com.belhadj.oilureader.Band(baseLarge, indexNewBand, v.size(), sum , prevBarType ));
    }


    public void deNoiseHisto(Direction direction) {
        if (direction != Direction.V_Direction) {
            Mat m = new Mat();
            Imgproc.medianBlur(Converters.vector_float_to_Mat(cumulHoriz), m, 3);
            Converters.Mat_to_vector_float(m, cumulHoriz);
        }
        if (direction != Direction.H_Direction) {
            Mat m = new Mat();
            Imgproc.medianBlur(Converters.vector_float_to_Mat(cumulVert), m, 3);
            Converters.Mat_to_vector_float(m, cumulVert);
        }
    }

    public MarkerDtectionError Analyze_HorizBands() {
        if (H_Bands == null || H_Bands.size() < 2) return MarkerDtectionError.NO_BANDS_ERR;

        // delete the 1st band if it is white and its width is low
        // to correct square contours start
        if ((H_Bands.get(0).getBandType() == BandType.WhiteBand))//  && (H_Bands[0].getBandWidth() <3))
            H_Bands.remove(0);

        /// To be reviewed !!!!!!!
        // Test if the square is OILU marker
        // 1- The number of bands is 10 : 5 blacks and 5 whites.
        // 2- ALL THE SIDES ARE BLACK
        // 3- The cener is white
        // 4- The alternance is allmost high balcks + very low whites.
        // 6- black bands are almost equal in width
        // 7- proportional ascendant/descendant peaks
        // 5- the bands have equal average number of pixels

        if (H_Bands.size() > MAX_BANDS_NUMBER_IN_TRIANGLE)
            return MarkerDtectionError.MAX_BANDS_NUMMBER_ERR;   // TODO: eliminate bands, lateral, with low bandwidth

        if (H_Bands.get(0).isWhiteBand())
            return MarkerDtectionError.BLACK_SIDES_ERR;         // TODO: check if the marker is inverted

        if (H_Bands.get(H_Bands.size() - 1).isBlackBand())
            return MarkerDtectionError.WHITE_CENTER_BAND_ERR;

        // Alternance of PAIR bands
        if (H_Bands.size() % 2 != 0) return MarkerDtectionError.BANDS_ALTERN_ERR;
        BandType prevBandType = H_Bands.get(0).getBandType();
        for (int i = 1; i < H_Bands.size(); i++) {
            if (H_Bands.get(i).getBandType() == prevBandType)
                return MarkerDtectionError.BANDS_ALTERN_ERR;
            else prevBandType = H_Bands.get(i).getBandType();
        }

        // 6 black bands are equal in widths
//        int prevBandWidth = H_Bands.get(0).getBandWidth();
//        for (int i = 2; i < H_Bands.size(); i += 2) {
//            if (Math.abs(H_Bands.get(i).getBandWidth() - prevBandWidth) > MAX_BANDS_WIDTH_DIFF)
//                return MarkerDtectionError.BANDS_WIDTH_ERR;
//            else prevBandWidth = H_Bands.get(i).getBandWidth();
//        }

        // proportial descendant black bands Baselarge
        double prevBandBaseLareg = H_Bands.get(0).getBaseLarge();
        for (int i = 1; i < H_Bands.size(); i++) {
            if (H_Bands.get(i).getBaseLarge() > prevBandBaseLareg)
                return MarkerDtectionError.PROPORTIANL_BASE_WIDTH_ERR;
            else prevBandBaseLareg = H_Bands.get(i).getBaseLarge();
        }

        // get whiteband averages
        float sum = 0;
        for (int i = 1; i < H_Bands.size(); i += 2) sum += H_Bands.get(i).getBandWidth();
        WhiteBandWidth = sum / (MAX_BANDS_NUMBER_IN_TRIANGLE - H_Bands.size() / 2);

        sum = 0;
        for (int i = 0; i < H_Bands.size(); i += 2) sum += H_Bands.get(i).getBandWidth();
        BlackBandWidth = sum / (H_Bands.size() / 2);


        return MarkerDtectionError.VALID_MARKER;
    }

    public MarkerDtectionError isValid_OILU_Triangle() {
        //1-  start by calculating horizontal histo

        CalculateDirectionalHisto(Direction.H_Direction);
        deNoiseHisto(Direction.H_Direction);
        if (debug) {
            // dwin.DisplayMat(src);
        }
        GetHorizontalBands();

        //2- Calculate vertical histo
        CalculateDirectionalHisto(Direction.V_Direction);
        deNoiseHisto(Direction.V_Direction);
        if (debug) {
            // dwin.DisplayMat(src);
            //dwin.showHisto("V plot after denoising", null, getCumuls(Direction.V_Direction), Color.Red);

        }

        MarkerDtectionError ret = Analyze_HorizBands();
        if (ret != MarkerDtectionError.VALID_MARKER) return ret;

        int nbBins = decode_HorizBands();

        if (nbBins != MAX_BANDS_NUMBER_IN_TRIANGLE)
            return MarkerDtectionError.BANDS_SIZE_ERR;
        return MarkerDtectionError.VALID_MARKER;

        // analyze vertical histo
        //ret = Analyze_Vert_Bands();
        //if (nb_H_BlackBands != V_Bands.size()) return MarkerDtectionError.NO_CORRESPONDANCE_H_V;

    }

    public String getTriangleBins() {
        String idd = "";
        for (int i = 0; i < H_Bands.size(); i++)
        {
            com.belhadj.oilureader.Band band = H_Bands.get(i);
            if(band.getBandType() == BandType.BlackBand)
                idd += band.bins;
            else
            {
                for (int j = 1; j < band.bins.length(); j+=2)
                {
                    idd = idd + band.bins.charAt(j);
                }
            }

        }
        return idd;
    }

    private int decode_HorizBands() {
        if (H_Bands == null) return -1;

        int nbBins = -1;
        int i = 0;
        while ( i < H_Bands.size() )
        {
            com.belhadj.oilureader.Band band = H_Bands.get(i);
            //if (band.isBlackBand())     // IT should CONTAIN ONLY ONE BEAN = 1, elsewhere
            {                           // CHECK THE CASE 
                if ((band.getBandSize() > band.getMinSize() * MIN_BAND_SIZE_FACTOR) ||
                        (band.getBandWidth() >= BlackBandWidth * MIN_BAND_SIZE_FACTOR))
                    band.bins = "1";
                else
                    band.bins = "0";    // TODO : return Error
                nbBins++;
            }
            i++;
            band = H_Bands.get(i);
            //if (band.isWhiteBand())
            {
                // TODO : DETECTANY ANOMALY FROM band size
                double d = 1.0 * band.getBandWidth() / WhiteBandWidth;
                int nb = (int) Math.round(d);
                // todo : if treating the last band ; conisder all resting bands.
                if (i == H_Bands.size() - 1)
                    //nb = (int) Math.Truncate(d); 
                    nb = MAX_BANDS_NUMBER_IN_TRIANGLE - nbBins;
                for (int j = 0; j < nb; j++) band.bins += "0";
                nbBins += nb;
            }
            i++;

        }

        if (nbBins != MAX_BANDS_NUMBER_IN_TRIANGLE)
            return -1;
        return nbBins;
    }

    public String decode_HorizBandsAccordingToTemplate(List<com.belhadj.oilureader.Band> template )
    {
        if (template == null) return "-1";

        String Bins = "";

        for (int i = 0; i < template.size(); i+=2)
        {
            com.belhadj.oilureader.Band band = template.get(i);

            int s = 0; // To check the number of included bins
            for (int j = band.Start; j < band.End; j++)
            {
                s += Mhh_Bars[j];
            }

            if ( s >= band.getBandWidth() * MIN_BAND_SIZE_FACTOR) Bins += "1";
            else
                Bins += "0";    // TODO : return Error
        }

        return Bins;
    }


    private MarkerDtectionError Check_Vert_Bands() {
        // 
        if (cumulVert == null) return MarkerDtectionError.NO_BANDS_ERR;
        int expected_V_BandsNbr = (int) Math.round(1.0 * Collections.max(cumulVert) / (BlackBandWidth * 255));

        V_Bands = new ArrayList<com.belhadj.oilureader.Band>();
        float previ = cumulVert.get(0);
        float prevj = cumulVert.get(cumulVert.size() - 1);
        if (previ - prevj > MAX_BANDS_SIZE_DIFF) return MarkerDtectionError.VERT_SYM_ERR;
        // TODO : make VERT_HISTO_EXPLOR_STEP dependent to the blackbandwith
        for (int i = 1; i < cumulVert.size() / 2; i++) {
            // CHECK THE SYMETRIE CONDITION
            if (cumulVert.get(i) - cumulVert.get(cumulVert.size() - 1 - i) > MAX_BANDS_SIZE_DIFF)
                return MarkerDtectionError.VERT_SYM_ERR;
            // CHECK THE PROPRTIONAL INCREASING AND SYMETRIC CONDITION
            if (cumulVert.get(i) - previ < -MAX_BANDS_SIZE_DIFF ||
                    cumulVert.get(cumulVert.size() - 1 - i) - prevj < -MAX_BANDS_SIZE_DIFF)
                return MarkerDtectionError.PROPORTIANL_SIZE_ERR;

            int L = 0;
            int ss = i;
            // COUNT THE NUMBER OF EQUAL BINS
            while ((i < cumulVert.size() / 2) && cumulVert.get(i) - previ == 0 &&
                    cumulVert.get(cumulVert.size() - 1 - i) - prevj == 0) {
                L++;
                previ = cumulVert.get(i);
                prevj = cumulVert.get(cumulVert.size() - 1 - i);
                i++;
            }
            if (L > 1)//&&  Math.Abs(L - BlackBandWidth) <= 2
            {
                // new band detected
                V_Bands.add(new com.belhadj.oilureader.Band(-1, ss, cumulVert.size() - ss - 1, L, BandType.VerticalBand));
            } else {
                previ = cumulVert.get(i);
                prevj = cumulVert.get(cumulVert.size() - 1 - i);
            }
        }

        return
                //V_Bands.size() == expected_V_BandsNbr ?
                MarkerDtectionError.VALID_MARKER
                //: MarkerDtectionError.INVALID_EXPECTED_VBANDS_NBR
                ;
    }

       private BandType getBarType(double v, int irow) {
        /// ..............
        ///  ............
        ///   .........
        ///     .   .   <---- to be eliminated   
        // return v  > threshold; 

           float expected_Bar_Sum = 255 * (srcWidth - 2 * irow);
           float thresh = expected_Bar_Sum * EXPECTED_BAR_SUM_FACTOR;
           if (thresh < 255 * 6) thresh = 255 *6 ;
           return v > thresh ? BandType.BlackBand : BandType.WhiteBand;
    }


    public enum BandType {
        BlackBand,
        WhiteBand,
        VerticalBand
    }



    public enum Direction {
        H_Direction,
        V_Direction,
        All_Directions
    }

    public enum MarkerDtectionError {
        VALID_MARKER ,
        MAX_BANDS_NUMMBER_ERR,
        BLACK_SIDES_ERR,
        WHITE_CENTER_BAND_ERR,
        BANDS_ALTERN_ERR,
        BANDS_SIZE_ERR,
        BANDS_WIDTH_ERR,
        PROPORTIANL_BASE_WIDTH_ERR,
        NO_BANDS_ERR,
        PROPORTIANL_SIZE_ERR,
        VERT_SYM_ERR,
        INVALID_EXPECTED_VBANDS_NBR,
        NO_CORRESPONDANCE_H_V
    }

}

