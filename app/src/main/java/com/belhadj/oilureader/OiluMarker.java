package com.belhadj.oilureader;


import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;


public class OiluMarker{


    public Vector<Point> cornersInImg;
    private double threshold;
    private Size CanonicalMarkerSize;

    public Vector<Point> CanonicalCorners ; //{ get; private set; }

    public String Id ; //{ get; set; }

    public Mat DataMat ; //{ get; private set; }

    public OiluMarker()
    {
        this(new Vector<Point>(),new Size(100,100));
    }


    public OiluMarker(Vector<Point> corners, Size canonicalMarkerSize )
    {
        Id = "-1";
        this.cornersInImg = corners;
        this.CanonicalMarkerSize = canonicalMarkerSize;
        CanonicalCorners = new Vector<>();
        CanonicalCorners.add(new Point(0, 0));
        CanonicalCorners.add( new Point((int) (canonicalMarkerSize.width - 1), 0));
        CanonicalCorners.add(new Point((int) canonicalMarkerSize.width - 1, (int) canonicalMarkerSize.height - 1));
        CanonicalCorners.add(new Point(0, (int) canonicalMarkerSize.height - 1));

        threshold = 127;
    }

    public void drawMarker(Mat inMat, String text, Scalar cinvalid, Scalar cvalid, Scalar caxis)
    {
        Scalar c1 = Id == "" ? cinvalid : cvalid;

        List<MatOfPoint> ps = new ArrayList<>();

        MatOfPoint matOfPoint = new MatOfPoint(Converters.vector_Point_to_Mat(cornersInImg));
        ps.add(matOfPoint);
        Imgproc.polylines(inMat, ps, true,c1);

        // draw axis
        Point p1 = cornersInImg.get(1);
        Point p2 = cornersInImg.get(2);
        Point p0 = cornersInImg.get(0);
        Point p3 = cornersInImg.get(3);
        Imgproc.line(inMat, p0, p2,caxis);
        Imgproc.line(inMat, p1, p3, caxis);

        Imgproc.putText(inMat, text, p0,1, 5, new Scalar(255, 0, 0),2);//Scalar(36, 255, 12)
    }





    public String getMarkerId(boolean debug ) throws Exception {
        String[] binaryCodes = new String[] { "", "", "", "" };

        // convert to binary
        // TODO : multithreshold
        Mat binaryInputImg = new Mat();
        Imgproc.threshold(DataMat, binaryInputImg, threshold, 255,Imgproc.THRESH_BINARY_INV | Imgproc.THRESH_OTSU);
        if (debug)
        {
            //HighGui.imshow("Binarized DATAMAT", binaryInputImg);
            //HighGui.waitKey();
        }


        // get the center of the quadrangle
        Point midPt = TwoLinesintersection(CanonicalCorners.get(0), CanonicalCorners.get(2), CanonicalCorners.get(1), CanonicalCorners.get(3));
        if (midPt == null) throw new Exception("invalid non convex quad");

        // Process vertical Triangles, then horizontal triangles
        //todo: launch in parallel
        Point[][] q = new Point[4][];
        q[0] = new Point[] { CanonicalCorners.get(0), CanonicalCorners.get(1), midPt }; // top tri
        q[1] = new Point[] { midPt, CanonicalCorners.get(2), CanonicalCorners.get(3) }; // bottom tri
        q[2] = new Point[] { CanonicalCorners.get(0), midPt, CanonicalCorners.get(3) }; // left tri
        q[3] = new Point[] { midPt, CanonicalCorners.get(1), CanonicalCorners.get(2) }; // right tri

        for (int i = 0; i < q.length; i++)
        //Parallel.For(0, 4, i =>
        {
            Mat tr = ExtractROI(binaryInputImg, q[i]);
            if (i == 1) Core.flip(tr, tr,  0);  //FlipType.Vertical
            if (i == 2)
            {
                Core.transpose(tr, tr);
                Core.flip(tr, tr, 1); //FlipType.Horizontal
            }
            if (i == 3)
            {
                Core.transpose(tr, tr);
                Core.flip(tr, tr, 0);    //FlipType.Vertical
            }
            tr = new Mat(tr, new Rect(0, 0, tr.width(), tr.height() / 2));
            Tri_histogramVH histo = new Tri_histogramVH(tr, debug);
            if (histo.isValid_OILU_Triangle() == Tri_histogramVH.MarkerDtectionError.VALID_MARKER)
                binaryCodes[i] = histo.getTriangleBins();
        }
        //);
        return Id = Decode(binaryCodes);
    }


    private String Decode(String[] idds) //, int nRotations) 
    {
        if (idds == null) return "";

        if ((idds[0].length() == 0)|| (idds[1].length() == 0) || (idds[2].length() == 0) || (idds[3].length() == 0)) return "";
        if (idds[0].length() + idds[1].length() + idds[2].length() + idds[3].length() != 4* idds[0].length()) return "";

        String digit = "";
        for (int i = 1; i < idds[0].length(); i++)
        {
            int b = idds[2].charAt(i) == '1' ? 1 : 0;

            b <<= 1;
            b = idds[1].charAt(i) == '1' ? b|1 : b|0;

            b <<= 1;
            b = idds[3].charAt(i) == '1' ? b|1 : b|0;

            b <<= 1;
            b = idds[0].charAt(i) == '1' ? b|1 : b|0;

            byte bb = binarySegmentsToOilu((byte)b);
            if (bb == -1) return "";
            else digit += bb;
        }

        return digit;
    }

    byte oiluToBinarySegments(byte digit)
    {

        switch (digit)
        {
            case 0:
                return 0b1111;
            case 1:
                return 0b1000;
            case 2:
                return 0b1100;
            case 3:
                return 0b1110;
            case 4:
                return 0b0110;
            case 5:
                return 0b0111;
            case 6:
                return 0b0011;
            case 7:
                return 0b1011;
            case 8:
                return 0b1001;
            case 9:
                return 0b1101;
            default:
                return 0;
        }
    }

    byte binarySegmentsToOilu(byte segments)
    {

        switch (segments)
        {
            case 0:
                return -1;
            case 1:
                return 1;// -1;
            case 2:
                return 1;// -1;
            case 3:
                return 6;
            case 4:
                return 1;// -1;
            case 5:
                return 1;// -1;
            case 6:
                return 4;
            case 7:
                return 5;
            case 8:
                return 1;
            case 9:
                return 8;
            case 10:
                return -1;
            case 11:
                return 7;
            case 12:
                return 2;
            case 13:
                return 9;
            case 14:
                return 3;
            case 15:
                return 0;
            default:
                return -1;
        }
    }



    public static Point TwoLinesintersection(Point p1, Point p2, Point q1, Point q2)
    {
        Point x = new Point(q1.x - p1.x, q1.y - p1.y);
        Point d1 = new Point(p2.x - p1.x, p2.y - p1.y);
        Point d2 = new Point(q2.x - q1.x, q2.y - q1.y);

        double cross = d1.x * d2.y - d1.y * d2.x;
        if (Math.abs(cross) < /*EPS*/1e-8)
            return null;

        double t1 = (x.x * d2.y - x.y * d2.x) / cross;
        return new Point(p1.x + d1.x * t1, p1.y + d1.y * t1);
    }

    /// <summary>
    ///
    /// </summary>
    /// <param name="src"></param>
    /// <param name="ps"></param>
    /// <returns></returns>
    private Mat ExtractROI(Mat src, Point[] ps )
    {
        Mat mask = new Mat(src.size(), src.type());
        mask.setTo(new Scalar(0, 0, 0));

        List<MatOfPoint> list = new ArrayList<MatOfPoint>();
        MatOfPoint v = new MatOfPoint();
        v.fromArray(ps);
        list.add(v);
        Imgproc.fillPoly(mask, list, new Scalar(255, 255, 255));

        Mat mmmm = new Mat(src.size(), src.type());
        Core.bitwise_and(src, mask, mmmm);

        return mmmm;
    }


    public void setDataMat(Mat m) { this.DataMat = m;}

    void correctPerspective(Mat inuputImg, Mat canonicalMarker)
    {
        Mat pointsIn = new Mat(4,1, CvType.CV_32FC2);
        pointsIn.put(0,0,  cornersInImg.get(0).x,cornersInImg.get(0).y,
                                    cornersInImg.get(1).x,cornersInImg.get(1).y,
                                    cornersInImg.get(2).x,cornersInImg.get(2).y,
                                    cornersInImg.get(3).x,cornersInImg.get(3).y);
        Mat pointsRes = new Mat(4,1,CvType.CV_32FC2);
        pointsRes.put(0,0, CanonicalCorners.get(0).x,CanonicalCorners.get(0).y,
                                    CanonicalCorners.get(1).x,CanonicalCorners.get(1).y,
                                    CanonicalCorners.get(2).x,CanonicalCorners.get(2).y,
                                    CanonicalCorners.get(3).x,CanonicalCorners.get(3).y);

        Mat M = Imgproc.getPerspectiveTransform(pointsIn, pointsRes);

        // Apply homography Transform image to get a canonical marker image
        Imgproc.warpPerspective(inuputImg, canonicalMarker, M, CanonicalMarkerSize);
        //CvInvoke.Imshow("perspetive", canonicalMarker);
    }

    public String getMarkerId_WithMHH(String[] bins, boolean debug) throws Exception {
        bins = new String[] { "", "", "", "" };
        Point midPt = new Point(-1, -1);
        // convert to binary
        // TODO : multithreshold
        Mat binaryInputImg = new Mat();
        Imgproc.threshold(DataMat, binaryInputImg, threshold, 255, Imgproc.THRESH_BINARY_INV | Imgproc.THRESH_OTSU);
//        if (debug)
//        {
//            CvInvoke.Imshow("Binarized DATAMAT", binaryInputImg);
//            CvInvoke.WaitKey();
//        }


        // get the center of the quadrangle
        midPt = TwoLinesintersection(CanonicalCorners.get(0), CanonicalCorners.get(2), CanonicalCorners.get(1), CanonicalCorners.get(3));
        if (midPt == null) throw new Exception("invalid non convex quad");

        // Process vertical Triangles, then horizontal triangles
        //todo: launch in parallel
        Point[][] q = new Point[4][];
        q[0] = new Point[] { CanonicalCorners.get(0), CanonicalCorners.get(1), midPt }; // top tri
        q[1] = new Point[] { midPt, CanonicalCorners.get(2), CanonicalCorners.get(3) }; // bottom tri
        q[2] = new Point[] { CanonicalCorners.get(0), midPt, CanonicalCorners.get(3) }; // left tri
        q[3] = new Point[] { midPt, CanonicalCorners.get(1), CanonicalCorners.get(2) }; // right tri

        Tri_histogramVH[] triHistos = new Tri_histogramVH[4];
        for (int i = 0; i < q.length; i++)
        //Parallel.For(0, 4, i =>
        {
            Mat tr = ExtractROI(binaryInputImg, q[i]);
            if (i == 1) Core.flip(tr, tr, 0);  //FlipType.Vertical
            if (i == 2) {
                Core.transpose(tr, tr);
                Core.flip(tr, tr, 1); //FlipType.Horizontal
            }
            if (i == 3) {
                Core.transpose(tr, tr);
                Core.flip(tr, tr, 0);    //FlipType.Vertical
            }
            tr = new Mat(tr, new Rect(0, 0, tr.width(), tr.height() / 2));
            triHistos[i] = new Tri_histogramVH(tr, debug);
            triHistos[i].CalculateDirectionalHisto(Tri_histogramVH.Direction.All_Directions);

            triHistos[i].deNoiseHisto(Tri_histogramVH.Direction.All_Directions);
            triHistos[i].GetHorizontalBands();
            //ret =
            triHistos[i].Analyze_HorizBands();
            //if (ret != MarkerDtectionError.VALID_MARKER) bins[i] = "-1";

        }
        //);

        Cumulatvie_H_Histo Cum_H_Template = new Cumulatvie_H_Histo(binaryInputImg, triHistos, debug);
        //if (debug) Cum_H_Template.ShowHistos();
        //if (Cum_H_Template.isValidOiluMarker() == false) return Id = "-1";
        bins = Cum_H_Template.IdentifyMarker();
        return Id = Decode(bins);
    }


    public Mat getDataMat() {
        return DataMat;
    }
}