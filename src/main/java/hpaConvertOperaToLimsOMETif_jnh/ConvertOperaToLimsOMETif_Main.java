package hpaConvertOperaToLimsOMETif_jnh;

/** ===============================================================================
* HPA_Convert_OPERA_To_LIMS-OMETIF_JNH.java Version 0.0.1
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
* Date: September 11, 2023 (This Version: September 11, 2023)
*   
* For any questions please feel free to contact me (jan.hansen@scilifelab.se).
* =============================================================================== */

import java.awt.Font;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;

import javax.swing.UIManager;
//For XML support
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.DOMException;
//W3C definitions for a DOM, DOM exceptions, entities, nodes
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import hpaConvertSp8ToOMETif_jnh.ProgressDialog;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.WaitForUserDialog;
import ij.plugin.PlugIn;
import loci.common.RandomAccessInputStream;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.FormatException;
import loci.formats.FormatTools;
import loci.formats.ImageReader;
import loci.formats.MetadataTools;
import loci.formats.in.MetadataOptions;
import loci.formats.meta.IMetadata;
import loci.formats.meta.MetadataRetrieve;
import loci.formats.meta.MetadataStore;
import loci.formats.ome.OMEXMLMetadata;
import loci.formats.out.OMETiffWriter;
import loci.formats.services.OMEXMLService;
import loci.formats.tiff.TiffParser;
import loci.formats.FormatException;
import loci.plugins.BF;
import loci.plugins.in.ImportProcess;
import loci.plugins.in.ImporterOptions;

//import loci.formats.FormatException;
import loci.formats.tiff.TiffSaver;
import ome.units.UNITS;
import ome.units.quantity.Length;
import ome.units.unit.Unit;
import ome.xml.meta.MetadataConverter;
import ome.xml.model.OME;
import ome.xml.model.OMEModel;
import ome.xml.model.enums.DetectorType;
import ome.xml.model.enums.EnumerationException;
import ome.xml.model.enums.Immersion;
import ome.xml.model.enums.MicroscopeType;
import ome.xml.model.primitives.PercentFraction;

public class ConvertOperaToLimsOMETif_Main implements PlugIn {
	// Name variables
	static final String PLUGINNAME = "HPA Convert Opera-Tifs to LIMS-OME-Tif";
	static final String PLUGINVERSION = "0.0.1";

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
	boolean extendedLogging = true;

	// -----------------define params for Dialog-----------------
	int tasks = 1;
	boolean logXMLProcessing = false;
	boolean logDetectedOriginalMetadata = false;
	boolean logWholeOMEXMLComments = false;
	
	boolean loadViaBioformats = true;
	boolean extendOnly = false;
	
	boolean cropImage = true;
	int newImgLength = 2048;
	
	//Calibration data for the 96-well plate, data derived from the technincal drawing for Greiner Sensoplate Cat. No. 655892
	double centerOfFirstWellXInMM = 14.38;
	double centerOfFirstWellYInMM = 11.24;
	double distanceBetweenNeighboredWellCentersXInMM = 9;
	double distanceBetweenNeighboredWellCentersYInMM = 9;
	
	String imageType [] = new String [] {"OPERA Phenix Folder"};
	String selectedImageType = imageType [0];
	
	String outPath = "E:" + System.getProperty("file.separator") + System.getProperty("file.separator") + "OME Out"
			+ System.getProperty("file.separator");
	// -----------------define params for Dialog-----------------

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

		// &&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&
		// --------------------------REQUEST USER-SETTINGS-----------------------------
		// &&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&
		
		GenericDialog gd = new GenericDialog(PLUGINNAME + " - set parameters");	
		//show Dialog-----------------------------------------------------------------
		gd.setInsets(0,0,0);	gd.addMessage(PLUGINNAME + ", Version " + PLUGINVERSION + ", \u00a9 2022-2023 JN Hansen", SuperHeadingFont);	
		

		gd.setInsets(15,0,0);	gd.addMessage("Notes:", SubHeadingFont);
		
		gd.setInsets(0,0,0);	gd.addMessage("The plugin processes output folders with tif file and an Index.idx.xml file", InstructionsFont);
		gd.setInsets(0,0,0);	gd.addMessage("generated by an Opera Phenix screening microscope.", InstructionsFont);
		gd.setInsets(0,0,0);	gd.addMessage("The plugin will load the files using FIJI's bioformats library and resave them as ome.tif files", InstructionsFont);
		gd.setInsets(0,0,0);	gd.addMessage("enriched with all available metadata. The output files are ready for classic import into the HPA LIMS.",InstructionsFont);

		gd.setInsets(20,0,0);	gd.addMessage("NOTE: This plugin runs only in FIJI (not in a blank ImageJ, where OME BioFormats library is missing).", InstructionsFont);		
			
		gd.setInsets(15,0,0);	gd.addMessage("Plate specifications:", SubHeadingFont);	
		gd.setInsets(0, 0, 0);	gd.addNumericField("Distance from the center of well A1 to the left plate border [mm]",centerOfFirstWellXInMM,2);
		gd.setInsets(0, 0, 0);	gd.addNumericField("Distance from the center of well A1 to the top plate border [mm]",centerOfFirstWellYInMM,2);
		gd.setInsets(0, 0, 0);	gd.addNumericField("Horizontal distance between the centers of two neighbored wells (e.g., A1 to A2) [mm]",distanceBetweenNeighboredWellCentersXInMM,2);
		gd.setInsets(0, 0, 0);	gd.addNumericField("Vertical distance between the centers of two neighbored wells (e.g., A1 to B1) [mm]",distanceBetweenNeighboredWellCentersYInMM,2);
		
		gd.setInsets(15,0,0);	gd.addMessage("Processing Settings", SubHeadingFont);		
		gd.setInsets(0,0,0);	gd.addChoice("Image type", imageType, selectedImageType);
		
		gd.setInsets(20,0,0);	gd.addCheckbox("Crop image | new image width and height (px):", cropImage);
		gd.setInsets(-23, 150, 0);	gd.addNumericField("",newImgLength,0);
		
		gd.setInsets(20,0,0);	gd.addStringField("Filepath to output file", outPath, 30);
		gd.setInsets(0,0,0);	gd.addMessage("This path defines where outputfiles are stored.", InstructionsFont);
		gd.setInsets(0,0,0);	gd.addMessage("Make sure this path does not contain similarly named files - the program will overwrite identically named files!.", InstructionsFont);
		
		gd.setInsets(15,0,0);	gd.addMessage("Logging settings (troubleshooting options)", SubHeadingFont);		
		gd.setInsets(0,0,0);	gd.addCheckbox("Log transfer of metadata", logXMLProcessing);
		gd.setInsets(5,0,0);	gd.addCheckbox("Log transfer of original metadata", logDetectedOriginalMetadata);
		gd.setInsets(5,0,0);	gd.addCheckbox("Log the OME metadata XML before and after extending", logWholeOMEXMLComments);
		
		gd.setInsets(15,0,0);	gd.addMessage("Input files", SubHeadingFont);
		gd.setInsets(0,0,0);	gd.addMessage("A dialog will be shown when you press OK that allows you to list folders to be processed.", InstructionsFont);
		gd.setInsets(0,0,0);	gd.addMessage("List the directories that contain .ome.tif files (including MetaData folders) to be processed..", InstructionsFont);
		
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
		logXMLProcessing = gd.getNextBoolean();
		logDetectedOriginalMetadata = gd.getNextBoolean();
		logWholeOMEXMLComments = gd.getNextBoolean();
		//read and process variables--------------------------------------------------
		if (gd.wasCanceled()) return;
		
		// &&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&
		// -------------------------------LOAD FILES-----------------------------------
		// &&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&

		ImporterOptions bfOptions;
		if (loadViaBioformats) {
			/*
			 * Explore file with bioformats
			 * */
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
				if(logDetectedOriginalMetadata) {
					IJ.log("ORIGINAL: " + fullPath[task]);
					IJ.log("name:" + name[task]);
					IJ.log("dir:" + dir[task]);					
				}
			}
			
			if (tasks == 0) {
				new WaitForUserDialog("No folders selected!").show();
				return;
			}
			
			for(int i = tasks-1; i >= 0; i--){
				IJ.showProgress((tasks-i)/tasks);
				try {
					bfOptions = new ImporterOptions();
					bfOptions.setId(""+dir[i]+ System.getProperty("file.separator") + name[i]+"");
					bfOptions.setVirtual(true);
					bfOptions.setOpenAllSeries(true);
					ImagePlus[] imps = BF.openImagePlus(bfOptions);
					if(imps.length > 1) {
						String [] nameTemp = new String [name.length+imps.length-1], 
								dirTemp = new String [name.length+imps.length-1], 
								seriesNameTemp = new String [name.length+imps.length-1];
						int [] seriesTemp = new int [nameTemp.length],
								totSeriesTemp = new int [nameTemp.length]; 
						for(int j = 0; j < i; j++) {
							nameTemp [j] = name [j]; 
							dirTemp [j] = dir [j];
							seriesTemp [j] = series [j];
							seriesNameTemp [j] = seriesName [j];
							totSeriesTemp [j] = totSeries [j];
							
						}
						for(int j = 0; j < imps.length; j++) {
							nameTemp [i+j] = name [i]; 
							dirTemp [i+j] = dir [i];
							seriesTemp [i+j] = j;
							seriesNameTemp [j] = getSeriesName(bfOptions, j);
							totSeriesTemp [i+j] = imps.length;
						}
						for(int j = i+1; j < name.length; j++) {
							nameTemp [j+imps.length-1] = name [j]; 
							dirTemp [j+imps.length-1] = dir [j];
							seriesTemp [j+imps.length-1] = series [j];
							seriesNameTemp [j+imps.length-1] = seriesName [j];
							totSeriesTemp [j+imps.length-1] = totSeries [j];
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
							
							if(logDetectedOriginalMetadata) {
								IJ.log("ORIGINAL: " + name[i] + ", " + dir [i] + ", " + fullPath[i]);
								IJ.log("series: " + (series[j]+1) + " of " + totSeries[j] + " with name: " + seriesName[j]);
								}
							
//							filesList += name[j] + "\t" + dir[j] + "\t" + series[j] + "\t" + totSeries[j] + "\n";
						}
					}
					
					for(int j = imps.length-1; j>= 0; j--) {
						imps[j].changes = false;
						imps[j].close();
					}
				} catch (Exception e) {
					IJ.log(e.getCause().getLocalizedMessage());
					IJ.log(e.getCause().getMessage());
					e.printStackTrace();
				}
			}
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
		
		for (int task = 0; task < tasks; task++) {
			running: while (continueProcessing) {
				Date startDate = new Date();
				progress.updateBarText("in progress...");

				ImagePlus imp = null;
				
				if(loadViaBioformats){
//					NOTES FROM FIJI TODO
//					imp = IJ.openImage("E:/CP/Data/20230412 Sperm OPERA test -/230412 hansen__2023-04-12T13_47_17-Measurement 1/Images/Index.idx.xml");
//					IJ.run(imp, "OME-TIFF...", "save=[E:/CP/Data/20230412 Sperm OPERA test -/OME Out/File1 A1] write_each_z_section write_each_timepoint write_each_channel use");
//					IJ.run(imp, "OME-TIFF...", "save=[E:/CP/Data/20230412 Sperm OPERA test -/OME Out/A1.ome.tif] write_each_z_section write_each_timepoint write_each_channel use compression=Uncompressed");
//					imp = IJ.openImage("E:/CP/Data/20230412 Sperm OPERA test -/OME Out/A1_Z0_C0_T0.ome.tif");
					
					try {
						//bio format reader
		   				bfOptions = new ImporterOptions();
		   				bfOptions.setId(""+dir[task]+ System.getProperty("file.separator") + name[task]+"");
		   				bfOptions.setVirtual(false);
		   				bfOptions.setAutoscale(true);
		   				bfOptions.setColorMode(ImporterOptions.COLOR_MODE_COMPOSITE);
		   				for(int i = 0; i < totSeries[task]; i++) {
		   					if(i==series[task]) {
		   						bfOptions.setSeriesOn(i, true);
		   					}else {
		   						bfOptions.setSeriesOn(i, false);
		   					}
		   				}
		   				ImagePlus [] imps = BF.openImagePlus(bfOptions);
//		   				IJ.run("Bio-Formats", "open=[" +dir[task] + name[task]
//		   						+ "] autoscale color_mode=Default rois_import=[ROI manager] view=Hyperstack stack_order=XYCZT");
		   				imp = imps[0];	
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
				}else {
					progress.notifyMessage("Task " + task + ": Conversion outside of bioformats readable files is not implemented.",
							ProgressDialog.ERROR);
				}

				//Crop image to user defined size
				if(cropImage) {					
					imp.setRoi((int)((double)(imp.getWidth() - newImgLength)/2.0),
							(int)((double)(imp.getHeight() - newImgLength)/2.0),
							newImgLength,
							newImgLength);
//					imp.show();
//					new WaitForUserDialog("checkRoi").show();
					IJ.run(imp, "Crop", "");
					
//					imp.show();
//					new WaitForUserDialog("check cropped").show();
					
					
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
					
					String orDirName = dir [task].substring(0,dir [task].lastIndexOf(System.getProperty("file.separator")));
					orDirName = orDirName.substring(0,orDirName.lastIndexOf(System.getProperty("file.separator")));
					orDirName = orDirName.substring(orDirName.lastIndexOf(System.getProperty("file.separator"))+1);

					String outFilename = orDirName + "_" + (series[task]+1);
					
					progress.notifyMessage("Retrieved original dir name: " + orDirName, ProgressDialog.LOG); // TODO
					
					/**
					 * Create a temporary file repository and write the files into an OME-TIF format, output images will be called <outFilename>_Z2_C4.ome
					 */
					String tempDir = outPath + System.getProperty("file.separator") + "temp_" + task + "" + System.getProperty("file.separator");
					new File(tempDir).mkdirs();
					IJ.run(imp, "OME-TIFF...", "save=[" + tempDir + outFilename + ".ome.tif] write_each_z_section write_each_channel use export compression=Uncompressed");
					
					/**
					 * Import the raw metadata XML file and generate document to read from it
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
					String metadataFilePath = dir [task] + System.getProperty("file.separator") + name [task];
					File metaDataFile = new File(metadataFilePath);				
					Document metaDoc = null;
					
					if(extendedLogging)	progress.notifyMessage("Series name: " + seriesName [task] + "", ProgressDialog.LOG);
					if(extendedLogging)	progress.notifyMessage("Metadata file path: " + metadataFilePath + "", ProgressDialog.LOG);
					
					try {
						DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
						DocumentBuilder db = dbf.newDocumentBuilder();
						metaDoc = db.parse(metaDataFile);
						metaDoc.getDocumentElement().normalize();
					} catch (SAXException | IOException | ParserConfigurationException e) {
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
						return;
					}				
					
					/**
					 * Get basic nodes to find information in metaDoc
					 */
					Node imagesNode = metaDoc.getElementsByTagName("Images").item(0);
					Node wellsNode = metaDoc.getElementsByTagName("Wells").item(0);
					Node platesNode = metaDoc.getElementsByTagName("Plates").item(0);
					
					if(extendedLogging) {
						progress.notifyMessage("Fetched <Images> node and found " + imagesNode.getChildNodes().getLength() + " images.", ProgressDialog.LOG);
						progress.notifyMessage("Fetched <Wells> node and found " + wellsNode.getChildNodes().getLength() + " wells.", ProgressDialog.LOG);
						progress.notifyMessage("Fetched <Plates> node and found " + platesNode.getChildNodes().getLength() + " plates.", ProgressDialog.LOG);
					}

					int plateIndexOriginalMetadata = 0;
					Node plateNode = platesNode.getChildNodes().item(plateIndexOriginalMetadata);
					String plateIDOriginalMetadata = getFirstNodeWithName(plateNode.getChildNodes(), "PlateID").getNodeValue();
					if(platesNode.getChildNodes().getLength() > 1){
						progress.notifyMessage("WARNING! " + platesNode.getChildNodes().getLength() + " different plates were found in the metadata xml file. "
								+ "So far this software can only handle recording from one plate. Metadata from plate " + plateIDOriginalMetadata + " will be used. "
								+ "Wrong metadata may be copied for files from other plates. "
								+ "Contact the developer to implement converting images from multiple plates! ", ProgressDialog.NOTIFICATION);
					}
										
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
						
						for(int channel = 0; channel < nChannels; channel++) {
							for(int slices = 0; slices < nSlices; slices++) {
								omeTifFileName = outFilename + "_Z"+(slices+1)+"_C"+(channel+1)+".ome.tif";
								try {
									/**
									 * Open the tif file and extract the tif comment (= OME XML String)
									 * */
									comment = new TiffParser(omeTifFileName).getComment();									
									progress.updateBarText("Reading " + omeTifFileName + " done!");
									// display comment, and prompt for changes
									if(logWholeOMEXMLComments) {
										progress.notifyMessage("Original comment:", ProgressDialog.LOG);
										progress.notifyMessage(comment, ProgressDialog.LOG);
									}
									
									Document metaDocOME;
									try {
										DocumentBuilderFactory dbf2 = DocumentBuilderFactory.newInstance();
										DocumentBuilder db2 = dbf2.newDocumentBuilder();
										metaDocOME = db2.parse(metaDataFile);
										metaDocOME.getDocumentElement().normalize();
									} catch (SAXException | IOException | ParserConfigurationException e) {
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
										return;
									}
									
									/**
									 * Generate a MetadatStore out of the tif comment (= OME XML String) to explore the xml-styled content
									 * */
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
									
									/**
									 * Find the plate, well, and wellSample that the image is from.
									 */
									int plateIndex, wellIndex, wellSampleIndex;
									plateIndex = -1;
									wellIndex = -1;
									wellSampleIndex = -1;
									String plateID, wellID;
									plateID = "NA";
									wellID = "NA";
									
									for(int p = 0; p < meta.getPlateCount(); p++) {
										plateID = meta.getPlateID(p);
										for(int w = 0; w < meta.getWellCount(p); w++) {
											wellID = meta.getWellID(p, w);
											for(int sam = 0; sam < meta.getWellSampleCount(p, w); sam ++) {
												if(meta.getWellSampleImageRef(p, w, sam).equals(imageID)){
													plateIndex = p;
													wellIndex = w;
													wellSampleIndex = sam;													
													break;
												}
											}
										}
									}
									
									if(plateIndex == -1 || wellIndex == -1 || wellSampleIndex == -1) {
										progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ", Image " + metadataFilePath + " - Could not find image noted in plate and well OME annotations!",
												ProgressDialog.ERROR);
										continue;										
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
									
									if(extendedLogging)	progress.notifyMessage("Fetched well coordinates: Column " + wellColumn + ", Row " + wellRow, ProgressDialog.LOG);

									/**
									 * Find image information in TiffData
									 */
									int imageC = -1, imageT = -1, imageZ = -1;
									for(int td = 0; td < metaDocOME.getElementsByTagName("TiffData").getLength(); td++) {
										if(meta.getUUID() == metaDocOME.getElementsByTagName("TiffData").item(td).getChildNodes().item(0).getNodeValue()) {
											imageC = Integer.parseInt(metaDocOME.getElementsByTagName("TiffData").item(td).getAttributes().getNamedItem("FirstC").getNodeValue());
											imageT = Integer.parseInt(metaDocOME.getElementsByTagName("TiffData").item(td).getAttributes().getNamedItem("FirstT").getNodeValue());
											imageZ = Integer.parseInt(metaDocOME.getElementsByTagName("TiffData").item(td).getAttributes().getNamedItem("FirstZ").getNodeValue());
											break;
										}										
									}

									if(imageC == -1 | imageT == -1 | imageZ == -1) {
										progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ", Image " + metadataFilePath + " - Could not find tiff data node in OME XML Document!",
												ProgressDialog.ERROR);
										continue;
									}
									
									if(extendedLogging)	progress.notifyMessage("Fetched TiffData information for the current image: UUID " + meta.getUUID() 
										+ ", C " + imageC
										+ ", T " + imageT
										+ ", Z " + imageZ, 
										ProgressDialog.LOG);
									
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
									String ImageLabelOPERA = "";
									if(String.valueOf(wellRow).length() == 1){
										ImageLabelOPERA += "0";										
									}
									ImageLabelOPERA += String.valueOf(wellRow);
									if(String.valueOf(wellColumn).length() == 1){
										ImageLabelOPERA += "0";										
									}
									ImageLabelOPERA += String.valueOf(wellColumn);
									ImageLabelOPERA += "K1";
									ImageLabelOPERA += "F" + String.valueOf(wellSampleIndex+1);
									ImageLabelOPERA += "P" + String.valueOf(imageZ+1);
									ImageLabelOPERA += "R" + String.valueOf(imageC+1);
									
									if(extendedLogging)	progress.notifyMessage("Reconstructed reference in OPERA metadata " + ImageLabelOPERA, ProgressDialog.LOG);
																		
									Node imageNode = getImageNodeWithID_OPERAMETADATA(imagesNode.getChildNodes(), ImageLabelOPERA);
									if(imageNode.equals(null)) {
										progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ", Image " + metadataFilePath + " - Could not find image node with id " + ImageLabelOPERA + "in OPERA Metadata XML!",
												ProgressDialog.ERROR);
										continue;
									}									
									if(extendedLogging)	progress.notifyMessage("Found image node with id " + ImageLabelOPERA + " in OPERA metadata!", ProgressDialog.LOG);
									
									
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
										 * Calculate and modify X position
										 * */
										newXInM = meta.getPlanePositionX(imageIndex, p).value().doubleValue();
										newXInM = wellCenterXInMM / 1000.0 + newXInM;

										if(extendedLogging)	progress.notifyMessage("Plane " + p + "(Original X coordinate: " + meta.getPlanePositionX(imageIndex, p).value().doubleValue() 
												+ " " + meta.getPlanePositionX(imageIndex, p).unit().getSymbol() 
												+ ") will get C coordinate " + newXInM + " m", ProgressDialog.LOG);

										meta.setPlanePositionX(FormatTools.createLength(newXInM,UNITS.METER), imageIndex, p);
										
										/**
										 * Calculate and modify Y position
										 * */
										newYInM = meta.getPlanePositionY(imageIndex, p).value().doubleValue();
										newYInM = wellCenterYInMM / 1000.0 + newYInM;

										if(extendedLogging)	progress.notifyMessage("Plane " + p + "(Original Y coordinate: " + meta.getPlanePositionY(imageIndex, p).value().doubleValue() 
												+ " " + meta.getPlanePositionY(imageIndex, p).unit().getSymbol() 
												+ ") will get Y coordinate " + newYInM + " m", ProgressDialog.LOG);

										meta.setPlanePositionY(FormatTools.createLength(newYInM,UNITS.METER), imageIndex, p);
										
										/**
										 * Correct unit of Z position
										 * In the originally generated file the Z position is given in micron, although as unit only "reference frame" is specified!
										 * */
										newZInM = meta.getPlanePositionZ(imageIndex, p).value().doubleValue() / 1000 / 1000;

										if(extendedLogging)	progress.notifyMessage("Plane " + p + "(Original Z coordinate: " + meta.getPlanePositionZ(imageIndex, p).value().doubleValue() 
												+ " " + meta.getPlanePositionZ(imageIndex, p).unit().getSymbol() 
												+ ") will get Z coordinate " + newZInM + " m", ProgressDialog.LOG);

										meta.setPlanePositionY(FormatTools.createLength(newYInM,UNITS.METER), imageIndex, p);
										
										/**
										 * For security purposes let us try to cross check that with the original metadata file
										 * */
										{
											Node tempNode = getFirstNodeWithName(imageNode.getChildNodes(), "AbsPositionZ");
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
											case "um":
												tempUnit = UNITS.MICROMETER;
												break;
											case "nm":
												tempUnit = UNITS.NANOMETER;
												break;										
											}											
											if(Double.parseDouble(tempNode.getNodeValue()) != meta.getPlanePositionZ(imageIndex, p).value(tempUnit).doubleValue()){
												progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ", Image " + metadataFilePath + " - Z location in tiff metadata did not match metadata of image with reference " 
														+ ImageLabelOPERA + "in OPERA Metadata XML!",
														ProgressDialog.ERROR);
												continue;
											}else if(extendedLogging) {
												progress.notifyMessage("Plane " + p + "(Original Z coordinate: " + meta.getPlanePositionZ(imageIndex, p).value().doubleValue()
														+ " " + meta.getPlanePositionZ(imageIndex, p).unit().getSymbol() 
														+ ") will get Z coordinate " + newZInM + " m", ProgressDialog.LOG);
											}
													
										}
									}
									
									/**
									 * Add original metadata TODO
									 * */
									
									
								} catch (IOException e) {
									String out = "";
									for (int err = 0; err < e.getStackTrace().length; err++) {
										out += " \n " + e.getStackTrace()[err].toString();
									}
									progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ": Error when trying to open Tif-XML-Comment in file " + omeTifFileName 
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
								}								
							}
						}
						
						
						
					}
					
					try {
						imp.changes = false;
						imp.close();						
					} catch (Exception e) {
						progress.notifyMessage("Task " + (task + 1) + "/" + tasks + ": No image could be loaded... ",
								ProgressDialog.ERROR);
						break running;
					}
				}
				
				
				processingDone = true;
				progress.updateBarText("finished!");
				progress.setBar(1.0);
				break running;
			}
			progress.moveTask(task);
			System.gc();
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
	 * TODO Add description
	 * @param tempNode
	 * @return name of the node with a number added
	 */
	private String getNumberedNodeName(Node tempNode) {
		int sibblings = 0;
		Node sibbling;
		for(int cn = 0; cn < tempNode.getParentNode().getChildNodes().getLength(); cn++) {
			if(tempNode.getParentNode().getChildNodes().item(cn).getNodeName().equals(tempNode.getNodeName())) {
				sibblings ++;
			}
		}
		
		int id = 0;
		if(sibblings > 1) {
			sibbling = tempNode;
			id = 0;
			for(int cn = 0; cn < sibblings; cn++) {
				sibbling = sibbling.getNextSibling();
				if(sibbling == null) {
					break;
				}
				if(sibbling.getNodeName().equals(tempNode.getNodeName())) {
					id++;
				}
			}
			id = sibblings - id - 1;
			return tempNode.getNodeName() + " " + id;
		}else {
			return tempNode.getNodeName();					
		}
	}
	
	/**
	 * Find the first node with a specific name in a NodeList
	 * @param A NodeList in which a Node shall be found
	 * @param The name of the Node that shall be found as a String
	 * @return First node in the list called 'nodes' that has the given name
	 */
	private Node getFirstNodeWithName(NodeList nodes, String name) {
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
	 */
	private Node getImageNodeWithID_OPERAMETADATA(NodeList imageNodes, String id) {
		for(int n = 0; n < imageNodes.getLength(); n++) {
			if(getFirstNodeWithName(imageNodes.item(n).getChildNodes(),"id").getNodeValue().equals(id)){
				return imageNodes.item(n);
			}
		}
		return null;
	}
	
}// end main class