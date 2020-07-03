import ij.IJ;
import ij.WindowManager;
import ij.gui.NonBlockingGenericDialog;
import ij.plugin.PlugIn;
import ij.ImagePlus;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Objects;
import java.awt.TextField;
import java.awt.event.TextListener;
import java.awt.event.TextEvent;

/** 
 * @abstract 
 * This ImageJ plugin loads an image or set of images from a web service (webpage). 
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
 * @installing
 * Copy this WSDL_Listen.java file to the ImageJ/plugins folder where you have ImageJ installed.
 * Start ImageJ and select the Plugins menu and choose the 'Compose and Run...' item in that menu.
 * A file explorer window will appear and you will need to navigate to the ImageJ/plugins folder and open the WSDL_Listen.java file you copied there.
 * This will run and install the plugin (create a WSDL_Listen.class file which is the compiled java script) and load a dialog box titled "WSDL_Listen Plugin".
 * At this point you can begin using the plugin, but if you close the WSDL_Listen dialog box and select the Help/Refresh Menus option in ImageJ, this will refresh the ImageJ menu. Now if you open the Plugins menu 'WSDL_Listen' is now accessible at the bottom of the Plugins menu.  
 * 
 * @logic
 * This plugin loads a dialog box that prompts you to 'Paste the web service URL provided to you'. To test the plugin
 * copy this URL and paste it into the text field:
 * https://raw.githubusercontent.com/kuhlaid/webServiceTesting/master/imageUrlList.text 
 * (if you visit this URL in your web browser you will see it is simply a text list of two URLs of image files)
 * The plugin checks the pasted web service URL every 2 seconds for changes to the content. If the list of images changes then the 
 * plugin will close any loaded images and load the new list of images.
 * 
 * @created
 * July 2, 2020
 * 
*/

public class WSDL_Listen implements PlugIn  {
	private static String strCurrentLoadedImg;	// used to store the first image in the WSDL image list
	private static String strWSUrl;	// the URL to the images web service

	public static String getCurrentLoadedImg(){
        return Objects.toString(WSDL_Listen.strCurrentLoadedImg,"");	// need to wrap in Objects.toString() otherwise if strCurrentLoadedImg is empty it will return null instead of an empty string
    }

    public static void setCurrentLoadedImg(String var){
        WSDL_Listen.strCurrentLoadedImg = var;
	}

	public static void setWSUrl(String var){
        WSDL_Listen.strWSUrl = var;
	}
	public static String getWSUrl(){
        return Objects.toString(WSDL_Listen.strWSUrl,"");	// need to wrap in Objects.toString() otherwise if strCurrentLoadedImg is empty it will return null instead of an empty string
    }
	
	public void run(String arg) {
      
		// creating timer task, timer
		Timer timer = new Timer();
  		TimerTask timerTask = new TimerScheduleFixedRateDelay();

		// scheduling the task at fixed rate delay of 4 seconds
		timer.scheduleAtFixedRate(timerTask,0,2000);

		// setup dialog box for canceling the WSDL timer
		NonBlockingGenericDialog gd = new NonBlockingGenericDialog("WSDL_Listen Plugin");
		gd.hideCancelButton();
		String defaultWSURL = "";
		gd.addStringField("Paste the web service URL provided to you: ","",30);
		gd.addMessage("***Close this dialog to stop listing for web service changes (checking for images to read)***");
		gd.addMessage("(images will be refreshed every 2 seconds)");
		// this listener monitors the web service URL field
		final TextField wsurl = (TextField)gd.getStringFields().get(0);
        wsurl.addTextListener(new TextListener() {
            public void textValueChanged(TextEvent e) {
				String text = wsurl.getText();
				//IJ.log("wsurl="+text);
				WSDL_Listen.setWSUrl(text);
            }
        });
		
		
		
		gd.showDialog();
		if (gd.wasCanceled() || gd.wasOKed()) {
			timer.cancel();
			timerTask.cancel();
			IJ.showStatus("Stopped listening for web service changes");
			//IJ.log("Stopped listening for web service changes");
			WindowManager.closeAllWindows();
			//System.exit(0);	// could use this if I simply want to close ImageJ
		}
	 }
	 // this method performs the task
}

class TimerScheduleFixedRateDelay extends TimerTask {
    void readTextFromUrl() {
		//IJ.log("readTextFromUrl");
		try {
			URL u = new URL(WSDL_Listen.getWSUrl());
			BufferedReader in = new BufferedReader(
			new InputStreamReader(u.openStream()));
			//IJ.log("try again 2");
			String currentLoadedImage = WSDL_Listen.getCurrentLoadedImg();
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
					//IJ.log("currentLoadedImage change");
					WSDL_Listen.setCurrentLoadedImg(firstLine);
					currentLoadedImage = WSDL_Listen.getCurrentLoadedImg();
				}
				// if we are changing the image set then load the new images
				if (imgChange==1) {
					//IJ.log("load image -"+inputLine);
					// should probably get the last image window width using getWindow() and position the next one based on where the first one is located
					new ImagePlus(inputLine).show(); // this loads new images
				}
				intImgCount++;
			}
			in.close();
		 }
		 catch(IOException ex) {
			// there was some connection problem, or the file did not exist on the server,
			// or your URL was not in the right format.
			// think about what to do now, and put it here.
			ex.printStackTrace(); // for now, simply output it.
		 }
		 catch(Exception ex) {
			// there was some connection problem, or the file did not exist on the server,
			// or your URL was not in the right format.
			// think about what to do now, and put it here.
			ex.printStackTrace(); // for now, simply output it.
		 }
	}

	public void run() {
		readTextFromUrl();
		////IJ.log("readTextFromUrl");
	}
 }
