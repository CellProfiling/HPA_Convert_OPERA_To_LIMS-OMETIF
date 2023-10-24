# HPA_Convert_OPERA_To_LIMS-OMETIF
 An ImageJ plugin to convert Opera Phenix image output folders to OME Tif format suitable for the HPA LIMS system. See more instructions under [User instructions](https://github.com/CellProfiling/HPA_Convert_OPERA_To_LIMS-OMETIF/edit/main/README.md#user-instructions) below.
 
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

In the output directory, a folder is created for each well. 
<p align="center">
   <img src="https://github.com/CellProfiling/HPA_Convert_OPERA_To_LIMS-OMETIF/assets/27991883/bb033163-9405-4d2a-aff7-efd875919119" width=250>
</p>

And within each well folder, a folder is created for each field of view or even image plane (depending on the setting, see further done). 
<p align="center">
   <img src="https://github.com/CellProfiling/HPA_Convert_OPERA_To_LIMS-OMETIF/assets/27991883/bf56016a-8e50-40ff-a6d5-5b6c8bd20ce4" width=350>
</p>

In each of these folders, there is an ome.tif file for each channel image and a folder with an xml file containing the metadata relevant for this particular image.
<p align="center">
   <img src="https://github.com/CellProfiling/HPA_Convert_OPERA_To_LIMS-OMETIF/assets/27991883/f9c087ae-0c2e-4a06-9008-7a0ee1a93f34" width=500>
</p>
<p align="center">
   <img src="https://github.com/CellProfiling/HPA_Convert_OPERA_To_LIMS-OMETIF/assets/27991883/052effed-8aa0-40d3-9abf-4bb40904226b" width=500>
</p>

Beyond the storage of metadata in that folder, there are also all metadata converted and written into OME XML annotations stored in the .ome.tif files itself.

### Installation = Getting started
This is a plugin for the ImageJ distribution FIJI. It requires the installation of Fiji on your computer (see below) to run this software. ImageJ/FIJI does not require any specific hardware, can run on Linux, Windows, and Mac, and can also run on low-performing computers. However, a RAM is required that allows to load one image sequence that you aim to analyze into your RAM at least twice. Our personal experience was that running this plugin in FIJI  requires at least about 2 GB in RAM. For geeks: ImageJ does not require any specific graphics card, the speed of the analysis depends mainly on the processor speed. 

#### Installing FIJI / ImageJ to your computer
FIJI is open-source, freely available software that can be downloaded [here](https://imagej.net/software/fiji/downloads). To install ImageJ / FIJI, you only need to download the distribution of your choice and fitting to your Operating system and extract the software directory from the downloaded archive. The resulting folder can be placed anywhere on your computer (where you have read and write permissions). 

On Mac OS, do not place the ImageJ / FIJI application into the *Applications* folder. Instead place it somewhere else than in the *Applications* folder, i.e. to the *desktop* or the *documents* directory. Thereby you can avoid a collision with a security feature by Mac OS, that might otherwise trigger software failures. If Mac OS does not allow you to launch FIJI by clicking the FIJI.app directory due to security concerns, run FIJI by right click (or holding option while clicking) to open more options for the file and click "open".

#### Installing the *HPA_Convert_OPERA_To_LIMS-OMETIF* FIJI plugin
- Download the .jar file from the latest software release listed on the [releases page in this repository](https://github.com/CellProfiling/HPA_Convert_OPERA_To_LIMS-OMETIF/releases). 
- Launch ImageJ and install the plugin by drag and drop of the downloaded .jar file into the ImageJ window (red marked region in the screenshot below):
<p align="center">
   <img src="https://user-images.githubusercontent.com/27991883/201358020-c3685947-b5d8-4127-88ec-ce9b4ddf0e56.png" width=500>
</p>
- Confirm the installation by pressing save in the upcoming dialog.
- Next, FIJI requires to be restarted (close it and start it again).
- You should now be able to start the plugin via the menu entry <i>Plugins > Cell Profiling > Convert Tif folder from Opera Phenix to LIMS-like ome.tif (...)</i>

### 
- 

---

To be continued...
