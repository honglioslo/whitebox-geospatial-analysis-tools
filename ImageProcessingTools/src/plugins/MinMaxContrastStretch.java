/*
 * Copyright (C) 2011-2012 Dr. John Lindsay <jlindsay@uoguelph.ca>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package plugins;

import java.util.Date;
import whitebox.geospatialfiles.WhiteboxRaster;
import whitebox.interfaces.WhiteboxPlugin;
import whitebox.interfaces.WhiteboxPluginHost;
/**
 * This tool performs a minimum-maximum contrast stretch on a raster image, which maps each grid cell value in the input raster image (z) onto a new scale that ranges from the user-specified lower-tail clip value (L) to the upper-tail clip value (U), with the specified number of tonal values (n), such that:
zn = (z - L) / (U - L) × n where zn is the output value.
 * @author Dr. John Lindsay email: jlindsay@uoguelph.ca
 */
public class MinMaxContrastStretch implements WhiteboxPlugin {

    private WhiteboxPluginHost myHost = null;
    private String[] args;
    /**
     * Used to retrieve the plugin tool's name. This is a short, unique name containing no spaces.
     * @return String containing plugin name.
     */
    @Override
    public String getName() {
        return "MinMaxContrastStretch";
    }
    /**
     * Used to retrieve the plugin tool's descriptive name. This can be a longer name (containing spaces) and is used in the interface to list the tool.
     * @return String containing the plugin descriptive name.
     */
    @Override
    public String getDescriptiveName() {
    	return "Min-Max Contrast Stretch";
    }
    /**
     * Used to retrieve a short description of what the plugin tool does.
     * @return String containing the plugin's description.
     */
    @Override
    public String getToolDescription() {
    	return "Performs a min-max contrast stretch on an input image.";
    }
    /**
     * Used to identify which toolboxes this plugin tool should be listed in.
     * @return Array of Strings.
     */
    @Override
    public String[] getToolbox() {
    	String[] ret = { "ImageEnhancement" };
    	return ret;
    }
    /**
     * Used to communicate feedback pop-up messages between a plugin tool and the main Whitebox user-interface.
     * @param feedback String containing the text to display.
     */
    private void showFeedback(String feedback) {
        if (myHost != null) {
            myHost.showFeedback(feedback);
        } else {
            System.out.println(feedback);
        }
    }
    /**
     * Used to communicate a return object from a plugin tool to the main Whitebox user-interface.
     * @return Object, such as an output WhiteboxRaster.
     */
    private void returnData(Object ret) {
        if (myHost != null) {
            myHost.returnData(ret);
        }
    }
    /**
     * Used to communicate a progress update between a plugin tool and the main Whitebox user interface.
     * @param progressLabel A String to use for the progress label.
     * @param progress Float containing the progress value (between 0 and 100).
     */
    private void updateProgress(String progressLabel, int progress) {
        if (myHost != null) {
            myHost.updateProgress(progressLabel, progress);
        } else {
            System.out.println(progressLabel + " " + progress + "%");
        }
    }
    /**
     * Used to communicate a progress update between a plugin tool and the main Whitebox user interface.
     * @param progress Float containing the progress value (between 0 and 100).
     */
    private void updateProgress(int progress) {
        if (myHost != null) {
            myHost.updateProgress(progress);
        } else {
            System.out.println("Progress: " + progress + "%");
        }
    }
    /**
     * Sets the arguments (parameters) used by the plugin.
     * @param args An array of string arguments.
     */
    @Override
    public void setArgs(String[] args) {
        this.args = args.clone();
    }
    /**
     * Sets the WhiteboxPluginHost to which the plugin tool is tied. This is the class
     * that the plugin will send all feedback messages, progress updates, and return objects.
     * @param host The WhiteboxPluginHost that called the plugin tool.
     */  
    @Override
    public void setPluginHost(WhiteboxPluginHost host) {
        myHost = host;
    }
    
    private boolean cancelOp = false;
    /**
     * Used to communicate a cancel operation from the Whitebox GUI.
     * @param cancel Set to true if the plugin should be canceled.
     */
    @Override
    public void setCancelOp(boolean cancel) {
        cancelOp = cancel;
    }
    
    private void cancelOperation() {
        showFeedback("Operation cancelled.");
        updateProgress("Progress: ", 0);
    }
    
    private boolean amIActive = false;
    /**
     * Used by the Whitebox GUI to tell if this plugin is still running.
     * @return a boolean describing whether or not the plugin is actively being used.
     */
    @Override
    public boolean isActive() {
        return amIActive;
    }
    
    /**
     * Used to execute this plugin tool.
     */
    @Override
    public void run() {
        amIActive = true;
        
        String inputHeader = null;
        String outputHeader = null;
        int row, col;
        double z;
        double noData;
        int progress;
        int i;
        double minVal, maxVal;
        int numBins = 1024;

        if (args.length <= 0) {
            showFeedback("Plugin parameters have not been set.");
            return;
        }
        
        inputHeader = args[0];
        outputHeader = args[1];
        minVal = Double.parseDouble(args[2]);
        maxVal = Double.parseDouble(args[3]);
        numBins = Integer.parseInt(args[4]);
        int numBinsLessOne = numBins - 1;
        
        // check to see that the inputHeader are not null.
        if ((inputHeader == null) || (outputHeader == null)) {
            showFeedback("One or more of the input parameters have not been set properly.");
            return;
        }
        
        try {
            
            WhiteboxRaster image = new WhiteboxRaster(inputHeader, "r");
            int nRows = image.getNumberRows();
            int nCols = image.getNumberColumns();
            noData = image.getNoDataValue();
            double scaleFactor = numBins / (maxVal - minVal);
            
            WhiteboxRaster output = new WhiteboxRaster(outputHeader, "rw", inputHeader, WhiteboxRaster.DataType.INTEGER, noData);
            output.setPreferredPalette(image.getPreferredPalette());
            
            double[] data = null;
            for (row = 0; row < nRows; row++) {
                data = image.getRowValues(row);
                for (col = 0; col < nCols; col++) {
                    if (data[col] != noData) {
                        z = (int)(data[col] - minVal) * scaleFactor;
                        if (z < 0) { z = 0; }
                        if (z > numBinsLessOne) { z = numBinsLessOne; }
                        output.setValue(row, col, z);
                    } else {
                        output.setValue(row, col, noData);
                    }
                }
                if (cancelOp) {
                    cancelOperation();
                    return;
                }
                progress = (int) (100f * row / (nRows - 1));
                updateProgress(progress);
            }
            
            image.close();

            output.addMetadataEntry("Created by the "
                    + getDescriptiveName() + " tool.");
            output.addMetadataEntry("Created on " + new Date());
            output.close();

            // returning a header file string displays the image.
            returnData(outputHeader);


        } catch (OutOfMemoryError oe) {
            myHost.showFeedback("An out-of-memory error has occurred during operation.");
        } catch (Exception e) {
            myHost.showFeedback("An error has occurred during operation. See log file for details.");
            myHost.logException("Error in " + getDescriptiveName(), e);
            showFeedback(e.getMessage());
        } finally {
            updateProgress("Progress: ", 0);
            // tells the main application that this process is completed.
            amIActive = false;
            myHost.pluginComplete();
        }
    }
    
}
