package jp.jaxa.iss.kibo.rpc.defaultapk;  //sample apk

import jp.jaxa.iss.kibo.rpc.api.KiboRpcService;
import java.util.ArrayList;
import java.util.List;

import gov.nasa.arc.astrobee.types.Point;
import gov.nasa.arc.astrobee.types.Quaternion;

import org.opencv.core.Mat;
import org.opencv.aruco.Dictionary;
import org.opencv.aruco.Aruco;
import org.opencv.core.CvType;


import org.opencv.calib3d.Calib3d;
import java.io.IOException;
import java.io.InputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import org.opencv.core.Core;
import org.opencv.android.Utils;
import org.opencv.imgproc.Imgproc;

import org.opencv.core.Size;
import org.opencv.core.Core;


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
        api.moveTo(point, quaternion, true);

        // Moverse a un punto para esquivar la KOZ
        Point point1_1 = new Point(11d, -9.92284, 5.195d);
        Quaternion quaternion1_1 = new Quaternion(0f, 0f, -0.707f, 0.707f);
        api.moveTo(point1_1, quaternion1_1, true);

        // Moverse a un punto para esquivar la KOZ
        Point point1_2 = new Point(11d, -9.25d, 5.195d);
        Quaternion quaternion1_2 = new Quaternion(0f, 0f, -0.707f, 0.707f);
        api.moveTo(point1_2, quaternion1_2, true);

        // Definir area dos (disminuye en z, baja al "piso")
        Point point2 = new Point(11d, -9.15d, 4.5);
        Quaternion quaternion2 = new Quaternion(0.707f, 0.707f,0f, 0f);
        api.moveTo(point2, quaternion2,true);

        // Get a camera image.
        Mat image = api.getMatNavCam();

        /* *********************************************************************** */
        /* Write your code to recognize type and number of items in the each area! */
        /* *********************************************************************** */

//...Detect AR
        Dictionary dictionary= Aruco.getPredefinedDictionary(Aruco.DICT_5X5_250);
        List<Mat> corners = new ArrayList<>();
        Mat markerIds = new Mat();
        Aruco.detectMarkers(image,dictionary, corners, markerIds);

        //importa un diccionario de los AR establecidos
        // utiliza 5p x 5p , 250 diferentes marcadores
        // crea las matrix de esquinas y de los id
        // los ids ya vienen preseleccionados del diccionarios. para detectar selecciona las 4 esquinas. la superior izquierda es la que meda la direccion asi este
        //rotado sobre el mismo plano

//Get Camera Matrix


        Mat cameraMatrix = new Mat( 3,  3, CvType.CV_64F);
        cameraMatrix.put( 0,  0, api.getNavCamIntrinsics()[0]);  // what is the  index 0 ?

        // la matrix de la camara

//Get Lens distortion parameters

        Mat cameraCoefficients = new Mat(1, 5, CvType.CV_64F);
        cameraCoefficients.put(0,  0, api.getNavCamIntrinsics()[1]);  // what is the  index1 ?

        // los coeficientes de distorcion
        cameraCoefficients.convertTo(cameraCoefficients, CvType.CV_64F); // crea una matrix para almacenar  los  datos de la camara

        // reafirma el formato

//undistor image

        Mat undistortImg = new Mat();
        Calib3d.undistort(image, undistortImg, cameraMatrix, cameraCoefficients);

        // recibe un input 3d de la camara y lo convierte a cooerdenadas . basicamente a plana la imagen

//Image Distortion
//Straightening the Captured image
//Camera martix and lens distortion are needed

        api.getNavCamIntrinsics();
        api.getDockCamIntrinsics();




//Pattern matching
//Load template images
        String[] TEMPLATE_FILE_NAME = {"goggle.png","beaker.png","hammer.png","kapton_tape.png","pipette.png","screwdriver.png","thermometer.png","top.png","watch.png","wrench.png"};
        String[] TEMPLATE_NAME =  { "goggle","beaker","hammer","kapton tape","pipette","screwdriver","thermometer","top","watch","wrench"};

        Mat[] templates = new Mat[TEMPLATE_FILE_NAME.length];
        for (int i = 0; i < TEMPLATE_FILE_NAME.length ; i++) {
            try {

                Log.i("debug", TEMPLATE_FILE_NAME[i]);
                //Open the template image file in Bitmap from the file name and convert to Mat
                InputStream inputStream = getAssets().open(TEMPLATE_FILE_NAME[i]);



                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);   /*  es un formato de imagen con bits
                . el codigo lo esta convirtiendo en matrix con el mat para luego hacere el analisis con el opencv*/
                Mat mat = new Mat();
                Utils.bitmapToMat(bitmap, mat);

                //Convert to grayscale
                Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY); // es una clase de opencv para hacer procesamiento de imagenes

                //cvcolor es el metodo  , 1 mat es la imagen de entrada, 2 mat es la salida con el cambio

                // BGR son las entradas del color del input, gray lleva la imagen a una escala de grises

                //Assign to an array of templates
                templates[i] = mat;

                Log.i("debug", "matrix" + templates[i]);

                inputStream.close();

            } catch (IOException e) {
                e.printStackTrace();  //  busca excepciones  del estilo IOExeption INPUT / OUTPUT
            }
        }



//Number of matches for each template
        int templateMatchCnt[] = new int[10];



//Get the number of template matches
        for (int tempNum = 0; tempNum < templates.length; tempNum++) {
            //Number of matches
            int matchCnt = 0;

            //Coordinates if the matched Location

            List<org.opencv.core.Point> matches = new ArrayList<>();

            //Loading tamplate image anf target item
            Mat template = templates[tempNum].clone();
            Mat targetImg = undistortImg.clone();

            //Pattern matching
            int widthMin = 20; //[px]
            int widthMax = 100; //[px]
            int changeWidth = 5; //[px]
            int changeAngle = 45; //[degree]

            for (int i = widthMin; i <= widthMax; i += changeWidth) {
                for (int j= 0; j <= 360; j+= changeAngle) {

                    Mat resizedTemp = resizeImg(template, i);
                    Mat rotResizedTemp = rotImg(resizedTemp, j);

                    Mat result = new Mat();
                    Imgproc.matchTemplate(targetImg, rotResizedTemp, result, Imgproc.TM_CCOEFF_NORMED); //

                    //Get coordinates with similarity equal or greater than "threshold"
                    double threshold = 0.8;
                    Core.MinMaxLocResult mmlr = Core.minMaxLoc(result);
                    double maxVal = mmlr.maxVal;

                    if (maxVal >= threshold) {
                        //Getting only the results equal on greater than the defined AK threshold (80%)

                        Mat thresholdedResult = new Mat ();
                        Imgproc.threshold(result, thresholdedResult, threshold, 1.0, Imgproc.THRESH_TOZERO); //

                        /* primero hace una matrix con los resultados , tiene unos parametros que son la matrix
                        * , el thresholdy el maximo (1) , todos los menores los que estan menores a 0.8 los hace 0 , solo me quedan en la matrix los que estan entre
                        * 0.8 y 1 */

                        //Obtained matches

                        for (int y = 0; y < thresholdedResult.rows(); y++) {
                            for (int x = 0; x < thresholdedResult.cols(); x++) {
                                if (thresholdedResult.get(y, x)[0] > 0) {
                                    matches.add(new org.opencv.core.Point(x,y));
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



        api.setAreaInfo( 1, TEMPLATE_NAME[mostMatchTemplateNum], templateMatchCnt[mostMatchTemplateNum]); //check this line

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

    //################################# FUNCTIONS ####################################

    // You can add your method.
    private String yourMethod(){
        return "your method";
    }

    //Resize image
    private Mat resizeImg (Mat img, int width) {
        int height = (int) (img.rows() * ((double) width / img.cols()));
        Mat resizedImg = new Mat();
        Imgproc.resize(img, resizedImg, new Size(width, height)); //  lo mismo de python

        return resizedImg;

    }

    // Rotate image - Defining methods
    private Mat rotImg (Mat img, int angle) {
        org.opencv.core.Point center = new org.opencv.core.Point (img.cols() / 2.0, img.rows() / 2.0);
        Mat rotatedMat = Imgproc.getRotationMatrix2D(center, angle,  1.0);
        Mat rotatedImg = new Mat();
        Imgproc.warpAffine(img, rotatedImg, rotatedMat, img.size());

        return rotatedImg;

    }

    //Remove multiple detections
    private static List<org.opencv.core.Point> removeDuplicates (List<org.opencv.core.Point> points) {
        double length = 10; // Width 10 px
        List<org.opencv.core.Point> filteredList = new ArrayList<>();

        for(org.opencv.core.Point point : points) {
            boolean isInclude = false;
            for (org.opencv.core.Point checkPoint : filteredList) {
                double distance = calculateDistance(point, checkPoint);

                if (distance <= length) {
                    isInclude = true;
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
}

