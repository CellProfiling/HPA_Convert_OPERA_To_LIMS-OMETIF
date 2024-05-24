# Converting OPERA-Files to LIMS-compatible, canonical, or Memento-compatible OME-TIF files
This is the github page for an ImageJ plugin that allows you to convert image output folders created by the Opera Phenix or Operetta CLS High-Content microscopes (also called "Sonata") into OME Tif format suitable for the HPA LIMS system, for OME-TIF-based analysis pipelines, or for [Memento](https://github.com/CellProfiling/memento) upload. Additionally, this software makes sure that metadata are correctly transferred from the OPERA metadata xml file into the OME-TIF Metadata XML information stored in the OME-TIF. 
 
See more instructions under [User instructions](https://github.com/CellProfiling/HPA_Convert_OPERA_To_LIMS-OMETIF/tree/main#user-instructions) below.
 
Contact jan.hansen@scilifelab.se for help.

---

## User instructions
### What does this software do?
This software converts output folders from the OPERA Phenix screening microscope into directories that are better structured for further downstream processing of the image, such as import into image handling software (LIMS) or annotation software (Memento).

#### Output files from the OPERA Phenix
Here is an example for an output folder created by the OPERA Phenix microscope:

<p align="center">
   <img src="https://github.com/CellProfiling/HPA_Convert_OPERA_To_LIMS-OMETIF/assets/27991883/8bb050b8-d748-4572-bac5-2d74ec13521d" width=200>
</p>

The output folder contains two folders: <i>Assaylayout</i> and <i>Images</i>. In the <i>Images</i> folder, there is a master .xml file, which contains all metadata and information on the recording, and individual tif images with certain ids.
<p align="center">
   <img src="https://github.com/CellProfiling/HPA_Convert_OPERA_To_LIMS-OMETIF/assets/27991883/c2726b6c-c2f9-4bab-b5cb-1f8d3c9c91f7" width=600>
</p>

To find out which images belong to which recording, one would need to look up the ids needed in the xml file and find the corresponding files. This is very cumbersome. That is why this software has been developed.

#### Output files by this software after processing the OPERA Phenix folders
This software automatically reads the master xml file in the OPERA Phenix <i>Images</i> folder and locates the tif files belonging to each recording noted in the xml file. Then this software creates a new folder directory and moves the files there but under more meaningful names and in a more reasonable folder structure. Additionally, this software makes sure that as much metadata as possible is preserved during transfer of images and shaping the new directory.

The user can select one of three different output directory structures, depending on how the user wishes to further use the output directory:
<p align="center">
   <img src="https://github.com/CellProfiling/HPA_Convert_OPERA_To_LIMS-OMETIF/assets/27991883/9e815a84-1551-4105-8c95-105aa4bb2a44" width=600>
</p>

- The first option (<b><i>LIMS style</i></b>) is tailored to upload the processed images to the HPA LIMS. In this option, a folder is created for each field of view and focal plane. This means that a z-stack is burst into individual folders for each z plane and that each z plane becomes an independent image including a metadata xml file.
- The second option (<b><i>canonical OME tif style</i></b>) is tailored to image analysis software that loads OME tif files. One folder is created for a z-stack in which all channel and plane images from a z-stack are stored together.
- The third option (<b><i>Memento style</i></b>) is tailored to create a tif folder structure that is compatible to further processing the files with the [TifCs_To_HPA-PNG-JPEG plugin](https://github.com/CellProfiling/TifCs_To_HPA-PNG-JPEG/), which can also scale the intensities and will output JPEG or PNG images suitable for upload to the annotation software [Memento](https://github.com/CellProfiling/memento).

##### Example output directory for LIMS upload
In the following we show how the output directory will look for the first option (<b><i>LIMS style</i></b>):
- In the output directory, a folder is created for each well. 
<p align="center">
   <img src="https://github.com/CellProfiling/HPA_Convert_OPERA_To_LIMS-OMETIF/assets/27991883/bb033163-9405-4d2a-aff7-efd875919119" width=250>
</p>

- And within each well folder, a folder is created for each field of view or even image plane (depending on the setting, see further done). 
<p align="center">
   <img src="https://github.com/CellProfiling/HPA_Convert_OPERA_To_LIMS-OMETIF/assets/27991883/bf56016a-8e50-40ff-a6d5-5b6c8bd20ce4" width=350>
</p>

- In each of these folders, there is an ome.tif file for each channel image and a folder with an xml file containing the metadata relevant for this particular image. Note that beyond the storage of metadata in that folder, there are also all metadata converted and written into OME XML annotations stored in the .ome.tif files itself.
<p align="center">
   <img src="https://github.com/CellProfiling/HPA_Convert_OPERA_To_LIMS-OMETIF/assets/27991883/f9c087ae-0c2e-4a06-9008-7a0ee1a93f34" width=500>
</p>
<p align="center">
   <img src="https://github.com/CellProfiling/HPA_Convert_OPERA_To_LIMS-OMETIF/assets/27991883/052effed-8aa0-40d3-9abf-4bb40904226b" width=500>
</p>

##### Example output directory for Memento
<p align="center">
   <p align="center">
      The top level folder contains all images in the same well and is named by the well coordinate:
   </p>
   <p align="center">
   <img src="https://github.com/CellProfiling/HPA_Convert_OPERA_To_LIMS-OMETIF/assets/27991883/f136343a-6e4a-41ae-95b5-39b34c53679a" width=400>
   </p>
   <p align="center">
      The middle level folder represents a z-stack, containing subfolders which represent each an individual z plane. The subfolders are named according to the z plane index.
   </p>
   <p align="center">
   <img src="https://github.com/CellProfiling/HPA_Convert_OPERA_To_LIMS-OMETIF/assets/27991883/06ab66f3-ea90-4209-a1a1-aa2954de8511" width=400>
   </p>
   <p align="center">
      The bottom level folder contains all channel images for one z plane and a copy of the xml metadata. Note that beyond the storage of metadata in that folder, there are also all metadata converted and written into OME XML annotations stored in the .ome.tif files itself.
   </p>
   <p align="center">
   <img src="https://github.com/CellProfiling/HPA_Convert_OPERA_To_LIMS-OMETIF/assets/27991883/9bff7db7-ee3a-4c39-bf82-0ff3fd1ff60a" width=400>
   </p>
</p>

### Installation = Getting started
This is a plugin for the ImageJ distribution FIJI. It requires the installation of Fiji on your computer (see below) to run this software. ImageJ/FIJI does not require any specific hardware, can run on Linux, Windows, and Mac, and can also run on low-performing computers. However, a RAM is required that allows to load one image sequence that you aim to analyze into your RAM at least twice. Our personal experience was that running this plugin in FIJI  requires at least about 2 GB in RAM. For geeks: ImageJ does not require any specific graphics card, the speed of the analysis depends mainly on the processor speed. 

#### Installing FIJI / ImageJ to your computer
FIJI is open-source, freely available software that can be downloaded [here](https://imagej.net/software/fiji/downloads). To install ImageJ / FIJI, you only need to download the distribution of your choice and fitting to your Operating system and extract the software directory from the downloaded archive. The resulting folder can be placed anywhere on your computer (where you have read and write permissions). 

On Mac OS, do not place the ImageJ / FIJI application into the *Applications* folder. Instead place it somewhere else than in the *Applications* folder, i.e. to the *desktop* or the *documents* directory. Thereby you can avoid a collision with a security feature by Mac OS, that might otherwise trigger software failures. If Mac OS does not allow you to launch FIJI by clicking the FIJI.app directory due to security concerns, run FIJI by right click (or holding option while clicking) to open more options for the file and click "open".

#### Installing the *HPA_Convert_OPERA_To_LIMS-OMETIF* FIJI plugin
- Download the .jar file from the latest software release listed on the [releases page in this repository](https://github.com/CellProfiling/HPA_Convert_OPERA_To_LIMS-OMETIF/releases).
<p align="center">
   <img src="https://github.com/CellProfiling/HPA_Convert_OPERA_To_LIMS-OMETIF/assets/27991883/79762215-e545-404f-b435-42840e310499" width=500>
</p>


- Launch ImageJ and install the plugin by drag and drop of the downloaded .jar file into the ImageJ window (red marked region in the screenshot below):
<p align="center">
   <img src="https://user-images.githubusercontent.com/27991883/201358020-c3685947-b5d8-4127-88ec-ce9b4ddf0e56.png" width=500>
</p>

- Confirm the installation by pressing save in the upcoming dialog.
- Next, FIJI requires to be restarted (close it and start it again).
- You should now be able to start the plugin via the menu entry <i>Plugins > Cell Profiling > Convert Tif folder from Opera Phenix to LIMS-like ome.tif (...)</i>

### Applying the plugin
Launch the plugin via the menu entry <i>Plugins > Cell Profiling > Convert Tif folder from Opera Phenix to LIMS-like ome.tif (...)</i> in FIJI. 
Next you will receive a settings dialog. Below each setting is explained in more detail.

#### 1. Setting up the settings
<p align="center">
   <img src="https://github.com/CellProfiling/HPA_Convert_OPERA_To_LIMS-OMETIF/assets/27991883/900cf66f-6bfe-478e-b107-ae019761c42c" width=800>
</p>

<b>NOTE</b>: For standard use of this plugin, there are only two options to be considered / customized: The <i>Filepath to output directory</i> and the <i>Output style</i>!

Further details about individual settings:
- <b>Plate specifications</b>: These settings are important when generating data for the HPA LIMS. The HPA LIMS reads the coordinates of each acquired image from the OME metadata settings and accordingly, places the acquired image into a certain well in the LIMS. In LIMS these coordinates are based on the whole plate, and the LIMS has an own sheet for which coordinates in X and Y belong to which well on a 96-well plate. By contrast, the OPERA Phenix microscope defines positions in a different way. It described the position of an image relative to each well center. Thus, this software corrects the X and Y coordinates stored in the images metadata to match the LIMS plate schemes. The default settings here are relating to the standard Greiner Sensoplates used in the HPA workflow and do not need to be changed, unless you use a different plate type.

- <b>Image type</b>: Obsolete setting since right now this software only accepts OPERA Phenix folders as input.

- <b>Crop image</b>: The OPERA Phenix records images at an unusual dimension, e.g., of 2160 x 2160 pixels. If you would like to crop the images to a smaller region fitting to your analysis pipeline, you can click this function and select an image width and height value. Accordingly, all processed images will be automatically cropped to that specified size.

- <b>Filepath to output directory</b>: This specifies where this software should save the processed images and create the output file directory.

- <b>Output style</b>: There are three different output options.
   - The first option (<b><i>LIMS style</i></b>) is tailored to upload the processed images to the HPA LIMS. In this option, a folder is created for each field of view and focal plane. This means that a z-stack is burst into individual folders for each z plane and that each z plane becomes an independent image including a metadata xml file.
   - The second option (<b><i>canonical OME tif style</i></b>) is tailored to image analysis software that loads OME tif files. One folder is created for a z-stack in which all channel and plane images from a z-stack are stored together.
   - The third option (<b><i>Memento style</i></b>) is tailored to create a tif folder structure that is compatible to further processing the files with the [TifCs_To_HPA-PNG-JPEG plugin](https://github.com/CellProfiling/TifCs_To_HPA-PNG-JPEG/), which can also scale the intensities and will output JPEG or PNG images suitable for upload to the annotation software [Memento](https://github.com/CellProfiling/memento). 
<p align="center">
   <img src="https://github.com/CellProfiling/HPA_Convert_OPERA_To_LIMS-OMETIF/assets/27991883/9e815a84-1551-4105-8c95-105aa4bb2a44" width=600>
</p>

- <b>Logging settings</b>: Leave these options to default settings unless you want to troubleshoot the translation of metadata. If you activate logging options, this software will print a lot of text into the LOG window that allows to see how metadata are translated. This printing however slows down the processing since the printing requires time and memory.

#### 2. Selecting the OPERA folders to be processed
- After you selected the settings and pressed OK, you will receive a new dialog that allows you to list input xml files to be processed.
<p align="center">
   <img src="https://github.com/CellProfiling/HPA_Convert_OPERA_To_LIMS-OMETIF/assets/27991883/17803341-567d-4bb1-9b56-7b3c3507b9a1" width=500>
</p>

- To add an OPERA folder to the to-process-list there, click select files individually, and enter the respective OPERA folder, the nested <i>images</i> folder, and then select the <i>Index.idx.xml</i> file in there.
<p align="center">
   <img src="https://github.com/CellProfiling/HPA_Convert_OPERA_To_LIMS-OMETIF/assets/27991883/a494e831-5d32-4a08-9739-eaaf81f2abd3" width=500>
</p>

- You can add as many folders as you like to the to-process-list. However, note that all outputs will be stored in the same output folder, so it might become complicated to further use the data if you do not want to have all input data merged into the same directory structure.
- When you are done with listing Index.idx.xml files, press <i>start processing</i>.
<p align="center">
   <img src="https://github.com/CellProfiling/HPA_Convert_OPERA_To_LIMS-OMETIF/assets/27991883/fa132ef2-fae5-41dc-8c5d-f1a498be6418" width=500>
</p>

#### 3. Following the plugin while processing
- As soon as you have clicked the <i>start processing</i> button, analysis has started. However, since in the first moment, the plugin opens the listed xml files to retrieve information about how many images are contained, it can take a while until you see the progress dialog popping up. As soon as initialization is finished and processing has started, you will see the progress dialog. Note: it can take time (from min to hours) until you reach the progress dialog, in case you have a huge data set to be processed.
<p align="center">
   <img src="https://github.com/CellProfiling/HPA_Convert_OPERA_To_LIMS-OMETIF/assets/27991883/b422ef56-a80f-43db-9b19-a315f60bc0b3" width=500>
</p>

- The progress dialog informs you about progress on the processing (which images still need to be done and which were done) and in the Log window, it informs you about errors or notifies you in case some metadata were not entirely clear and how the software reacted to it.
   - Here is a few common example notifications, that you do not need to worry about but are still good to notice:
      - `Task 1/90: There are images with different Z spacings available in this OPERA output file: [0.7, 0.79, 0.69, 0.6] with the observed frequencies of [349, 1, 8, 2].This program cannot guarantee accurate translation of Z step size information into the image calibration data stored in the .tif files.This program will save the most frequent observed Z step size (0.7 micron) in the OPERA file as a Z calibration value for all converted images.`
      - `Task 3/90:Physical size X in image OME metadata and original metadata disagree! Replaced image metadata physical X size with value from original OPERA XML metadata! Image OME data: 9.491683824710502E-8 m (Original entry0.09491683824710503 µm), XML Metadata:  9.491683824710504E-8 m (Original entry9.4916838247105038E-08 m). For comparison these values were converted to 18 digits as: 9.49168382471050200e-08 and 9.49168382471050400e-08.`
      - `Task 3/90:Physical size Y in image OME metadata and original metadata disagree! Replaced image metadata physical Y size with value from original OPERA XML metadata! Image OME data: 9.491683824710502E-8 m (Original entry0.09491683824710503 µm), XML Metadata:  9.491683824710504E-8 m (Original entry9.4916838247105038E-08 m). For comparison these values were converted to 18 digits as: 9.49168382471050200e-08 and 9.49168382471050400e-08.`
      - `Task 5/90: There are images with different Z spacings available in this OPERA output file: [0.7, 0.79, 0.69, 0.6] with the observed frequencies of [349, 1, 8, 2].This program cannot guarantee accurate translation of Z step size information into the image calibration data stored in the .tif files.This program will save the most frequent observed Z step size (0.7 micron) in the OPERA file as a Z calibration value for all converted images.`
      - A common message on Windows due to problems of java being allowed to delete files: `Task 4/90: Could not delete temporary folder E:\\OME Out\\temp_3\, please delete manually! 
Error message: Cannot delete file: E:\OME Out\temp_3\20230412 Sperm OPERA test -_4_Z0_C0.ome.tif
Error localized message: Cannot delete file: E:\OME Out\temp_3\20230412 Sperm OPERA test -_4_Z0_C0.ome.tif
Error cause: java.nio.file.FileSystemException: E:\OME Out\temp_3\20230412 Sperm OPERA test -_4_Z0_C0.ome.tif: The process cannot access the file because it is being used by another process.
Detailed message: 
 org.apache.commons.io.FileUtils.forceDelete(FileUtils.java:1344) 
 hpaConvertOperaToLimsOMETif_jnh.ConvertOperaToLimsOMETif_Main.forceDeleteDirectory(ConvertOperaToLimsOMETif_Main.java:2377) 
 hpaConvertOperaToLimsOMETif_jnh.ConvertOperaToLimsOMETif_Main.run(ConvertOperaToLimsOMETif_Main.java:2025) 
 ij.IJ.runUserPlugIn(IJ.java:217) 
 ij.IJ.runPlugIn(IJ.java:181) 
 ij.Executer.runCommand(Executer.java:137) 
 ij.Executer.run(Executer.java:66) 
 java.lang.Thread.run(Thread.java:750)`

- Wait until all processing is done, you can follow the progress by following the status bar progressing:
<p align="center">
   <img src="https://github.com/CellProfiling/HPA_Convert_OPERA_To_LIMS-OMETIF/assets/27991883/92bfc60a-7cd5-430a-8069-45777ad5e69a" width=500>
</p>

- On windows, when everything has been processed, you might receive a message telling you to manually delete temp folders that the software did not have permission to delete (see error noted above also). Delete those folders manully to free your disk space since they are neither important nor usable.
<p align="center">
   <img src="https://github.com/CellProfiling/HPA_Convert_OPERA_To_LIMS-OMETIF/assets/27991883/2360d3c2-a748-4baa-ac6f-256a2d6326e8" width=200>
</p>

- In the well folders you will find the data ready for your further use in LIMS, Memento, or other software.


### Updating the plugin version
Download the new version's .jar file from the [release page](https://github.com/CellProfiling/HPA_Convert_OPERA_To_LIMS-OMETIF/releases). Make sure FIJI is closed - if still open, close it. Next, locate the FIJI software file / folder on your computer and go on below depending on your OS.

#### Windows
In Windows or Linux, FIJI is a directory called FIJI.app. Enter this directory and navigate to the "plugins" folder and enter it. Find the old version of the HPA_Convert_OPERA_To_LIMS-OMETIF_JNH-X.X.X-SNAPSHOT.jar file and delete it. Then place the new plugin version in the "plugins" folder. Exit the FIJI.app folder. Start FIJI.

#### Mac
In Mac OS, FIJI is just a software file (FIJI.app). Right click on the FIJI icon (or hold option and do normal click on it), then select "Show Package Content". A folder will open, which contains the contents of the FIJI.app. Navigate to the "plugins" folder folder and enter it. Find the old version of the HPA_Convert_OPERA_To_LIMS-OMETIF_JNH-X.X.X-SNAPSHOT.jar file and delete it. Then place the new plugin version in the "plugins" folder . Exit the FIJI.app folder. Start FIJI.


---

(c) 2023-2024 J.N. Hansen, Cell Profiling group
