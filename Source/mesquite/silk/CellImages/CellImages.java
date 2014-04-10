/* SILK source code.  Copyright 2007-2009 W. Maddison and M. Ramirez. Disclaimer:  The Mesquite source code is lengthy and we are few.  There are no doubt inefficiencies and goofs in this code. The commenting leaves much to be desired. Please approach this source code with the spirit of helping out.Perhaps with your help we can be more than a few, and make Mesquite better.Mesquite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.Mesquite's web site is http://mesquiteproject.orgThis source code and its compiled class files are free and modifiable under the terms of GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html) */package mesquite.silk.CellImages; import java.util.*;import java.awt.*;import javax.swing.JEditorPane;import mesquite.lib.*;import mesquite.lib.characters.*;import mesquite.lib.duties.*;import mesquite.lib.table.*;import mesquite.silk.lib.*;import mesquite.categ.lib.*;/** ======================================================================== */public class CellImages extends DataWindowAssistantI implements CellColorer, CellColorerCharacters, CellColorerMatrix, CellColorerTaxa, PanelOwner {	public void getEmployeeNeeds(){  //This gets called on startup to harvest information; override this and inside, call registerEmployeeNeed		EmployeeNeed e2 = registerEmployeeNeed(CellImageSource.class, "A cell images source supplies images for display in the cells of a character matrix editor.",		"This is activated when you choose Show Cell Info Panel from the Matrix menu of a Character Matrix Editor");	}	CharacterData data;	//	Taxa taxa;	CellDWPanel panel;	CellDWImagePanel iPanel;	MesquiteBoolean showPanel;	CellImageSource imageSource = null;	CharOntologySource ontologySource = null;	MesquiteTable table;	TableTool statesPageTool;	MesquiteBoolean showSums;	MesquiteBoolean showAnnotations;	MesquiteBoolean showHistory;	MesquiteBoolean showLocations;	MesquiteMenuSpec menu;	MesquiteMenuItemSpec sami, shmi, slmi, stcc;	/*.................................................................................................................*/	public boolean startJob(String arguments, Object condition, boolean hiredByName){		showPanel = new MesquiteBoolean(false);		showAnnotations = new MesquiteBoolean(false);		showSums = new MesquiteBoolean(false);		showHistory = new MesquiteBoolean(false);		showLocations = new MesquiteBoolean(false);		statesPageTool = new TableTool(this, "statesPageTool", getPath(), "states.gif", 7, 8, "Show States Page", "This tool shows states of character touched.", MesquiteModule.makeCommand("showStates", this), null, null); 		getEmployer().addCheckMenuItem(null, "Show Cell Info Panel", makeCommand("togglePanel", this), showPanel);		return true;	}	public void toggleShowOntology(){		if (iPanel != null)			iPanel.toggleShowOntology();	}	/*.................................................................................................................*/	public Snapshot getSnapshot(MesquiteFile file) {		Snapshot temp = new Snapshot();		temp.addLine("togglePanel " + showPanel.toOffOnString());		if (!showPanel.getValue()){			temp.suppressCommandsToEmployee(imageSource);			temp.suppressCommandsToEmployee(ontologySource);		}		temp.addLine("toggleAnnotations " + showAnnotations.toOffOnString());		temp.addLine("toggleSums " + showSums.toOffOnString());		temp.addLine("toggleHistory " + showHistory.toOffOnString());		temp.addLine("toggleLocations " + showLocations.toOffOnString());		//todo here: set source of images, and split as separate command below		return temp;	}	/*.................................................................................................................*/	public Object doCommand(String commandName, String arguments, CommandChecker checker) {		if (checker.compare(this.getClass(), "Sets whether or not the image panel is shown", "[on = shown; off]", commandName, "togglePanel")) {			showPanel.toggleValue(parser.getFirstToken(arguments));			if (showPanel.getValue() && imageSource == null) {				imageSource = (CellImageSource)hireEmployee(CellImageSource.class, "Source of images");				ontologySource = (CharOntologySource)hireEmployee(CharOntologySource.class, "Source of ontology");				if (imageSource == null)					showPanel.setValue(false);				else {					imageSource.setData(data);					if (ontologySource != null)						ontologySource.setData(data);				}			}			setPanel();		}		else if (checker.compare(this.getClass(), "Reassures module no source is used yet", null, commandName, "noSource")) {		}		else if (checker.compare(this.getClass(), "here for backward compatibility", null, commandName, "showColors")) {		}		else if (checker.compare(this.getClass(),  "Shows states for that character", "[column touched][row touched]", commandName, "showStates")) {			if (data == null || !(data instanceof CategoricalData))				return null;			MesquiteInteger io = new MesquiteInteger(0);			int column= MesquiteInteger.fromString(arguments, io);			int row= MesquiteInteger.fromString(arguments, io);			if (MesquiteInteger.isNonNegative(column)&& (MesquiteInteger.isNonNegative(row))) {				CategoricalData cData = ((CategoricalData)data);				long state = cData.getState(column, row);				if (CategoricalState.cardinality(state)==1){					showStatePage(cData, column, CategoricalState.minimum(state));				}			}		}		else if (checker.compare(this.getClass(), "Sets whether or not the colors show sums for taxa and characters", "[on = shown; off]", commandName, "toggleSums")) {			showSums.toggleValue(parser.getFirstToken(arguments));			if (!MesquiteThread.isScripting())				parametersChanged(null);		}		else if (checker.compare(this.getClass(), "Sets whether or not the annotations are shown", "[on = shown; off]", commandName, "toggleAnnotations")) {			showAnnotations.toggleValue(parser.getFirstToken(arguments));			if (iPanel != null)				iPanel.reset();		}		else if (checker.compare(this.getClass(), "Sets whether or not the state scoring histories are shown", "[on = shown; off]", commandName, "toggleHistory")) {			showHistory.toggleValue(parser.getFirstToken(arguments));			if (iPanel != null)				iPanel.reset();			if (panel != null && data != null)				panel.setCell(previousChar, previousTax, data.getTaxa().getTaxonName(previousTax), true);		}		else if (checker.compare(this.getClass(), "Sets whether or not the image locations are shown", "[on = shown; off]", commandName, "toggleLocations")) {			showLocations.toggleValue(parser.getFirstToken(arguments));			if (iPanel != null){				iPanel.reset();				iPanel.showLocation.setValue(showLocations.getValue());			}			if (panel != null && data != null)				panel.setCell(previousChar, previousTax, data.getTaxa().getTaxonName(previousTax), true);		}		else			return  super.doCommand(commandName, arguments, checker);		return null;	}	public void showStatePage(CategoricalData cData, int ic, int state){		if (imageSource == null)			return;		Vector locations = new Vector();		Vector names = new Vector();		MesquiteString comment = new MesquiteString();		for (int it=0; it<cData.getNumTaxa(); it++){			long otherState = cData.getState(ic, it);			if (CategoricalState.cardinality(otherState)==1 && CategoricalState.minimum(otherState) == state){				int[] images = imageSource.getNumCellImages(cData, ic, it);				int numImages = 0;				if (images != null)					numImages = images.length;				for(int i=0; i<numImages; i++) {					String location = imageSource.getCellImageLocation( i, cData, ic, it,  comment);					locations.addElement(location);					names.addElement(cData.getTaxa().getTaxonName(it)+ ": " + comment);				}			}		}		if (locations.size() == 0)			return;		StringBuffer html = new StringBuffer(100);		html.append("<html><head><title>Character " + StringUtil.protectForXML(cData.getCharacterName(ic) + ": State:  " + cData.getStateName(ic, state)) + "</title>  </head><body><table>");		for (int i=0; i<locations.size(); i++){			if (i % 4 == 0 && i != 0)				html.append("</tr>");			if (i % 4 == 0)				html.append("<tr>");			html.append("<td width = 100><img src=\"" + locations.elementAt(i) + "\"><br>" + names.elementAt(i) + "</td>");		}		html.append("</tr>");		html.append("</table></body></html>");		String page = MesquiteModule.prefsDirectory + MesquiteFile.fileSeparator + "stateImages" + state + ".html";		MesquiteFile.putFileContents(page, html.toString(), true);		showWebPage(page, false);	}	/*.................................................................................................................*/	public boolean setActiveColors(boolean active){		if (!MesquiteThread.isScripting() && active && !showPanel.getValue()){			discreetAlert( "Sorry, can't color by cell images if the Cell Info Panel is not turned on");			return false;		}		setActive(active);		return true; 	}	ColorRecord[] legend;	public ColorRecord[] getLegendColors(){		if (legend == null) {			legend = new ColorRecord[4];			legend[0] = new ColorRecord(Color.white, "No Images");			legend[1] = new ColorRecord(ColorDistribution.veryLightBlue, "One image");			legend[2] = new ColorRecord(ColorDistribution.lightBlue, "Two Images");			legend[3] = new ColorRecord(Color.blue, "More than Two");		}		return legend;	}	public String getColorsExplanation(){		return null;	}	private int getNumImages(int ic, int it){		int[] nums = imageSource.getNumCellImages(data, ic, it);		if (nums !=  null)			return nums.length;		return 0;	}	public Color getCellColor(int ic, int it){		if (data == null || imageSource == null)			return null;		if (ic<0){			if (!showSums.getValue() || it<0)				return Color.white;			int sum = 0;			for (int i = 0; i<data.getNumChars(); i++){				if (getNumImages(i, it)>0)					sum++;			}			return MesquiteColorTable.getGreenScale(sum*1.0/data.getNumChars(), 0, 1, false);		}		if (it<0){			if (!showSums.getValue())				return Color.white;			int sum = 0;			for (int i = 0; i<data.getNumTaxa(); i++){				if (getNumImages(ic, i)>0)					sum++;			}			return MesquiteColorTable.getGreenScale(sum*1.0/data.getNumTaxa(), 0, 1, false);		}		int num = getNumImages(ic, it);		if (num <= 0)			return Color.white;		if (num== 1)			return ColorDistribution.veryLightBlue;		if (num== 2)			return ColorDistribution.lightBlue;		return Color.blue;	}	public String getCellString(int ic, int it){		if (data == null || imageSource == null)			return null;		if (ic<0){			if (  it<0)				return "";			int sum = 0;			for (int i = 0; i<data.getNumChars(); i++){				if (getNumImages(i, it)>0)					sum++;			}			return "Proportion cells with images attached: " + (sum*1.0/data.getNumChars());		}		if (it<0){			if (!showSums.getValue())				return "";			int sum = 0;			for (int i = 0; i<data.getNumTaxa(); i++){				if (getNumImages(ic, i)>0)					sum++;			}			return "Proportion cells with images attached: " + (sum*1.0/data.getNumTaxa());		}		int num = getNumImages(ic, it);		if (num == 0)			return "No images for this cell";		if (num == 1)			return "One image for this cell";		if (num == 2)			return "Two images for this cell";		return "More than two images for this cell";	}	/*.................................................................................................................*/	public boolean isSubstantive(){		return false;	}	public void viewChanged(){	}	public void setTableAndData(MesquiteTable table, CharacterData data){		if (imageSource != null)			imageSource.setData(data);		if (ontologySource != null)			ontologySource.setData(data);		this.table = table;		this.data = data;		setPanel();	}	/*.................................................................................................................*/	void setPanel(){		MesquiteWindow f = containerOfModule();		if (f instanceof TableWindow){			if (showPanel.getValue()){				if (containerOfModule() instanceof MesquiteWindow) {					((MesquiteWindow)containerOfModule()).addTool(statesPageTool);				}				if (panel == null) {					panel = new CellDWPanel(this);				}				MesquiteModule mb = findEmployerWithDuty(DataWindowMaker.class);				if (mb != null && mb instanceof DataWindowMaker){					if (!MesquiteThread.isScripting())						((DataWindowMaker)mb).demandCellColorer(this,0, 0, null);										}				((TableWindow)f).addSidePanel(panel, DWPanel.width);				iPanel = (CellDWImagePanel)panel.getPanel();				iPanel.setData(data);				iPanel.setImageSource(imageSource);				iPanel.setOntologySource(ontologySource);				if (menu == null){					menu = makeMenu("Cell_Info");					sami = addCheckMenuItem(null, "Show Annotations", makeCommand("toggleAnnotations", this), showAnnotations);					shmi = addCheckMenuItem(null, "Show Scoring History", makeCommand("toggleHistory", this), showHistory);					slmi = addCheckMenuItem(null, "Show Image Locations", makeCommand("toggleLocations", this), showLocations);					stcc = addCheckMenuItem(null, "Show Taxon/Character Sums", makeCommand("toggleSums", this), showSums);					resetContainingMenuBar();				}				if (imageSource != null){					String waitingMessage = imageSource.waitingMessage();					if (!StringUtil.blank(waitingMessage))						panel.blank(waitingMessage);				}				focusInCell(previousChar, previousTax);			}			else {				if (panel != null)					((TableWindow)f).removeSidePanel(panel);				MesquiteModule mb = findEmployerWithDuty(DataWindowMaker.class);				if (mb != null && mb instanceof DataWindowMaker)					((DataWindowMaker)mb).resignCellColorer(this);				if (containerOfModule() instanceof MesquiteWindow) {					((MesquiteWindow)containerOfModule()).removeTool(statesPageTool);				}				if (menu != null){					destroyMenu();					deleteMenuItem(sami);					deleteMenuItem(shmi);					deleteMenuItem(slmi);					deleteMenuItem(stcc);					resetContainingMenuBar();				}			}		}	}	/*.................................................................................................................*/	public boolean hasDisplayModifications(){		return false;	}	public void employeeParametersChanged(MesquiteModule employee, MesquiteModule source, Notification notification) {		if (employee == imageSource){			if (notification != null && notification.getCode() == CellImageSource.IMAGESREADY && iPanel != null){				iPanel.setReady(true);				iPanel.repaint("Ready to display images");			}			else if (notification != null && notification.getCode() == CellImageSource.IMAGESNOTREADY && iPanel != null){				iPanel.setReady(false);				iPanel.repaint(imageSource.waitingMessage());				return;			}			else if (panel != null)				panel.blank("");			if (table != null)				table.repaintAll();			parametersChanged();			focusInCell(previousChar, previousTax);		}	}	int previousChar = -1;	int previousTax = -1;	/*.................................................................................................................*/	public void focusInCell(int ic, int it){		if (data == null || iPanel == null)			return;		previousChar = ic;		previousTax = it;		if (ic <0 && it<0)			return;		if (showPanel.getValue()){			if (imageSource != null){				String waitingMessage = imageSource.waitingMessage();				if (!StringUtil.blank(waitingMessage))					panel.blank(waitingMessage);			}			panel.setCell(ic, it, data.getTaxa().getTaxonName(it),  false);		}	}	/*.................................................................................................................*/	public String getName() {		return "Matrix Cell Information";	}	/*.................................................................................................................*/	public String getNameForMenuItem() {		return "*Databased Images Present";	}	/*.................................................................................................................*/	public String getVersion() {		return null;	}	public void panelGoAway(Panel p){		showPanel.setValue(false);		setPanel();	}	public void colorsLegendGoAway(){	}	public void panelIncreaseDecrease(boolean increase) {		if (iPanel != null)			iPanel.fontSizeChange(increase);	}	/*.................................................................................................................*/	public String getExplanation() {		return "Installs a panel with images and other information for cells of a character matrix (part of the SILK package).";	}}/*======================================================*/class CellDWPanel extends DWPanel {	int ic = -1;	int it = -1;	String taxonName = null;	CellImages module;	Image ontologyQuery;	public CellDWPanel (CellImages pw){		super(pw, true);		module = pw;		setBackground(ColorDistribution.sienna);		setForeground(Color.white);		ontologyQuery = MesquiteImage.getImage(module.getPath() + "ontology.gif");	}	public DWImagePanel makePanel(){		return new CellDWImagePanel((CellImages)pw);	}	void blank(String s){		((CellDWImagePanel)getPanel()).repaint(s);	}	void setCell(int ic, int it, String name, boolean resetRegardless){		if (!resetRegardless && ic == this.ic && it == this.it) {			return;		}		this.ic = ic;		this.it = it;		taxonName = name;		int[] numImages= module.imageSource.getNumCellImages(module.data, ic, it);		if (numImages == null)			return;		this.numImagesVertical = numImages.length;		//scroll.setMaximumValue(numImages-1);		blank("Retrieving image...");		((CellDWImagePanel)getPanel()).showImages(ic, it, numImages);		repaint();		getPanel().repaint();	}	public String getTitleString(){		if (taxonName == null)			return "Cell Information";		return  "Character " + (ic+1) + " in taxon " + taxonName;	}	boolean showOntology = false;	public void paint(Graphics g){		super.paint(g);		showOntology = (module.ontologySource != null & module.ontologySource.ontologyAvailable());		if (showOntology) {			g.drawImage(ontologyQuery, getBounds().width-17, 2+ 16, this);		}	}	/* to be used by subclasses to tell that panel touched */	public void mouseUp(int modifiers, int x, int y, MesquiteTool tool) {		if (showOntology){			if (y> 2 + 16 && y< 2 + 16+ 16 && x >= getBounds().width-17 && x <= getBounds().width-17 + 16) 				module.toggleShowOntology();			else 				super.mouseUp(modifiers,  x,  y,  tool);		}		else super.mouseUp(modifiers,  x,  y,  tool);	}}/*======================================================*/class CellAnotImagePanel extends DWImagePanel implements MoverHolder{	int moverHeight = Mover.HEIGHT;	NameReference notesNameRef = NameReference.getNameReference("notes");		int currentIC = 0;	int currentIT = 0;	CharacterData data;	CellDWImagePanel parent;	Mover mover;	AttachedNotesVector notes;	public CellAnotImagePanel (CellDWImagePanel parent){		super(false);		verbose = true;		topBuffer = moverHeight;		setLayout(null);		mover = new Mover(this, "Annotations");		add(mover);		mover.setBounds(0, 0, getBounds().width, moverHeight); 		this.parent = parent;	}	public int getMainSectionHeight(){		int h = getBounds().height;		if (false && parent.historyShown)			h -= parent.historyHeight;		return h;	}	public void setData(CharacterData data){		this.data = data;	}	public void setCell(int ic, int it){		if (ic < 0 || it < 0)			notes = null;		else			notes = (AttachedNotesVector)data.getCellObject(notesNameRef, ic, it);		if (notes == null){			images = null;			comments = null;			locations = null;			repaint();			return;		}		int[] numImages = new int[notes.getNumNotes()];		for (int i=0; i< notes.getNumNotes(); i++){			numImages[i] = 1;		}		prepareMemory(numImages);		for (int i=0; i< notes.getNumNotes(); i++){			AttachedNote note = notes.getAttachedNote(i);			images[i] = note.getImage() ;			String c = note.getComment();			String a = note.getAuthorName();			String s = "";			if (!StringUtil.blank(c))				s = c;			if (!StringUtil.blank(a))				s += " (" + a + ")";			comments[i] = new StringInABox(s, font, getBounds().width-8);		}		repaint();	}	public void setBounds(int x, int y, int width, int height){		super.setBounds(x, y, width, height);		mover.setBounds(0, 0, getBounds().width, moverHeight); 	}	public void setSize( int width, int height){		super.setSize(width, height);		mover.setBounds(0, 0, getBounds().width, moverHeight); 	}	public void blank(Graphics g, String s){		g.setColor(Color.white);		g.fillRect(0, 0, getBounds().width, getBounds().height);		g.setColor(Color.blue);		if (s != null)			g.drawString(s, 10, 50);		g.setColor(Color.black);	}	public void requestVertChange(int change){		parent.requestSizeAnot(change);	}}/*======================================================*/class HistoryPanel extends MousePanel implements MoverHolder{	int currentIC = 0;	int currentIT = 0;	int moverHeight = Mover.HEIGHT;	CharacterData data;	CellDWImagePanel parent;	Mover mover;	JEditorPane pane;	String s;	public HistoryPanel (CellDWImagePanel parent){		super();		setLayout(null);		mover = new Mover(this, "History of State Changes");		add(mover);		mover.setBounds(0, 0, getBounds().width, moverHeight); 		pane = new JEditorPane("text/html","<html></html>");		add(pane);		pane.setBounds(0, moverHeight, getBounds().width, getBounds().height - moverHeight);		this.parent = parent;	}	public void setData(CharacterData data){		this.data = data;	}	NameReference historyNameRef = NameReference.getNameReference("ChangeHistory");	public void setCell(int ic, int it){		if (ic < 0 || it< 0){			pane.setText("<html><p></html>");			return;		}		ChangeHistory cH = (ChangeHistory)data.getCellObject(historyNameRef, ic, it);		String text = "<html>";		String s ="";		if (cH != null)			for (int i = 0; i<cH.getNumEvents(); i++){				ChangeEvent e = cH.getEvent(i);				s += "<li>to " + e.getChange();  //what changed to				//by whom				Author a = e.getAuthor();				if (a != null)					s += " <strong>(" + a.getName() + ")</strong>";				//when				GregorianCalendar c = new GregorianCalendar();				c.setTime(new Date(e.getTime()));				s += "  " + c.get(Calendar.YEAR) + "." + (c.get(Calendar.MONTH)+1)  + "." + c.get(Calendar.DAY_OF_MONTH) ;				s += "</li>";			}		if (StringUtil.blank(s))			text += "<h3>No History Recorded for Cell</h3>";		else			text += "<ul>" + s + "</ul>";		text += "</html>";		pane.setText(text);	}	public void setBounds(int x, int y, int width, int height){		super.setBounds(x, y, width, height);		mover.setBounds(0, 0, getBounds().width, moverHeight); 		pane.setBounds(0, moverHeight, getBounds().width, getBounds().height - moverHeight);	}	public void setSize( int width, int height){		super.setSize(width, height);		mover.setBounds(0, 0, getBounds().width, moverHeight); 		pane.setBounds(0, moverHeight, getBounds().width, getBounds().height - moverHeight);	}	public void blank(Graphics g, String s){		g.fillRect(0, 0, getBounds().width, getBounds().height);		g.setColor(Color.blue);		if (s != null)			g.drawString(s, 10, 50);		g.setColor(Color.black);	}	/*	 * 	public void paint(Graphics g){		if (s != null)			g.drawString(s, 5, 25);		for(int i=0; i<500; i+=20)			g.drawString(Integer.toString(i), i, i);	}	 */	public void requestVertChange(int change){		parent.requestSizeHist(change);	}}/*======================================================*/class CellDWImagePanel extends DWImagePanel{	CellImageSource source;	CharOntologySource oSource;	CharacterData data;	int currentIC = 0;	int currentIT = 0;	int noteImagesHeight = 50;	int historyHeight = 50;	int ontologyHeight = 50;	CellAnotImagePanel anotPanel;	HistoryPanel historyPanel;	CellImages module;	boolean notesShown = false;	boolean historyShown = false;	boolean ready = false;	boolean showOntology = false;	OntologyTerm[] ontologyTerms = null;	StringInABox[] ontologyBoxes = null;	public CellDWImagePanel (CellImages module){		super(true);		this.module = module;		//	setBackground(Color.green);		anotPanel = new CellAnotImagePanel(this);		setLayout(null);		add(anotPanel);		anotPanel.setBounds(0, getBounds().height - noteImagesHeight - historyHeight, getBounds().width, noteImagesHeight);		anotPanel.setVisible(notesShown);		historyPanel = new HistoryPanel(this);		add(historyPanel);		historyPanel.setBounds(0, getBounds().height - historyHeight, getBounds().width, historyHeight);		historyPanel.setVisible(historyShown);		warnIfNoImage = true;	}	public void fontSizeChange(boolean increase){		super.fontSizeChange(increase);		anotPanel.fontSizeChange(increase);	}	public void setReady(boolean ready){		this.ready = ready;	}	public void reset(){		notesShown = module.showAnnotations.getValue();		anotPanel.setVisible(notesShown);		historyShown = module.showHistory.getValue();		historyPanel.setVisible(historyShown);		repaint();	}	public void toggleShowOntology(){		showOntology = !showOntology;		repaint();	}	public void resetBounds(){		anotPanel.setBounds(0, getBounds().height - noteImagesHeight - historyHeight, getBounds().width, noteImagesHeight);		historyPanel.setBounds(0, getBounds().height - historyHeight, getBounds().width, historyHeight);	}	public void setBounds(int x, int y, int width, int height){		super.setBounds(x, y, width, height);		resetBounds();	}	public void setSize( int width, int height){		super.setSize(width, height);		resetBounds();	}	public void setData(CharacterData data){		this.data = data;		anotPanel.setData(data);		historyPanel.setData(data);	}	public void setImageSource(CellImageSource source){		this.source = source;	}	public void setOntologySource(CharOntologySource oSource){		this.oSource = oSource;	}	public int getMainSectionHeight(){		int h = getBounds().height;		if (ontologyBoxes != null){			ontologyHeight = 0;			for (int i=0; i< ontologyBoxes.length; i++)				ontologyHeight += ontologyBoxes[i].getHeight() + 10;		}		if (notesShown)			h -= noteImagesHeight;		if (historyShown)			h -= historyHeight;		if (showOntology)			h -= ontologyHeight;		return h;	}	public int getMainSectionTop(){		if (ontologyBoxes != null){			ontologyHeight = 0;			for (int i=0; i< ontologyBoxes.length; i++)				ontologyHeight += ontologyBoxes[i].getHeight() + 10;		}		if (showOntology)			return super.getMainSectionTop() + ontologyHeight;		return super.getMainSectionTop();	}	public void blank(Graphics g, String s){		if (!ready){			g.setColor(Color.gray);			g.fillRect(0, 0, getBounds().width, getBounds().height);			g.setColor(Color.white);			if (s != null)				g.drawString(s, 10, 50);			g.setColor(Color.black);		}		else {			g.setColor(Color.white);			g.fillRect(0, 0, getBounds().width, getBounds().height);			g.setColor(Color.blue);			if (s != null)				g.drawString(s, 10, 50);			g.setColor(Color.black);		}	}	public void paint(Graphics g){		if (showOntology){			Color c = g.getColor();			if (ontologyBoxes != null){				int h = 0;				for (int i=0; i< ontologyBoxes.length; i++) {					if (ontologyBoxes[i] != null){						ontologyBoxes[i].draw(g, 0, h);						h += ontologyBoxes[i].getHeight();					}				}			}			g.setColor(Color.gray);			g.fillRect(0, ontologyHeight-2, getWidth(), 2);			g.setColor(c);		}		super.paint(g);	}	int[] numImages;	public void showImages(int ic, int it, int[] numImages){		stopCurrentPreparations();		currentIC = ic;		currentIT = it;		this.numImages = numImages;		prepareImages();	}	public void loadInformation(){		if (source == null)			return ;		try {			repaint("Retrieving images");			if (stop)				return;			int ic = currentIC;			int it = currentIT;			anotPanel.setCell(ic, it);			historyPanel.setCell(ic, it);			prepareMemory(numImages.length+1);			comments[0] = new StringInABox(data.getCharacterName(ic) + " in " + data.getTaxa().getTaxonName(it), fontBOLD, getBounds().width-8);			suppressImageWarning[0] = true;			specialCase[0] = NoColumn;			ontologyTerms = oSource.getOntologyTerms(data, ic);			if (ontologyTerms == null){				if (oSource.ontologyAvailable()){					ontologyBoxes = new StringInABox[1];					ontologyBoxes[0] = new StringInABox("No ontology terms available", font, getBounds().width -8);					ontologyHeight = ontologyBoxes[0].getHeight()+10;				}				else {					ontologyBoxes = new StringInABox[1];					ontologyHeight = 0;				}			}			else {				ontologyBoxes = new StringInABox[ontologyTerms.length];				ontologyHeight = 0;				int linkWidth = 0;				for (int i=0; i< ontologyTerms.length; i++){					String s = ontologyTerms[i].name;					if (s == null){						if (ontologyTerms[i].def == null)							s ="Ontology term undefined";						else 							s = ontologyTerms[i].def;					}					else {						linkWidth = s.length();						if (ontologyTerms[i].def != null)							s += ": " + ontologyTerms[i].def;					}					ontologyBoxes[i] = new StringInABox(s, font, getBounds().width -8);					if (linkWidth>0)						ontologyBoxes[i].setLink(0, linkWidth);					ontologyHeight += ontologyBoxes[i].getHeight()+10;				}			}			for(int k=0; k<numImages.length; k++) {				if (stop)					return;				int i = k+1;				location.setValue("");				MesquiteWindow.tickClock("Loading image " + (k+1) + " of " + numImages.length);				repaint("Retrieving image " + (k+1)  + " of " + numImages.length);				origImageNumbers[i] = k;				images[i] = source.getCellImage(k, data, ic, it, comment, location);				comments[i] = new StringInABox(comment.getValue(), font, getBounds().width-8);				locations[i] = new StringInABox(location.getValue(), font, getBounds().width-8);			}			/**/		}		catch (Exception e){		}		/**/		MesquiteWindow.hideClock();	}	void requestSizeAnot(int change){		if (noteImagesHeight + change >8 && noteImagesHeight + change + historyHeight < getBounds().height - 16){			noteImagesHeight += change;			resetBounds();			repaint();		}	}	void requestSizeHist(int change){		if (historyHeight + change >8 && historyHeight + change + noteImagesHeight < getBounds().height - 16){			historyHeight += change;			resetBounds();			repaint();		}	}	public void mouseDown (int modifiers, int clickCount, long when, int x, int y, MesquiteTool tool) {		int im = findImage(x, y);		if (im >= 0 && clickCount>1 && source != null){			source.showCloseupCellImage(im,  data, currentIC, currentIT, modifiers);			return;		}		else if (ontologyBoxes != null){			for (int i=0; i< ontologyBoxes.length; i++)				if (ontologyBoxes[i] != null && ontologyBoxes[i].inLink(x, y))					MesquiteModule.showWebPage(oSource.getOntologyURL(ontologyTerms[i].id), false);		}	}}