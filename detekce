import java.awt.image.BufferedImage;
        import java.io.ByteArrayInputStream;
        import java.io.InputStream;
        import java.util.ArrayList;
        import java.util.List;

        import javax.imageio.ImageIO;
        import javax.swing.ImageIcon;
        import javax.swing.JFrame;
        import javax.swing.JLabel;

        import org.opencv.calib3d.Calib3d;
        import org.opencv.core.*;
        import org.opencv.imgproc.Imgproc;
        import org.opencv.video.Video;
        import org.opencv.features2d.FeatureDetector;
        import org.opencv.imgcodecs.Imgcodecs;

        import static org.opencv.imgproc.Imgproc.*;
        import static org.opencv.utils.Converters.vector_Point_to_Mat;

public class optic {
    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        Mat image = Imgcodecs.imread("C:/Users/mitka/Downloads/prazdno.jpg");
        Mat lmaeft1 = Imgcodecs.imread("C:/Users/mitka/Downloads/plech.jpg");
        System.out.println(lmaeft1.width()+"x"+lmaeft1.height());
      
        JFrame window = new JFrame();
        JFrame window1 = new JFrame();
        Mat vysledek = new Mat();
        int l = 100;
        int r = 130;
        float a = 255/(r-l);
        float b = l;
        Core.subtract(lmaeft1, new Scalar(b,b,b), vysledek);
        Core.multiply(vysledek, new Scalar(a,a,a), vysledek);
        Mat kelner = getStructuringElement(MORPH_RECT, new Size(10, 10));
        Core.absdiff(image,vysledek,vysledek);
        Imgproc.morphologyEx(vysledek, vysledek, MORPH_OPEN, kelner);
        Imgproc.morphologyEx(vysledek, vysledek, MORPH_DILATE , kelner);
        List<MatOfPoint> contours = new ArrayList<>();
        Mat mat = new Mat();
        Imgproc.cvtColor(vysledek, vysledek, COLOR_RGB2GRAY);
        Util.imshow(vysledek, window);
        Imgproc.findContours(vysledek, contours,mat, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE);
        MatOfInt hulk = new MatOfInt();
        MatOfPoint2f approxCurve = new MatOfPoint2f();
        //For each contour found
        for (int i=0; i<contours.size(); i++)
        {
            //Convert contours(i) from MatOfPoint to MatOfPoint2f
            MatOfPoint2f contour2f = new MatOfPoint2f( contours.get(i).toArray() );
            //Processing on mMOP2f1 which is in type MatOfPoint2f
            double approxDistance = Imgproc.arcLength(contour2f, true)*0.02;
            Imgproc.approxPolyDP(contour2f, approxCurve, approxDistance, true);

            //Convert back to MatOfPoint
            MatOfPoint points = new MatOfPoint( approxCurve.toArray() );
            // Get bounding rect of contour
            Rect rect = Imgproc.boundingRect(points);
            List<Point> plechovky = new ArrayList<Point>();
            int x = bounds.width/2;
    		double y = bounds.height*0.2;
    		plechovky.add(new Point( x, y));
            int pointCount = points.toList().size();
            if (pointCount >= 4 ) {
            	Rect bounds = Imgproc.boundingRect(points);
            	float ar = bounds.width/(float)bounds.height;
            	System.out.println(bounds.width);
            	System.out.println(bounds.height);
            	System.out.println(ar);
            	if (ar >= 0.50 & ar <= 0.90){
            		System.out.println("plechovka");
            	} else {
            			System.out.println("nepratesky robot");
            	}
            } else {
        		System.out.println("neni obdelnik");
        	}

             // draw enclosing rectangle (all same color, but you could use variable i to make them unique)
            Imgproc.rectangle(image, rect.tl(),  rect.br(), new Scalar(255, 0, 0),1, 8,0);
      

        }
        Util.imshow(image, window1);
    }

	private static Scalar Scalar(int i, int j, int k) {
		// TODO Auto-generated method stub
		return null;
	}
}

class Util {


    public static void imshow(Mat img, JFrame frame) {
        MatOfByte matOfByte = new MatOfByte();
        Imgcodecs.imencode(".png", img, matOfByte);
        byte[] byteArray = matOfByte.toArray();
        BufferedImage bufImage = null;
        try {
            InputStream in = new ByteArrayInputStream(byteArray);
            bufImage = ImageIO.read(in);
            frame.getContentPane().removeAll();
            frame.getContentPane().add(new JLabel(new ImageIcon(bufImage)));
            frame.pack();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
