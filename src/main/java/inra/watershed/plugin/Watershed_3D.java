package inra.watershed.plugin;

/**
 *
 * License: GPL
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * Authors: Ignacio Arganda-Carreras, Philippe Andrey, Axel Poulet
 */

import ij.IJ;
//import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import inra.watershed.process.ComponentLabelling;
import inra.watershed.process.RegionalMinimaFilter;
import inra.watershed.process.WatershedTransform3D;


/**
 * ProcessPixels
 *
 * A template for processing each pixel of either
 * GRAY8, GRAY16, GRAY32 or COLOR_RGB images.
 *
 * @author The Fiji Team
 */
public class Watershed_3D implements PlugIn 
{



	/**
	 * Process an image.
	 *
	 * Please provide this method even if {@link ij.plugin.filter.PlugInFilter} does require it;
	 * the method {@link ij.plugin.filter.PlugInFilter#run(ij.process.ImageProcessor)} can only
	 * handle 2-dimensional data.
	 *
	 * If your plugin does not change the pixels in-place, make this method return the results and
	 * change the {@link #setup(java.lang.String, ij.ImagePlus)} method to return also the
	 * <i>DOES_NOTHING</i> flag.
	 *
	 * @param image the image (possible multi-dimensional)
	 */
	public ImagePlus process(ImagePlus input, ImagePlus seed) 
	{
		final long start = System.currentTimeMillis();
		
		IJ.log("-> Running regional minima filter...");
		
		RegionalMinimaFilter rmf = new RegionalMinimaFilter();
		rmf.setup("", seed);
		ImagePlus regionalMinima = rmf.apply();
		
		//regionalMinima.show();
		
		final long step1 = System.currentTimeMillis();
		
		IJ.log( "Regional minima took " + (step1-start) + " ms.");
		
		IJ.log("-> Running connected components...");
		
		ComponentLabelling cl = new ComponentLabelling( regionalMinima );
		ImagePlus connectedMinima = cl.apply();
		
		//connectedMinima.show();
		
		final long step2 = System.currentTimeMillis();
		IJ.log( "Connected components took " + (step2-step1) + " ms.");
		
		IJ.log("-> Running watershed...");
		
		WatershedTransform3D wt = new WatershedTransform3D(input, connectedMinima, null);
		ImagePlus resultImage = wt.apply();
		
		final long end = System.currentTimeMillis();
		IJ.log( "Watershed 3d took " + (end-step2) + " ms.");
		
		return resultImage;
				
	}

	public void showAbout() {
		IJ.showMessage("ProcessPixels",
			"a template for processing each pixel of an image"
		);
	}
	

	@Override
	public void run(String arg0) 
	{
		int nbima = WindowManager.getImageCount();
        String[] names = new String[ nbima ];

        for (int i = 0; i < nbima; i++) 
        {
            names[ i ] = WindowManager.getImage(i + 1).getShortTitle();
            
        }
        
        GenericDialog dia = new GenericDialog("Watershed 3D");

        int spot = 0;
        int seed = nbima > 1 ? nbima - 1 : 0;
        
        dia.addChoice("Input image", names, names[spot]);
        dia.addChoice("Image to seed from", names, names[seed]);

        dia.showDialog();
        
        if (dia.wasOKed()) 
        {
            spot = dia.getNextChoiceIndex();
            seed = dia.getNextChoiceIndex();

            ImagePlus inputImage = WindowManager.getImage(spot + 1);
            ImagePlus seedImage = WindowManager.getImage(seed + 1);
            
            process( inputImage, seedImage ).show();;
        }

        
	}
	

	/**
	 * Main method for debugging.
	 *
	 * For debugging, it is convenient to have a method that starts ImageJ, loads an
	 * image and calls the plugin, e.g. after setting breakpoints.
	 *
	 * @param args unused
	 */
	
/*	
	public static void main(String[] args) {
		// set the plugins.dir property to make the plugin appear in the Plugins menu
		Class<?> clazz = Watershed_3D.class;
		String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
		String pluginsDir = url.substring(5, url.length() - clazz.getName().length() - 6);
		System.setProperty("plugins.dir", pluginsDir);

		// start ImageJ
		new ImageJ();

		// open the Clown sample
		ImagePlus image = IJ.openImage("http://imagej.net/images/clown.jpg");
		image.show();

		// run the plugin
		IJ.runPlugIn(clazz.getName(), "");
	}
*/	
	
}
