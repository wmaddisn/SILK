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
import mesquite.lib.ui.MesquiteImage;
import mesquite.lib.ui.MesquiteTool;
import mesquite.lib.ui.MousePanel;

import java.awt.*;

public abstract class DWPanel extends MousePanel{
	public static final int width = 300;
	public static final int height = 36;
	int ic;
	protected int numImagesVertical = 0;
	Image goaway, plus, minus;
	protected PanelOwner pw;
	DWImagePanel iPanel;
	boolean showPlusMinus = false;
	public DWPanel (PanelOwner pw){
		this(pw, false);
	}
	public DWPanel (PanelOwner pw, boolean showPlusMinus){
		super();
		this.pw = pw; 
		iPanel = makePanel();
		iPanel.setVisible(true);
		add(iPanel);
		this.showPlusMinus = showPlusMinus;
		setLayout(null);
		goaway = MesquiteImage.getImage(MesquiteModule.getRootImageDirectoryPath() + "goaway.gif");
		if (showPlusMinus){
			plus = MesquiteImage.getImage(MesquiteModule.getRootImageDirectoryPath() + "increase.gif");
			minus = MesquiteImage.getImage(MesquiteModule.getRootImageDirectoryPath() + "decrease.gif");
		}
		setSize(width, 500);
		setCursor(Cursor.getDefaultCursor());
	}
	public abstract DWImagePanel makePanel();
	public DWImagePanel getPanel(){
		return iPanel;
	}
	public void setSize(int w, int h){
		iPanel.setSize(w, h-height);
		super.setSize(w, h);
	}
	public void setBounds(int x, int y, int w, int h){
		iPanel.setBounds(0, height, w, h-height);
		super.setBounds(x, y, w, h);
	}
	public abstract String getTitleString();

	public void paint(Graphics g){
		super.paint(g);
		int oW = 8;
		int oH = 12;
		g.drawImage(goaway, 2, 2, this);
		String s = getTitleString();
		if (s != null)
			g.drawString(s, 20, 18);
		if(showPlusMinus){
			g.drawImage(plus, getBounds().width-17, 2, this);
			g.drawImage(minus, getBounds().width-17 - 14, 2, this);
		}
		g.fillRect(0, height-2, getBounds().width, 2);
		g.setColor(Color.black);
	}
	/* to be used by subclasses to tell that panel touched */
	public void mouseUp(int modifiers, int x, int y, MesquiteTool tool) {
		if (y> 2 && y< 2 + 16 && x >= 2 && x <= 2 + 16) 
			pw.panelGoAway(this);
		else if (showPlusMinus){
			if (y> 2 && y< 2 + 16 && x >= getBounds().width-17 && x <= getBounds().width-17 + 16) 
				pw.panelIncreaseDecrease(true);
			else if (y>  2 && y< 16+ 2 && x >= getBounds().width-17 -14 && x <= getBounds().width-17 + 16 - 14) 
				pw.panelIncreaseDecrease(false);
		}
	}
}
