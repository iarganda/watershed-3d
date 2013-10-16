package inra.watershed.plugin;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import inra.watershed.process.GeodesicReconstruction;

public class GeodesicReconstruction_3D implements PlugIn
{
	static int markerIndex = 0;
	static int maskIndex = 1;
	static int operationIndex = 0;
	
	@Override
	public void run(String arg) 
	{
		int numImages = WindowManager.getImageCount();
		
		if( numImages == 0 )
		{
			IJ.error( "Geodesic reconstruction 3D", 
					"At least one image needs to be open to run Geodesic reconstruction in 3D" );
			return;
		}
		
        String[] names = new String[ numImages ];
        
        for (int i = 0; i < numImages; i++) 
            names[ i ] = WindowManager.getImage(i + 1).getShortTitle();      
       
        GenericDialog gd = new GenericDialog("Geodesic reconstruction 3D");

        if( maskIndex >= numImages)
        	maskIndex = 0;
        if( markerIndex >= numImages)
        	markerIndex = 0;
        
        
        gd.addChoice( "Marker", names, names[ markerIndex ] );
        gd.addChoice( "Mask", names, names[ maskIndex ] );
        String[] operations = new String[]{ "reconstruction by dilation" };
        gd.addChoice( "Geodesic operation", operations, operations[ operationIndex ] );

        gd.showDialog();
        
        if ( gd.wasOKed() )
        {
        	markerIndex = gd.getNextChoiceIndex();
            maskIndex = gd.getNextChoiceIndex();
            operationIndex = gd.getNextChoiceIndex();
            
            final ImagePlus marker = WindowManager.getImage( markerIndex + 1 );
            final ImagePlus mask = WindowManager.getImage( maskIndex + 1 );
            
            if( null == marker || null == mask )
            {
            	IJ.error( "Geodesic reconstruction 3D", 
    					"Error when opening input images" );
            	return;
            }
            
            GeodesicReconstruction gr = new GeodesicReconstruction( marker, mask );
            ImagePlus output = null;
            
            final long start = System.currentTimeMillis();
            
            switch( operationIndex )
            {
            	case 0:
            		output = gr.reconstructionByDilationHybrid();
            		break;
            	default:            		
            }
            if ( null != output )
            	output.show();
            
            final long end = System.currentTimeMillis();
            IJ.log("Geodesic operation took " + (end-start) + " ms.");
        }
		
	}

}
