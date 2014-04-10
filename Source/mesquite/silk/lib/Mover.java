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

public class Mover extends MousePanel{
	int touchedY = -1;
	MoverHolder parent;
	public static int HEIGHT = 16;
	String title;
	Font font = new Font("SanSerif", Font.PLAIN, 10);
	public Mover(MoverHolder parent, String title){
		this.parent = parent;
		this.title = title;
		setBackground(Color.darkGray);
		setFont(font);
		setForeground(Color.white);
		setCursor(new Cursor(Cursor.N_RESIZE_CURSOR));  
	}
	public void paint(Graphics g){
		g.drawString(title, 8, getBounds().height - 3);
	}
	public void mouseDown (int modifiers, int clickCount, long when, int x, int y, MesquiteTool tool) {
		touchedY = y;
	}

	public void mouseUp(int modifiers, int x, int y, MesquiteTool tool) {
		if (touchedY>-1){
			int change = touchedY - y;
			parent.requestVertChange(change);
		}
		touchedY = -1;
	}
}
