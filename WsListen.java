import ij.*;
import ij.plugin.PlugIn;
import ij.process.*;
import ij.io.Opener;
import ij.gui.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.TextField;
import java.awt.event.TextListener;
import java.awt.event.TextEvent;

/** 
 * @abstract 
 * This ImageJ plugin loads images from a web service (URL) that simply outputs a list of image URLs (one per line) 
 * such as can be found at https://raw.githubusercontent.com/kuhlaid/ImageJPlugins/master/imageUrlList.text
 * The purpose of this plugin is for dynamically loading radiograph, ultrasound, or other sets of images for 'reading'.
 * For example if you have a radiograph reading form used to measure joint features of participants in an arthritis study
 * then you could build a form that triggers an action of saving the list of images to read for the selected participant to a database and then setup 
 * a web service that simply prints the list of images for the currently open reading form. If this plugin were pointed
 * to this web service URL (listening to it in a sense) then ImageJ would automatically load the latest set of images 
 * from the web service and close any previously loaded images. This plugin turns ImageJ into a sort of PACS image reader
 * for radiologists.
 *
 * @author https://github.com/kuhlaid
 *
*/

public class WsListen implements PlugIn  {
	private static String strCurrentLoadedImg;	// used to store the first image in the web service image list
	private static String strWSUrl;	// the URL to the images web service

	public static String getCurrentLoadedImg(){
        return Objects.toString(WsListen.strCurrentLoadedImg,"");	// need to wrap in Objects.toString() otherwise if strCurrentLoadedImg is empty it will return null instead of an empty string
  }

  public static void setCurrentLoadedImg(String var){
        WsListen.strCurrentLoadedImg = var;
	}

	public static void setWSUrl(String var){
        WsListen.strWSUrl = var;
	}
  
	public static String getWSUrl(){
        return Objects.toString(WsListen.strWSUrl,"");
  }
	
	public void run(String arg) {
      
		// creating timer task, timer
		Timer timer = new Timer();
  	TimerTask timerTask = new TimerScheduleFixedRateDelay();

		// scheduling the task at fixed rate delay of 2 seconds (checks the webservice every 2 seconds for new image list)
		timer.scheduleAtFixedRate(timerTask,0,2000);

		// setup dialog box for canceling the WSDL timer
		NonBlockingGenericDialog gd = new NonBlockingGenericDialog("Stop listening for image updates");
		gd.hideCancelButton();
		String defaultWSURL = ""; // set this if you want to default to a specific web service URL
		gd.addMessage("Close this dialog to stop listing for web service changes");
		gd.addStringField("Paste the web service URL provided to you: ",defaultWSURL,30);
		gd.addMessage("(images will be updated every 2 seconds)");
		// this listener monitors the web service URL field
		final TextField wsurl = (TextField)gd.getStringFields().get(0);
        wsurl.addTextListener(new TextListener() {
            public void textValueChanged(TextEvent e) {
				String text = wsurl.getText();
				IJ.log("wsurl="+text);
				WsListen.setWSUrl(text);
            }
        });
		
		
		
		gd.showDialog();
		if (gd.wasCanceled() || gd.wasOKed()) {
			timer.cancel();
			timerTask.cancel();
			IJ.showStatus("Stopped listening for web service changes");
			IJ.log("Stopped listening for web service changes");
			WindowManager.closeAllWindows();
			//System.exit(0);	// could use this if I simply want to close ImageJ
		}
	 }
	 // this method performs the task
}

class TimerScheduleFixedRateDelay extends TimerTask {
	//String urlWSDL = WsListen.getWSUrl();

    void readTextFromUrl() {
		IJ.log("readTextFromUrl");
		try {
			URL u = new URL(WsListen.getWSUrl());
			BufferedReader in = new BufferedReader(
			new InputStreamReader(u.openStream()));
			IJ.log("try again 2");
			String currentLoadedImage = WsListen.getCurrentLoadedImg();
			String firstLine="empty";
			String inputLine;
			int imgChange=0;
			int intImgCount=0;
			while ((inputLine = in.readLine()) != null){
				if (intImgCount==0) firstLine = inputLine;	// save the first line from the web service
				// the image list has changed or we are loading the list for the first time
				if (!currentLoadedImage.equals(firstLine)){
					WindowManager.closeAllWindows(); // close all currently open images to prepare for new set
					imgChange=1;
					IJ.log("currentLoadedImage change");
					WsListen.setCurrentLoadedImg(firstLine);
					currentLoadedImage = WsListen.getCurrentLoadedImg();
				}

				// if we are changing the image set then load the new images
				if (imgChange==1) {
					IJ.log("load image -"+inputLine);
					// @todo - should probably get the last image window width using getWindow() and position the next image based on where the first one is located
					new ImagePlus(inputLine).show(); // this loads new images
				}
				intImgCount++;
			}
			in.close();
		 }
		 catch(IOException ex) {
			ex.printStackTrace(); // for now, simply output it.
		 }
		 catch(Exception ex) {
			ex.printStackTrace(); // for now, simply output it.
		 }
	}

	public void run() {
		readTextFromUrl();
	}
 }
