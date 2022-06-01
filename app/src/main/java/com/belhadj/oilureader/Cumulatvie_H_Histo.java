package com.belhadj.oilureader;

import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.List;

public class Cumulatvie_H_Histo {
    public static int MAX_BANDS_NUMBER_IN_TRIANGLE = 10;


    private Mat src;
    private  Tri_histogramVH[]  vHs;
    private byte[] Merged_H_Histo;

    private  int srcWidth;
    private  int srcHeiht;
    private  boolean debug;
    //private debugWindow dwin;
    public int ID ; //{ get; private set; }



    public List<Band> Merged_H_template; // list of zones on the current band that might contain barcode data

    public int totalDetectedBands ;//{ get; private set; }

    public Cumulatvie_H_Histo(Mat src, Tri_histogramVH vH0, Tri_histogramVH vH1, Tri_histogramVH vH2, Tri_histogramVH vH3, boolean debug )
    {
        this.src = src ;
        vHs = new Tri_histogramVH[4];
        vHs[0] = vH0 ;
        vHs[1] = vH1 ;
        vHs[2] = vH2 ;
        vHs[3] = vH3 ;

        srcWidth = src.width();
        srcHeiht = src.height();

        this.debug = debug;
        //if (this.debug) dwin = new debugWindow();
        ID = -1;
    }

    public Cumulatvie_H_Histo(Mat src, Tri_histogramVH[] vHs, boolean debug )
    {
        this.src = src ;

        this.vHs = vHs ;
        srcWidth = src.width();
        srcHeiht = src.height();

        this.debug = debug;
        //if (this.debug) dwin = new debugWindow();
        ID = -1;
    }

    boolean isValidOiluMarker()
    {
        MergeTriHistos();
        
        FindMergd_Template();
        return CheckMarged_Template() == Tri_histogramVH.MarkerDtectionError.VALID_MARKER;

    }

    public String[] IdentifyMarker()
    {
        if (isValidOiluMarker() == false) return null;
        String[] bins = new String[4];
        for (int i = 0; i < 4; i++)
        {
            bins[i] = vHs[i].decode_HorizBandsAccordingToTemplate(Merged_H_template);
        }


        return bins;
    }


    void MergeTriHistos()
    {
        Merged_H_Histo = new byte[srcHeiht/2];
        for (int i = 0; i < Merged_H_Histo.length; i++)
        {
            Merged_H_Histo[i] = (byte)(vHs[0].Mhh_Bars[i] | vHs[1].Mhh_Bars[i] | vHs[2].Mhh_Bars[i] | vHs[3].Mhh_Bars[i]);
        }
    }

    void FindMergd_Template()
    {
        byte[] v = Merged_H_Histo;
        Merged_H_template = new ArrayList<>();

        int baseLarge = srcWidth;
        int i = 0;
        int indexNewBand = i;
        double sum = 0;

        Tri_histogramVH.BandType prevBarType = getBarType(v[i], 0);

        for (i = 1; i < v.length; i++)
        {
            Tri_histogramVH.BandType curBarType = (getBarType(v[i], i));

            if (curBarType != prevBarType)
            {
                Merged_H_template.add(new Band(baseLarge, indexNewBand, i, sum, prevBarType));

                baseLarge -= 2 * (i - indexNewBand);
                prevBarType = curBarType;
                indexNewBand = i;
                sum = 0;
            }
            else sum += v[i];

        }
        // the last band ends at the border
        if (indexNewBand != v.length)
            Merged_H_template.add(new Band(baseLarge, indexNewBand, v.length, sum, prevBarType));
    }

//    void ShowHistos()
//    {
//        if (debug)
//        {
//            dwin.showHisto("0 - LEFT Hist plot ", null, vHs[0].Mhh_Bars, Color.Black);
//            dwin.showHisto("1 - Bot Hist plot ", null, vHs[1].Mhh_Bars, Color.Red);
//            dwin.showHisto("2 - Right Hist plot ", null, vHs[2].Mhh_Bars, Color.Green);
//            dwin.showHisto("3 - TOP Hist plot ", null, vHs[3].Mhh_Bars, Color.Blue);
//
//        }
//    }

    Tri_histogramVH.MarkerDtectionError CheckMarged_Template()
    {
        if (Merged_H_template == null || Merged_H_template.size() < 2) return Tri_histogramVH.MarkerDtectionError.NO_BANDS_ERR;

        // to correct square contours start
        // delete the 1st band if it is white and its width is low
        if ((Merged_H_template.get(0).getBandType() == Tri_histogramVH.BandType.WhiteBand))//  && (H_Bands[0].getBandWidth() <3))
            Merged_H_template.remove(0);

        /// To be reviewed !!!!!!!
        // Test if the square is OILU marker
        // 1- The number of bands is 10 : 5 blacks and 5 whites.
        // 2- ALL THE SIDES ARE BLACK
        // 3- The center is white
        // 4- The alternance is allmost high balcks + very low whites.
        // 6- black bands are almost equal in width
        // 7- proportional ascendant/descendant peaks
        // 5- the bands have equal average number of pixels

        //if (Merged_H_Bands.Count > MAX_BANDS_NUMBER_IN_TRIANGLE)
        //    return MarkerDtectionError.MAX_BANDS_NUMMBER_ERR;   // TODO: eliminate bands, lateral, with low bandwidth

        totalDetectedBands = Merged_H_template.size();

        if (Merged_H_template.get(0).isWhiteBand())
            return Tri_histogramVH.MarkerDtectionError.BLACK_SIDES_ERR;         // TODO: check if the marker is inverted

        if (Merged_H_template.get(totalDetectedBands - 1).isBlackBand()) return Tri_histogramVH.MarkerDtectionError.WHITE_CENTER_BAND_ERR;

        // Alternance of PAIR bands
        if (Merged_H_template.size() % 2 != 0) return Tri_histogramVH.MarkerDtectionError.BANDS_ALTERN_ERR;
        Tri_histogramVH.BandType prevBandType = Merged_H_template.get(0).getBandType();
        for (int i = 1; i < totalDetectedBands; i++)
        {
            if (Merged_H_template.get(i).getBandType() == prevBandType) return Tri_histogramVH.MarkerDtectionError.BANDS_ALTERN_ERR;
            else prevBandType = Merged_H_template.get(i).getBandType();
        }

        // 6 black bands are equal in widths
        //int prevBandWidth = H_Bands[0].getBandWidth();
        //for (int i = 2; i < H_Bands.Count; i += 2)
        //{
        //    if (Math.Abs(H_Bands[i].getBandWidth() - prevBandWidth) > MAX_BANDS_WIDTH_DIFF)
        //        return MarkerDtectionError.BANDS_WIDTH_ERR;
        //    else prevBandWidth = H_Bands[i].getBandWidth();
        //}

        // proportial descendant black bands Baselarge
        double prevBandBaseLareg = Merged_H_template.get(0).getBaseLarge();
        for (int i = 1; i < totalDetectedBands; i++)
        {
            if (Merged_H_template.get(i).getBaseLarge() > prevBandBaseLareg) return Tri_histogramVH.MarkerDtectionError.PROPORTIANL_BASE_WIDTH_ERR;
            else prevBandBaseLareg = Merged_H_template.get(i).getBaseLarge();
        }

        //Decode Horiz bands in each triangle according to the merged Gabarit



        return Tri_histogramVH.MarkerDtectionError.VALID_MARKER;
    }



    private Tri_histogramVH.BandType getBarType(double v, int irow)
    {
        return v == 1 ? Tri_histogramVH.BandType.BlackBand : Tri_histogramVH.BandType.WhiteBand;
    }


}
