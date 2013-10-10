package inra.watershed.process;

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


import java.util.Collections;
import java.util.LinkedList;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;

public class WatershedTransform3D 
{
	ImagePlus inputImage = null;
	ImagePlus seedImage = null;
	ImagePlus maskImage = null;
	
	public WatershedTransform3D(
			final ImagePlus input,
			final ImagePlus seed,
			final ImagePlus mask )
	{
		this.inputImage = input;
		this.seedImage = seed;
		this.maskImage = mask;
	}
	
	public ImagePlus apply()
	{
		final ImageStack inputStack = inputImage.getStack();
	    final int size1 = inputStack.getWidth();
	    final int size2 = inputStack.getHeight();
	    final int size3 = inputStack.getSize();
	    
		// list of original voxels values and corresponding coordinates
		LinkedList<VoxelRecord> voxelList = new LinkedList<VoxelRecord>();
		
		final int[][][] tabLabels = new int[ size1 ][ size2 ][ size3 ]; 
		
		// Make list of voxels and sort it in ascending order
		IJ.showStatus( "Extracting voxel values..." );
		if( null != maskImage )
		{
			final ImageStack mask = maskImage.getImageStack();
			for (int k = 0; k < size3; ++k)	
			{
				IJ.showProgress(k, size3);
				for (int i = 0; i < size1; ++i)
					for (int j = 0; j < size2; ++j)

						if( mask.getVoxel( i, j, k ) > 0 )
						{
							voxelList.addLast( new VoxelRecord( i, j, k, inputStack.getVoxel( i, j, k )));
							tabLabels[i][j][k] = (int) seedImage.getStack().getVoxel(i, j, k);
						}
			}
			IJ.showProgress(1.0);
		}
		else
		{
			for (int k = 0; k < size3; ++k)
			{
				IJ.showProgress(k, size3);
				for (int i = 0; i < size1; ++i)
					for (int j = 0; j < size2; ++j)

					{
						voxelList.addLast( new VoxelRecord( i, j, k, inputStack.getVoxel( i, j, k )));
						tabLabels[i][j][k] = (int) seedImage.getStack().getVoxel(i, j, k);
					}
			}
			IJ.showProgress(1.0);
		}
		
		final long t1 = System.currentTimeMillis();		
		IJ.log("  Sorting voxels by value..." );
		IJ.showStatus("Sorting voxels by value...");
		Collections.sort( voxelList );
		final long t2 = System.currentTimeMillis();
		IJ.log("  Sorting took " + (t2-t1) + "ms.");
			    
		// Watershed
	    boolean found = false;	    

	    final long start = System.currentTimeMillis();

	    boolean change = true;
	    while ( voxelList.isEmpty() == false && change)
	    {
	    	change = false;
			final int count = voxelList.size();
	      	IJ.log( "  Flooding " + count + " voxels..." );
	      	IJ.showStatus("Flooding " + count + " voxels...");

			for (int p = 0; p < count; ++p)
	      	{
				IJ.showProgress(p, count);
	       		final VoxelRecord voxelRecord = voxelList.removeFirst();
	       		final int[] coord = voxelRecord.getCoordinates();
	       		final int i = coord[0];
	       		final int j = coord[1];
	       		final int k = coord[2];
	       		
	       		// If the voxel is unlabeled
				if( tabLabels[ i ][ j ][ k ] == 0 )
	       		{
			       	found = false;
			       	double voxelValue = inputStack.getVoxel( i, j, k );
			       	// Look in neighborhood for labeled voxels with
			       	// smaller or equal original value
			       	for (int u = i-1; u <= i+1; ++u) 
			        	for (int v = j-1; v <= j+1; ++v) 
					        for (int w = k-1; w <= k+1; ++w) 
	          				{
								if ( u >= 0 && u < size1 && v >= 0 && v < size2 && w >= 0 && w < size3 )
								{
						            if ( tabLabels[u][v][w] != 0 && inputStack.getVoxel(u,v,w) <= voxelValue )
	    				    	    {
										tabLabels[i][j][k] = tabLabels[u][v][w];
		              					voxelValue = inputStack.getVoxel(u,v,w);
										found = true;
	        					    }
								}
	          				}

					if ( found == false )    
						voxelList.addLast( voxelRecord );
					else
						change = true;
	      		}
	        }
		}

		final long end = System.currentTimeMillis();
		IJ.log("  Flooding took: " + (end-start) + "ms");
		
		// Create result label image
		ImageStack labelStack = seedImage.duplicate().getStack();
	    
	    for (int i = 0; i < size1; ++i)
	      for (int j = 0; j < size2; ++j)
	        for (int k = 0; k < size3; ++k)
	            labelStack.setVoxel( i, j, k, tabLabels[i][j][k] );
	    return new ImagePlus( "watershed", labelStack );
	}
}
