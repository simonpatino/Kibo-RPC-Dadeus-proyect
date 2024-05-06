package jp.jaxa.iss.kibo.rpc.sampleapk;

import jp.jaxa.iss.kibo.rpc.api.KiboRpcService;
import java.until.ArrayList;

import gov.nasa.arc.astrobee.types.Point;
import gov.nasa.arc.astrobee.types.Quaternion;

import org.opencv.core.Mat;
import org.opencv.aruco.Dictionary;
import org.opencv.aruco.Aruco;
import org.opencv.CvType;
import org.opencv.core.Mat;
import org.opencv.calib3d.Calib3d;

import java.io.IOException;
import java.io.InputStream;

import android.graphics.Bitmap;
import andoird.graphics.BitmapFactory;

import org.opencv.core.Core;
import org.opencv.android.Utils;
import org.opencv.imgproc.Imgproc;

/**
 * Class meant to handle commands from the Ground Data System and execute them in Astrobee.
 */

public class YourService extends KiboRpcService {
    @Override
    protected void runPlan1(){
        // The mission starts.
        api.startMission();

        // Move to a point.
        Point point = new Point(10.9d, -9.92284d, 5.195d);
        Quaternion quaternion = new Quaternion(0f, 0f, -0.707f, 0.707f);
        api.moveTo(point, quaternion, false);

        // Get a camera image.
        Mat image = api.getMatNavCam();

        /* *********************************************************************** */
        /* Write your code to recognize type and number of items in the each area! */
        /* *********************************************************************** */

        // When you recognize items, let’s set the type and number.
        api.setAreaInfo(1, "item_name", 1);

        // Method Aruco.detectMarkers

        //...
//
//
//...Detect AR
        Dictionary dicttionary= Aruco.getPredefinedDictionary(Aruco.DICT_5X5_250);
        List<Mat> corners = new ArrayList<>();
        Mat markerIds = new Mat();
        Aruco.detectMarkers(image_dictionary, corners, markerIds);

//Get Camera Matrix

        Mat cameraMatrix = new Mat(rows 3, cols 3, CvType.CV_64F);
        cameraMatrix.put(row 0, col 0, api.getNavCamIntrinsics()[0]);

//Get Lens distortion parameters

        Mat cameraCoefficients = new Mat(rows 1, cols 5, CvType.CV_64F);
        cameraCoefficients.put(row 0, col 0, api.getNavCamIntrinsics()[1]);
        cameraCoefficients.convertTo(cameraCoefficients, CvType.CV_64F);

// Undistort image

        Mat undistortImg = new Mat();
        Calib3d.undistort(image, undistortImg, cameraMatrix, cameraCoefficients);

//Image Distortion
//Straightening the Captured image
//Camera martix and lens distortion are needed

        api.getNavCamIntrinsics()
        api.getDockCamIntrinsics()


//Pattern matching
//Load template images
        Mat [] templates = ner Mat[TEMPLATE_FILE_NAME.length];
        for (int i = 0; i < TEMPLATE_FILE_NAME.lenght; i++) {
            try {
                //Open the template image file in Bitmap from the file name and convert to Mat
                InputStream inpurStream = getAssets().open(TEMPLATE_FILE_NAME[i]);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                Mat mat = new Mat();
                Utils.bitmapToMat(bitmap, mat);

                //Convert to grayscale
                Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY);

                //Assign to an array of templates
                templates[i] = mat;

                Input Stream.close();


            } catch (IOException e) {
                e.printStackTrace();
            }
        }

//Number of matches for each template
        int templateMatchCnt[] = new int[10];

//Get the number of template matches
        for (int tempNum = 0; tempNum < template.lenght; tempNum++) {
            //Number of matches
            int mstchCnt = 0;

            //Coordinates if the matched Location

            List<org.opencv.core.Point> matches = new ArrayList<>();

            //Loading tamplate image anf target item
            Mat tamplate = templates[tempNum].clone();
            Mat targetImg = undistortImg.clone();

            //Pattern matching
            int widthMin = 20; //[px]
            int widthMax = 100; //[px]
            int changeWidth = 5; //[px]
            int changeAngle = 45; //[degree]

            for (int i = widthMin; i <= widthMax; i= += changeWidth) {
                for (int j= 0; j <= 360; j+= changeAngle) {
                    Mat resizedTemp = resizeImg(template, i);
                    Mat rotResizedTemp = rotImg(resizedTemp, j);

                    Mat result = new Mat();
                    Imgproc.matchTemplate(targetImg, rotResizedTemp, result, Imgproc. TM_CCOEFF_NORMED);

                    //Get coordinates with similarity equal or greater than "threshold"
                    double threshold = 0.8;
                    Core.MinMaxLocResult mmlr = Core.minMaxLoc(result);
                    double maxVal = mmlr.maxVal;
                    if (maxVal >= threshold) {
                        //Getting only the results equal on greater than the defined AK threshold (80%)

                        Mat thresholdedResult = new Mat ();
                        Imgproc.threshold(result, thresholdedResult, threshold, maxval: 1.0, Imgproc.THRESH_TOZERO);

                        //Obtained mathches

                        for (int y = 0; y < thresholdedResult.rows(); y++) {
                            for (int x = 0; x < thresholdedResult.cols(); x++) {
                                if (thresholdedResult.get(y, x)[0] > 0) {
                                    matches.add(new org.opencv.core.Point(x, y));
                                }
                            }
                        }
                    }
                }
            }
            //Avoid Detecting the same location multiple times
            List<org.opencv.core.Point> filteredMatches = removeDuplicates(matches);
            matchCnt += filteredMatches.size();

            //Number of matches for each template
            templateMatchCnt[tempNum] = matchCnt;

        }

//When you recognize items, Let's set the type and number.

        int mostMatchTemplateNum = getMaxIndex(templateMatchCnt);
        api.setAreaInfo(areald: 1, TEMPLATE_NAME[mostMatchTemplateNum], templateMatchCnt[mostMatchTemplateNum]);



//Resize image
        private Mat resizeImg (Mat img, int width) {
            int height = (int) (img.rows() * ((double) width / img.cols()));
            Mat resizedImg = new Mat();
            Imgproc.resize(img, resizedImg, newSize(width, height));

            return resizedImg;
        }

// Rotate image - Defining methods
        private Mat rotImg (Mat img, int angle) {
            org.opencv.core.Point center = new org.opencv.core.Point (x: img.cols() / 2.0, y:img.rows() / 2.0);
            Mat rotatedMat = Imgproc.getRotationMatrix2D(center, angle, scale: 1.0);
            Mat rotatedImg = new Mat();
            Imgproc.warpAffine(img, rotatedImg, rotatedMat, img.size());

            return rotatedImg;

        }

//Remove multiple detections
        private static List<org.opencv.core.Point> removeDuplicates (List<org.opencv.core.Point> points) {
            doubles length = 10; // Width 10 px
            List<org.opencv.core.Point> filteredList = new ArrayList<>();

            for(org.opencv.core.Point point : point) {
                boolean inInclude = false;
                for (org.opencv.core.Point checkPoint : filteredList) {
                    double distance = calculateDistance(point, checkPoint);

                    if (distance <= length) {
                        inInclude = true;
                        break;
                    }
                }

                if (!isInclude) {
                    filteredList.add(point);
                }
            }
            return filteredList;
        }

//Find the distance between two points
        private static double calculateDistance (org.opencv.core.Point p1, org.opencv.core.Point p2) {
            double dx = p1.x - p2.x ;
            double dy = p1.y - p2.y ;
            return Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2));
        }

//Get the maximun value of an array
        private int getMaxIndex (int[] array) {
            int max = 0;
            int maxIndex = 0;

            //Find the index of the element with the largest value
            for (int i =0; i < array.length; i++) {
                if (array[i] > max) {
                    max = array[i];
                    maxIndex = i;
                }
            }
            return maxIndex;
        }

        /* **************************************************** */
        /* Let's move to the each area and recognize the items. */
        /* **************************************************** */

        // When you move to the front of the astronaut, report the rounding completion.
        api.reportRoundingCompletion();

        /* ********************************************************** */
        /* Write your code to recognize which item the astronaut has. */
        /* ********************************************************** */

        // Let's notify the astronaut when you recognize it.
        api.notifyRecognitionItem();

        /* ******************************************************************************************************* */
        /* Write your code to move Astrobee to the location of the target item (what the astronaut is looking for) */
        /* ******************************************************************************************************* */

        // Take a snapshot of the target item.
        api.takeTargetItemSnapshot();
    }

    @Override
    protected void runPlan2(){
       // write your plan 2 here.
    }

    @Override
    protected void runPlan3(){
        // write your plan 3 here.
    }

    // You can add your method.
    private String yourMethod(){
        return "your method";
    }
}

