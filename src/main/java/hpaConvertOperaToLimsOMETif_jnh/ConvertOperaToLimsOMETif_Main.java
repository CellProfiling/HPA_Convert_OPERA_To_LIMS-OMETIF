package hpaConvertOperaToLimsOMETif_jnh;

/** ===============================================================================
* HPA_Convert_OPERA_To_LIMS-OMETIF_JNH.java Version 0.2.10
* 
* This program is free software; you can redistribute it and/or
* modify it under the terms of the GNU General Public License
* as published by the Free Software Foundation (http://www.gnu.org/licenses/gpl.txt )
* 
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*  
* See the GNU General Public License for more details.
*  
* Copyright (C) Jan Niklas Hansen
* Date: September 11, 2023 (This Version: May 28, 2025)
*   
* For any questions please feel free to contact me (jan.hansen@scilifelab.se).
* =============================================================================== */

import java.awt.Font;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;

//For XML support
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.io.FileUtils;
import org.joda.time.DateTimeZone;
//W3C definitions for a DOM, DOM exceptions, entities, nodes
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.WaitForUserDialog;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import loci.common.RandomAccessInputStream;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.FormatException;
import loci.formats.FormatTools;
import loci.formats.ImageWriter;
import loci.formats.ome.OMEXMLMetadata;
import loci.formats.out.OMETiffWriter;
import loci.formats.services.OMEXMLService;
import loci.formats.tiff.TiffParser;
import loci.plugins.BF;
import loci.plugins.in.ImportProcess;
import loci.plugins.in.ImporterOptions;

//import loci.formats.FormatException;
import loci.formats.tiff.TiffSaver;
import ome.units.UNITS;
import ome.units.quantity.Length;
import ome.units.quantity.Time;
import ome.units.unit.Unit;
import ome.xml.model.enums.Binning;
import ome.xml.model.enums.DetectorType;
import ome.xml.model.enums.EnumerationException;
import ome.xml.model.enums.IlluminationType;
import ome.xml.model.enums.MicroscopeType;

// For validating the xml file
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

public class ConvertOperaToLimsOMETif_Main implements PlugIn {
	// Name variables
	static final String PLUGINNAME = "HPA Convert Opera-Tifs to LIMS-OME-Tif";
	static final String PLUGINVERSION = "0.2.10";

	// Fix fonts
	static final Font SuperHeadingFont = new Font("Sansserif", Font.BOLD, 16);
	static final Font HeadingFont = new Font("Sansserif", Font.BOLD, 14);
	static final Font SubHeadingFont = new Font("Sansserif", Font.BOLD, 12);
	static final Font TextFont = new Font("Sansserif", Font.PLAIN, 12);
	static final Font InstructionsFont = new Font("Sansserif", 2, 12);
	static final Font RoiFont = new Font("Sansserif", Font.PLAIN, 20);

	// Fix formats
	DecimalFormat dformat6 = new DecimalFormat("#0.000000");
	DecimalFormat dformat3 = new DecimalFormat("#0.000");
	DecimalFormat dformat0 = new DecimalFormat("#0");
	DecimalFormat dformatDialog = new DecimalFormat("#0.000000");

	static final String[] nrFormats = { "US (0.00...)", "Germany (0,00...)" };

	static SimpleDateFormat NameDateFormatter = new SimpleDateFormat("yyMMdd_HHmmss");
	static SimpleDateFormat FullDateFormatter = new SimpleDateFormat("yyyy-MM-dd	HH:mm:ss");
	static SimpleDateFormat FullDateFormatter2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	// Progress Dialog
	ProgressDialog progress;
	boolean processingDone = false;
	boolean continueProcessing = true;
	boolean deleteManually = false;

	// -----------------define params for Dialog-----------------
	int tasks = 1;
	boolean extendedLogging = false;
	boolean logInitialFileScreening = false;
	boolean logWholeOMEXMLComments = false;
	boolean noWarningsForXYZConflicts = true;
	boolean noWarningsForMissingCustomMetadata = true;
	boolean noWarningsForDiverseZ = true;
	
	boolean extendOnly = false;
	
	boolean cropImage = false;
	int newImgLength = 2048;
	
	//Calibration data for the 96-well plate, data derived from the technincal drawing for Greiner Sensoplate Cat. No. 655892
	double centerOfFirstWellXInMM = 14.38;
	double centerOfFirstWellYInMM = 11.24;
	double distanceBetweenNeighboredWellCentersXInMM = 9;
	double distanceBetweenNeighboredWellCentersYInMM = 9;
	
	String imageType [] = new String [] {"OPER(ETT)A Phenix Index File"};
	String selectedImageType = imageType [0];
	
	String outPath = "E:" + System.getProperty("file.separator") + System.getProperty("file.separator") + "OME Out"
			+ System.getProperty("file.separator");
	
	String outputType [] = new String [] {"Separate z planes into individual image folders (LIMS style)","Separate fields of view into individual folders (canonical OME tif style, all z planes in same folder)","Separate fields of view into folders, separate z planes into subfolders (Memento style)"};
	String selectedOutputType = outputType [0];
	
	// -----------------define params for Dialog-----------------
	
	
	// Temporary variables used for the original metadata file
	String loadedSrcMetadataFilePath = "";
	int loadedSrcTask = -1;	 // TODO delete
	String srcMetadataFilePath = "";
	File srcMetaDataFile = null;
	Document srcMetaDoc = null;
	Node srcImagesNode = null, srcWellsNode = null, srcPlatesNode = null;
	double srcZStepSizeInMicronAcrossWholeOPERAFile = -1.0;
	String srcLoadingLog = "";
	int srcLoadingLogMode = ProgressDialog.LOG;
	String srcMetaXMLVersion = "1"; //Variants: 1, 2
	
	
	// Temporary variables used for the individual OPERA metadata file
	String loadedMetadataFilePath = ""; // TODO delete
	int loadedTask = -1;	 // TODO delete
	String metadataFilePath = "";
	File metaDataFile = null;
	Document metaDoc = null;
	Node imagesNode = null, wellsNode = null, platesNode = null;
	double zStepSizeInMicronAcrossWholeOPERAFile = -1.0;
	String loadingLog = "";
	int loadingLogMode = ProgressDialog.LOG;
	
	// For xpath loading
	XPathFactory xPathfactory = null;
	XPath xp = null;
	
	// Developer variables
	static final boolean LOGPOSITIONCONVERSIONFORDIAGNOSIS = false;// This fixed variable is just used when working on the code and to retrieve certain log output only
	static final boolean LOGZDISTFINDING = false;// This fixed variable is just used when working on the code and to retrieve certain log output only

	@Override
	public void run(String arg) {

		// &&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&
		// ---------------------------------INIT JOBS----------------------------------
		// &&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&
		
		dformat6.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
		dformat3.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
		dformat0.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
		dformatDialog.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));

		int series[] = {0,0};
		int totSeries[] = {0,0};
		String name[] = { "", "" };
		String seriesName [] =  { "", "" };
		String dir[] = { "", "" };
		String fullPath[] = { "", "" };
		
		xPathfactory = XPathFactory.newInstance();
		xp = xPathfactory.newXPath();

		// &&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&
		// --------------------------REQUEST USER-SETTINGS-----------------------------
		// &&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&
		
		GenericDialog gd = new GenericDialog(PLUGINNAME + " - set parameters");	
		//show Dialog-----------------------------------------------------------------
		gd.setInsets(0,0,0);	gd.addMessage(PLUGINNAME + ", Version " + PLUGINVERSION + ", \u00a9 2022-2025 JN Hansen", SuperHeadingFont);	
		

		gd.setInsets(15,0,0);	gd.addMessage("Notes:", SubHeadingFont);
		
		gd.setInsets(0,0,0);	gd.addMessage("The plugin processes output folders with tif file and an Index.idx.xml file generated by an Opera Phenix screening microscope.", InstructionsFont);
		gd.setInsets(-3,0,0);	gd.addMessage("The plugin will load the files using FIJI's bioformats library and resave them as ome.tif files", InstructionsFont);
		gd.setInsets(-3,0,0);	gd.addMessage("enriched with all available metadata. The output files are ready for classic import into the HPA LIMS.",InstructionsFont);

		gd.setInsets(3,0,0);	gd.addMessage("NOTE: This plugin runs only in FIJI (not in a blank ImageJ, where OME BioFormats library is missing).", InstructionsFont);		
			
		gd.setInsets(10,0,0);	gd.addMessage("Plate specifications:", SubHeadingFont);	
		gd.setInsets(0, 0, 0);	gd.addNumericField("Distance from the center of well A1 to the left plate border [mm]",centerOfFirstWellXInMM,2);
		gd.setInsets(0, 0, 0);	gd.addNumericField("Distance from the center of well A1 to the top plate border [mm]",centerOfFirstWellYInMM,2);
		gd.setInsets(0, 0, 0);	gd.addNumericField("Horizontal distance between the centers of two neighbored wells (e.g., A1 to A2) [mm]",distanceBetweenNeighboredWellCentersXInMM,2);
		gd.setInsets(0, 0, 0);	gd.addNumericField("Vertical distance between the centers of two neighbored wells (e.g., A1 to B1) [mm]",distanceBetweenNeighboredWellCentersYInMM,2);
		
		gd.setInsets(10,0,0);	gd.addMessage("Processing Settings", SubHeadingFont);
		gd.setInsets(0,0,0);	gd.addChoice("Image type", imageType, selectedImageType);
		
		gd.setInsets(10,0,0);	gd.addCheckbox("Crop image | new image width and height (px):", cropImage);
		gd.setInsets(-23, 0, 0);	gd.addNumericField("",newImgLength,0);

		gd.setInsets(0,0,0);	gd.addStringField("Filepath to output directory", outPath, 50);
		gd.setInsets(0,0,0);	gd.addMessage("This path defines where outputfiles will be stored.", InstructionsFont);
		gd.setInsets(0,0,0);	gd.addMessage("Make sure this path does not contain identically named files - the program may overwrite them.", InstructionsFont);

		gd.setInsets(0,0,0);	gd.addChoice("Output style", outputType, selectedOutputType);		
				
		gd.setInsets(10,0,0);	gd.addMessage("Logging settings (troubleshooting options)", SubHeadingFont);		
		gd.setInsets(0,0,0);	gd.addCheckbox("Log all processing steps extensively", extendedLogging);
		gd.setInsets(5,0,0);	gd.addCheckbox("Log initial screening original file", logInitialFileScreening);
		gd.setInsets(5,0,0);	gd.addCheckbox("Log the OME metadata XML before and after extending", logWholeOMEXMLComments);
		gd.setInsets(5,0,0);	gd.addCheckbox("Don't log warnings for resolved x,y,z coordinate conflicts", noWarningsForXYZConflicts);
		gd.setInsets(5,0,0);	gd.addCheckbox("Don't log warnings for differences in z-step size across data set", noWarningsForDiverseZ);
		gd.setInsets(5,0,0);	gd.addCheckbox("Don't log warnings for missing custom metadata", noWarningsForMissingCustomMetadata);
		
		gd.setInsets(10,0,0);	gd.addMessage("Input files", SubHeadingFont);
		gd.setInsets(0,0,0);	gd.addMessage("A dialog will be shown when you press OK that allows you to list Index.idx.xml files to be processed.", InstructionsFont);
		
		
		gd.addHelp("https://github.com/CellProfiling/HPA_Convert_OPERA_To_LIMS-OMETIF");
		
		gd.showDialog();
		//show Dialog-----------------------------------------------------------------

		//read and process variables--------------------------------------------------	
		centerOfFirstWellXInMM = gd.getNextNumber();
		centerOfFirstWellYInMM = gd.getNextNumber();
		distanceBetweenNeighboredWellCentersXInMM = gd.getNextNumber();
		distanceBetweenNeighboredWellCentersYInMM = gd.getNextNumber();
		selectedImageType = gd.getNextChoice();
		cropImage = gd.getNextBoolean();
		newImgLength = (int) Math.round(gd.getNextNumber());
		outPath = gd.getNextString();
		selectedOutputType = gd.getNextChoice();
		extendedLogging = gd.getNextBoolean();
		logInitialFileScreening = gd.getNextBoolean();
		logWholeOMEXMLComments = gd.getNextBoolean();
		noWarningsForXYZConflicts = gd.getNextBoolean();
		noWarningsForDiverseZ = gd.getNextBoolean();
		noWarningsForMissingCustomMetadata = gd.getNextBoolean();
		//read and process variables--------------------------------------------------
		if (gd.wasCanceled()) return;
		
		// &&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&
		// -------------------------------LOAD FILES-----------------------------------
		// &&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&

		{
			/**
			 * Explore XML file to read information
			 */
			OpenFilesDialog od = new OpenFilesDialog(false);
			od.setLocation(0, 0);
			od.setVisible(true);

			od.addWindowListener(new java.awt.event.WindowAdapter() {
				public void windowClosing(WindowEvent winEvt) {
					return;
				}
			});

			// Waiting for od to be done
			while (od.done == false) {
				try {
					Thread.currentThread().sleep(50);
				} catch (Exception e) {
				}
			}
			
			tasks = od.filesToOpen.size();
			name = new String[tasks];
			seriesName = new String[tasks];
			dir = new String[tasks];
			series = new int [tasks];
			totSeries = new int [tasks];
			Arrays.fill(series, 0);
			Arrays.fill(totSeries, 1);
			
			fullPath = new String[tasks];
			for (int task = 0; task < tasks; task++) {
				fullPath[task] = od.filesToOpen.get(task).toString();
				name[task] = od.filesToOpen.get(task).getName();
				dir[task] = od.filesToOpen.get(task).getParent();
				seriesName [task] = "NA";
				if(logInitialFileScreening) {
					IJ.log("Logging information for selected index files" + " (time: " + FullDateFormatter2.format(new Date()) + ")");
					IJ.log("ORIGINAL: " + fullPath[task]);
					IJ.log("name:" + name[task]);
					IJ.log("dir:" + dir[task]);					
				}
			}
			
			if (tasks == 0) {
				new WaitForUserDialog("No folders selected!").show();
				return;
			}
			
			/**
			 * Now open index files and use them to load tasks
			 */
			for(int task = tasks-1; task >= 0; task--){
				IJ.showProgress((tasks-task)/tasks);
				String tempPath = dir [task] + System.getProperty("file.separator") + name [task];					
				
				if(!loadSourceOPERAMetadatafile(tempPath, task)) {
					IJ.error("Task " + (task + 1) + "/" + tasks + ": Could not load metadata file " + tempPath + "!\n" + loadingLog);
				}else {
					if(extendedLogging) {
						IJ.log("Task " + (task + 1) + "/" + tasks + ": Metadata file loaded:" + tempPath + "!\n");
					}else if(loadingLogMode != ProgressDialog.LOG) {
						IJ.log("Task " + (task + 1) + "/" + tasks + ": There were WARNINGS or ERRORS when loading metadata! See log information here:\n" + loadingLog + "");
					}
				}
								
				// Now explore the file
				String ImageIds [] = getAllImageIDBeginningsFromListOfImageNodes_UsingXPath(srcImagesNode.getChildNodes());
				if(ImageIds.length == 0) {
					IJ.error("Could not translate image ids for:\n" + tempPath + ".\nSee log for details.");					
					return;
				}
				
				if(ImageIds.length > 1) {
					String [] nameTemp = new String [name.length+ImageIds.length-1], 
							dirTemp = new String [name.length+ImageIds.length-1], 
							seriesNameTemp = new String [name.length+ImageIds.length-1];
					int [] seriesTemp = new int [nameTemp.length],
							totSeriesTemp = new int [nameTemp.length]; 
					for(int j = 0; j < task; j++) {
						nameTemp [j] = name [j]; 
						dirTemp [j] = dir [j];
						seriesTemp [j] = series [j];
						seriesNameTemp [j] = seriesName [j];
						totSeriesTemp [j] = totSeries [j];
						
					}
					for(int j = 0; j < ImageIds.length; j++) {
						nameTemp [task+j] = name [task]; 
						dirTemp [task+j] = dir [task];
						seriesTemp [task+j] = j;
						seriesNameTemp [j] = ImageIds[j];
						totSeriesTemp [task+j] = ImageIds.length;
						if(logInitialFileScreening) {
							IJ.log("Fetched name for series: " + j + " in task " + task + ": " + seriesNameTemp [j] + ", time: " + FullDateFormatter2.format(new Date()));
						}
					}
					for(int j = task+1; j < name.length; j++) {
						nameTemp [j+ImageIds.length-1] = name [j]; 
						dirTemp [j+ImageIds.length-1] = dir [j];
						seriesTemp [j+ImageIds.length-1] = series [j];
						seriesNameTemp [j+ImageIds.length-1] = seriesName [j];
						totSeriesTemp [j+ImageIds.length-1] = totSeries [j];
					}
					
					//copy arrays

					tasks = nameTemp.length;
					name = new String [tasks];
					dir = new String [tasks];
					seriesName = new String [tasks];
					series = new int [tasks];
					totSeries = new int [tasks];
					
					for(int j = 0; j < nameTemp.length; j++) {
						name [j] = nameTemp [j];
						dir [j] = dirTemp [j];
						seriesName [j] = seriesNameTemp [j];
						series [j] = seriesTemp [j];
						totSeries [j] = totSeriesTemp [j];
						
						if(logInitialFileScreening) {
							IJ.log("Logging loaded tasks here: "  + ", time: " + FullDateFormatter2.format(new Date()));
							IJ.log("ORIGINAL: " + name[task] + ", " + dir [task] + ", " + fullPath[task]);
							IJ.log("series: " + (series[j]+1) + " of " + totSeries[j] + " with name: " + seriesName[j]);
						}							
					}
				}else {
					seriesName[task] = ImageIds[0];
				}
			}
			
		}
		
		if(logInitialFileScreening) {
			IJ.log("Task list has been finished, starting processing now."  + " Time: " + FullDateFormatter2.format(new Date()));
		}
		
		// add progressDialog
		progress = new ProgressDialog(name, seriesName);
		progress.setLocation(0, 0);
		progress.setVisible(true);
		progress.addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(WindowEvent winEvt) {
				if (processingDone == false) {
					IJ.error("Script stopped...");
				}
				continueProcessing = false;
				return;
			}
		});

		// &&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&
		// -----------------------------PROCESS TASKS----------------------------------
		// &&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&
		
		int nChannels, nSlices;
		
		javax.xml.transform.TransformerFactory transformerFactory = TransformerFactory.newInstance();
		javax.xml.transform.Transformer transformer;
		try {
			transformer = transformerFactory.newTransformer();
		} catch (TransformerConfigurationException e) {
			String out = "";
			for (int err = 0; err < e.getStackTrace().length; err++) {
				out += " \n " + e.getStackTrace()[err].toString();
			}
			progress.notifyMessage("Could not initialize transformer for XMLs. No easy fix available. Report problem to developer!"
					+ "\nError message: " + e.getMessage()
					+ "\nError localized message: " + e.getLocalizedMessage()
					+ "\nError cause: " + e.getCause() 
					+ "\nDetailed message:"
					+ "\n" + out,
					ProgressDialog.ERROR);
			return;
		}		
		DOMSource source;
		FileWriter writer = null;
		ImporterOptions bfOptions = null;
		
		for (int task = 0; task < tasks; task++) {
			running: while (continueProcessing) {
				progress.updateBarText("In progress...");
				if(extendedLogging)	progress.notifyMessage("Series name: " + seriesName [task] + "", ProgressDialog.LOG);
				
				/**
				 * Import the raw metadata XML file and generate document to read from it (only regenerate it if it was different in previous file)
				 * 
				 * Useful information about the XML file that contains the metadata
				 * The OPERA setup assigns a specific ID to each image. This image ID looks e.g. as follows: "0501K1F1P1R1"
				 * An ID is created for each individual image (so also individual plane, channel, position.
				 * The ID encodes for the specific field of view, plane, channel, well, ... as follows (refers to the example above):
				 * 	- '0501' refers to the <Well> id and is composed of the Row (05 in this example) and Column (01 in this example)
				 * 	- 'K1': Unknown for now what it refers to, maybe the time point? Or plate ID?
				 * 	- 'F1' refers to the field of view so the position in the well
				 * 	- 'P1' refers to the focal plane so the z position
				 * 	- 'R1' refers to the channel position
				 * Under the main Node <Wells> all wells are listed as a <Well> node and image ids for the images in that well are noted
				 * Under the main node <Images> each image is listed as an <Image> node, which has as childs all metadata for that image, including channel information, id, etc.
				 */
				{
					String tempPath = dir [task] + System.getProperty("file.separator") + name [task];					
					if(tempPath.equals(loadedSrcMetadataFilePath)) {
						// Metadatadocument is already loaded
						if(extendedLogging)	progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ": Metadata file path: " + srcMetadataFilePath + "", ProgressDialog.LOG);
						if(extendedLogging) {
							progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ": Metadata file was already loaded in task " + (loadedSrcTask + 1) 
									+ " and thus not reloaded. The logging information from loading the metadatafile were:\n" + srcLoadingLog + "", srcLoadingLogMode);
						}else if(loadingLogMode != ProgressDialog.LOG) {
							progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ": Metadata file was already loaded in task " + (loadedSrcTask + 1) 
									+ " and thus not reloaded. There were WARNINGS! See log information here:\n" + srcLoadingLog + "", srcLoadingLogMode);
						}						
					}else{
						// Document needs to be loaded
						if(!loadSourceOPERAMetadatafile(tempPath, task)) {
							progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ": Could not load metadata file " + tempPath + "!",
									ProgressDialog.ERROR);
							break running;
						}
						if(extendedLogging) {
							progress.notifyMessage("Metadata file path: " + srcMetadataFilePath + "", ProgressDialog.LOG);
							progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ": Metadata file loaded:" + srcLoadingLog + "", srcLoadingLogMode);
						}else if(loadingLogMode != ProgressDialog.LOG) {
							progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ": Metadata file loaded. There were WARNINGS:" + srcLoadingLog + "", srcLoadingLogMode);
						}						
					}
				}

				Document tempMetaDoc = null;
				File newTemporaryMetadataFile = null, srcFile = null, destFile = null;
				String tempProjectDir = "";
				String dataStructureVersion = "unknown";
				{
					/**
					 * We create a temporary folder only for the image we want to process and copy files that we need there
					 */
					tempProjectDir = outPath + System.getProperty("file.separator") + "temp_4Load_" + task + "" + System.getProperty("file.separator");
					new File(tempProjectDir).mkdirs();
					
					// Copy the Assay Layout file there
					try{
						srcFile = new File(dir[task].substring(0,dir[task].lastIndexOf("Images")) 
								+ System.getProperty("file.separator") + "Assaylayout" + System.getProperty("file.separator"));
						destFile = new File(tempProjectDir + "Assaylayout" + System.getProperty("file.separator"));
						
						try {
							FileUtils.copyDirectory(srcFile, destFile);
						} catch (IOException e) {
							e.printStackTrace();
							return;
						}
						dataStructureVersion = "V1";
					}catch (Exception e){
						/*
						 * Newer folder structures are somehow different and this is relevant here:
						 * hs
						 * > _hs.txt
						 * > _objmeta.txt
						 * > _structure.txt
						 * > SomeNumberCode
						 * > > flatfieldcorrection
						 * > > > numbercode.txt
						 * > > > numbercode.xml
						 * > > images
						 * > > > folders for each well named r01c01
						 * > > > image.idx.txt
						 * > > > index.xml
						 * > > index
						 * > > > numbercode.txt
						 * > > > numbercode.xml
						 * > > numbercode.txt
						 * > > numbercode.xml
						 * > > dataset.version.txt
						 */	
						
						//Copy folders with metadata
						File mainDir = new File(dir[task].substring(0,dir[task].lastIndexOf(System.getProperty("file.separator") + "hs" + System.getProperty("file.separator"))) 
								+ System.getProperty("file.separator") + "hs" + System.getProperty("file.separator"));
						tempProjectDir += "hs" + System.getProperty("file.separator");						
						new File(tempProjectDir).mkdir();
						
						String addDir = "";
						String[] allFiles = mainDir.list();
						for (String file : allFiles) {
							if(new File(mainDir + System.getProperty("file.separator") + file).isDirectory()) {
								File createDir = new File(tempProjectDir + System.getProperty("file.separator")
											+ file + System.getProperty("file.separator"));
								createDir.mkdir();
//								IJ.log("Created: " + createDir.getAbsolutePath());
								
								String[] allFiles2 = new File(mainDir + System.getProperty("file.separator") + file + System.getProperty("file.separator")).list();
								for (String file2 : allFiles2) {
//									IJ.log("Eventually copying " + file2);
									
									srcFile = new File(mainDir + System.getProperty("file.separator") + file + System.getProperty("file.separator") + file2);
									if(file2.toUpperCase().equals("IMAGES") && srcFile.isDirectory()) {
										addDir = file + System.getProperty("file.separator");
										
										//Copy a text file about image indexes if it exists.
										srcFile = new File(mainDir + System.getProperty("file.separator") + file + System.getProperty("file.separator") + file2 + System.getProperty("file.separator") + "image.index.txt");
										destFile = new File(tempProjectDir + file + System.getProperty("file.separator") + file2 + System.getProperty("file.separator") + "image.index.txt");
										if(srcFile.exists()) {
											try {
												FileUtils.copyFile(srcFile, destFile);
//												IJ.log("Copied:	" + srcFile.getAbsolutePath() + "	to	" + destFile.getAbsolutePath());
											} catch (IOException ioE) {
												ioE.printStackTrace();
												return;
											}
										}										
//										IJ.log("Skipping " + file2);
										continue;
									}								
																		
									destFile = new File(tempProjectDir + file + System.getProperty("file.separator") + file2);
									
									try {
										if(srcFile.isDirectory()) {
											FileUtils.copyDirectory(srcFile, destFile);
										}else {
											FileUtils.copyFile(srcFile, destFile);
										}										
//										IJ.log("Copied:	" + srcFile.getAbsolutePath() + "	to	" + destFile.getAbsolutePath());
									} catch (IOException ioE) {
										ioE.printStackTrace();
										return;
									}
								}								
							}else {
								srcFile = new File(mainDir + System.getProperty("file.separator") + file);					
								destFile = new File(tempProjectDir + file);								
								try {
									FileUtils.copyFile(srcFile, destFile);
//									IJ.log("Copied:	" + srcFile.getAbsolutePath() + "	to	" + destFile.getAbsolutePath());
								} catch (IOException ioE) {
									ioE.printStackTrace();
									return;
								}
							}
							
						}
						dataStructureVersion = "V2";
						tempProjectDir += addDir;
					}
					
					
					
					/**
					 * Copy a cleaned metadata file to that place
					 */
					String imgDir = "";
					if(dataStructureVersion == "V1") {
						imgDir = tempProjectDir + System.getProperty("file.separator") + "Images" + System.getProperty("file.separator");
					}else if(dataStructureVersion == "V2") {
						imgDir = tempProjectDir + System.getProperty("file.separator") + "images" + System.getProperty("file.separator");
					}else {
						IJ.error("Unkown data structure! Can't process!");
					}
					new File(imgDir).mkdirs();
										
					/** 
					 * Clean meatadata from unneccessary information
					 */
					progress.updateBarText("Copying metadata xml to create temp folder for image " + seriesName [task] + "...");
					newTemporaryMetadataFile = new File(imgDir + name[task]);
					
					try {
						DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
						DocumentBuilder db = dbf.newDocumentBuilder();
						
						//Create a new document and copy it.
						tempMetaDoc = db.newDocument();			
					    Node originalRoot = srcMetaDoc.getDocumentElement();
				        Node copiedRoot = tempMetaDoc.importNode(originalRoot, true);
				        tempMetaDoc.appendChild(copiedRoot);
					} catch (ParserConfigurationException e) {
						String out = "";
						for (int err = 0; err < e.getStackTrace().length; err++) {
							out += " \n " + e.getStackTrace()[err].toString();
						}
						progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ": Could not process metadata file " + metadataFilePath 
								+ "\nError message: " + e.getMessage()
								+ "\nError localized message: " + e.getLocalizedMessage()
								+ "\nError cause: " + e.getCause() 
								+ "\nDetailed message:"
								+ "\n" + out,
								ProgressDialog.ERROR);
						continue;
					}
										
					cleanMetadataDocFromUselessImages(tempMetaDoc, seriesName[task], task);
					 
					/**
					 *  Save metadata file
					 */
					 try {
						source = new DOMSource(tempMetaDoc);
						writer = new FileWriter(newTemporaryMetadataFile);
						StreamResult result = new StreamResult(writer);
						
						transformer.setOutputProperty(OutputKeys.INDENT, "yes");
						transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "3");
						transformer.transform(source, result);
						
						if(extendedLogging) {
							progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ": Metadata XML has been modified...", ProgressDialog.LOG);	
						}
					} catch (TransformerException e) {
						String out = "";
						for (int err = 0; err < e.getStackTrace().length; err++) {
							out += " \n " + e.getStackTrace()[err].toString();
						}
						progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ": " 
								+ "Error when writing modified xml." 
								+ "\nError message: " + e.getMessage()
								+ "\nError localized message: " + e.getLocalizedMessage()
								+ "\nError cause: " + e.getCause() 
								+ "\nDetailed message:"
								+ "\n" + out,
								ProgressDialog.ERROR);
						continue;
					} catch (IOException e) {
						String out = "";
						for (int err = 0; err < e.getStackTrace().length; err++) {
							out += " \n " + e.getStackTrace()[err].toString();
						}
						progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ": " 
								+ "Error when writing modified xml." 
								+ "\nError message: " + e.getMessage()
								+ "\nError localized message: " + e.getLocalizedMessage()
								+ "\nError cause: " + e.getCause() 
								+ "\nDetailed message:"
								+ "\n" + out,
								ProgressDialog.ERROR);
						continue;
					} finally {
						try {
							writer.close();
							writer = null;
							source = null;
						} catch (IOException e) {
							String out = "";
							for (int err = 0; err < e.getStackTrace().length; err++) {
								out += " \n " + e.getStackTrace()[err].toString();
							}
							progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ": " 
									+ "Error when closing xml writer." 
									+ "\nError message: " + e.getMessage()
									+ "\nError localized message: " + e.getLocalizedMessage()
									+ "\nError cause: " + e.getCause() 
									+ "\nDetailed message:"
									+ "\n" + out,
									ProgressDialog.ERROR);
							continue;
						}
					}
					 
					if(extendedLogging)	progress.notifyMessage("Cleaned meta data file (" + metaDataFile + ") as " + newTemporaryMetadataFile.getAbsolutePath(), ProgressDialog.LOG);
					progress.updateBarText("Metadata XML has been cleaned...");
					
					/**
					 * Now copy the images
					 */
					{
						NodeList nl = null;
						try {
							XPathExpression expr = xp.compile("//Image[id[starts-with(text(),'" + seriesName[task] + "')]]");
							nl = (NodeList) expr.evaluate(tempMetaDoc, XPathConstants.NODESET);
						} catch (XPathExpressionException e) {
							String out = "";
							for (int err = 0; err < e.getStackTrace().length; err++) {
								out += " \n " + e.getStackTrace()[err].toString();
							}
							progress.notifyMessage("Task " + (0 + 1) + "/" + tasks + ": Could not fetch image nodes beginning with id " + seriesName[task] 
									+ "\nError message: " + e.getMessage()
									+ "\nError localized message: " + e.getLocalizedMessage()
									+ "\nError cause: " + e.getCause() 
									+ "\nDetailed message:"
									+ "\n" + out,
									ProgressDialog.ERROR);
						}
						
						for(int img = 0; img < nl.getLength(); img++) {
							String tempImgName = getFirstNodeWithName(nl.item(img).getChildNodes(),"URL").getTextContent();
							
							String subFolder = "";
							if(dataStructureVersion == "V1") {
								subFolder = ""; // In version 1 of OPERA files, no subfolders were used.
							}else if(dataStructureVersion == "V1") {
								subFolder = "r" + seriesName[task].substring(0,2) 	// retrieves row two digit code
										+ "c" + seriesName[task].substring(0,2)				// retrieves column two digit code
										+ System.getProperty("file.separator");
							}
							
							// Copy the Assay Layout file there
							try {
								FileUtils.copyFile(new File(dir[task] + System.getProperty("file.separator") + subFolder + tempImgName),
										new File(imgDir + subFolder + tempImgName), 
										true);
							} catch (IOException e) {
								String out = "";
								for (int err = 0; err < e.getStackTrace().length; err++) {
									out += " \n " + e.getStackTrace()[err].toString();
								}
								progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ": Error when copying images to the temp folder for fast loading (image ids starting with "
										+ seriesName[task] + ")!\n"
										+ "Source:	" + dir[task] + System.getProperty("file.separator") + subFolder + tempImgName + "\n"
										+ "Destination:	" + imgDir + subFolder + tempImgName + "\n"
										+ "Error " + e.getCause() + " - Detailed message:\n" + out,
										ProgressDialog.ERROR);
								break running;
							}
						}
					}
					
					
					if(!loadOPERAMetadatafile(newTemporaryMetadataFile.getAbsolutePath(), task)) {
						progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ": Could not load new metadata file at " + newTemporaryMetadataFile.getAbsolutePath(),
								ProgressDialog.ERROR);
						break running;
					}else {
						if(extendedLogging) {
							progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ": Metadata file loaded:" + loadingLog + "", loadingLogMode);
						}else if(loadingLogMode != ProgressDialog.LOG) {
							progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ": There were WARNINGS when loading metadata! See log information here:" + loadingLog + "", loadingLogMode);
						}
					}
				}
				
				
				/***
				 * Open the image and save it as OME TIF in a temp folder
				 */				
				progress.updateBarText("Loading images via BioFormats...");
				ImagePlus imp = null;				
				{
					//Load from the new created project (if not loaded via bioformats)
					try {
						//bio format reader
						bfOptions = new ImporterOptions();
		   				bfOptions.setId(newTemporaryMetadataFile.getAbsolutePath());
		   				bfOptions.setVirtual(false);
		   				bfOptions.setAutoscale(true);
		   				bfOptions.setColorMode(ImporterOptions.COLOR_MODE_COMPOSITE);
		   				bfOptions.setOpenAllSeries(true);
		   				ImagePlus imps [] = BF.openImagePlus(bfOptions); // PROBLEM
		   				if(imps.length > 1) {
							progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ": Could not process "
									+ series[task] + " - Error: Too many series in temp file!",
									ProgressDialog.ERROR);
							break running;
		   				}
		   				imp = imps [0];
		   				imp.setDisplayMode(IJ.COMPOSITE);						
					} catch (Exception e) {
						String out = "";
						for (int err = 0; err < e.getStackTrace().length; err++) {
							out += " \n " + e.getStackTrace()[err].toString();
						}
						progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ": Could not process "
								+ series[task] + " - Error " + e.getCause() + " - Detailed message:\n" + out,
								ProgressDialog.ERROR);
						break running;
					}
					System.gc();
				}

				// Set Z calibration information since not read correctly for OPERA files
				if(zStepSizeInMicronAcrossWholeOPERAFile > 0.0 && zStepSizeInMicronAcrossWholeOPERAFile != imp.getCalibration().pixelDepth) {
					if(extendedLogging || LOGZDISTFINDING) {
						progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ": "
								+ "Corrected voxel depth in image calibration settings - new value is " + zStepSizeInMicronAcrossWholeOPERAFile 
								+ " whereas old value was " + imp.getCalibration().pixelDepth + " " + imp.getCalibration().getZUnit() + ".",
								ProgressDialog.LOG);
					}
					imp.getCalibration().pixelDepth = zStepSizeInMicronAcrossWholeOPERAFile;
					imp.getCalibration().setZUnit("micron");					
				}
				
				//Crop image to user defined size
				if(cropImage) {
					progress.updateBarText("Cropping...");
					imp.setRoi((int)((double)(imp.getWidth() - newImgLength)/2.0),
							(int)((double)(imp.getHeight() - newImgLength)/2.0),
							newImgLength,
							newImgLength);
//					imp.show();
//					new WaitForUserDialog("checkRoi").show();
					IJ.run(imp, "Crop", "");
				}
//				imp.show();
//				new WaitForUserDialog("check cropped").show();
				
				
				/**
				 * Extract information from image needed for later tif extension					 * 
				 */
				nChannels = imp.getNChannels();
				nSlices = imp.getNSlices();
								
				/**
				 * Create filename for the output files
				 * Example filename: Index.idx.xml
				 * Example directory: \E:\CP\Data\20230412 Sperm OPERA test -\230412 hansen__2023-04-12T13_47_17-Measurement 1\Images 
				 * Example series name: Series_90: Well 10, Field 9: 2160 x 2160; 25 planes 
				 */
				
				// Find out the directory name for the whole OPER(ETT)A export folder
				String orDirName = "";
				if(dataStructureVersion == "V1") {
					// In version 1 of OPERA files, no subfolders are used.
					orDirName = dir [task].substring(0,dir [task].lastIndexOf(System.getProperty("file.separator")+"Images"));
				}else if(dataStructureVersion == "V2") {
					// In version 2 of OPERETTA files, subfolders for each well are used.
					orDirName = dir [task].substring(0,dir [task].lastIndexOf(System.getProperty("file.separator")+"hs"+System.getProperty("file.separator")));
				}
				orDirName = orDirName.substring(orDirName.lastIndexOf(System.getProperty("file.separator"))+1);
								
				// Create filename by combining the export folder name and the series label
				String outFilename = orDirName + "_" + (series[task]+1);
				outFilename = outFilename.replace(" ", "_"); // Replace blanks with underscores for better readability.
				
				if(extendedLogging) {
					progress.notifyMessage("Retrieved original dir name: " + orDirName, ProgressDialog.LOG);					
				}	
					
				progress.updateBarText("Creating temporary ome.tif files...");
				/**
				 * Create a temporary file repository and write the files into an OME-TIF format, output images will be called <outFilename>_Z2_C4.ome
				 */
				String tempDir = outPath + System.getProperty("file.separator") + "temp_" + task + "" + System.getProperty("file.separator");
				new File(tempDir).mkdirs();
				
				IJ.run(imp, "OME-TIFF...", "save=[" + tempDir + outFilename + ".ome.tif] write_each_z_section write_each_channel use export compression=Uncompressed");						
				
				//TODO Try new ome tiff export below for speed - needs more debugging, meanwhile using the FIJI based command above.	
//				try {					
//					IJ.log((String)imp.getProperty("Info"));
//					
//					ServiceFactory factory = new ServiceFactory();
//					OMEXMLService service = factory.getInstance(OMEXMLService.class);
////					OMEXMLMetadata md = service.createOMEXMLMetadata((String)imp.getProperty("Info"));
//					OMEXMLMetadata md = service.createOMEXMLMetadata();
//					
//					OMETiffWriter OTWriter = new OMETiffWriter();					
//					OTWriter.setMetadataRetrieve(md);
//					OTWriter.setWriteSequentially(true);
//					OTWriter.setId(tempDir + outFilename + ".ome.tif");
//					OTWriter.setCompression("Uncompressed");
//					
//					//Save each plane
//					for (int t=0; t<imp.getNFrames(); t++) {
//					    for (int z=0; z<imp.getNSlices(); z++) {
//					        for (int c=0; c<imp.getNChannels(); c++) {
//					            byte[] planePixelArray = getBytes(imp, z, c, t);
//					            int index = imp.getStackIndex(c+1, z+1, t+1)-1;
//					            OTWriter.saveBytes(index, planePixelArray);            
//					        }
//					    }
//					}					
//					OTWriter.close();
//					service = null;
//					md = null;					
//				} catch (FormatException | IOException e) {
//					String out = "";
//					for (int err = 0; err < e.getStackTrace().length; err++) {
//						out += " \n " + e.getStackTrace()[err].toString();
//					}
//					progress.notifyMessage("Task " + (0 + 1) + "/" + tasks + ": PROBLEM WITH SAVING TEMP FILE "
//							+ tempDir + outFilename + ".ome.tif"
//							+ "\nError message: " + e.getMessage()
//							+ "\nError localized message: " + e.getLocalizedMessage()
//							+ "\nError cause: " + e.getCause() 
//							+ "\nDetailed message:"
//							+ "\n" + out,
//							ProgressDialog.ERROR);
//				} catch (ServiceException e) {
//					String out = "";
//					for (int err = 0; err < e.getStackTrace().length; err++) {
//						out += " \n " + e.getStackTrace()[err].toString();
//					}
//					progress.notifyMessage("Task " + (0 + 1) + "/" + tasks + ": PROBLEM WITH SAVING TEMP FILE "
//							+ tempDir + outFilename + ".ome.tif"
//							+ "\nError message: " + e.getMessage()
//							+ "\nError localized message: " + e.getLocalizedMessage()
//							+ "\nError cause: " + e.getCause() 
//							+ "\nDetailed message:"
//							+ "\n" + out,
//							ProgressDialog.ERROR);
//				} catch (DependencyException e) {
//					String out = "";
//					for (int err = 0; err < e.getStackTrace().length; err++) {
//						out += " \n " + e.getStackTrace()[err].toString();
//					}
//					progress.notifyMessage("Task " + (0 + 1) + "/" + tasks + ": PROBLEM WITH SAVING TEMP FILE "
//							+ tempDir + outFilename + ".ome.tif"
//							+ "\nError message: " + e.getMessage()
//							+ "\nError localized message: " + e.getLocalizedMessage()
//							+ "\nError cause: " + e.getCause() 
//							+ "\nDetailed message:"
//							+ "\n" + out,
//							ProgressDialog.ERROR);
//				}
				
				
				/***
				 * Close the image
				 */
				try {
					imp.changes = false;
					imp.close();
					System.gc();
				} catch (Exception e) {
					progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ": No image could be loaded... ",
							ProgressDialog.ERROR);
					break running;
				}				
				progress.updateBarText("Created temporary ome.tif files... now starting reformating");
				
				/*
				 * Reopen each written file and resave it 
				*/
				{
					/**
					 * Shuffle through the different images to retrieve and resave them with extended comments
					 * Example file name: <outFilename>_Z2_C4.ome.tif
					 */
					String omeTifFileName, comment;
					ServiceFactory factory;
					OMEXMLService service;
					OMEXMLMetadata meta;
					TiffParser tp;
					
					for(int channel = 0; channel < nChannels; channel++) {
						for(int slice = 0; slice < nSlices; slice++) {
							String zCode = "Z"+(slice);
							omeTifFileName = tempDir + outFilename + "_" + zCode +"_C"+(channel)+".ome.tif";	
							
							if(!new File(omeTifFileName).exists() && slice < 10 && nSlices > 10) {
								zCode = "Z0"+(slice);
								omeTifFileName = tempDir + outFilename + "_" + zCode +"_C"+(channel)+".ome.tif";
							}
							
//							IJ.log(omeTifFileName + " (exists? " + new File(omeTifFileName).exists() + " s " + slice + "/" + nSlices);
														
							try {
								/**
								 * Open the tif file and extract the tif comment (= OME XML String)
								 * */
								tp = new TiffParser(omeTifFileName);
								comment = "" + tp.getComment();
								tp = null;
								System.gc();
								
								progress.updateBarText("Reading " + omeTifFileName + " done!");
								// display comment, and prompt for changes
								if(logWholeOMEXMLComments) {
									progress.notifyMessage("Original comment:\n" + comment, ProgressDialog.LOG);
								}
																
								/**
								 * Generate a MetadatStore out of the tif comment (= OME XML String) to explore the xml-styled content
								 * */
								progress.updateBarText("Generate metadata store from tiff comment for image " + omeTifFileName);
								factory = new ServiceFactory();
								service = factory.getInstance(OMEXMLService.class);
								meta = service.createOMEXMLMetadata(comment);
								
								/**
								 * Check whether there is more than one image and then find out image ID
								 */
								if(meta.getImageCount() > 1) {
									progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ", Image " + metadataFilePath + " - OME XML Annotation features more than one image. Unclear which image is meant!",
											ProgressDialog.ERROR);
									continue;
								}
								int imageIndex = 0;
								String imageID = meta.getImageID(imageIndex);
								if(extendedLogging){
									progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ", Image " + metadataFilePath + ". Fetched imageId: " + imageID + ".", ProgressDialog.LOG);
								}
																
								/**
								 * Find the plate, well, and wellSample that the image is from.
								 */
								progress.updateBarText("Fetcb plate, well, and sample ids for image " + omeTifFileName);
								int plateIndex, wellIndex, wellSampleIndex;
								plateIndex = -1;
								wellIndex = -1;
								wellSampleIndex = -1;
								String plateID, wellID;
								plateID = "NA";
								wellID = "NA";
								
								findingImage: for(int p = 0; p < meta.getPlateCount(); p++) {
									plateID = meta.getPlateID(p);
									for(int w = 0; w < meta.getWellCount(p); w++) {
										wellID = meta.getWellID(p, w);
										for(int sam = 0; sam < meta.getWellSampleCount(p, w); sam ++) {
											try {
												if(meta.getWellSampleImageRef(p, w, sam).equals(imageID)){
													plateIndex = p;
													wellIndex = w;
													wellSampleIndex = sam;													
													break findingImage;
												}
											}catch(Exception e) {
											}											
										}
									}
								}
								
								if(plateIndex == -1 || wellIndex == -1 || wellSampleIndex == -1) {
									progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ", Image " + metadataFilePath + " - Could not find image noted in plate and well OME annotations!",
											ProgressDialog.ERROR);
									continue;										
								}else if(extendedLogging){
									progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ", Image " + metadataFilePath + " - Found image in plate and well OME annotations under"
											+ " plateIndex " + plateIndex
											+ " wellIndex " + wellIndex
											+ " wellSampleIndex " + wellSampleIndex
											+ " well ID " + wellID
											+ " plate ID " + plateID,
											ProgressDialog.LOG);
								}
								
								/**
								 * Determine column and row coordinate on the plate
								 */
								int wellColumn, wellRow;
								wellColumn = -1;
								wellRow = -1;
								try {
									wellColumn = meta.getWellColumn(plateIndex, wellIndex).getNumberValue().intValue();
									wellRow = meta.getWellRow(plateIndex, wellIndex).getNumberValue().intValue();										
								}catch(Exception e) {
									String out = "";
									for (int err = 0; err < e.getStackTrace().length; err++) {
										out += " \n " + e.getStackTrace()[err].toString();
									}
									progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ", file" + metadataFilePath 
											+ ":\nFailed to fetch Column or Row information from the Well in OME metadata object." 
											+ "\nError message: " + e.getMessage()
											+ "\nError localized message: " + e.getLocalizedMessage()
											+ "\nError cause: " + e.getCause() 
											+ "\nDetailed message:"
											+ "\n" + out,
											ProgressDialog.ERROR);
									continue;							
								}
								
								if(wellColumn == -1 || wellRow  == -1) {
									progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ", Image " + metadataFilePath + " - Could not fetch well and column coordinate from OME annotations!",
											ProgressDialog.ERROR);
									continue;	
								}
								
								if(extendedLogging)	progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ": Fetched well coordinates (starting at 0): Column " + wellColumn + ", Row " + wellRow, ProgressDialog.LOG);

								/**
								 * Find image information in TiffData
								 */
								progress.updateBarText("Exploring TiffData of image " + omeTifFileName);
									
								int imageC = -1, imageT = -1, imageZ = -1, imageIFD = -1;
								String imageUUID = "NA";
								if(meta.getTiffDataCount(imageIndex)==0) {
									progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ", Image " + metadataFilePath + " is missing tiff data nodes in OME structure. Image Skipped!",
											ProgressDialog.ERROR);
									continue;
								}else if(extendedLogging) {
									progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ", Image " + metadataFilePath + " , found " + meta.getTiffDataCount(imageIndex) + " TiffData nodes in OME XML.",
											ProgressDialog.LOG);
								}
								
								
								for(int td = 0; td < meta.getTiffDataCount(imageIndex); td++) {																		
									if(extendedLogging) progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ", Image " + metadataFilePath 
											+ " , searching [" 
											+ (outFilename + "_"+zCode+"_C"+(channel)+".ome.tif") 
											+ "]. Is it [" 
											+ meta.getUUIDFileName(imageIndex, td)
											+ "]? Answer: "
											+ (meta.getUUIDFileName(imageIndex, td).equals(outFilename + "_"+zCode+"_C"+(channel)+".ome.tif")),
											ProgressDialog.LOG);
									
									if(meta.getUUIDFileName(imageIndex, td).equals(outFilename + "_"+zCode+"_C"+(channel)+".ome.tif")) {
										imageC = meta.getTiffDataFirstC(imageIndex, td).getValue();
										imageT = meta.getTiffDataFirstT(imageIndex, td).getValue();
										imageZ = meta.getTiffDataFirstZ(imageIndex, td).getValue();
										imageUUID = meta.getUUIDValue(imageIndex, td);
										imageIFD = meta.getTiffDataIFD(imageIndex, td).getValue();
										break;
									}
								}
													

								if(imageC == -1 | imageT == -1 | imageZ == -1 | imageIFD == -1) {
									progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ", Image " + metadataFilePath + " - Could not find tiff data node in OME XML Document!"
											+ "	C:" + imageC
											+ "	T:" + imageT
											+ "	Z:" + imageZ
											+ "	UUID:" + imageUUID
											+ "	IFD:" + imageIFD,
											ProgressDialog.ERROR);
									continue;
								}
								
								if(imageUUID == "NA") {
									progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ", Image " + metadataFilePath + " - Could not find image UUID in OME XML Document!",
											ProgressDialog.ERROR);
									continue;
								}
								
								if(extendedLogging) {
									progress.notifyMessage("Fetched TiffData information for the current image: "
											+ "UUID " + imageUUID
											+ ", C " + imageC
											+ ", T " + imageT
											+ ", Z " + imageZ
											+ ", IFD " + imageIFD, 
											ProgressDialog.LOG);
								}
									
								
								/**
								 * Find the relating image node in the original metdata xml from the OPERA folder system
								 * To do this first create the label that is used in opera based on the information obtained.
								 * The OPERA setup assigns a specific ID to each image. This image ID looks e.g. as follows: "0501K1F1P1R1"
								 * An ID is created for each individual image (so also individual plane, channel, position.
								 * The ID encodes for the specific field of view, plane, channel, well, ... as follows (refers to the example above):
								 * 	- '0501' refers to the <Well> id and is composed of the Row (05 in this example) and Column (01 in this example)
								 * 	- 'K1': Unknown for now what it refers to, maybe the time point? Or plate ID?
								 * 	- 'F1' refers to the field of view so the position in the well
								 * 	- 'P1' refers to the focal plane so the z position
								 * 	- 'R1' refers to the channel position
								 */
								progress.updateBarText("Searching the metadata note in the original OPERA index file for image " + omeTifFileName);
								String imageLabelOPERA = getOPERAString(wellRow, wellColumn, imageT, wellSampleIndex, imageZ, imageC);
								imageLabelOPERA = seriesName[task] + "P" + String.valueOf(imageZ+1) + "R" + String.valueOf(imageC+1);
								
								if(extendedLogging)	progress.notifyMessage("Reconstructed reference in OPERA metadata " + imageLabelOPERA, ProgressDialog.LOG);
								
								Node imageNode = getImageNodeWithID_OPERAMETADATA_UsingXPath(imagesNode.getChildNodes(), imageLabelOPERA);
								if(imageNode.equals(null)) {
									progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ", Image " + metadataFilePath + " - Could not find image node with id " + imageLabelOPERA + "in OPERA Metadata XML!",
											ProgressDialog.ERROR);
									continue;
								}
								
								if(extendedLogging)	progress.notifyMessage("Found image node with id " + imageLabelOPERA + " in OPERA metadata!", ProgressDialog.LOG);
							
								progress.updateBarText("Exploring the original OPERA index file for image " + omeTifFileName + " (OPERA ID " + imageLabelOPERA + ")");
								/**
								 * Determine X and Y position of well center
								 */
								double wellCenterXInMM = centerOfFirstWellXInMM + (double) wellColumn * distanceBetweenNeighboredWellCentersXInMM;
								double wellCenterYInMM = centerOfFirstWellYInMM + (double) wellRow * distanceBetweenNeighboredWellCentersYInMM;
								
								if(extendedLogging)	progress.notifyMessage("Well coordinates are " + wellCenterXInMM + " | " + wellCenterYInMM , ProgressDialog.LOG);
																
								/**
								 * Correct X, Y, Z positions in each stored plane
								 */
								double newXInM, newYInM, newZInM;
								for(int p = 0; p < meta.getPlaneCount(imageIndex); p++) {
									/**
									 * Find the nodes where to read plane information from 
									 */
									String OPERAString = seriesName[task];
									OPERAString += "P" + String.valueOf(meta.getPlaneTheZ(imageIndex, p).getValue()+1);
									OPERAString += "R" + String.valueOf(meta.getPlaneTheC(imageIndex, p).getValue()+1);
									
									Node planeImageNode = null;									
									try {
										planeImageNode = getImageNodeWithID_OPERAMETADATA_UsingXPath(imagesNode.getChildNodes(), OPERAString);
									}catch(java.lang.NullPointerException e) {
										progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ", plane " + (p+1) + ":" + "Searching node " 
												+ OPERAString
												+ " in OPERA metadata xml failed. Processing of this image skipped. Output data will be missing a lot of metadata information." 
												+ "", ProgressDialog.ERROR);
										continue;
									}
									
									/**
									 * Some new files are not correctly importing position and require fetching from XML file. TODO
									 * Thus, checking metadata first.
									 * TODO
									 */									
									{
										Node tempNode = getFirstNodeWithName(planeImageNode.getChildNodes(), "PositionX");
										Unit<Length> tempUnit = getLengthUnitFromNodeAttribute(tempNode);
										
										double 	val1 = Double.parseDouble(tempNode.getTextContent()),
												val2 = meta.getPlanePositionX(imageIndex, p).value(tempUnit).doubleValue();  
										
										int digitsToCompare = getMinNrOfDigits(val1, val2);
										String val1Str = String.format("%." + String.valueOf(digitsToCompare) + "g%n", val1);
										String val2Str = String.format("%." + String.valueOf(digitsToCompare) + "g%n", val2);
										
										if(!val1Str.equals(val2Str)) {
											if(!noWarningsForXYZConflicts) {
												progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ", Image " 
														+ metadataFilePath + " - X location in OME metadata did not match metadata of image with reference " + imageLabelOPERA + "in OPERA Metadata XML. "
														+ "We need to correct the absolute X position based on OPERA Metadata to be" + val1 + " " + tempUnit.getSymbol() + "!"
														+ " (XML metadata x value: " + val1 + " " + tempUnit.getSymbol()
														+ " OME x value: " + val2 + " " + tempUnit.getSymbol() 
														+ ", converted to " + meta.getPlanePositionX(imageIndex, wellSampleIndex).value().doubleValue() 
														+ " " + meta.getPlanePositionX(imageIndex, wellSampleIndex).unit().getSymbol()
														+ digitsToCompare + " were " + val1Str + " and " + val2Str + ")"
														, ProgressDialog.LOG);
											}
											
											meta.setPlanePositionX(FormatTools.createLength(FormatTools.createLength(val1,tempUnit).value(UNITS.METER).doubleValue(),
													UNITS.METER), imageIndex, p);
											
											//RECHECKING post writing:
											val2 = meta.getPlanePositionX(imageIndex, p).value(tempUnit).doubleValue();
											val2Str = String.format("%." + String.valueOf(digitsToCompare) + "g%n", val2);
											if(!val1Str.equals(val2Str)) {
												progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ", Image " 
														+ metadataFilePath + " rechecked post writing X from XML and did still not match:"
														+ " XML metadata x value: "
														+ val1
														+ " OME x value: "
														+ val2
														+ ", converted to "
														+ digitsToCompare
														+ " were "
														+ val1Str
														+ " and "
														+ val2Str
														, ProgressDialog.ERROR);
											}else if(extendedLogging || LOGPOSITIONCONVERSIONFORDIAGNOSIS) {
												progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ", Image " 
														+ metadataFilePath + " rechecked post writing X from XML:"
														+ " XML metadata x value: "
														+ val1
														+ " OME x value: "
														+ val2
														+ ", converted to "
														+ digitsToCompare
														+ " were "
														+ val1Str
														+ " and "
														+ val2Str
														, ProgressDialog.ERROR);
											}
										}else if(extendedLogging || LOGPOSITIONCONVERSIONFORDIAGNOSIS) {
											progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ", Image " + metadataFilePath + " - X location in tiff metadata matched in OPERA XML to the image with reference " 
													+ imageLabelOPERA + "!"
													+ " XML metadata x value: "
													+ val1
													+ " OME x value: "
													+ val2
													+ ", converted to "
													+ digitsToCompare
													+ " were "
													+ val1Str
													+ " and "
													+ val2Str
													, ProgressDialog.LOG);
										}
									}									
									{
										Node tempNode = getFirstNodeWithName(planeImageNode.getChildNodes(), "PositionY");
										Unit<Length> tempUnit = getLengthUnitFromNodeAttribute(tempNode);
										
										double 	val1 = Double.parseDouble(tempNode.getTextContent()),
												val2 = meta.getPlanePositionY(imageIndex, p).value(tempUnit).doubleValue();  
										
										int digitsToCompare = getMinNrOfDigits(val1, val2);
										String val1Str = String.format("%." + String.valueOf(digitsToCompare) + "g%n", val1);
										String val2Str = String.format("%." + String.valueOf(digitsToCompare) + "g%n", val2);
										
										if(!val1Str.equals(val2Str)) {
											if(!noWarningsForXYZConflicts) {
												progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ", Image " 
														+ metadataFilePath + " - Y location in OME metadata did not match metadata of image with reference " + imageLabelOPERA + "in OPERA Metadata XML. "
														+ "We need to correct the absolute Y position based on OPERA Metadata to be" + val1 + " " + tempUnit.getSymbol() + "!"
														+ " (XML metadata y value: " + val1 + " " + tempUnit.getSymbol()
														+ " OME y value: " + val2 + " " + tempUnit.getSymbol() 
														+ ", converted to " + meta.getPlanePositionY(imageIndex, wellSampleIndex).value().doubleValue()
														+ " " + meta.getPlanePositionY(imageIndex, wellSampleIndex).unit().getSymbol()
														+ digitsToCompare + " were " + val1Str + " and " + val2Str + ")"
														, ProgressDialog.LOG);
											}
											
											meta.setPlanePositionY(FormatTools.createLength(FormatTools.createLength(val1,tempUnit).value(UNITS.METER).doubleValue(),
													UNITS.METER), imageIndex, p);
											
											//RECHECKING post writing:
											val2 = meta.getPlanePositionY(imageIndex, p).value(tempUnit).doubleValue();
											val2Str = String.format("%." + String.valueOf(digitsToCompare) + "g%n", val2);
											if(!val1Str.equals(val2Str)) {
												progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ", Image " 
														+ metadataFilePath + " rechecked post writing Y from XML and did still not match:"
														+ " XML metadata y value: "
														+ val1
														+ " OME y value: "
														+ val2
														+ ", converted to "
														+ digitsToCompare
														+ " were "
														+ val1Str
														+ " and "
														+ val2Str
														, ProgressDialog.ERROR);
											}else if(extendedLogging || LOGPOSITIONCONVERSIONFORDIAGNOSIS) {
												progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ", Image " 
														+ metadataFilePath + " rechecked post writing Y from XML:"
														+ " XML metadata y value: "
														+ val1
														+ " OME y value: "
														+ val2
														+ ", converted to "
														+ digitsToCompare
														+ " were "
														+ val1Str
														+ " and "
														+ val2Str
														, ProgressDialog.ERROR);
											}
										}else if(extendedLogging || LOGPOSITIONCONVERSIONFORDIAGNOSIS) {
											progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ", Image " + metadataFilePath + " - Y location in tiff metadata matched in OPERA XML to the image with reference " 
													+ imageLabelOPERA + "!"
													+ " XML metadata y value: "
													+ val1
													+ " OME y value: "
													+ val2
													+ ", converted to "
													+ digitsToCompare
													+ " were "
													+ val1Str
													+ " and "
													+ val2Str
													, ProgressDialog.LOG);
										}
									}
									
									/**
									 * Calculate and modify X position
									 * Note: in the original metadata the positions are saved as micron values, but the unit indicates is reference frame and thus wrong. Need to correct that.
									 * */
									if(!meta.getPlanePositionX(imageIndex, p).unit().getSymbol().equals(UNITS.METER.getSymbol())) {
										if(meta.getPlanePositionX(imageIndex, p).unit().isConvertible(UNITS.METER)) {
											newXInM = meta.getPlanePositionX(imageIndex, p).value(UNITS.METER).doubleValue();
										}else {
											//The unit is probably reference frame and should be micron in real
											newXInM = meta.getPlanePositionX(imageIndex, p).value().doubleValue() / 1000.0 / 1000.0;
										}										
									}else {
										newXInM = meta.getPlanePositionX(imageIndex, p).value().doubleValue();
									}
									newXInM = wellCenterXInMM / 1000.0 + newXInM;
									
									if(extendedLogging || LOGPOSITIONCONVERSIONFORDIAGNOSIS)	progress.notifyMessage("Plane " + p + "(Original X coordinate: " 
											+ meta.getPlanePositionX(imageIndex, p).value().doubleValue() + " " + meta.getPlanePositionX(imageIndex, p).unit().getSymbol()
											+ " " + meta.getPlanePositionX(imageIndex, p).unit().getSymbol() 
											+ "; well center: " + wellCenterXInMM + " mm"
											+ ") will get X coordinate " + newXInM + " m", ProgressDialog.LOG);

									meta.setPlanePositionX(FormatTools.createLength(newXInM,UNITS.METER), imageIndex, p);
																		
									/**
									 * Calculate and modify Y position
									 * Note: in the original metadata the positions are saved as micron values, but the unit indicates is reference frame and thus wrong. Need to correct that to reach meter.
									 * */									
									if(!meta.getPlanePositionY(imageIndex, p).unit().getSymbol().equals(UNITS.METER.getSymbol())) {
										if(meta.getPlanePositionY(imageIndex, p).unit().isConvertible(UNITS.METER)) {
											newYInM = meta.getPlanePositionY(imageIndex, p).value(UNITS.METER).doubleValue();
										}else {
											//The unit is probably reference frame and should be micron in real
											newYInM = meta.getPlanePositionY(imageIndex, p).value().doubleValue() / 1000.0 / 1000.0;
										}										
									}else {
										newYInM = meta.getPlanePositionY(imageIndex, p).value().doubleValue();
									}
									newYInM = wellCenterYInMM / 1000.0 + newYInM;
									

									if(extendedLogging || LOGPOSITIONCONVERSIONFORDIAGNOSIS)	progress.notifyMessage("Plane " + p + "(Original Y coordinate: " + meta.getPlanePositionY(imageIndex, p).value().doubleValue() 
											+ " " + meta.getPlanePositionY(imageIndex, p).unit().getSymbol()  
											+ "; well center: " + wellCenterYInMM + " mm"
											+ ") will get Y coordinate " + newYInM + " m", ProgressDialog.LOG);

									meta.setPlanePositionY(FormatTools.createLength(newYInM,UNITS.METER), imageIndex, p);
									
									/**
									 * Correct unit of Z position if applicable
									 * In the originally generated file the Z position is often given in micron, although as unit only "reference frame" is specified!
									 * */
									if(meta.getPlanePositionZ(imageIndex, p).unit().isConvertible(UNITS.METER)) {
										// Z is correctly defined and nothing needs to be done except eventual conversion to meter
										if(!meta.getPlanePositionZ(imageIndex, p).unit().getSymbol().equals(UNITS.METER.getSymbol())) {
											meta.setPlanePositionZ(FormatTools.createLength(meta.getPlanePositionZ(imageIndex, p).value(UNITS.METER).doubleValue(),UNITS.METER), imageIndex, p);
										}
									}else {
										// Z is probably in reference frame corresponding to micron and needs to be converted to meter.
										newZInM = meta.getPlanePositionZ(imageIndex, p).value().doubleValue() / 1000 / 1000;
										if(extendedLogging || LOGPOSITIONCONVERSIONFORDIAGNOSIS)	progress.notifyMessage("Plane " + p 
												+ "(Original Z coordinate: " + meta.getPlanePositionZ(imageIndex, p).value().doubleValue() 
												+ " " + meta.getPlanePositionZ(imageIndex, p).unit().getSymbol() 
												+ ") will get Z coordinate " + newZInM + " m", ProgressDialog.LOG);
										meta.setPlanePositionZ(FormatTools.createLength(newZInM,UNITS.METER), imageIndex, p);
									}
									
									/**
									 * For security purposes let us try to cross check that with the original metadata file
									 * */
									{
										
										Node tempNode = getFirstNodeWithName(planeImageNode.getChildNodes(), "AbsPositionZ");
										Unit<Length> tempUnit = getLengthUnitFromNodeAttribute(tempNode);									
										if(!meta.getPlanePositionZ(imageIndex, p).unit().isConvertible(tempUnit)) {
											progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ":" + "Plane " + p
													+ " PROBLEM. The unit in the OME XML meta data is not convertible to the unit used for the Z position in the original metadata." 
													+ " Thus, metadata enrichment for this plane was skipped, and output files will miss metadata!!" 
													+ " Symbol stored in OME metadata: " + meta.getPlanePositionZ(imageIndex, p).unit().getSymbol()
													+ " . Symbol stored in original metadata xml: " + tempUnit.getSymbol(), ProgressDialog.ERROR);
											continue;
										}
										
										double 	val1 = Double.parseDouble(tempNode.getTextContent()),
												val2 = meta.getPlanePositionZ(imageIndex, p).value(tempUnit).doubleValue();
										int digitsToCompare = getMinNrOfDigits(val1, val2);
										String val1Str = String.format("%." + String.valueOf(digitsToCompare) + "g%n", val1);
										String val2Str = String.format("%." + String.valueOf(digitsToCompare) + "g%n", val2);
										
										if(!val1Str.equals(val2Str)) {
											if(!noWarningsForXYZConflicts) {
												progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ", Image " 
														+ metadataFilePath + " - Z location in OME metadata did not match metadata of image with reference " 
														+ imageLabelOPERA + "in OPERA Metadata XML. We need to correct the absolute Z position based on OPERA Metadata!"
														+ " (XML metadata z value: "
														+ val1
														+ " OME z value: "
														+ val2
														+ ", converted to "
														+ digitsToCompare
														+ " were "
														+ val1Str
														+ " and "
														+ val2Str
														+ ")"
														, ProgressDialog.LOG);
											}
											
											
											meta.setPlanePositionZ(FormatTools.createLength(FormatTools.createLength(val1,tempUnit).value(UNITS.METER).doubleValue(),
													UNITS.METER), imageIndex, p);
											
											//RECHECKING post writing:
											val2 = meta.getPlanePositionZ(imageIndex, p).value(tempUnit).doubleValue();
											val2Str = String.format("%." + String.valueOf(digitsToCompare) + "g%n", val2);
											if(!val1Str.equals(val2Str)) {
												progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ", Image " 
														+ metadataFilePath + " rechecked post writing Z from XML and did still not match:"
														+ " XML metadata z value: "
														+ val1
														+ " OME z value: "
														+ val2
														+ ", converted to "
														+ digitsToCompare
														+ " were "
														+ val1Str
														+ " and "
														+ val2Str
														, ProgressDialog.ERROR);
											}else if(extendedLogging || LOGPOSITIONCONVERSIONFORDIAGNOSIS) {
												progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ", Image " 
														+ metadataFilePath + " rechecked post writing Z from XML:"
														+ " XML metadata z value: "
														+ val1
														+ " OME z value: "
														+ val2
														+ ", converted to "
														+ digitsToCompare
														+ " were "
														+ val1Str
														+ " and "
														+ val2Str
														, ProgressDialog.ERROR);
											}
										}else if(extendedLogging || LOGPOSITIONCONVERSIONFORDIAGNOSIS) {
											progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ", Image " + metadataFilePath + " - Z location in tiff metadata matched in OPERA XML to the image with reference " 
													+ imageLabelOPERA + "!"
													+ " XML metadata z value: "
													+ val1
													+ " OME z value: "
													+ val2
													+ ", converted to "
													+ digitsToCompare
													+ " were "
													+ val1Str
													+ " and "
													+ val2Str
													, ProgressDialog.LOG);
										}
												
									}
								}									
								
								progress.updateBarText("Verify XY resolution information with original OPERA index file (OPERA ID " + imageLabelOPERA + ")");
								{
									
									Node tempNode;
									Node tempNode2;
									Unit<Length> tempUnit;
									Length tempLength;
									double 	val1, val2;
									int digitsToCompare;
									String val1Str, val2Str;
									
									if(srcMetaXMLVersion.equals("1")){
										tempNode = imageNode;
										if(extendedLogging) {
											progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ":" + "Using image node to check physical size X matches metadata (since metadata V1)!", ProgressDialog.LOG);
										}
									}else {
										/*
										 * In V2 metadata most acquisition information is stored in a separated channel node and not in an image node.
										 */
										tempNode = getChannelMasterNodeUsingXPath(metaDoc.getChildNodes(),String.valueOf(meta.getPlaneTheC(imageIndex, 0).getValue()+1));
										if(extendedLogging) {
											progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ":" + "Using channel node to check physical size X matches metadata (since metadata V" + srcMetaXMLVersion + ")!", ProgressDialog.LOG);
										}
									}
									
									/**
									 * Verify that image resolution is same in metadata
									 * <ImageResolutionX Unit="m">9.4916838247105038E-08</ImageResolutionX>
									 * <ImageResolutionY Unit="m">9.4916838247105038E-08</ImageResolutionY>
									 */
									{
										tempNode2 = getFirstNodeWithName(tempNode.getChildNodes(), "ImageResolutionX");
										
										tempUnit = getLengthUnitFromNodeAttribute(tempNode2);
										tempLength = FormatTools.createLength(Double.parseDouble(tempNode2.getTextContent()), 
												tempUnit);

										val1 = meta.getPixelsPhysicalSizeX(imageIndex).value(tempUnit).doubleValue();
										val2 = tempLength.value(tempUnit).doubleValue();
										digitsToCompare = getMinNrOfDigits(val1, val2);
										val1Str = String.format("%." + String.valueOf(digitsToCompare) + "g%n", val1);
										val2Str = String.format("%." + String.valueOf(digitsToCompare) + "g%n", val2);
										
										if(val1Str.equals(val2Str)) {
											if(extendedLogging || LOGPOSITIONCONVERSIONFORDIAGNOSIS) {
												progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ":" + "Confirmed that physical size X matches metadata!", ProgressDialog.LOG);
											}
										}else {
											progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ":" + "Physical size X in image OME metadata and original metadata disagree!"
													+ " Replaced image metadata physical X size with value from original OPERA XML metadata!"
													+ " Image OME data: " 
													+ val1
													+ " " 
													+ tempUnit.getSymbol() 
													+ " (Original entry "
													+ meta.getPixelsPhysicalSizeX(imageIndex).value().doubleValue()
													+ " "
													+ meta.getPixelsPhysicalSizeX(imageIndex).unit().getSymbol()
													+ ")"
													+ ", XML Metadata:  " 
													+ val2
													+ " " 
													+ tempUnit.getSymbol()
													+ " (Original entry "
													+ tempNode2.getTextContent()
													+ " "
													+ tempUnit.getSymbol()
													+ "). For comparison these values were converted to " + digitsToCompare + " digits as: "
													+ val1Str
													+ " and "
													+ val2Str
													+ ".", ProgressDialog.NOTIFICATION);
											meta.setPixelsPhysicalSizeX(tempLength, imageIndex);
										}
									}
									{
										tempNode2 = getFirstNodeWithName(tempNode.getChildNodes(), "ImageResolutionY");
										tempUnit = getLengthUnitFromNodeAttribute(tempNode2);
										tempLength = FormatTools.createLength(Double.parseDouble(tempNode2.getTextContent()), 
												tempUnit);

										val1 = meta.getPixelsPhysicalSizeY(imageIndex).value(tempUnit).doubleValue();
										val2 = tempLength.value(tempUnit).doubleValue();
										digitsToCompare = getMinNrOfDigits(val1, val2);
										val1Str = String.format("%." + String.valueOf(digitsToCompare) + "g%n", val1);
										val2Str = String.format("%." + String.valueOf(digitsToCompare) + "g%n", val2);
										
										if(val1Str.equals(val2Str)) {
											if(extendedLogging || LOGPOSITIONCONVERSIONFORDIAGNOSIS) {
												progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ":" + "Confirmed that physical size Y matches metadata!", ProgressDialog.LOG);
											}
										}else {
											progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ":" + "Physical size Y in image OME metadata and original metadata disagree!"
													+ " Replaced image metadata physical Y size with value from original OPERA XML metadata!"
													+ " Image OME data: " 
													+ val1
													+ " " 
													+ tempUnit.getSymbol() 
													+ " (Original entry "
													+ meta.getPixelsPhysicalSizeY(imageIndex).value().doubleValue()
													+ " "
													+ meta.getPixelsPhysicalSizeY(imageIndex).unit().getSymbol()
													+ ")"
													+ ", XML Metadata:  " 
													+ val2
													+ " " 
													+ tempUnit.getSymbol()
													+ " (Original entry "
													+ tempNode2.getTextContent()
													+ " "
													+ tempUnit.getSymbol()
													+ "). For comparison these values were converted to " + digitsToCompare + " digits as: "
													+ val1Str
													+ " and "
													+ val2Str
													+ ".", ProgressDialog.NOTIFICATION);
											meta.setPixelsPhysicalSizeY(tempLength, imageIndex);
										}
									}
								}
								
								{
									/**
									 * Verify and eventually correct the PhysicalSizeZ parameter in the ome.tif and in the image calibration
									 */
									progress.updateBarText("Verify Z position information with original OPERA index file (OPERA ID " + imageLabelOPERA + ")");
									double tempZStepSizeInMicron;
									{
										Node tempZNode;
										double tempAbsZValue;
										String previousPlaneImageID = "", currentPlaneImageID = "";
										Length previousPlaneAbsZ = null, currentPlaneAbsZ;
										Unit<Length> unitForComparingValues = UNITS.MICROMETER, currentPlaneAbsZUnit;
										
										LinkedList<Double> observedZValues = new LinkedList<Double>();
										LinkedList<Integer> observedZValueOccurences = new LinkedList<Integer>();
										
										String imageLabelOPERAStart = imageLabelOPERA.substring(0,imageLabelOPERA.lastIndexOf("P"));
										
										NodeList nl = null;
										try {
											XPathExpression expr = xp.compile("//Image[id[starts-with(text(),'" + imageLabelOPERAStart + "')]]");
											nl = (NodeList) expr.evaluate(metaDoc, XPathConstants.NODESET);
//											progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ", xpath for image nodes starting with " + imageLabelOPERAStart + " yields " + nl.getLength() + "nodes!",
//													ProgressDialog.LOG);
										} catch (XPathExpressionException e) {
											String out = "";
											for (int err = 0; err < e.getStackTrace().length; err++) {
												out += " \n " + e.getStackTrace()[err].toString();
											}
											progress.notifyMessage("Task " + (0 + 1) + "/" + tasks + ": Could not fetch image nodes beginning with id " + imageLabelOPERAStart 
													+ "\nError message: " + e.getMessage()
													+ "\nError localized message: " + e.getLocalizedMessage()
													+ "\nError cause: " + e.getCause() 
													+ "\nDetailed message:"
													+ "\n" + out,
													ProgressDialog.ERROR);
										}
										
										screening: for(int img = 0; img < nl.getLength(); img++){
											Node tempNode = nl.item(img);
											if(!tempNode.hasChildNodes()) {
//												progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ", xpath for image nodes starting with " + imageLabelOPERAStart + " yields node to be skipped:"
//														+ img,
//														ProgressDialog.LOG);
												continue;
											}
											try {
												currentPlaneImageID = getFirstNodeWithName(tempNode.getChildNodes(), "id").getTextContent();
//												progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ", xpath for image nodes starting with " + imageLabelOPERAStart + " yields node "
//														+ currentPlaneImageID,
//														ProgressDialog.LOG);												
												// Only consider first channels for extracting z information, thus skip all nodes related to other channels:
												if(!currentPlaneImageID.endsWith("R1")) continue;
												 // Skip nodes not belonging to this image:
												if(!currentPlaneImageID.startsWith(imageLabelOPERAStart)) continue;
//												if(!currentPlaneImageID.substring(0,currentPlaneImageID.lastIndexOf("P")).equals(imageLabelOPERAStart)) continue;
												
												tempZNode = getFirstNodeWithName(tempNode.getChildNodes(), "AbsPositionZ");
												currentPlaneAbsZUnit = getLengthUnitFromNodeAttribute(tempZNode);
												currentPlaneAbsZ = new Length (Double.parseDouble(tempZNode.getTextContent()), currentPlaneAbsZUnit);
												
												if(previousPlaneImageID.equals("")) {
													previousPlaneImageID = currentPlaneImageID;
													previousPlaneAbsZ = currentPlaneAbsZ;
													continue screening;
												}else if(Integer.parseInt(currentPlaneImageID.substring(currentPlaneImageID.lastIndexOf("P")+1,currentPlaneImageID.lastIndexOf("R"))) == Integer.parseInt(previousPlaneImageID.substring(previousPlaneImageID.lastIndexOf("P")+1,previousPlaneImageID.lastIndexOf("R")))+1) {
													//This condition is met only if the plane is a follow up plane of the previous stored plane
													//Now we verify that the remaining identifiers match and if so we create a z value
													if(currentPlaneImageID.substring(0,currentPlaneImageID.lastIndexOf("P")).equals(previousPlaneImageID.substring(0,previousPlaneImageID.lastIndexOf("P")))) {
														tempAbsZValue = currentPlaneAbsZ.value(unitForComparingValues).doubleValue() - previousPlaneAbsZ.value(unitForComparingValues).doubleValue();
														tempAbsZValue = Double.parseDouble(String.format("%." + String.valueOf(2) + "g%n", tempAbsZValue));
														if(tempAbsZValue < 0) tempAbsZValue *= -1.0;
														for(int zV = 0; zV < observedZValues.size();zV++) {
															if(observedZValues.get(zV) == tempAbsZValue) {
																observedZValueOccurences.set(zV,observedZValueOccurences.get(zV)+1);
																previousPlaneImageID = currentPlaneImageID;
																previousPlaneAbsZ = currentPlaneAbsZ;
																continue screening;
															}
														}
														observedZValues.add(tempAbsZValue);
														observedZValueOccurences.add(1);
														
														if(extendedLogging || LOGZDISTFINDING) {
															progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ", image with ID "
																	+ imageLabelOPERA
																	+ ": Added a calculated ZValue (" + observedZValues
																	+ ") based on comparing the AbsPositionZ of "
																	+ previousPlaneImageID
																	+ " (Z value of Length object: "
																	+ previousPlaneAbsZ.value(unitForComparingValues)
																	+ " " 
																	+ unitForComparingValues.getSymbol()
																	+ ") and "
																	+ currentPlaneImageID
																	+ "(Z text: "
																	+ tempZNode.getTextContent()
																	+ " " 
																	+ currentPlaneAbsZUnit.getSymbol()
																	+ "). "
																	,
																	ProgressDialog.LOG);
														}
														previousPlaneImageID = currentPlaneImageID;
														previousPlaneAbsZ = currentPlaneAbsZ;
														continue screening;
													}else {
														previousPlaneImageID = currentPlaneImageID;
														previousPlaneAbsZ = currentPlaneAbsZ;
														continue screening;
													}
												}else {
													previousPlaneImageID = currentPlaneImageID;
													previousPlaneAbsZ = currentPlaneAbsZ;
													continue screening;
												}							
											}catch(Exception e) {
												String out = "";
												for (int err = 0; err < e.getStackTrace().length; err++) {
													out += " \n " + e.getStackTrace()[err].toString();
												}
												progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ", image with ID "
														+ imageLabelOPERA
														+ ": Could not find Z information in image node " 
														+ img 
														+ " to determine Z information for the images starting with label"
														+ imageLabelOPERA.substring(0,imageLabelOPERA.lastIndexOf("P"))
														+ ". Node name: "
														+ tempNode.getNodeName()
														+ ". Node value: "
														+ tempNode.getNodeValue()
														+ ". Error " + e.getCause() + " - Detailed message:\n" + out,
														ProgressDialog.ERROR);
												continue screening;
											}						
										}
										
										/**
										 * Checking the determined Z values
										 */
										tempZStepSizeInMicron = 0.0;
										if(observedZValues.size() > 1) {
											int tempCt = 0;
											for(int i = 0; i < observedZValues.size(); i++) {
												if(observedZValueOccurences.get(i) > tempCt) {
													tempCt = observedZValueOccurences.get(i);
													tempZStepSizeInMicron = observedZValues.get(i);
												}
											}
											if(!noWarningsForDiverseZ) {
												progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ", image with ID "
														+ imageLabelOPERA
														+ ": There are images with different Z spacings available in this OPERA output file for all images starting with " 
														+ imageLabelOPERA.substring(0,imageLabelOPERA.lastIndexOf("P"))
														+ ". The following z steps were observed: "
														+ observedZValues 
														+ " with the observed frequencies of " 
														+ observedZValueOccurences 
														+ "."
														+ "This program cannot guarantee accurate translation of Z step size information into the OME 'PixelsPhysicalSizeZ' parameter stored in the OME XML Metadata for this file."
														+ "This program will save the most frequent observed Z step size ("
														+ tempZStepSizeInMicron
														+ " micron) as Physical Size Z.",
														ProgressDialog.NOTIFICATION);
											}											
										}else if (observedZValues.size() == 1){
											tempZStepSizeInMicron = observedZValues.get(0);
											observedZValues = null;
											observedZValueOccurences = null;
											if(extendedLogging || LOGZDISTFINDING) {
												progress.notifyMessage("Task " + (task + 1) + "/" + tasks 
														+ ", image with ID "
														+ imageLabelOPERA
														+ ": Found Z step size in metadata to compare to the OME 'PixelsPhysicalSizeZ' parameter in OME metadata - value " + tempZStepSizeInMicron + ".",
														ProgressDialog.LOG);
											}
										}else {
											tempZStepSizeInMicron = 0.0;
											observedZValues = null;
											observedZValueOccurences = null;
											if(extendedLogging || LOGZDISTFINDING) {
												progress.notifyMessage("Task " + (task + 1) + "/" + tasks 
														+ ", image with ID "
														+ imageLabelOPERA
														+ ": Found no Z steps in metadata to compare to the OME 'PixelsPhysicalSizeZ' parameter in OME metadata - will compare to value " + tempZStepSizeInMicron + ".",
														ProgressDialog.LOG);
											}
										}
										
										if(meta.getPixelsPhysicalSizeZ(imageIndex).value(unitForComparingValues).doubleValue() != tempZStepSizeInMicron) {
											if(!noWarningsForXYZConflicts) {
												progress.notifyMessage("Task " + (task + 1) + "/" + tasks 
														+ ", image with ID "
														+ imageLabelOPERA
														+ ": OME 'PixelsPhysicalSizeZ' parameter in OME metadata ("
														+ meta.getPixelsPhysicalSizeZ(imageIndex).value(unitForComparingValues).doubleValue()
														+ " "
														+ UNITS.MICROMETER.getSymbol()
														+ ") was different compared to what this program read from the OPERA XML metadata ("
														+ tempZStepSizeInMicron 
														+ " " 
														+ UNITS.MICROMETER.getSymbol()
														+ "). Replacing the 'PixelsPhysicalSizeZ' value in OME metadata with " 
														+ tempZStepSizeInMicron 
														+ " " 
														+ UNITS.MICROMETER.getSymbol()+ ".",
														ProgressDialog.LOG);	
											}											
											//Since we store the PhysicalSize parameters generally in meter, convert to meter here, too:
											meta.setPixelsPhysicalSizeZ(new Length(tempZStepSizeInMicron / 1000.0 / 1000.0,UNITS.METER), imageIndex);
										}else if(!meta.getPixelsPhysicalSizeZ(imageIndex).unit().equals(UNITS.METER)) {
											meta.setPixelsPhysicalSizeZ(new Length(meta.getPixelsPhysicalSizeZ(imageIndex).value(UNITS.METER),UNITS.METER), imageIndex);
											if(extendedLogging || LOGZDISTFINDING) {
												progress.notifyMessage("Task " + (task + 1) + "/" + tasks 
														+ ", image with ID "
														+ imageLabelOPERA
														+ ": Found unit for the OME value 'PixelsPhysicalSizeZ' to be other than meter - converted the value to METER: " 
														+ meta.getPixelsPhysicalSizeZ(imageIndex) 
														+ ".",
														ProgressDialog.LOG);
											}
										}
									}
								}
								
								progress.updateBarText("Transfer missing metadata from original OPERA index file to OME metadata (OPERA ID " + imageLabelOPERA + ")");
								{
									//Find node with information about the acquisition
									Node srcNode;
									if(srcMetaXMLVersion.equals("1")){
										srcNode = imageNode;
									}else {
										/*
										 * In V2 metadata most acquisition information is stored in a separated channel node and not in an image node.
										 */
										srcNode = getChannelMasterNodeUsingXPath(metaDoc.getChildNodes(),String.valueOf(meta.getPlaneTheC(imageIndex, 0).getValue()+1));
									}
									
									/**
									 * Generate instrument in metadata, use following information
									 * <AcquisitionType>NipkowConfocal</AcquisitionType>
									 * */
									meta.setInstrumentID("Instrument:0", 0);
									meta.setImageInstrumentRef("Instrument:0", imageIndex);
									meta.setMicroscopeType(MicroscopeType.fromString("Other"), 0);
									{
										String model = getFirstNodeWithName(srcNode.getChildNodes(), "AcquisitionType").getTextContent(); // "NipkowConfocal"
										meta.setMicroscopeModel(model, 0);
										if(extendedLogging) {
											progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ":" + "Transfered microscope model (Original: " 
													+ model
													+ ") and stored as "
													+ meta.getMicroscopeModel(0)
													+ ".", ProgressDialog.LOG);
										}
									}
									{
										/**
										 *  Transferring the Microscope Serial Number
										 *  OPERA files have no serial number and thus, we are using the InstrumentType as Serial Number
										 *  <InstrumentType>Phenix</InstrumentType>
										 *  <InstrumentType>Sonata</InstrumentType>
										 */
										meta.setMicroscopeSerialNumber(metaDoc.getElementsByTagName("InstrumentType").item(0).getTextContent(), 0);
										if(selectedImageType.equals(imageType[0])) { // Input as OPERA Phenix has been selected, Revvity is the manufacturer of Opera Phenix
											meta.setMicroscopeManufacturer("Revvity, Inc.", 0);
										}								
									}
									
									
									/**
									 * Fetch objective stats from specifications
									 * <ObjectiveNA Unit="">1.15</ObjectiveNA>	
									 * <ObjectiveMagnification Unit="">63</ObjectiveMagnification>
									 */
									String objectiveID = "Objective:0";
									meta.setObjectiveID(objectiveID, 0, 0);
									// Now assign the objective to the image:
									meta.setObjectiveSettingsID(objectiveID, imageIndex);
									// From here one extend the settings for the objective
									meta.setObjectiveLensNA(Double.parseDouble(getFirstNodeWithName(srcNode.getChildNodes(), "ObjectiveNA").getTextContent()),
											0,0);
									if(extendedLogging) {
										progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ":" + "Transfered objective NA (Original: " 
												+ getFirstNodeWithName(srcNode.getChildNodes(), "ObjectiveNA").getTextContent() 
												+ ") and stored as "
												+ meta.getObjectiveLensNA(0, 0)
												+ ".", ProgressDialog.LOG);
									}
									
									meta.setObjectiveNominalMagnification(Double.parseDouble(getFirstNodeWithName(srcNode.getChildNodes(), "ObjectiveMagnification").getTextContent()),
											0, 0);
									if(extendedLogging) {
										progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ":" + "Transfered objective Nominal Magnification (Original: " 
												+ getFirstNodeWithName(srcNode.getChildNodes(), "ObjectiveMagnification").getTextContent() 
												+ ") and stored as "
												+ meta.getObjectiveNominalMagnification(0, 0)
												+ ".", ProgressDialog.LOG);
									}
								}							
								
								/** Find out the well sample time from time stamps for individual planes
								 *	<AbsTime>2023-09-14T14:50:58.43+02:00</AbsTime>
								 */								
								ome.xml.model.primitives.Timestamp start = null, end = null;									
								for(int p = 0; p < meta.getPlaneCount(imageIndex); p++) {
									String OPERAString = seriesName[task];
									OPERAString += "P" + String.valueOf(meta.getPlaneTheZ(imageIndex, p).getValue()+1);
									OPERAString += "R" + String.valueOf(meta.getPlaneTheC(imageIndex, p).getValue()+1);
									
									Node planeImageNode = null;									
									try {
										planeImageNode = getImageNodeWithID_OPERAMETADATA_UsingXPath(imagesNode.getChildNodes(), OPERAString);
									}catch(java.lang.NullPointerException e) {
										progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ", plane " + (p+1) + ":" + "Searching node " 
												+ OPERAString
												+ " in OPERA metadata xml failed. Processing of this image skipped. Output data will be missing a lot of metadata information." 
												+ "", ProgressDialog.ERROR);
										continue;
									}
									
									Node tempNode = this.getFirstNodeWithName(planeImageNode.getChildNodes(), "AbsTime");
									if(extendedLogging){
										progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ", plane " + (p+1) + ":" +  "Screening planes for acquisition times. Image  " 
												+ OPERAString
												+ " has time " 
												+ tempNode.getTextContent()
												+ "", ProgressDialog.LOG);
									}
									
									ome.xml.model.primitives.Timestamp pTime = ome.xml.model.primitives.Timestamp.valueOf(tempNode.getTextContent());
									if(extendedLogging){
										progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ", plane " + (p+1) + ", " + OPERAString + ":" + "Screening planes for acquisition times. Time converted to  " 
												+ pTime.getValue()
												+ "", ProgressDialog.LOG);
									}
									
									if(p == 0){
										start = pTime;
										end = pTime;
									}else{
										if(pTime.asDateTime(DateTimeZone.UTC).isBefore(start.asDateTime(DateTimeZone.UTC))) {
											start = pTime;
										}
										if(pTime.asDateTime(DateTimeZone.UTC).isAfter(start.asDateTime(DateTimeZone.UTC))) {
											end = pTime;
										}
									}
									if(extendedLogging){
										progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ", plane " + (p+1) + ", " + OPERAString + ":" 
												+ "Screening planes for acquisition times. Currently found times are for Start " 
												+ start.asDateTime(DateTimeZone.UTC).toString()
												+ " and for End " 
												+ end.asDateTime(DateTimeZone.UTC).toString()
												+ "!", ProgressDialog.LOG);
									}
									
									
									/**
									 * Add metadata from planes that are out of format
									 * */
									
									try {
										service.populateOriginalMetadata(meta, "Plane " + (p+1) + "|" + OPERAString + "|Originalid" , OPERAString);
										service.populateOriginalMetadata(meta, "Plane " + (p+1) + "|" + OPERAString + "|OriginalURL" , getFirstNodeWithName(planeImageNode.getChildNodes(), "URL").getTextContent());
										service.populateOriginalMetadata(meta, "Plane " + (p+1) + "|" + OPERAString + "|OriginalAbsTime" , getFirstNodeWithName(planeImageNode.getChildNodes(), "AbsTime").getTextContent());
										service.populateOriginalMetadata(meta, "Plane " + (p+1) + "|" + OPERAString + "|OriginalOrientationMatrix" , getFirstNodeWithName(planeImageNode.getChildNodes(), "OrientationMatrix").getTextContent());									
									}catch(Exception e) {
										if(!noWarningsForMissingCustomMetadata) {
											progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ", plane " + (p+1) + ", " + OPERAString + ":" 
													+ "WARNING: Storing one or all custom plane metadata information (Originalid, URL, AbsTime, and/or OrientationMatrix) failed...",
													ProgressDialog.ERROR);
										}
									}									
								}
								meta.setWellSampleTimepoint(start, plateIndex, wellIndex, wellSampleIndex);
								if(extendedLogging){
									progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ":" + "Found start and end of acuqisition time points. Start " 
											+ start.asDateTime(DateTimeZone.UTC).toString()
											+ " end " 
											+ end.asDateTime(DateTimeZone.UTC).toString()
											+ " is saved as "
											+ meta.getWellSampleTimepoint(plateIndex, wellIndex, wellSampleIndex)
											+ "", ProgressDialog.LOG);
								}
								
								progress.updateBarText("Transfer channel metadata from original OPERA index file to OME metadata (OPERA ID " + imageLabelOPERA + ")");
								/**
								 * Set channel information
								 * */
								for(int channelId = 0; channelId < nChannels; channelId++) {
									Node channelInfoNode;
									if(srcMetaXMLVersion.equals("1")){
										channelInfoNode = getImageNodeWithID_OPERAMETADATA_UsingXPath(imagesNode.getChildNodes(),
												imageLabelOPERA.substring(0,imageLabelOPERA.length()-1)+String.valueOf(channelId+1));
										if(channelInfoNode==null) {
											progress.notifyMessage("Task " + (task + 1) + "/" + tasks 
													+ ", channel" + (channelId+1) 
													+ ":" + " Could not find the node with id " 
													+ imageLabelOPERA.substring(0,imageLabelOPERA.length()-1)+String.valueOf(channelId+1)
													+ " in image nodes in OPERA metadata xml.", ProgressDialog.LOG);
										}else if(extendedLogging) {
											progress.notifyMessage("Task " + (task + 1) + "/" + tasks 
													+ ", channel" + (channelId+1) 
													+ ":" + " Found the node with id " 
													+ imageLabelOPERA.substring(0,imageLabelOPERA.length()-1)+String.valueOf(channelId+1)
													+ " in image nodes in OPERA metadata xml.", ProgressDialog.LOG);
										}
									}else {
										/*
										 * In V2 metadata most acquisition information is stored in a separated channel node and not in an image node.
										 */
										channelInfoNode = getChannelMasterNodeUsingXPath(metaDoc.getChildNodes(),String.valueOf(channelId+1));
										if(channelInfoNode==null) {
											progress.notifyMessage("Task " + (task + 1) + "/" + tasks 
													+ ", channel" + (channelId+1) 
													+ ":" + " Could not find the channel node in the OPERA metadata xml.", ProgressDialog.LOG);
										}else if(extendedLogging) {
											progress.notifyMessage("Task " + (task + 1) + "/" + tasks 
													+ ", channel" + (channelId+1) 
													+ ":" + " Found the channel node in OPERA metadata xml.", ProgressDialog.LOG);
										}
									}
									
									/**
									 * Add detector or find detector
									 * <CameraType>AndorZylaCam</CameraType>
									 */
									int detectorId = meta.getDetectorCount(0);
									{
										Node tempNode = getFirstNodeWithName(channelInfoNode.getChildNodes(), "CameraType");
										for(int dt = 0; dt < meta.getDetectorCount(0); dt++) {
											if(meta.getDetectorModel(0, dt).equals(tempNode.getTextContent())) {
												detectorId = dt;
												break;
											}
										}	
										if(detectorId == meta.getDetectorCount(0)) {
											//In this case the detector has not been found in list and we need to add it
											meta.setDetectorModel(tempNode.getTextContent(), 0, detectorId);
											if(extendedLogging){
												progress.notifyMessage("Task " + (task + 1) + "/" + tasks 
														+ ", channel" + (channelId+1) 
														+ ": " 
														+ "Added detector with id " 
														+ detectorId
														+ " to metadata object. Named the detector " 
														+ tempNode.getTextContent()
														+ " which was saved as "
														+ meta.getDetectorModel(0, detectorId)
														+ ".", ProgressDialog.LOG);
											}
										}
										
										// Now we check whether the detector has an ID setting and if not we create a new ID for it
										try {
											if(meta.getDetectorID(0, detectorId).equals(null) || meta.getDetectorID(0, detectorId).equals("")) {
												meta.setDetectorID("Detector:"+detectorId, 0, detectorId);
											}
										}catch(NullPointerException e) {
											meta.setDetectorID("Detector:"+detectorId, 0, detectorId);
										}
										
										// We register the found detector also in the channel settings
										meta.setDetectorSettingsID(meta.getDetectorID(0, detectorId), imageIndex, channelId);
										
										// We can assign the detector type for some known detector models
										if(meta.getDetectorModel(0, detectorId).equals("AndorZylaCam")) {
											meta.setDetectorType(DetectorType.fromString("CMOS"), 0, detectorId);
										}
									}
									
									/**
									 * 	Set excitation wavelength based on xml entry
									 * 	<MainExcitationWavelength Unit="nm">640</MainExcitationWavelength>
									 */
									{
										Node tempNode = this.getFirstNodeWithName(channelInfoNode.getChildNodes(), "MainExcitationWavelength");
										meta.setChannelExcitationWavelength(FormatTools.createLength(Double.parseDouble(tempNode.getTextContent()),
												this.getLengthUnitFromNodeAttribute(tempNode)),
												imageIndex, channelId);
										if(extendedLogging) {
											progress.notifyMessage("Task " + (task + 1) + "/" + tasks 
													+ ", channel" + (channelId+1) 
													+ ": " 
													+ "Set excitation wavelength to " 
													+ meta.getChannelExcitationWavelength(imageIndex, channelId).value().doubleValue()
													+ " " + meta.getChannelExcitationWavelength(imageIndex, channelId).unit().getSymbol()
													+ "(Original entry " + tempNode.getTextContent() + " "
													+ tempNode.getAttributes().getNamedItem("Unit").getNodeValue() 
													+ ")", ProgressDialog.LOG);
										}										
									}
									

									/**
									 * 	Set emission wavelength based on xml entry
									 * 	<MainEmissionWavelength Unit="nm">706</MainEmissionWavelength>
									 */
									{
										Node tempNode = this.getFirstNodeWithName(channelInfoNode.getChildNodes(), "MainEmissionWavelength");
										meta.setChannelEmissionWavelength(FormatTools.createLength(Double.parseDouble(tempNode.getTextContent()),
												this.getLengthUnitFromNodeAttribute(tempNode)),
												imageIndex, channelId);
										if(extendedLogging) {
											progress.notifyMessage("Task " + (task + 1) + "/" + tasks 
													+ ", channel" + (channelId+1) 
													+ ": " 
													+ "Set emission wavelength to " 
													+ meta.getChannelEmissionWavelength(imageIndex, channelId).value().doubleValue()
													+ " " + meta.getChannelEmissionWavelength(imageIndex, channelId).unit().getSymbol()
													+ "(Original entry " + tempNode.getTextContent() + " "
													+ tempNode.getAttributes().getNamedItem("Unit").getNodeValue() 
													+ ")", ProgressDialog.LOG);
										}										
									}
									
									
									/**
									 * 	Transfer binning
									 * 	<BinningX>1</BinningX>
									 * 	<BinningY>1</BinningY>			
									*/
									{
										String binString = getFirstNodeWithName(channelInfoNode.getChildNodes(), "BinningX").getTextContent()
												+ "x" + getFirstNodeWithName(channelInfoNode.getChildNodes(), "BinningY").getTextContent();
										meta.setDetectorSettingsBinning(Binning.fromString(binString), imageIndex, channelId);
										if(extendedLogging) {
											progress.notifyMessage("Task " + (task + 1) + "/" + tasks 
													+ ", channel" + (channelId+1) 
													+ ": " 
													+ "Added binning for channel " + channelId + " :" 
													+ meta.getDetectorSettingsBinning(imageIndex, channelId)
													+ "(Original entry " + binString + ")", ProgressDialog.LOG);
										}							
									}
									
									/**
									 * Convert illumination type
									 * <IlluminationType>Epifluorescence</IlluminationType>
									 * <ChannelType>Fluorescence</ChannelType>
									 */										
									try {
										meta.setChannelIlluminationType(IlluminationType.fromString(getFirstNodeWithName(channelInfoNode.getChildNodes(), "IlluminationType").getTextContent()),
												imageIndex, channelId);
										if(extendedLogging) {
											progress.notifyMessage("Task " + (task + 1) + "/" + tasks 
													+ ", channel" + (channelId+1) 
													+ ": " 
													+ "Added illumination type for channel " + channelId + " :" 
													+ meta.getChannelIlluminationType(imageIndex, channelId)
													+ "(Original entry " + getFirstNodeWithName(channelInfoNode.getChildNodes(), "IlluminationType").getTextContent() + ")", ProgressDialog.LOG);
										}
									}catch(EnumerationException en) {
										progress.notifyMessage("Task " + (task + 1) + "/" + tasks 
												+ ", channel" + (channelId+1) 
												+ ": " 
												+ "IlluminationType could not be translated to OME xml. Thus IlluminationType in OME xml was set to 'Other'."
												+ "(Original Illumination type: " + getFirstNodeWithName(channelInfoNode.getChildNodes(), "IlluminationType").getTextContent() + ")",
												ProgressDialog.NOTIFICATION);
										meta.setChannelIlluminationType(IlluminationType.fromString("Other"),
												imageIndex, channelId);
									}
																			
									/**
									 *	Fetch exposure time and unit and write for the channel planes
									 * 	<ExposureTime Unit="s">0.2</ExposureTime>ExposureTime
									 */
									{
										Node tempNode = getFirstNodeWithName(channelInfoNode.getChildNodes(), "ExposureTime");
										for(int p = 0; p < meta.getPlaneCount(imageIndex); p++) {
											if(meta.getPlaneTheC(imageIndex, p).getNumberValue().intValue()==channelId) {
												meta.setPlaneExposureTime(FormatTools.createTime(Double.parseDouble(tempNode.getTextContent()), 
														this.getTimeUnitFromNodeAttribute(tempNode)),
														imageIndex, p);
												if(extendedLogging) {
													progress.notifyMessage("Task " + (task + 1) + "/" + tasks 
															+ ", channel" + (channelId+1) 
															+ ", plane " + p + ": "
															+ "Set exposure time to " + meta.getPlaneExposureTime(imageIndex, p).value().doubleValue()
															+ " " + meta.getPlaneExposureTime(imageIndex, p).unit().getSymbol() 
															+ "(Original entry " + tempNode.getTextContent() + " "
															+ tempNode.getAttributes().getNamedItem("Unit").getNodeValue() 
															+ ")", ProgressDialog.LOG);
												}
											}
										}
									}
									
									if(srcMetaXMLVersion.equals("2")){						
										/*
										 *  V2 has some special metadata for each channel to be added,
										 *  e.g., a Flatfield profile which could be populated to original metadata
										 */
										String temp = getFlatfieldProfileUsingXPath(metaDoc.getChildNodes(),String.valueOf(channelId+1));
										if(!temp.equals("none")) {
											service.populateOriginalMetadata(meta, "Channel " + String.valueOf(channelId+1) + "|FlatfieldProfile", temp);
										}
										
										String [] originalMetadataTypes = new String [] {"ImageType", "ExcitationPower", "OrientationMatrix", "CropArea"};
										
										for(String oMType : originalMetadataTypes) {
											try {
												Node tempNode = getFirstNodeWithName(channelInfoNode.getChildNodes(), oMType);												
												if(tempNode.hasAttributes()) {
													String collectedAtt = "";
													for(int att = 0; att < tempNode.getAttributes().getLength(); att++) {
														collectedAtt += tempNode.getAttributes().item(att).getNodeName() + "=" + tempNode.getAttributes().item(0).getNodeValue() + ";";
													}													
													collectedAtt = collectedAtt.substring(0,collectedAtt.length()-1);
													
													service.populateOriginalMetadata(meta, "Channel " + String.valueOf(channelId+1) + "|" + oMType, 
															tempNode.getTextContent() + "|Attributes:" + collectedAtt);
												}else {
													service.populateOriginalMetadata(meta, "Channel " + String.valueOf(channelId+1) + "|" + oMType, 
															tempNode.getTextContent());
												}
											}catch(Exception e) {
												if(!noWarningsForMissingCustomMetadata) {
													progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ", channel " + (channelId+1) + ":" 
															+ "WARNING: Storing " + oMType + " as original metadata failed...",
															ProgressDialog.NOTIFICATION);
												}
											}
											
										}
										
										
										
									}
								}
									
								progress.updateBarText("Find out acquisition date from original OPERA index file to OME metadata (OPERA ID " + imageLabelOPERA + ")");
								/**
								 * Get acquisition date
								 */
								String dateString = "unknownDate";
								try {
									dateString = meta.getImageAcquisitionDate(imageIndex).getValue();
								}catch(Exception e){
									try {
										meta.setImageAcquisitionDate(meta.getWellSampleTimepoint(plateIndex, wellIndex, wellSampleIndex), imageIndex);
										if(extendedLogging) progress.notifyMessage("Task " + (1+task) + ": Could not detect image acquisition timestamp for " + outFilename + ", Z" + imageZ + ". "
												+ "Thus, wrote first time point of individual stack image acquisitions (also set as WellSampleTimepoint as image acquisition date: "
												+ meta.getWellSampleTimepoint(plateIndex, wellIndex, wellSampleIndex), ProgressDialog.NOTIFICATION);
										dateString = meta.getImageAcquisitionDate(imageIndex).getValue();
									}catch(Exception e2){
										progress.notifyMessage("Task " + (1+task) + ": Error. Could not detect or find an image acquisition date for " + outFilename + ", Z" + imageZ + ". "
												+ "This will be missing in the metadata of the generated image!", ProgressDialog.NOTIFICATION);
									}
									
								}
								dateString = dateString.replace("-", "");
								dateString = dateString.replace(":", "");
								dateString = dateString.replace(".", "_");
								dateString = dateString.replace("T", "_");
								

								progress.updateBarText("Create folder structure for saving the image (if necessary) (OPERA ID " + imageLabelOPERA + ")");
								/**
								 * Create folder structure and file names for saving
								 * */
								// Generate a directory for each well, where images shall be saved						
								String wellString = "";
								try{
									wellString = this.rowNumberToLetter(wellRow+1) + (wellColumn+1); 
								}catch(IndexOutOfBoundsException e) {
									wellString = "R" + (wellRow+1) + "C" + (wellColumn+1) + System.getProperty("file.separator"); 
									String out = "";
									for (int err = 0; err < e.getStackTrace().length; err++) {
										out += " \n " + e.getStackTrace()[err].toString();
									}
									progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ": " 
											+ "Error when converting row number to alphabetical for output file name. Created coordinate-based name instead." 
											+ "\nError message: " + e.getMessage()
											+ "\nError localized message: " + e.getLocalizedMessage()
											+ "\nError cause: " + e.getCause() 
											+ "\nDetailed message:"
											+ "\n" + out,
											ProgressDialog.NOTIFICATION);
								}
//								
								String wellDir = outPath + System.getProperty("file.separator") + wellString + System.getProperty("file.separator");									
								File wellDirFile = new File(wellDir);
								if(wellDirFile.exists()) {				
									if(extendedLogging)	progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ": " 
											+ "Directory for the well already existed: " + wellDirFile.getAbsolutePath(), ProgressDialog.LOG);
								}else {
									if(extendedLogging)	progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ": " 
											+ "Creating directory for the well to save files: " + wellDirFile.getAbsolutePath(), ProgressDialog.LOG);
									wellDirFile.mkdir();
								}
								
								// Create a unique folder for each image or field of view
								String savingDir = wellDir + System.getProperty("file.separator") + outFilename + "_" + wellString + "_" + dateString;
								if(selectedOutputType.equals(outputType [0])) {
									// LIMS compatible output file structures = folder by focal plane
									if(imageZ >= 10) {
										savingDir += "_Z" + String.valueOf(imageZ);
									}else {
										savingDir += "_Z0" + String.valueOf(imageZ);
									}
								}else if(selectedOutputType.equals(outputType [1])) {
									// More OME Tif compatible output file structures = folder by field of view, all focal planes in one folder
									// No Z info added to folder name									
								}else if(selectedOutputType.equals(outputType [2])) {
									// Memento compatible. Create a subfolder for the z plane.
									if(imageZ >= 10) {
										savingDir += System.getProperty("file.separator") + "z" + String.valueOf(imageZ);
									}else {
										savingDir += System.getProperty("file.separator") + "z0" + String.valueOf(imageZ);
									}
								}
								
								File savingDirFile = new File(savingDir);
								if(savingDirFile.exists()) {				
									if(extendedLogging)	progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ": " 
											+ "Directory for the image already existed: " + savingDirFile.getAbsolutePath(), ProgressDialog.LOG);
								}else {
									if(extendedLogging)	progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ": " 
											+ "Creating directory for the image to save files: " + savingDirFile.getAbsolutePath(), ProgressDialog.LOG);
									savingDirFile.mkdir();
								}

								// Create filename for the image	
								String outImageName = getOutputImageName(outFilename, wellString, dateString, imageZ, imageC);
								if(extendedLogging)	progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ": " 
										+ "Computed file name: " + outImageName, ProgressDialog.LOG);

								progress.updateBarText("Finishing up corrected OME Tiff comment (OPERA ID " + imageLabelOPERA + ")");
								/**
								 * Replace the FileName attribute in the UUID under TiffData in the OME XML with names following the nametypes applied here
								 * */		
								{
									String tempName;
									int tempZ, tempC, tempT;
									for(int td = 0; td < meta.getTiffDataCount(imageIndex); td++) {										
										tempC = meta.getTiffDataFirstC(imageIndex, td).getValue();
										tempT = meta.getTiffDataFirstT(imageIndex, td).getValue();
										tempZ = meta.getTiffDataFirstZ(imageIndex, td).getValue();
																				
										tempName = getOutputImageName(outFilename, wellString, dateString, tempZ, tempC);
										
										meta.setUUIDFileName(tempName, imageIndex, td);
										
										if(extendedLogging) progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ", Image " + metadataFilePath 
												+ " , Renaming [" 
												+ (outFilename + "_"+zCode+"_C"+(channel)+".ome.tif") 
												+ "] to ["
												+ meta.getUUIDFileName(imageIndex, td)
												+ "]."
												, ProgressDialog.LOG);
									}
								}
																
								/**
								 * Inject notes into the image description that allow to trace back to where the image came from
								 */
								{
									String imgDescription = "";
//									imgDescription += "ImageCoordinates: x="
//											+ "" 
//											+ ", y="
//											+ ""
//											+ ", z="
//											+ "";
									imgDescription += "This OME Metadatafile was enriched based on an Index.idx.xml metadata file, with the help of the FIJI plugin '" + PLUGINNAME 
											+ "' (Version: " + PLUGINVERSION 
											+ ", more information at " 
											+ "https://github.com/CellProfiling/HPA_Convert_OPERA_To_LIMS-OMETIF)";
									imgDescription += ";" + "Filename after output from plugin: '" + outImageName + "'";
									meta.setImageDescription(imgDescription + ".", imageIndex);

								}
								
								
								/**
								 * Retrieve new comment
								 * */
								comment = service.getOMEXML(meta);								
								if(logWholeOMEXMLComments) {
									progress.notifyMessage("Comment after adjustments:", ProgressDialog.LOG);
									progress.notifyMessage(comment, ProgressDialog.LOG);
									
								}
								
								progress.updateBarText("Copying file to new folder system (OPERA ID " + imageLabelOPERA + ")");
								/**
								 * Copy the file to the new folder.
								 * */								
								{
									srcFile = new File(omeTifFileName);
									destFile = new File(savingDir + System.getProperty("file.separator") + outImageName);
									if(destFile.exists()) {
										progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ": " 
												+ "There are identical images in the target folder. Did not overwrite the image!!" + wellDirFile.getAbsolutePath(), ProgressDialog.ERROR);
										continue;
									}
									FileUtils.copyFile(srcFile, destFile, true);
								}
								
								/**
								 * Copying metadata file
								 */	
								FileUtils.copyFile(newTemporaryMetadataFile,
									new File(savingDir + System.getProperty("file.separator") + "metadata" + System.getProperty("file.separator") + "image.ome.xml"), 
									true);							
										
								/**
								 * Saving modified omexml tif comment into copied image
								 * */
								progress.updateBarText("Saving modified tif comment into copied file " + outImageName + ")");
								TiffSaver saver = new TiffSaver(savingDir + System.getProperty("file.separator") + outImageName);
							    RandomAccessInputStream in = new RandomAccessInputStream(savingDir + System.getProperty("file.separator") + outImageName);
							    try {
									saver.overwriteComment(in, comment);
								} catch (FormatException e) {
									String out = "";
									for (int err = 0; err < e.getStackTrace().length; err++) {
										out += " \n " + e.getStackTrace()[err].toString();
									}
									progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ": Error when trying to write the tiff comment. " 
											+ "\nError message: " + e.getMessage()
											+ "\nError localized message: " + e.getLocalizedMessage()
											+ "\nError cause: " + e.getCause() 
											+ "\nDetailed message:"
											+ "\n" + out,
											ProgressDialog.ERROR);
								}
							    in.close();
							    saver.close();
							    
								progress.updateBarText("Saving " + savingDir + System.getProperty("file.separator") + outImageName + " done!");
								if(extendedLogging)	progress.notifyMessage("Saved " + savingDir + System.getProperty("file.separator") + outImageName, ProgressDialog.LOG);
								
								//Closing all OME services

								factory = null;
								service = null;
								meta = null;
								
							} catch (IOException e) {
								String out = "";
								for (int err = 0; err < e.getStackTrace().length; err++) {
									out += " \n " + e.getStackTrace()[err].toString();
								}
								progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ": IO Error when processing the file " + omeTifFileName 
										+ "\nError message: " + e.getMessage()
										+ "\nError localized message: " + e.getLocalizedMessage()
										+ "\nError cause: " + e.getCause() 
										+ "\nDetailed message:"
										+ "\n" + out,
										ProgressDialog.ERROR);
							} catch (DependencyException e) {
								String out = "";
								for (int err = 0; err < e.getStackTrace().length; err++) {
									out += " \n " + e.getStackTrace()[err].toString();
								}
								progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ": Error when trying to generate OMEXMLService for file " + omeTifFileName 
										+ "\nError message: " + e.getMessage()
										+ "\nError localized message: " + e.getLocalizedMessage()
										+ "\nError cause: " + e.getCause() 
										+ "\nDetailed message:"
										+ "\n" + out,
										ProgressDialog.ERROR);
							} catch (ServiceException e) {
								String out = "";
								for (int err = 0; err < e.getStackTrace().length; err++) {
									out += " \n " + e.getStackTrace()[err].toString();
								}
								progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ": Error when creating OME XML Metadata object for file " + omeTifFileName 
										+ "\nError message: " + e.getMessage()
										+ "\nError localized message: " + e.getLocalizedMessage()
										+ "\nError cause: " + e.getCause() 
										+ "\nDetailed message:"
										+ "\n" + out,
										ProgressDialog.ERROR);
							} catch (EnumerationException e) {
								String out = "";
								for (int err = 0; err < e.getStackTrace().length; err++) {
									out += " \n " + e.getStackTrace()[err].toString();
								}
								progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ": Error when creating file " + omeTifFileName 
										+ "\nError message: " + e.getMessage()
										+ "\nError localized message: " + e.getLocalizedMessage()
										+ "\nError cause: " + e.getCause() 
										+ "\nDetailed message:"
										+ "\n" + out,
										ProgressDialog.ERROR);
							}
						}
					}
				}
				
				/**
				 * Add repository to the to delete list
				 */
				try {
					forceDeleteDirectory(new File(tempDir),0);			
					//TODO Deletion does not work right now on windows... Could not find a solution for it
					//TODO Try deleting also other temp folder!
				} catch (IOException e) {
					deleteManually = true;
					String out = "";
					for (int err = 0; err < e.getStackTrace().length; err++) {
						out += " \n " + e.getStackTrace()[err].toString();
					}
					progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ": Could not delete temporary folder " + tempDir + ", please delete manually!" 
							+ "\nError message: " + e.getMessage()
							+ "\nError localized message: " + e.getLocalizedMessage()
							+ "\nError cause: " + e.getCause() 
							+ "\nDetailed message:"
							+ "\n" + out,
							ProgressDialog.NOTIFICATION);
				} catch (NullPointerException e) {
					String out = "";
					for (int err = 0; err < e.getStackTrace().length; err++) {
						out += " \n " + e.getStackTrace()[err].toString();
					}
					progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ": Could not delete temporary folder " + tempDir + " since it does not exist!" 
							+ "\nError message: " + e.getMessage()
							+ "\nError localized message: " + e.getLocalizedMessage()
							+ "\nError cause: " + e.getCause() 
							+ "\nDetailed message:"
							+ "\n" + out,
							ProgressDialog.NOTIFICATION);
				} catch (InterruptedException e) {
					String out = "";
					for (int err = 0; err < e.getStackTrace().length; err++) {
						out += " \n " + e.getStackTrace()[err].toString();
					}
					progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ": Interrupted exception when deleting " + tempDir + "!" 
							+ "\nError message: " + e.getMessage()
							+ "\nError localized message: " + e.getLocalizedMessage()
							+ "\nError cause: " + e.getCause() 
							+ "\nDetailed message:"
							+ "\n" + out,
							ProgressDialog.NOTIFICATION);
				}
				
				if(tempProjectDir != "") {
					try {
						forceDeleteDirectory(new File(tempProjectDir),0);			
						//TODO Deletion does not work right now on windows... Could not find a solution for it
						//TODO Try deleting also other temp folder!
					} catch (IOException e) {
						deleteManually = true;
						String out = "";
						for (int err = 0; err < e.getStackTrace().length; err++) {
							out += " \n " + e.getStackTrace()[err].toString();
						}
						progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ": Could not delete temporary folder " + tempDir + ", please delete manually!" 
								+ "\nError message: " + e.getMessage()
								+ "\nError localized message: " + e.getLocalizedMessage()
								+ "\nError cause: " + e.getCause() 
								+ "\nDetailed message:"
								+ "\n" + out,
								ProgressDialog.NOTIFICATION);
					} catch (NullPointerException e) {
						String out = "";
						for (int err = 0; err < e.getStackTrace().length; err++) {
							out += " \n " + e.getStackTrace()[err].toString();
						}
						progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ": Could not delete temporary folder " + tempDir + " since it does not exist!" 
								+ "\nError message: " + e.getMessage()
								+ "\nError localized message: " + e.getLocalizedMessage()
								+ "\nError cause: " + e.getCause() 
								+ "\nDetailed message:"
								+ "\n" + out,
								ProgressDialog.NOTIFICATION);
					} catch (InterruptedException e) {
						String out = "";
						for (int err = 0; err < e.getStackTrace().length; err++) {
							out += " \n " + e.getStackTrace()[err].toString();
						}
						progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ": Interrupted exception when deleting " + tempDir + "!" 
								+ "\nError message: " + e.getMessage()
								+ "\nError localized message: " + e.getLocalizedMessage()
								+ "\nError cause: " + e.getCause() 
								+ "\nDetailed message:"
								+ "\n" + out,
								ProgressDialog.NOTIFICATION);
					}
				}
				
				/**
				 * Finish
				 */
				processingDone = true;
				progress.updateBarText("processing finished!");
				progress.setBar(0.9);
				break running;
			}
			progress.moveTask(task);
			System.gc();	
		}
		if(deleteManually) {
			//Try deleting one last time
			deleteManually = false;
			for(int task = 0; task < tasks; task++) {
				progress.updateBarText("Trying to delete temp files (" + String.valueOf(1+task) + " of " + String.valueOf(tasks) + ")");
				String tempDir = outPath + System.getProperty("file.separator") + "temp_" + task + "" + System.getProperty("file.separator");
				try {
					forceDeleteDirectory(new File(tempDir),1);
				} catch (IOException e) {
					deleteManually = true;
					String out = "";
					for (int err = 0; err < e.getStackTrace().length; err++) {
						out += " \n " + e.getStackTrace()[err].toString();
					}
					progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ": Could not delete temporary folder " + tempDir + ", please delete manually!" 
							+ "\nError message: " + e.getMessage()
							+ "\nError localized message: " + e.getLocalizedMessage()
							+ "\nError cause: " + e.getCause() 
							+ "\nDetailed message:"
							+ "\n" + out,
							ProgressDialog.NOTIFICATION);
				} catch (NullPointerException e) {
					deleteManually = true;
					String out = "";
					for (int err = 0; err < e.getStackTrace().length; err++) {
						out += " \n " + e.getStackTrace()[err].toString();
					}
					progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ": Could not delete temporary folder " + tempDir + " since it does not exist!" 
							+ "\nError message: " + e.getMessage()
							+ "\nError localized message: " + e.getLocalizedMessage()
							+ "\nError cause: " + e.getCause() 
							+ "\nDetailed message:"
							+ "\n" + out,
							ProgressDialog.NOTIFICATION);
				} catch (InterruptedException e) {
					deleteManually = true;
					String out = "";
					for (int err = 0; err < e.getStackTrace().length; err++) {
						out += " \n " + e.getStackTrace()[err].toString();
					}
					progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ": Interrupted exception when deleting " + tempDir + "!" 
							+ "\nError message: " + e.getMessage()
							+ "\nError localized message: " + e.getLocalizedMessage()
							+ "\nError cause: " + e.getCause() 
							+ "\nDetailed message:"
							+ "\n" + out,
							ProgressDialog.NOTIFICATION);
				}
			}
			
			if(deleteManually) {
				new WaitForUserDialog("We could not delete temp folders automatically. Please delete all directories that are called [temp_0] to [temp_"
						+ String.valueOf(tasks-1)
						+ "] in the output folder (" + outPath + ") manually!").show();				
			}
		}
	}

	/**
	 * Cleans an OPERA metadata Document from all information not related to a specific imageID
	 * @param tempMetaDoc: The document to be cleaned from entries that do not match the wellIDToKeep and the imageIDToKeep
	 * @param wellIDToKeep
	 * @param imageIDToKeep
	 * @param task: the task to which the document belongs, only required for indication in Progress Dialog
	 */
	private void cleanMetadataDocFromUselessImages(Document tempMetaDoc, String imageIDToKeep, int task) {
		String wellIDToKeep = imageIDToKeep.substring(0,4);					
		
		/**
		 * Removing useless wells from plates node 
		 */
		Node tempPlatesNode = tempMetaDoc.getElementsByTagName("Plates").item(0);
		Node tempPlateNode;
		for(int p = 0; p < tempPlatesNode.getChildNodes().getLength(); p++){
			if(tempPlatesNode.getChildNodes().item(p).getNodeName().equals("Plate")) {
				tempPlateNode = tempPlatesNode.getChildNodes().item(p);
				for(int q = tempPlateNode.getChildNodes().getLength()-1; q >= 0; q--){
					if(!tempPlateNode.getChildNodes().item(q).hasAttributes()) continue;														
					if(tempPlateNode.getChildNodes().item(q).getAttributes().getNamedItem("id").getNodeValue().equals(wellIDToKeep)) {
						if(extendedLogging)	progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ": "
								+ "Saved correct well node in Plate Nodes with well id " + tempPlateNode.getChildNodes().item(q).getAttributes().getNamedItem("id").getNodeValue() + ".", ProgressDialog.LOG);
						progress.updateBarText("Saved correct well node in Plate Nodes with well id " + tempPlateNode.getChildNodes().item(q).getAttributes().getNamedItem("id").getNodeValue() + ".");
					} else {
						tempPlateNode.removeChild(tempPlateNode.getChildNodes().item(q));
					}
				}
			}
		}
		
		/**
		 * Removing useless wells from Wells node
		 */
		Node tempWellsNode = tempMetaDoc.getElementsByTagName("Wells").item(0);
		Node tempWellNode;
		String tempImageID;
		for(int w = 0; w < tempWellsNode.getChildNodes().getLength(); w++){
			if(tempWellsNode.getChildNodes().item(w).getNodeName().equals("Well")) {
				tempWellNode = tempWellsNode.getChildNodes().item(w);
				for(int i = tempWellNode.getChildNodes().getLength()-1; i >= 0; i--){
					if(tempWellNode.getChildNodes().item(i).getNodeName().equals("id")) {
						if(tempWellNode.getChildNodes().item(i).getTextContent().equals(wellIDToKeep)) {
							if(extendedLogging)	progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ": "
									+ "Saved correct well node in Well nodes with id " + tempWellNode.getChildNodes().item(i).getTextContent() + ".", ProgressDialog.LOG);															
						}else {
							tempWellsNode.removeChild(tempWellNode);
							break;
						}
					}
					
					if(tempWellNode.getChildNodes().item(i).getNodeName().equals("Image")) {
						if(!tempWellNode.getChildNodes().item(i).hasAttributes()) continue;
						tempImageID = tempWellNode.getChildNodes().item(i).getAttributes().getNamedItem("id").getNodeValue();
						tempImageID = tempImageID.substring(0,tempImageID.lastIndexOf("P"));
						if(tempImageID.equals(wellIDToKeep)) {
							if(extendedLogging) {
								progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ": "
										+ "Kept correct image id in well node: " + tempWellNode.getChildNodes().item(i).getAttributes().getNamedItem("id").getNodeValue() + ".", ProgressDialog.LOG);			
							}													
						}else {
							tempWellNode.removeChild(tempWellNode.getChildNodes().item(i));
						}
					}													
				}												
			}
		}
		System.gc();
		
		/**
		 * Removing useless images from image node
		 */
		Node tempImagesNode = tempMetaDoc.getElementsByTagName("Images").item(0);
		Node tempImageNode;
		 for(int i = 0; i < tempImagesNode.getChildNodes().getLength(); i++){
			 if(tempImagesNode.getChildNodes().item(i).getNodeName().equals("Image")) {
				 tempImageNode = tempImagesNode.getChildNodes().item(i);
				 for(int i2 = tempImageNode.getChildNodes().getLength()-1; i2 >= 0; i2--){
					 if(tempImageNode.getChildNodes().item(i2).getNodeName().equals("id")) {
						tempImageID = tempImageNode.getChildNodes().item(i2).getTextContent();
						tempImageID = tempImageID.substring(0,tempImageID.lastIndexOf("P"));
						if(tempImageID.equals(imageIDToKeep)) {
							if(extendedLogging) {
								progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ": Kept correct image id in image nodes: " + tempImageNode.getChildNodes().item(i2).getTextContent() + ".", ProgressDialog.LOG);	
							}
						}else {
							tempImagesNode.removeChild(tempImageNode);
							break;
						}
					 }
				 }
			 }
		 }		
	}

	/**
	 * @return name of the @param series (0 <= series < number of series)
	 * */
	private String getSeriesName(ImporterOptions options, int series) throws FormatException, IOException{
		ImportProcess process = new ImportProcess(options);
		if (!process.execute()) return "NaN";
		return process.getSeriesLabel(series);
	}
	
	/**
	 * Read a File and return the whole content as one concatenated string.
	 * Useful to read xml files.
	 * @param file:
	 * @return String containing the whole file content.
	 * @throws FileNotFoundException
	 */
	private static String readFileAsOneString(File file) throws FileNotFoundException {
		FileReader fr = new FileReader(file);
		BufferedReader br = new BufferedReader(fr);
		String line = "", collectedString = "";
		copyPaste: while (true) {
			try {
				line = br.readLine();
				if (line.equals(null))
					break copyPaste;
				collectedString += line;
			} catch (Exception e) {
				break copyPaste;
			}
		}
		try {
			br.close();
			fr.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return collectedString;
	}
		
	/**
	 * Find the first node with a specific name in a NodeList
	 * @param A NodeList in which a Node shall be found
	 * @param The name of the Node that shall be found as a String
	 * @return First node in the list called 'nodes' that has the given name
	 */
	private static Node getFirstNodeWithName(NodeList nodes, String name) {
		for(int n = 0; n < nodes.getLength(); n++) {
			if(nodes.item(n).getNodeName().equals(name)) {
				return nodes.item(n);
			}
		}
		return null;
	}
	

	/**
	 * Find the first node with a specific name in a NodeList
	 * @param imageNodes: The list of image nodes in an OPERA XML Metadata file
	 * @param id: The id for the image in that imageNodes list that shall be returned
	 * @return First node in the list that has a subnode of type <id> with a value of @param id
	 * 
	 * @deprecated Use getImageNodeWithID_OPERAMETADATA_UsingXPath instead
	 */
	private Node getImageNodeWithID_OPERAMETADATA(NodeList imageNodes, String id) {
		for(int n = 0; n < imageNodes.getLength(); n++) {
			if(imageNodes.item(n).getChildNodes().getLength() == 0) continue;
			if(getFirstNodeWithName(imageNodes.item(n).getChildNodes(),"id").getTextContent().equals(id)){
				return imageNodes.item(n);
			}
		}
		return null;
	}
	
	/**
	 * Find the first node with a specific name in a NodeList
	 * @param imageNodes: The list of image nodes in an OPERA XML Metadata file
	 * @param id: The id for the image in that imageNodes list that shall be returned
	 * @return First node in the list that has a subnode of type <id> with a value of @param id
	 */
	private Node getImageNodeWithID_OPERAMETADATA_UsingXPath(NodeList imageNodes, String id) {
		NodeList nl = null;
		try {
			XPathExpression expr = xp.compile("//Image[id='" + id + "']");
			nl = (NodeList) expr.evaluate(imageNodes, XPathConstants.NODESET);
//			progress.notifyMessage("Xpath inquiry for image node " + id + " yielded " + nl.getLength() + " node(s)! Id of fetched node: " 
//					+ "..." + nl.item(0).getNodeName()
//					+ "..." + nl.item(0).getNodeValue()
//					+ "..." + nl.item(0).getTextContent()
//					+ "..." + getFirstNodeWithName(nl.item(0).getChildNodes(),"id").getTextContent(),
//					ProgressDialog.LOG);			
		} catch (XPathExpressionException e) {
			String out = "";
			for (int err = 0; err < e.getStackTrace().length; err++) {
				out += " \n " + e.getStackTrace()[err].toString();
			}
			progress.notifyMessage("Task " + (0 + 1) + "/" + tasks + ": Could not fetch image node with id " + id 
					+ "\nError message: " + e.getMessage()
					+ "\nError localized message: " + e.getLocalizedMessage()
					+ "\nError cause: " + e.getCause() 
					+ "\nDetailed message:"
					+ "\n" + out,
					ProgressDialog.ERROR);
		}
		return nl.item(0);
	}
	
	/**
	 * Find the channel node
	 * @param node: The master document node
	 * @param channelId: The channel id (>=1)
	 * @return First an channel node with image information for channel with a channelID of @param channelId
	 */
	private Node getChannelMasterNodeUsingXPath(NodeList node, String channelId) {
		NodeList nl = null;
		String theXpath = "//Entry[@ChannelID='" + channelId + "' and ChannelName]";
		try {
			XPathExpression expr = xp.compile(theXpath);
			nl = (NodeList) expr.evaluate(node, XPathConstants.NODESET);
			if(nl.getLength() > 1) {
				progress.notifyMessage("Task " + (0 + 1) + "/" + tasks + ": WARNING Xpath inquiry for channel " + channelId + " yielded " + nl.getLength() + " node(s)! First node: " 
						+ "\n" + nl.item(0).getNodeName()
						+ "\n" + nl.item(0).getNodeValue()
						+ "\n" + nl.item(0).getTextContent()
						+ "\n" + getFirstNodeWithName(nl.item(0).getChildNodes(),"id").getTextContent(),
						ProgressDialog.LOG);			
			}			
		} catch (XPathExpressionException e) {
			String out = "";
			for (int err = 0; err < e.getStackTrace().length; err++) {
				out += " \n " + e.getStackTrace()[err].toString();
			}
			progress.notifyMessage("Task " + (0 + 1) + "/" + tasks + ": Could not fetch image node with id " + channelId 
					+ "\nError message: " + e.getMessage()
					+ "\nError localized message: " + e.getLocalizedMessage()
					+ "\nError cause: " + e.getCause() 
					+ "\nDetailed message:"
					+ "\n" + out,
					ProgressDialog.ERROR);
		} catch (Exception e) {
			String out = "";
			for (int err = 0; err < e.getStackTrace().length; err++) {
				out += " \n " + e.getStackTrace()[err].toString();
			}
			progress.notifyMessage("Task " + (0 + 1) + "/" + tasks + ": Error during xpath request for channel " + channelId + " and xpath " +  theXpath
					+ "\nError message: " + e.getMessage()
					+ "\nError localized message: " + e.getLocalizedMessage()
					+ "\nError cause: " + e.getCause() 
					+ "\nDetailed message:"
					+ "\n" + out,
					ProgressDialog.ERROR);
		}
		return nl.item(0);
	}
	
	/**
	 * Find the flatfield node
	 * @param node: The master document node
	 * @param channelId: The channel id (>=1)
	 * @return First an channel node with image information for channel with a channelID of @param channelId
	 */
	private String getFlatfieldProfileUsingXPath(NodeList node, String channelId) {
		NodeList nl = null;
		String theXpath = "//Entry[@ChannelID='" + channelId + "' and FlatfieldProfile]";
		try {
			XPathExpression expr = xp.compile(theXpath);
			nl = (NodeList) expr.evaluate(node, XPathConstants.NODESET);
			if(nl.getLength() > 1) {
				progress.notifyMessage("Task " + (0 + 1) + "/" + tasks + ": WARNING Xpath inquiry for channel " + channelId + " yielded " + nl.getLength() + " node(s)! First node: " 
						+ "\n" + nl.item(0).getNodeName()
						+ "\n" + nl.item(0).getNodeValue()
						+ "\n" + nl.item(0).getTextContent()
						+ "\n" + getFirstNodeWithName(nl.item(0).getChildNodes(),"id").getTextContent(),
						ProgressDialog.LOG);			
			}
			return nl.item(0).getTextContent();
		} catch (XPathExpressionException e) {
			String out = "";
			for (int err = 0; err < e.getStackTrace().length; err++) {
				out += " \n " + e.getStackTrace()[err].toString();
			}
			if(!noWarningsForMissingCustomMetadata) {
				progress.notifyMessage("Task " + (0 + 1) + "/" + tasks + ": Could not fetch flatfield image node with id " + channelId 
						+ "\nError message: " + e.getMessage()
						+ "\nError localized message: " + e.getLocalizedMessage()
						+ "\nError cause: " + e.getCause() 
						+ "\nDetailed message:"
						+ "\n" + out,
						ProgressDialog.NOTIFICATION);
			}			
			return "none";
		} catch (Exception e) {
			String out = "";
			for (int err = 0; err < e.getStackTrace().length; err++) {
				out += " \n " + e.getStackTrace()[err].toString();
			}
			if(!noWarningsForMissingCustomMetadata) {
				progress.notifyMessage("Task " + (0 + 1) + "/" + tasks + ": Error during xpath request for channel " + channelId + " and xpath " +  theXpath
						+ "\nError message: " + e.getMessage()
						+ "\nError localized message: " + e.getLocalizedMessage()
						+ "\nError cause: " + e.getCause() 
						+ "\nDetailed message:"
						+ "\n" + out,
						ProgressDialog.NOTIFICATION);
			}			
			return "none";
		}		
	}
	
	/**
	 * Find the first node with a specific name in a NodeList
	 * @param imageNodes: The list of image nodes in an OPERA XML Metadata file
	 * @return First node in the list that has a subnode of type <id> with a value of @param id
	 */
	private String [] getAllImageIDBeginningsFromListOfImageNodes_UsingXPath(NodeList imageNodes) {
		NodeList nl = null;
		try {
//			XPathExpression expr = xp.compile("//Image[ends-with(id,'P1R1']"); // ends-with does not exist in xpath 1.0 need to use other function below
			XPathExpression expr = xp.compile("//Image[substring(id, string-length(id) - 3) = 'P1R1']");
//			substring(@name, string-length(@name) - 1) = '_1'
			
			nl = (NodeList) expr.evaluate(imageNodes, XPathConstants.NODESET);		
		} catch (XPathExpressionException e) {
			String out = "";
			for (int err = 0; err < e.getStackTrace().length; err++) {
				out += " \n " + e.getStackTrace()[err].toString();
			}
			IJ.error("Could not translate image nodes into image ids! " 
					+ "\nError message: " + e.getMessage()
					+ "\nError localized message: " + e.getLocalizedMessage()
					+ "\nError cause: " + e.getCause() 
					+ "\nDetailed message:"
					+ "\n" + out);
			return new String [0];
		}
		String idList [] = new String [nl.getLength()];
		for(int n = 0; n < nl.getLength(); n++) {
			idList [n] = getFirstNodeWithName(nl.item(n).getChildNodes(), "id").getTextContent();
			idList [n] = idList [n].substring(0,idList [n].lastIndexOf("P"));
		}		
		return idList;
	}
	
	/**
	 * Find the first node with a specific name in a NodeList
	 * @param imageNodes: The list of image nodes in an OPERA XML Metadata file
	 * @param id: The id for the image in that imageNodes list that shall be returned
	 * @return First node in the list that has a subnode of type <id> with a value of @param id
	 */
	private Node getWellNodeWithID_OPERAMETADATA_UsingXPath(NodeList wellNodes, String id) {
		NodeList nl = null;
		try {
			XPathExpression expr = xp.compile("//Well[id='" + id + "']");
			nl = (NodeList) expr.evaluate(wellNodes, XPathConstants.NODESET);
		} catch (XPathExpressionException e) {
			String out = "";
			for (int err = 0; err < e.getStackTrace().length; err++) {
				out += " \n " + e.getStackTrace()[err].toString();
			}
			progress.notifyMessage("Task " + (0 + 1) + "/" + tasks + ": Could not fetch image node with id " + id 
					+ "\nError message: " + e.getMessage()
					+ "\nError localized message: " + e.getLocalizedMessage()
					+ "\nError cause: " + e.getCause() 
					+ "\nDetailed message:"
					+ "\n" + out,
					ProgressDialog.ERROR);
		}
		return nl.item(0);
	}
	
	
	private Unit<Time> getTimeUnitFromNodeAttribute(Node tempNode) {
		Unit<Time> tempUnit = null;										
		switch(tempNode.getAttributes().getNamedItem("Unit").getNodeValue()) {
		case "s":
			tempUnit = UNITS.SECOND;
			break;
		case "ms":
			tempUnit = UNITS.MILLISECOND;
			break;
		case "ns":
			tempUnit = UNITS.NANOSECOND;
			break;
		case "us":
			tempUnit = UNITS.MICROSECOND;
			break;
		case "\u00b5s":
			tempUnit = UNITS.MICROSECOND;
			break;
		}
		
		if(extendedLogging) {
			progress.notifyMessage("Casting Time Unit " + tempNode.getAttributes().getNamedItem("Unit").getNodeValue() + " to "
					+ tempUnit.getSymbol(), ProgressDialog.LOG);
		}
		
		return tempUnit;
		
	}
	
	private Unit<Length> getLengthUnitFromNodeAttribute(Node tempNode) {
		Unit<Length> tempUnit = null;
		switch(tempNode.getAttributes().getNamedItem("Unit").getNodeValue()) {
		case "m":
			tempUnit = UNITS.METER;
			break;
		case "mm":
			tempUnit = UNITS.MILLIMETER;
			break;
		case "micron":
			tempUnit = UNITS.MICROMETER;
			break;
		case "\u00b5m":
			tempUnit = UNITS.MICROMETER;
			break;
		case "um":
			tempUnit = UNITS.MICROMETER;
			break;
		case "nm":
			tempUnit = UNITS.NANOMETER;
			break;
		}
		
		if(extendedLogging) {
			try {
				progress.notifyMessage("Casting Length Unit " + tempNode.getAttributes().getNamedItem("Unit").getNodeValue() + " to "
						+ tempUnit.getSymbol(), ProgressDialog.LOG);
			}catch(Exception e){				
				//Catched in case Progress Dialog does not exist.
			}			
		}
		
		return tempUnit;
		
	}
	
	/***
	 * @param number, needs to be >=1 & < 27
	 * @return the letter at the position of number in the alphabet
	 */
	private String rowNumberToLetter(int number) {
		if(number > 26 || number < 1) throw new IndexOutOfBoundsException();		
		return String.valueOf((char)(number + 64));
	}
	
	/***
	 * get OPERA metadata string identifier
	 * @param wellRow: well coordinate, starting at 0
	 * @param wellColumn: column coordinate, starting at 0
	 * @param theT: time point, starting at 0
	 * @param theFieldOfViewID: starting at 0
	 * @param theZ: plane position, starting at 0
	 * @param theC: channel id, starting at 0
	 * @return a String that is a unique identifier in OPERA metadata for the image at the coordinates input as parameters.
	 */
	private static String getOPERAString(int wellRow, int wellColumn, int theT, int theFieldOfViewID, int theZ, int theC) {
		String OPERAString= "";
		if(String.valueOf(wellRow+1).length() == 1){
			OPERAString += "0";										
		}
		OPERAString += String.valueOf(wellRow+1);
		if(String.valueOf(wellColumn+1).length() == 1){
			OPERAString += "0";										
		}
		OPERAString += String.valueOf(wellColumn+1);
		OPERAString += "K" + String.valueOf(theT+1);
		OPERAString += "F" + String.valueOf(theFieldOfViewID+1);
		OPERAString += "P" + String.valueOf(theZ+1);
		OPERAString += "R" + String.valueOf(theC+1);
		
		return OPERAString;
	}
	
	
	/*** 
	 * @param originalFilename: the base filename
	 * @param wellString: the String refering to the well, to be added to the name - enter empty String if should not be added
	 * @param dateString: the String refering to the date, to be added to the name - enter empty String if should not be added
	 * @param theZ: Focal plane (Z) position as an index, >=0 
	 * @param theC: Channel position as an index, >=0
	 * @return the filename to be used for the output file, as a String
	 */			
	private String getOutputImageName(String originalFilename, String wellString, String dateString, int theZ, int theC) {
		String outImageName = originalFilename;
		if(wellString.length()>0) outImageName += "_" + wellString;
		if(dateString.length()>0) outImageName += "_" + dateString;
		
		if(theZ >= 10) {
			outImageName += "_Z" + String.valueOf(theZ);
		}else {
			outImageName += "_Z0" + String.valueOf(theZ);
		}
		if(theC >= 10) {
			outImageName += "_C" + String.valueOf(theC);
		}else {
			outImageName += "_C0" + String.valueOf(theC);
		}
		outImageName += ".ome.tif";
		
		return outImageName;
	}
	
	/**
	 * Force delete a directory file by file, ignoring streams on the directory
	 * @param theDirectory to the directory
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	private static void forceDeleteDirectory(File theDirectory, int retries) throws IOException, NullPointerException, InterruptedException {
		if (theDirectory.exists()){
	        File[] filesInDir = theDirectory.listFiles();
	        for (int file = 0; file < filesInDir.length; file++){
	        	for(int i = 0; i < retries+1; i++){
	        		if(filesInDir[file].isDirectory()) {
		                forceDeleteDirectory(filesInDir[file], retries);
		            }else{
//		            	FileDeleteStrategy.FORCE.delete(filesInDir[file]);
		            	FileUtils.forceDelete(filesInDir[file]);
		            	if(!filesInDir[file].exists()) break;
		            }
	        		if(!filesInDir[file].exists()) break;
	        		System.gc();
	        		Thread.sleep(5);
//	        		IJ.log("File try " + (i+1));
	        	}
	        }

	        for(int i = 0; i < retries; i++){
//			    FileDeleteStrategy.FORCE.delete(theDirectory);
    		    FileUtils.forceDelete(theDirectory);
    		    if(!theDirectory.exists()) break;
        		System.gc();
        		Thread.sleep(5);
//        		IJ.log("Dir try " + (i+1));
        	}
	    }else {
	    	throw new NullPointerException(); 
	    }
	}
	
	private static int getMinNrOfDigits(double val1, double val2) {
		int outDigits = String.valueOf(val1).split("\\.")[1].length();
		if(String.valueOf(val2).split("\\.")[1].length()<outDigits) {
			outDigits = String.valueOf(val2).split("\\.")[1].length();
		}
		return outDigits;
	}
	
	/**
	 * 
	 * @param path
	 * @param task
	 */
	private boolean loadOPERAMetadatafile(String path, int task) {
		loadingLog = "";
		loadingLogMode = ProgressDialog.LOG;
		String tempMsg = "";
		
		// Initialize metadata document
		{
			metadataFilePath = path;
			metaDataFile = new File(metadataFilePath);				
			metaDoc = null;
					
			try {
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				DocumentBuilder db = dbf.newDocumentBuilder();
				
				//Check if closingTag appears twice, and if so, remove all behind for loading
				String xml = new String(Files.readAllBytes(metaDataFile.toPath()), StandardCharsets.UTF_8);
			    int beforeLength = xml.length();
				
			    String closingTag = "</EvaluationInputData>";
			    int closingTagIndex = xml.indexOf(closingTag);
			    if (closingTagIndex >= 0) {
			        xml = xml.substring(0, closingTagIndex + closingTag.length());
			    }
			    int afterLength = xml.length();
			    if(beforeLength != afterLength) {
			    	tempMsg = "Task " + (task + 1) + "/" + tasks + ": Removed duplicated ending from xml file - removed " 
			    			+ (beforeLength-afterLength) + " duplicated chars from xml file end.";
					loadingLog += "\n" + tempMsg;
					
					InputStream is = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));    			    
					metaDoc = db.parse(is);					
			    }else {
			    	metaDoc = db.parse(metaDataFile);			    	
			    }	    
				
				metaDoc.getDocumentElement().normalize();
			} catch (SAXException | IOException | ParserConfigurationException e) {
				String out = "";
				for (int err = 0; err < e.getStackTrace().length; err++) {
					out += " \n " + e.getStackTrace()[err].toString();
				}
//				progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ": Could not process metadata file " + metadataFilePath 
//						+ "\nError message: " + e.getMessage()
//						+ "\nError localized message: " + e.getLocalizedMessage()
//						+ "\nError cause: " + e.getCause() 
//						+ "\nDetailed message:"
//						+ "\n" + out,
//						ProgressDialog.ERROR);
				tempMsg = "Task " + (task + 1) + "/" + tasks + ": Could not process metadata file " + metadataFilePath 
						+ "\nError message: " + e.getMessage()
						+ "\nError localized message: " + e.getLocalizedMessage()
						+ "\nError cause: " + e.getCause() 
						+ "\nDetailed message:"
						+ "\n" + out;
				loadingLog += "\n" + tempMsg;
				loadingLogMode = ProgressDialog.ERROR;
				setOPERAMetaDataToUnloaded();
				return false;
			}
			

			imagesNode = metaDoc.getElementsByTagName("Images").item(0);
			wellsNode = metaDoc.getElementsByTagName("Wells").item(0);
			platesNode = metaDoc.getElementsByTagName("Plates").item(0);			
		}
		
		/**
		 * Exploring the number of plates
		 */				
		int plateIndexOriginalMetadata = 0;
		for(int p = 0; p < platesNode.getChildNodes().getLength(); p++){
			if(platesNode.getChildNodes().item(p).getNodeName().equals("Plate")) {
				plateIndexOriginalMetadata = p;
				break;
			}
		}
		Node plateNode = platesNode.getChildNodes().item(plateIndexOriginalMetadata);	
		String plateIDOriginalMetadata = getFirstNodeWithName(plateNode.getChildNodes(), "PlateID").getTextContent();
		if(extendedLogging){
			tempMsg = "Fetched first plate node (id = " + plateIndexOriginalMetadata 
					+ ") with id " + plateIDOriginalMetadata + ".";
			loadingLog += "\n" + tempMsg;
//			progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ": " + tempMsg, ProgressDialog.LOG);
			
		}
		
		/**
		 * Verifying the number of plates
		 */
		{
			int nrOfPlates = 0;
			for(int pp = 0; pp < platesNode.getChildNodes().getLength(); pp++){
				if(platesNode.getChildNodes().item(pp).getNodeName().equals("Plate")) {
					nrOfPlates++;
				}
			}
			if(nrOfPlates > 1){
//				progress.notifyMessage("ERROR! Task " + (task + 1) + "/" + tasks + ": " + nrOfPlates + " different plates were found in the metadata xml file. "
//						+ "So far this software can only handle recording from one plate. Metadata from plate " + plateIDOriginalMetadata + " will be used. "
//						+ "Wrong metadata may be copied for files from other plates. "
//						+ "Contact the developer to implement converting images from multiple plates! ", ProgressDialog.ERROR);
				tempMsg = "ERROR! Task " + (task + 1) + "/" + tasks + ": " + nrOfPlates + " different plates were found in the metadata xml file. "
						+ "So far this software can only handle recording from one plate. Metadata from plate " + plateIDOriginalMetadata + " will be used. "
						+ "Wrong metadata may be copied for files from other plates. "
						+ "Contact the developer to implement converting images from multiple plates! ";
				loadingLog += "\n" + tempMsg;
				loadingLogMode = ProgressDialog.ERROR;
				setOPERAMetaDataToUnloaded();
				return false;
				
			}
		}			
		
		/***
		 * Find the plane step size by scanning the images.
		 */
		zStepSizeInMicronAcrossWholeOPERAFile = 0.0;
		{
			Node tempZNode;
			double tempAbsZ;
			String previousPlaneImageID = "", currentPlaneImageID = "";
			Length previousPlaneAbsZ = null, currentPlaneAbsZ;
			Unit<Length> unitForComparison = UNITS.MICROMETER, currentPlaneAbsZUnit;
			
			LinkedList<Double> observedZValues = new LinkedList<Double>();
			LinkedList<Integer> observedZValueOccurences = new LinkedList<Integer>();
			
			screening: for(int img = 0; img < imagesNode.getChildNodes().getLength(); img++){
				if(!imagesNode.getChildNodes().item(img).hasChildNodes()) {
					continue;
				}
				try {
					currentPlaneImageID = getFirstNodeWithName(imagesNode.getChildNodes().item(img).getChildNodes(), "id").getTextContent();
					if(!currentPlaneImageID.endsWith("R1")) continue; // Only consider first channels for extracting z information
												
					tempZNode = getFirstNodeWithName(imagesNode.getChildNodes().item(img).getChildNodes(), "AbsPositionZ");
					currentPlaneAbsZUnit = getLengthUnitFromNodeAttribute(tempZNode);
					currentPlaneAbsZ = new Length (Double.parseDouble(tempZNode.getTextContent()), currentPlaneAbsZUnit);
					
					if(previousPlaneImageID.equals("")) {
						previousPlaneImageID = currentPlaneImageID;
						previousPlaneAbsZ = currentPlaneAbsZ;
						continue screening;
					}else if(Integer.parseInt(currentPlaneImageID.substring(currentPlaneImageID.lastIndexOf("P")+1,currentPlaneImageID.lastIndexOf("R"))) == Integer.parseInt(previousPlaneImageID.substring(previousPlaneImageID.lastIndexOf("P")+1,previousPlaneImageID.lastIndexOf("R")))+1) {
						//This condition is met only if the plane is a follow up plane of the previous stored plane
						//Now we verify that the remaining identifiers match and if so we create a z value
						if(currentPlaneImageID.substring(0,currentPlaneImageID.lastIndexOf("P")).equals(previousPlaneImageID.substring(0,previousPlaneImageID.lastIndexOf("P")))) {
							tempAbsZ = currentPlaneAbsZ.value(unitForComparison).doubleValue() - previousPlaneAbsZ.value(unitForComparison).doubleValue();
							tempAbsZ = Double.parseDouble(String.format("%." + String.valueOf(2) + "g%n", tempAbsZ));
							if(tempAbsZ < 0) tempAbsZ *= -1.0;
							for(int zV = 0; zV < observedZValues.size();zV++) {
								if(observedZValues.get(zV) == tempAbsZ) {
									observedZValueOccurences.set(zV,observedZValueOccurences.get(zV)+1);
									previousPlaneImageID = currentPlaneImageID;
									previousPlaneAbsZ = currentPlaneAbsZ;
									continue screening;
								}
							}
							observedZValues.add(tempAbsZ);
							observedZValueOccurences.add(1);
							
							if(extendedLogging || LOGZDISTFINDING) {
								tempMsg = "Added a calculated ZValue (" + observedZValues
										+ ") based on comparing the AbsPositionZ of "
										+ previousPlaneImageID
										+ " (Z value of Length object: "
										+ previousPlaneAbsZ.value(unitForComparison)
										+ " " 
										+ unitForComparison.getSymbol()
										+ ") and "
										+ currentPlaneImageID
										+ "(Z text: "
										+ tempZNode.getTextContent()
										+ " " 
										+ currentPlaneAbsZUnit.getSymbol()
										+ "). ";
								loadingLog += "\n" + tempMsg;
//								progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ": " + tempMsg, ProgressDialog.LOG);
							}
							previousPlaneImageID = currentPlaneImageID;
							previousPlaneAbsZ = currentPlaneAbsZ;
							continue screening;
						}else {
							previousPlaneImageID = currentPlaneImageID;
							previousPlaneAbsZ = currentPlaneAbsZ;
							continue screening;
						}
					}else {
						previousPlaneImageID = currentPlaneImageID;
						previousPlaneAbsZ = currentPlaneAbsZ;
						continue screening;
					}							
				}catch(Exception e) {
					String out = "";
					for (int err = 0; err < e.getStackTrace().length; err++) {
						out += " \n " + e.getStackTrace()[err].toString();
					}
					tempMsg = "Could not find Z information in image node " 
							+ img 
							+ ". Node name: "
							+ imagesNode.getChildNodes().item(img).getNodeName()
							+ ". Node value: "
							+ imagesNode.getChildNodes().item(img).getNodeValue()
							+ ". Text content: "
							+ imagesNode.getChildNodes().item(img).getTextContent()
							+ ". Error " + e.getCause() + " - Detailed message:\n" + out;
					loadingLog += "\n" + tempMsg;	
					loadingLogMode = ProgressDialog.ERROR;
					setOPERAMetaDataToUnloaded();
					return false;
//					progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ": " + tempMsg,
//							ProgressDialog.ERROR);
					
				}						
			}
			
			/**
			 * Checking the determined Z values
			 */
			if(observedZValues.size() > 1) {
				int tempCt = 0;
				for(int i = 0; i < observedZValues.size(); i++) {
					if(observedZValueOccurences.get(i) > tempCt) {
						tempCt = observedZValueOccurences.get(i);
						zStepSizeInMicronAcrossWholeOPERAFile = observedZValues.get(i);
					}
				}

				if(!noWarningsForDiverseZ) {
					tempMsg = "There are images with different Z spacings available in this OPERA output file: " 
							+ observedZValues 
							+ " with the observed frequencies of " 
							+ observedZValueOccurences 
							+ "."
							+ " This program cannot guarantee accurate translation of Z step size information into the image calibration data stored in the .tif files."
							+ " This program will save the most frequent observed Z step size ("
							+ zStepSizeInMicronAcrossWholeOPERAFile
							+ " micron) in the OPERA file as a Z calibration value for all converted images.";
					loadingLog += "\nWARNING: " + tempMsg;
					if(loadingLogMode != ProgressDialog.ERROR) {
						loadingLogMode = ProgressDialog.NOTIFICATION;
					}
				}				
//				progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ": " + tempMsg,
//						ProgressDialog.NOTIFICATION);
			}else if(observedZValues.size() == 1){
				zStepSizeInMicronAcrossWholeOPERAFile = observedZValues.get(0);
				observedZValues = null;
//				if(extendedLogging || LOGZDISTFINDING) {
//					progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ": Found Z step size in metadata and will set it as image calibration - value " + observedZValues + ".",
//							ProgressDialog.LOG); //TODO
//				}
			}else {
				zStepSizeInMicronAcrossWholeOPERAFile = 0.0;
				observedZValues = null;
			}
		}				
		System.gc();	
		
		// Finish loading
		loadedMetadataFilePath = metadataFilePath;
		loadedTask = task;
		return true;
	}
	
	/**
	 * TODO Write Description
	 * @param path
	 * @param task
	 */
	private boolean loadSourceOPERAMetadatafile(String path, int task) {
		srcLoadingLog = "";
		srcLoadingLogMode = ProgressDialog.LOG;
		String tempMsg = "";
		srcMetaXMLVersion = "";
		
		// Initialize metadata document
		{
			srcMetadataFilePath = path;
			srcMetaDataFile = new File(srcMetadataFilePath);				
			srcMetaDoc = null;
					
			try {
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				DocumentBuilder db = dbf.newDocumentBuilder();
				
				// Remove duplicated ending (related to reported issue #2 in github)
				String xml = new String(Files.readAllBytes(srcMetaDataFile.toPath()), StandardCharsets.UTF_8);
				
				int beforeLength = xml.length();
				
			    String closingTag = "</EvaluationInputData>";
			    int closingTagIndex = xml.indexOf(closingTag);
			    if (closingTagIndex >= 0) {
			        xml = xml.substring(0, closingTagIndex + closingTag.length());
			    }
			    int afterLength = xml.length();
			    if(beforeLength != afterLength) {
			    	tempMsg = "Task " + (task + 1) + "/" + tasks + ": Removed duplicated ending from xml file - removed " 
			    			+ (beforeLength-afterLength) + " duplicated chars from xml file end.";
					srcLoadingLog += "\n" + tempMsg;
					
					InputStream is = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));    			    
					srcMetaDoc = db.parse(is);					
			    }else {
					srcMetaDoc = db.parse(srcMetaDataFile);			    	
			    }
			    
			    srcMetaDoc.getDocumentElement().normalize();
			} catch (SAXException | IOException | ParserConfigurationException e) {
				String out = "";
				for (int err = 0; err < e.getStackTrace().length; err++) {
					out += " \n " + e.getStackTrace()[err].toString();
				}
				tempMsg = "Task " + (task + 1) + "/" + tasks + ": Could not process metadata file " + srcMetadataFilePath 
						+ "\nError message: " + e.getMessage()
						+ "\nError localized message: " + e.getLocalizedMessage()
						+ "\nError cause: " + e.getCause() 
						+ "\nDetailed message:"
						+ "\n" + out;
				srcLoadingLog += "\n" + tempMsg;
				srcLoadingLogMode = ProgressDialog.ERROR;
				setSrcOPERAMetaDataToUnloaded();
				return false;
			}
			

			srcImagesNode = srcMetaDoc.getElementsByTagName("Images").item(0);
			srcWellsNode = srcMetaDoc.getElementsByTagName("Wells").item(0);
			srcPlatesNode = srcMetaDoc.getElementsByTagName("Plates").item(0);
			
			// Detecting the metadata version - implemented variants: 1 or 2
			srcMetaXMLVersion = srcMetaDoc.getElementsByTagName("EvaluationInputData").item(0).getAttributes().getNamedItem("Version").getNodeValue();
			if(extendedLogging){
				tempMsg = "Fetched metadata type: " + srcMetaXMLVersion + ".";
				srcLoadingLog += "\n" + tempMsg;
				
			}
		}
		
		/**
		 * Exploring the number of plates
		 */				
		int plateIndexOriginalMetadata = 0;
		for(int p = 0; p < srcPlatesNode.getChildNodes().getLength(); p++){
			if(srcPlatesNode.getChildNodes().item(p).getNodeName().equals("Plate")) {
				plateIndexOriginalMetadata = p;
				break;
			}
		}
		Node plateNode = srcPlatesNode.getChildNodes().item(plateIndexOriginalMetadata);	
		String plateIDOriginalMetadata = getFirstNodeWithName(plateNode.getChildNodes(), "PlateID").getTextContent();
		if(extendedLogging){
			tempMsg = "Fetched first plate node (id = " + plateIndexOriginalMetadata 
					+ ") with id " + plateIDOriginalMetadata + ".";
			srcLoadingLog += "\n" + tempMsg;
			
		}
		
		/**
		 * Verifying the number of plates
		 */
		{
			int nrOfPlates = 0;
			for(int pp = 0; pp < srcPlatesNode.getChildNodes().getLength(); pp++){
				if(srcPlatesNode.getChildNodes().item(pp).getNodeName().equals("Plate")) {
					nrOfPlates++;
				}
			}
			if(nrOfPlates > 1){
				tempMsg = "ERROR! Task " + (task + 1) + "/" + tasks + ": " + nrOfPlates + " different plates were found in the metadata xml file. "
						+ "So far this software can only handle recording from one plate. Metadata from plate " + plateIDOriginalMetadata + " will be used. "
						+ "Wrong metadata may be copied for files from other plates. "
						+ "Contact the developer to implement converting images from multiple plates! ";
				srcLoadingLog += "\n" + tempMsg;
				srcLoadingLogMode = ProgressDialog.ERROR;
				setSrcOPERAMetaDataToUnloaded();
				return false;
				
			}
		}			
		
		/***
		 * Find the plane step size by scanning the images.
		 */
		srcZStepSizeInMicronAcrossWholeOPERAFile = 0.0;
		{
			Node tempZNode;
			double tempAbsZ;
			String previousPlaneImageID = "", currentPlaneImageID = "";
			Length previousPlaneAbsZ = null, currentPlaneAbsZ;
			Unit<Length> unitForComparison = UNITS.MICROMETER, currentPlaneAbsZUnit;
			
			LinkedList<Double> observedZValues = new LinkedList<Double>();
			LinkedList<Integer> observedZValueOccurences = new LinkedList<Integer>();
			
			screening: for(int img = 0; img < srcImagesNode.getChildNodes().getLength(); img++){
				if(!srcImagesNode.getChildNodes().item(img).hasChildNodes()) {
					continue;
				}
				try {
					currentPlaneImageID = getFirstNodeWithName(srcImagesNode.getChildNodes().item(img).getChildNodes(), "id").getTextContent();
					if(!currentPlaneImageID.endsWith("R1")) continue; // Only consider first channels for extracting z information
												
					tempZNode = getFirstNodeWithName(srcImagesNode.getChildNodes().item(img).getChildNodes(), "AbsPositionZ");
					currentPlaneAbsZUnit = getLengthUnitFromNodeAttribute(tempZNode);
					currentPlaneAbsZ = new Length (Double.parseDouble(tempZNode.getTextContent()), currentPlaneAbsZUnit);
					
					if(previousPlaneImageID.equals("")) {
						previousPlaneImageID = currentPlaneImageID;
						previousPlaneAbsZ = currentPlaneAbsZ;
						continue screening;
					}else if(Integer.parseInt(currentPlaneImageID.substring(currentPlaneImageID.lastIndexOf("P")+1,currentPlaneImageID.lastIndexOf("R"))) == Integer.parseInt(previousPlaneImageID.substring(previousPlaneImageID.lastIndexOf("P")+1,previousPlaneImageID.lastIndexOf("R")))+1) {
						//This condition is met only if the plane is a follow up plane of the previous stored plane
						//Now we verify that the remaining identifiers match and if so we create a z value
						if(currentPlaneImageID.substring(0,currentPlaneImageID.lastIndexOf("P")).equals(previousPlaneImageID.substring(0,previousPlaneImageID.lastIndexOf("P")))) {
							tempAbsZ = currentPlaneAbsZ.value(unitForComparison).doubleValue() - previousPlaneAbsZ.value(unitForComparison).doubleValue();
							tempAbsZ = Double.parseDouble(String.format("%." + String.valueOf(2) + "g%n", tempAbsZ));
							if(tempAbsZ < 0) tempAbsZ *= -1.0;
							for(int zV = 0; zV < observedZValues.size();zV++) {
								if(observedZValues.get(zV) == tempAbsZ) {
									observedZValueOccurences.set(zV,observedZValueOccurences.get(zV)+1);
									previousPlaneImageID = currentPlaneImageID;
									previousPlaneAbsZ = currentPlaneAbsZ;
									continue screening;
								}
							}
							observedZValues.add(tempAbsZ);
							observedZValueOccurences.add(1);
							
							if(extendedLogging || LOGZDISTFINDING) {
								tempMsg = "Added a calculated ZValue (" + observedZValues
										+ ") based on comparing the AbsPositionZ of "
										+ previousPlaneImageID
										+ " (Z value of Length object: "
										+ previousPlaneAbsZ.value(unitForComparison)
										+ " " 
										+ unitForComparison.getSymbol()
										+ ") and "
										+ currentPlaneImageID
										+ "(Z text: "
										+ tempZNode.getTextContent()
										+ " " 
										+ currentPlaneAbsZUnit.getSymbol()
										+ "). ";
								srcLoadingLog += "\n" + tempMsg;
//								progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ": " + tempMsg, ProgressDialog.LOG);
							}
							previousPlaneImageID = currentPlaneImageID;
							previousPlaneAbsZ = currentPlaneAbsZ;
							continue screening;
						}else {
							previousPlaneImageID = currentPlaneImageID;
							previousPlaneAbsZ = currentPlaneAbsZ;
							continue screening;
						}
					}else {
						previousPlaneImageID = currentPlaneImageID;
						previousPlaneAbsZ = currentPlaneAbsZ;
						continue screening;
					}							
				}catch(Exception e) {
					String out = "";
					for (int err = 0; err < e.getStackTrace().length; err++) {
						out += " \n " + e.getStackTrace()[err].toString();
					}
					tempMsg = "Could not find Z information in image node " 
							+ img 
							+ ". Node name: "
							+ srcImagesNode.getChildNodes().item(img).getNodeName()
							+ ". Node value: "
							+ srcImagesNode.getChildNodes().item(img).getNodeValue()
							+ ". Text content: "
							+ srcImagesNode.getChildNodes().item(img).getTextContent()
							+ ". Error " + e.getCause() + " - Detailed message:\n" + out;
					srcLoadingLog += "\n" + tempMsg;	
					srcLoadingLogMode = ProgressDialog.ERROR;
					setSrcOPERAMetaDataToUnloaded();
					return false;
				}						
			}
			
			/**
			 * Checking the determined Z values
			 */
			if(observedZValues.size() > 1) {
				int tempCt = 0;
				for(int i = 0; i < observedZValues.size(); i++) {
					if(observedZValueOccurences.get(i) > tempCt) {
						tempCt = observedZValueOccurences.get(i);
						srcZStepSizeInMicronAcrossWholeOPERAFile = observedZValues.get(i);
					}
				}
				
				if(!noWarningsForDiverseZ) {
					tempMsg = "There are images with different Z spacings available in this OPERA output file: " 
							+ observedZValues 
							+ " with the observed frequencies of " 
							+ observedZValueOccurences 
							+ "."
							+ " This program cannot guarantee accurate translation of Z step size information into the image calibration data stored in the .tif files."
							+ " This program will save the most frequent observed Z step size ("
							+ srcZStepSizeInMicronAcrossWholeOPERAFile
							+ " micron) in the OPERA file as a Z calibration value for all converted images.";
					srcLoadingLog += "\nWARNING: " + tempMsg;
					if(srcLoadingLogMode != ProgressDialog.ERROR) {
						srcLoadingLogMode = ProgressDialog.NOTIFICATION;
					}
				}				
			}else if(observedZValues.size() == 1){
				srcZStepSizeInMicronAcrossWholeOPERAFile = observedZValues.get(0);
				observedZValues = null;
			}else {
				srcZStepSizeInMicronAcrossWholeOPERAFile = 0.0;
				observedZValues = null;
			}
		}				
		System.gc();	
		
		// Finish loading
		loadedSrcMetadataFilePath = srcMetadataFilePath;
		loadedSrcTask = task;
		return true;
	}
	
	private void setOPERAMetaDataToUnloaded() {
		loadedMetadataFilePath = "";
		loadedTask = -1;
		metadataFilePath = "";
		metaDataFile = null;
		metaDoc = null;
		imagesNode = null;
		wellsNode = null;
		platesNode = null;
		zStepSizeInMicronAcrossWholeOPERAFile = -1.0;
//		loadingLog = "";
//		loadingLogMode = ProgressDialog.LOG;
	}
	
	private void setSrcOPERAMetaDataToUnloaded() {
		loadedSrcMetadataFilePath = "";
		loadedSrcTask = -1;
		srcMetadataFilePath = "";
		srcMetaDataFile = null;
		srcMetaDoc = null;
		srcImagesNode = null;
		srcWellsNode = null;
		srcPlatesNode = null;
		srcZStepSizeInMicronAcrossWholeOPERAFile = -1.0;
//		srcLoadingLog = "";
//		srcLoadingLogMode = ProgressDialog.LOG;
	}
	
	private byte[] getBytes(ImagePlus imp, int z, int c, int t) {
		if(imp.getBitDepth()==8) {
			return getBytes8Bit(imp,z,c,t);
		}else if(imp.getBitDepth() == 16){
			return getBytes16Bit(imp,z,c,t);			
		}else {
			throw new IllegalArgumentException("Bit depth " + imp.getBitDepth() + " is not 8 or 16!");
		}
	}
	
	private byte[] getBytes8Bit(ImagePlus imp, int z, int c, int t) {
	    imp.setSlice(t * imp.getNChannels() * imp.getNSlices() + c * imp.getNSlices() + z + 1);
	    ImageProcessor ip = imp.getProcessor();
	    byte[] pixels = (byte[])ip.getPixelsCopy();
	    return pixels;
	}
	
	private byte[] getBytes16Bit(ImagePlus imp, int z, int c, int t) {
	    imp.setSlice(t * imp.getNChannels() * imp.getNSlices() + c * imp.getNSlices() + z + 1);
	    ImageProcessor ip = imp.getProcessor();
	    short[] pixels = (short[])ip.getPixelsCopy();

	    // We need to transform the short array into a byte array
	    ByteBuffer byteBuffer = ByteBuffer.allocate(pixels.length * 2); //x2 because short = 2 bytes
	    for(short pixelValue: pixels){
	        byteBuffer.putShort(pixelValue);
	    }
	    byte[] bytePixels = byteBuffer.array();	    
	    return bytePixels;	    
	}
	
}// end main class