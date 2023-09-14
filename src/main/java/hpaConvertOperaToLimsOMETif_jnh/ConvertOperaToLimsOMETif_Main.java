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

	// -----------------define params for Dialog-----------------
	int tasks = 1;
	boolean logXMLProcessing = false;
	boolean logDetectedOriginalMetadata = false;
	boolean logWholeOMEXMLComments = false;
	
	boolean loadViaBioformats = true;
	boolean extendOnly = false;
	
	boolean cropImage = true;
	int newImgLength = 2048;
	
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

				//Crop image to 2048x2048
				if(cropImage) {					
					imp.setRoi((int)((double)(imp.getWidth() - newImgLength)/2.0),
							(int)((double)(imp.getHeight() - newImgLength)/2.0),
							newImgLength,
							newImgLength);
					imp.show();
//					new WaitForUserDialog("checkRoi").show();
					IJ.run(imp, "Crop", "");
					
					imp.show();
//					new WaitForUserDialog("check cropped").show();
					
					//Write it to ome.tif
					String orDirName = dir [task].substring(0,dir [task].lastIndexOf(System.getProperty("file.separator")));
					orDirName = orDirName.substring(0,orDirName.lastIndexOf(System.getProperty("file.separator")));
					orDirName = orDirName.substring(orDirName.lastIndexOf(System.getProperty("file.separator"))+1);

					progress.notifyMessage("Retrieved original dir name: " + orDirName, ProgressDialog.LOG); // TODO
					
//					outFilename = orDirName + "";				
					
					IJ.run(imp, "OME-TIFF...", "save=[" + outPath + System.getProperty("file.separator") + "testing.ome.tif] write_each_z_section write_each_channel use export compression=Uncompressed");
					
					
					//reopen each written file and resave it
					
					
//					String xmlString;
//					//xmlString = imp.getInfoProperty();
//					xmlString = imp.getFileInfo().description;
//					if(logDetectedOriginalMetadata) {
//						progress.notifyMessage("desc: " + xmlString, ProgressDialog.LOG);
//					}
//					xmlString = imp.getFileInfo().toString();
//					if(logDetectedOriginalMetadata) {
//						progress.notifyMessage("str: " + xmlString, ProgressDialog.LOG);
//					}
//					xmlString = imp.getFileInfo().info;
//					if(logDetectedOriginalMetadata) {
//						progress.notifyMessage("info: " + xmlString, ProgressDialog.LOG);
//					}
					
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
	
}// end main class