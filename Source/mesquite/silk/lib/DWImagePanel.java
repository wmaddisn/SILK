/* SILK source code.  Copyright 2007-2009 W. Maddison and M. Ramirez. 

Disclaimer:  The Mesquite source code is lengthy and we are few.  There are no doubt inefficiencies and goofs in this code. 
The commenting leaves much to be desired. Please approach this source code with the spirit of helping out.
Perhaps with your help we can be more than a few, and make Mesquite better.

Mesquite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Mesquite's web site is http://mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
 */

package mesquite.silk.lib; 

import mesquite.lib.*;

import java.awt.*;

public class DWImagePanel extends MousePanel{
	Image image = null;
	protected int topBuffer = 0;
	protected Image[] images; //images[vertical][horizontal]


	//	protected int[] whichImageHoriz;
	protected int[] specialCase;
	public static final int UseColumnsLineAfter = -1;
	public static final int UseColumns = 0;
	public static final int NoColumn = 1;

	public static final int NoColumnLineAfter = 2;
	public static final int NoColumnLineAfterPale = 3;
	protected boolean stop = false;  //used to stop loading and display of images if went to other cell, for example

	protected StringInABox[] comments;
	protected StringInABox[] locations;
	protected StringInABox[] explanations;
	protected Rectangle[] imageRects;
	protected int[] origImageNumbers;
	protected boolean[] suppressImageWarning;
	protected int[] spread;
	protected String[] marker;
	//public int WHICHIMAGE = 0;
	protected boolean verbose = false;
	protected boolean warnIfNoImage = false;
	int initialBuffer = 8;
	int numColumns = 2;
	protected MesquiteString comment = new MesquiteString();
	protected MesquiteString location = new MesquiteString();
	StringInABox error;
	int baseFontSize = 10;
	protected Font fontSMALL = new Font("SanSerif", Font.PLAIN, baseFontSize);
	protected Font font = new Font("SanSerif", Font.PLAIN, baseFontSize+2);
	protected Font fontBOLD = new Font("SanSerif", Font.BOLD, baseFontSize+2);
	protected Font fontBIGBOLD = new Font("SanSerif", Font.BOLD, baseFontSize+4);
	ImagePanelThread thread;
	public MesquiteBoolean showLocation = new MesquiteBoolean(false);
	TextRotator textRotator;
	boolean showBlank = true;
	String blankMessage = null;

	public DWImagePanel (boolean startThread){
		setBackground(Color.white);
		error = new StringInABox("error", fontSMALL, getBounds().width-8);
		textRotator = new TextRotator(1);
		if (startThread){
			thread = new ImagePanelThread(this);
			thread.start();
		}
	}
	public void fontSizeChange(boolean increase){
		if (increase)
			baseFontSize += 2;
		else
			baseFontSize -= 2;

		fontSMALL = new Font("SanSerif", Font.PLAIN, baseFontSize);
		font = new Font("SanSerif", Font.PLAIN, baseFontSize+2);
		fontBOLD = new Font("SanSerif", Font.BOLD, baseFontSize+2);
		fontBIGBOLD = new Font("SanSerif", Font.BOLD, baseFontSize+4);
		stopCurrentPreparations();
		prepareImages();
	}
	public void repaint(String s){
		blankMessage = s;
		showBlank = true;
		super.repaint();
	}
	public void repaint(){
		blankMessage = "";
		showBlank = false;
		super.repaint();
	}
	public void dispose(){
		super.dispose();

		if (thread != null)
			thread.continuing = false;
	}
	public void stopCurrentPreparations(){
		if (thread == null || !thread.loading){
			MesquiteWindow.hideClock();
			return;
		}
		stop = true;
		try {
			while (stop){
				Thread.sleep(2);
			}
		}
		catch(InterruptedException e){
		}
	}
	public void prepareImages(){
		if (thread != null)
			thread.load = true;
		stop = false;
	}
	public void loadInformation(){
	}
	public void prepareMemory(int[] numImages){
		if (numImages == null){
			prepareMemory(0);
			return;
		}
		int sum = 0;
		for (int i=0; i<numImages.length; i++)
			sum += numImages[i];
		prepareMemory(sum);
	}
	public void prepareMemory(int totalNumImages){
		try{
			while (drawing)
				Thread.sleep(10);
		}
		catch (Exception e){
			MesquiteFile.throwableToLog(this, e);
		}
		if (totalNumImages == 0 ){
			images = null;
			comments = null;
			locations = null;
			explanations = null;
			specialCase = null;
			suppressImageWarning = null;
			marker = null;
			origImageNumbers = null;
			return;
		}
		images = new Image[totalNumImages];
		comments = new StringInABox[totalNumImages];
		locations = new StringInABox[totalNumImages];
		explanations = new StringInABox[totalNumImages];
		spread = new int[totalNumImages];
		origImageNumbers = new int[totalNumImages];
		specialCase = new int[totalNumImages];
		imageRects = new Rectangle[totalNumImages];
		suppressImageWarning = new boolean[totalNumImages];
		marker = new String[totalNumImages];
		for (int i=0; i< origImageNumbers.length; i++)
			origImageNumbers[i] = -1;
	}
	public void blank(Graphics g, String s){

		g.setColor(Color.white);
		g.fillRect(0, 0, getBounds().width, getBounds().height);
		g.setColor(Color.blue);
		if (s != null)
			g.drawString(s, 10, 50);

	}
	public int getMainSectionHeight(){
		return getBounds().height - topBuffer;
	}
	public int getMainSectionTop(){
		return 0;
	}
	protected boolean drawing;
	public void paint(Graphics g){
		drawing = true;
		if (showBlank || thread != null && thread.loading){
			blank(g, blankMessage);
			drawing = false;
			return;
		}
		int panelWidthBuffer = 16;
		if (images!= null && images.length>0) {
			//	try {

			//************************************************  first, measuring space.  First pass
			//first, assume that comments will be written beneath images
			int mainSectionHeight = getMainSectionHeight() - topBuffer;
			int countSpaces = 0;
			int biggestBlock = 0;
			int blockSize = 0;
			for (int i = 0; i<images.length; i++){
				if (specialCase[i]<=UseColumns){
					blockSize++;
					if (blockSize > biggestBlock)
						biggestBlock = blockSize;
				}
				else
					blockSize = 0;
				if (images[i] != null)
					countSpaces++;
				else if (specialCase[i] <= UseColumns && comments != null && comments[i] != null && !StringUtil.blank(comments[i].getString()))
					countSpaces++;
			}

			if (countSpaces > 6){
				numColumns = 3;
			}
			else if (countSpaces > 2 || (mainSectionHeight < 200 && countSpaces == 2)){
				numColumns = 2;
			}
			else{
				numColumns = 1;
			}
			if (countSpaces == 0)
				countSpaces =1;
			if (numColumns > biggestBlock)
				numColumns = biggestBlock;
			if (numColumns == 0)
				numColumns = 1;
			//	countSpaces = images.length;
			//numColumns = 3;
			int genPanelWidth = (getBounds().width)/numColumns-panelWidthBuffer;
			int panelWidth = genPanelWidth;
			int numRowsEst = countSpaces/numColumns;
			if (countSpaces/numColumns*numColumns != countSpaces)
				numRowsEst++;
			int heightPerSpread = (mainSectionHeight - initialBuffer*numColumns)/(numRowsEst);
			int buffer = initialBuffer;
			int columnComments = 0;
			int maxColumnComments = 0;
			int sumComments = 0;
			int numHoriz = 0;
			int counter = -1;
			for (int i=0; i< images.length; i++) {
				if (stop || (thread != null && thread.loading)){
					drawing = false;
					return;
				}
				int space = 0;
				counter++;

				if (specialCase[i] > UseColumns)
					panelWidth = getBounds().width - panelWidthBuffer;
				else
					panelWidth = genPanelWidth;
				if (images[i] == null)
					space = panelWidth;
				else if (images[i].getHeight(this) > 0)
					space = panelWidth - images[i].getWidth(this)*heightPerSpread/images[i].getHeight(this);
				if (space > panelWidth/2) {
					if (specialCase[i] > UseColumns){
						if (columnComments > maxColumnComments)
							maxColumnComments = columnComments;
						sumComments += maxColumnComments;
						maxColumnComments = 0;
						counter = -1;
					}
					numHoriz++;
					space = space-8;
					spread[i] = space;
					if (comments[i]!= null) {
						if (comments[i].getWidth() != space)
							comments[i].setWidth(space);
					}
					if (locations[i] != null &&  showLocation.getValue()) {
						if (locations[i].getWidth() != space)
							locations[i].setWidth(space);
					}
					if (explanations[i]!= null ) {
						if (explanations[i].getWidth() != space)
							explanations[i].setWidth(space);
					}
				}
				else {
					if (specialCase[i] > UseColumns){
						if (columnComments > maxColumnComments)
							maxColumnComments = columnComments;
						sumComments += maxColumnComments;
						maxColumnComments = 0;
						counter = -1;
					}
					else if (counter % numColumns == 0 && numColumns != 1)
						columnComments = 0;
					spread[i] = 0;
					if (comments[i]!= null) {
						if (comments[i].getWidth() != panelWidth - 8)
							comments[i].setWidth(panelWidth - 8);
						columnComments += comments[i].getHeight();
					}
					if (locations[i]!= null && showLocation.getValue()) {
						if (locations[i].getWidth() != panelWidth - 8)
							locations[i].setWidth(panelWidth - 8);
						columnComments += locations[i].getHeight();
					}
					if (explanations[i]!= null ) {
						if (explanations[i].getWidth() != panelWidth - 8)
							explanations[i].setWidth(panelWidth - 8);
						columnComments += explanations[i].getHeight();
					}
					if (specialCase[i] > UseColumns){
						sumComments += columnComments;
					}
					else if ((counter+1) % numColumns == 0 || numColumns == 1)
						if (columnComments > maxColumnComments)
							maxColumnComments = columnComments;
				}
			}
			int heightPerStacked = 0;
			if (countSpaces == numHoriz)
				heightPerStacked = heightPerSpread;
			else
				heightPerStacked = (mainSectionHeight-sumComments)/(numRowsEst);

			//************************************************  second, measuring space.  Second pass
			//	int top = 0;
			int topOfColumn = topBuffer + getMainSectionTop();
			int topOfRow = topBuffer + getMainSectionTop();
			int maxColumnDepth = topBuffer + getMainSectionTop();
			counter = -1;
			for(int i=0; i<images.length; i++) {
				if (stop || (thread != null && thread.loading)){
					drawing = false;
					return;
				}
				counter++;
				if (specialCase[i] > UseColumns) {
					panelWidth = getBounds().width - panelWidthBuffer;
					topOfRow = maxColumnDepth;
					counter = -1;
				}
				else
					panelWidth = genPanelWidth;
				int column = counter % numColumns;
				if (column < 0 || numColumns == 1) {
					topOfColumn = topOfRow;
				}
				int panelLeft = getBounds().width*column/numColumns + (column*panelWidthBuffer);
				if (specialCase[i] > UseColumns)
					panelLeft = 0;
				if (images[i] == null || images[i].getWidth(this) <= 0){
					if (warnIfNoImage && !suppressImageWarning[i] && locations[i]!= null && !StringUtil.blank(locations[i].getString())){
						boolean hasText = (comments != null && i<comments.length && comments[i] != null );
						hasText = hasText || (showLocation.getValue());// &&locations[i] != null);
						hasText = hasText || (explanations != null && i<explanations.length && explanations[i] != null);
						error.setWidth(panelWidth - 24);
						String s = "Image file not obtained. ";
						//	if ( locations[i] != null)
						s += "  Location sought: " + locations[i].getString();
						error.setString(s);
						topOfColumn += error.getHeight()+4;
					}
					if (comments != null && i<comments.length && comments[i] != null ){
						topOfColumn += comments[i].getHeight();
					}
					if (showLocation.getValue() &&locations[i] != null) {
						topOfColumn += locations[i].getHeight();
					}
					if (explanations != null && i<explanations.length && explanations[i] != null){
						topOfColumn += explanations[i].getHeight();
					}
					if (specialCase[i] <= UseColumns)
						topOfColumn += 32;
					//topOfColumn += heightPerSpread;
					if ((counter+1) % numColumns == 0  || numColumns == 1) 
						topOfRow= topOfColumn + buffer;
					if (topOfColumn > maxColumnDepth)
						maxColumnDepth = topOfColumn;
					topOfColumn = topOfRow;
				}
				else {
					int textLeft = panelLeft + 4;
					boolean center = true;
					int heightAllowed = heightPerStacked;
					if (spread[i]>0) {
						textLeft = panelLeft + panelWidth - spread[i];
						center = false;
						heightAllowed = heightPerSpread;
					}
					Rectangle r = MesquiteImage.drawImageWithinRect(null, images[i],  panelLeft, topOfColumn, panelWidth,  heightAllowed-buffer,  center, this);
					int textTop = r.y +r.height;
					if (spread[i]>0)
						textTop = r.y;
					int textHeight = 0;
					if (comments != null && i<comments.length && comments[i] != null){
						textHeight += comments[i].getHeight();
					}
					if (images != null && i<images.length && images[i]!=null && images[i].getWidth(this) * images[i].getHeight(this)>4000000){
					}
					if (showLocation.getValue() && locations != null &&  locations[i]!= null) {
						textHeight += locations[i].getHeight();
					}
					if (explanations != null && i<explanations.length && explanations[i] != null){
						textHeight += explanations[i].getHeight();
					}
					textTop += textHeight;
					if (textHeight>heightAllowed)
						heightAllowed = textHeight;
					if (spread[i]<=0)
						topOfColumn += textTop - r.y - r.height;
					topOfColumn += heightAllowed;
					//**
					if ((counter+1) % numColumns == 0 || numColumns == 1) 
						topOfRow = topOfColumn + buffer;
					if (topOfColumn > maxColumnDepth)
						maxColumnDepth = topOfColumn;
					topOfColumn = topOfRow;
				}
				if (specialCase[i] >= NoColumnLineAfter){
					topOfColumn += buffer;
					if (topOfColumn > maxColumnDepth)
						maxColumnDepth = topOfColumn;
				}
			}
			//at this point topOfColumn would be the bottom of where we came to.  Thus, rescale
			if (mainSectionHeight < (maxColumnDepth + 30)){
				heightPerStacked = heightPerStacked*mainSectionHeight/(maxColumnDepth + 30);
				heightPerSpread = heightPerSpread*mainSectionHeight/(maxColumnDepth + 30);
				buffer = buffer*mainSectionHeight/maxColumnDepth;
			}
			for(int i=0; i<images.length; i++) {
				if (comments != null && i<comments.length && comments[i] != null){
					comments[i].storeFont();
				}
				if (showLocation.getValue() && locations != null &&  locations[i]!= null) {
					locations[i].storeFont();
				}
				if (explanations != null && i<explanations.length && explanations[i] != null){
					explanations[i].storeFont();
				}
			}
			if (1.0*mainSectionHeight/maxColumnDepth < 1.0)
				for(int i=0; i<images.length; i++) {
					if (comments != null && i<comments.length && comments[i] != null){
						comments[i].scaleFont(1.0*mainSectionHeight/maxColumnDepth);
					}
					if (showLocation.getValue() && locations != null &&  locations[i]!= null) {
						locations[i].scaleFont(1.0*mainSectionHeight/maxColumnDepth);
					}
					if (explanations != null && i<explanations.length && explanations[i] != null){
						explanations[i].scaleFont(1.0*mainSectionHeight/maxColumnDepth);
					}
				}

			//************************************************  Third, drawing
			//	int top = 0;
			topOfColumn = topBuffer + getMainSectionTop();
			topOfRow = topBuffer + getMainSectionTop();
			maxColumnDepth = topBuffer  + getMainSectionTop();
			counter = -1;
			for(int i=0; i<images.length; i++) {
				counter++;
				if (specialCase[i] > UseColumns) {
					panelWidth = getBounds().width - panelWidthBuffer;
					topOfRow = maxColumnDepth;
					counter = -1;
				}
				else
					panelWidth = genPanelWidth;
				int column = counter % numColumns;
				if (counter < 0 || numColumns == 1) {
					topOfColumn = topOfRow;
					if (i != 0 && specialCase[i] <= UseColumns)
						g.drawLine(16, topOfColumn -1, getBounds().width -32, topOfColumn -1);
				}
				int panelLeft = getBounds().width*column/numColumns + (column*panelWidthBuffer);
				if (specialCase[i] > UseColumns)
					panelLeft = 0;
				if (images[i] == null || images[i].getWidth(this) <= 0){
					Color c = g.getColor();
					if (warnIfNoImage && !suppressImageWarning[i] && locations[i]!= null && !StringUtil.blank(locations[i].getString())){
						boolean hasText = (comments != null && i<comments.length && comments[i] != null );
						hasText = hasText || (showLocation.getValue() &&locations[i] != null);
						hasText = hasText || (explanations != null && i<explanations.length && explanations[i] != null);
						g.setColor(Color.blue);
						error.setWidth(panelWidth - 24);
						String s = "Image file not obtained. ";
						if (locations[i] != null && !StringUtil.blank(locations[i].getString()))
							s += "  Location sought: " + locations[i].getString();

						error.setString(s);
						error.draw(g, panelLeft, topOfColumn+4);
						g.drawRect(panelLeft, topOfColumn, panelLeft + panelWidth -20, error.getHeight()+4);
						topOfColumn += error.getHeight()+4;

						g.setColor(c);

					}
					if (comments != null && i<comments.length && comments[i] != null ){
						comments[i].draw(g, panelLeft + 4, topOfColumn);
						topOfColumn += comments[i].getHeight();
					}
					if (showLocation.getValue() &&locations[i] != null) {
						locations[i].draw(g, panelLeft + 4, topOfColumn);
						topOfColumn += locations[i].getHeight();
					}
					if (explanations != null && i<explanations.length && explanations[i] != null){
						explanations[i].draw(g, panelLeft + 4, topOfColumn);
						topOfColumn += explanations[i].getHeight();
					}
					if (marker[i] != null){
						g.setColor(Color.gray);
						g.fillRect(panelLeft -16 + panelWidthBuffer, topOfColumn, 16, heightPerSpread - buffer);
						//	g.fillRect(panelLeft + panelWidth-16 + panelWidthBuffer, topOfColumn, 16, heightPerSpread - buffer);
						//g.fillRect(panelLeft, topOfColumn+heightAllowed - buffer, panelWidth, 8);
						g.setColor(Color.white);
						if (!StringUtil.blank(marker[i]))
							textRotator.drawRotatedText(marker[i], 0, g, this, panelLeft + panelWidth-12 + panelWidthBuffer, topOfColumn+heightPerSpread - buffer - 4);
						g.setColor(Color.black);
					}
					if (specialCase[i] <= UseColumns)
						topOfColumn += 32;
					//topOfColumn += heightPerSpread;
					if ((counter+1) % numColumns == 0  || numColumns == 1) 
						topOfRow = topOfColumn + buffer;
					if (topOfColumn > maxColumnDepth)
						maxColumnDepth = topOfColumn;
					topOfColumn = topOfRow;
				}
				else {
					int textLeft = panelLeft + 4;

					boolean center = true;
					int heightAllowed = heightPerStacked;
					if (spread[i]>0) {
						textLeft = panelLeft + panelWidth - spread[i];
						center = false;
						heightAllowed = heightPerSpread;
					}
					Rectangle r = MesquiteImage.drawImageWithinRect(g, images[i],  panelLeft, topOfColumn, panelWidth,  heightAllowed-buffer,  center, this);
					if (imageRects != null && i < imageRects.length)
						imageRects[i] = r;
					if (r == null)
						comments[i].draw(g, 4, topOfColumn);
					else {
						int oldTop = topOfColumn;

						int textTop = r.y +r.height;
						if (spread[i]>0)
							textTop = r.y;

						if (comments != null && i<comments.length && comments[i] != null){
							//g.setColor(Color.red);
							comments[i].draw(g, textLeft, textTop);
							textTop += comments[i].getHeight();
						}
						if (images != null && i<images.length && images[i]!=null && images[i].getWidth(this) * images[i].getHeight(this)>4000000){
							Color c = g.getColor();
							g.setColor(Color.red);
							error.setWidth(r.width);
							String s = "Image file very large (with many pixels).  If the image does not appear quickly, you may need to use a smaller image.  Location: " + locations[i].getString();
							error.setString(s);
							error.draw(g, textLeft, textTop);
							g.setColor(c);
							MesquiteImage.drawImageWithinRect(g, images[i],   panelLeft, topOfColumn, panelWidth,  heightAllowed-buffer,  center, this);
						}
						if (showLocation.getValue() && locations != null &&  locations[i]!= null) {
							Color c = g.getColor();
							g.setColor(Color.blue);
							locations[i].draw(g, textLeft, textTop);
							g.setColor(c);
							textTop += locations[i].getHeight() ;
						}
						if (explanations != null && i<explanations.length && explanations[i] != null){
							//g.setColor(Color.green);
							explanations[i].draw(g, textLeft, textTop);
							textTop += explanations[i].getHeight();
						}
						if (spread[i]<=0) {
							topOfColumn += textTop - r.y - r.height;
							//	g.drawRect(panelLeft-1, oldTop-1, panelWidth +2, topOfColumn - oldTop); //-buffer
						}
						//else
						//	g.drawRect(panelLeft-1, oldTop-1, panelWidth +2, topOfColumn - oldTop); //-buffer
						int rectHeight = Math.max(textTop - oldTop + buffer, r.height+buffer);
						g.setColor(Color.gray);
						g.drawRect(panelLeft-1, oldTop-1, panelWidth +2, rectHeight); //-buffer
						g.setColor(Color.black);
					}
					if (specialCase[i] == UseColumnsLineAfter){
						g.setColor(Color.gray);
						g.fillRect(panelLeft + panelWidth-16 + panelWidthBuffer, topOfColumn, 16, heightAllowed - buffer);
						//g.fillRect(panelLeft, topOfColumn+heightAllowed - buffer, panelWidth, 8);
						g.setColor(Color.white);
						if (!StringUtil.blank(marker[i]))
							textRotator.drawRotatedText(marker[i], 0, g, this, panelLeft + panelWidth-12 + panelWidthBuffer, topOfColumn+heightAllowed - buffer - 4);
						g.setColor(Color.black);
					}
					topOfColumn += heightAllowed;
					if ((counter+1) % numColumns == 0 || numColumns == 1) {
						topOfRow = topOfColumn + buffer;
					}
					if (topOfColumn  > maxColumnDepth)
						maxColumnDepth = topOfColumn;
					topOfColumn = topOfRow;
			}
				if (specialCase[i] >= NoColumnLineAfter){
					if (specialCase[i] == NoColumnLineAfter)
						g.setColor(Color.gray);
					else if (specialCase[i] == NoColumnLineAfterPale)
						g.setColor(ColorDistribution.veryLightGray);
					g.fillRect(8, topOfColumn+ buffer, getBounds().width - 16, 3);
					topOfColumn += buffer;
					if (topOfColumn > maxColumnDepth)
						maxColumnDepth = topOfColumn;
					g.setColor(Color.black);
				}
				if (comments != null && i<comments.length && comments[i] != null)
					comments[i].recallFont();

				if (showLocation.getValue() && locations != null &&  locations[i]!= null) 
					locations[i].recallFont();

				if (explanations != null && i<explanations.length && explanations[i] != null)
					explanations[i].recallFont();
			}
			/**
			 }
			catch (Exception e){
			}
			/**/
		}
		else {
			blank(g, "");
		}
		drawing = false;
	}

	protected int findImage(int x, int y){
		if (imageRects!= null && imageRects.length>0){
			for(int i=0; i<imageRects.length; i++) {
				if (imageRects[i] != null && y>imageRects[i].y && y<imageRects[i].y+ imageRects[i].height && x>imageRects[i].x && x<imageRects[i].x+ imageRects[i].width){			
					return origImageNumbers[i];
				}
			}
		}
		return -1;
	}

}

class ImagePanelThread extends Thread {
	DWImagePanel panel;
	boolean load = false;
	boolean continuing = true;
	boolean loading = false;
	int runs = 0;
	public ImagePanelThread (DWImagePanel panel){
		this.panel = panel;
	}
	public void run() {
		try {
			while (continuing){
				while(!load && continuing){
					Thread.sleep(20);
				}
				if (continuing){
					int thisRun = runs++;
					//("RUN " + thisRun + "  " + panel.getClass() + " " +  this);
					loading = true;
					panel.loadInformation();
					MesquiteWindow.hideClock();
					panel.stop = false;
					load = false;
					loading = false;
					//("RUNFINISHED " + thisRun + " "  + panel.getClass() + " " +  this);
					panel.repaint();
				}
			}

		}
		catch (Throwable e){
			MesquiteFile.throwableToLog(this, e);
			MesquiteWindow.hideClock();
			panel.stop = false;
			load = false;
			loading = false;
			panel.repaint();
		}
	}
}