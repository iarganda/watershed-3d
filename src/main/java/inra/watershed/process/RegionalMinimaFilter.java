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

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Prefs;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import ij.util.ThreadUtil;



/**
 * Class to detect the region minima from a 3D image gradient
 * with or without a binary mask. Refactored from Axel Poulet's version.
 * 
 * @author Ignacio Arganda-Carreras, Philippe Andrey, Axel Poulet 
 */

public class RegionalMinimaFilter implements PlugInFilter
{
	/** image to process */
	private ImagePlus input;
	/** table to stock the binary mask if is necessary */
	private double tabMask[][][] = null;


	/**
	 * Setup method to be used as plugin filter
	 * @param arg unused argument
	 * @param imagePlusInput input image
	 * @return 0
	 */

	public int setup(String arg, ImagePlus imagePlusInput)
	{
		this.input = imagePlusInput;
		//return DOES_8G;// + STACK_REQUIRED;
		return 0;
	}

	/**
	 * To run the plugin with imageJ
	 * 
	 * @param imageProcessor
	 */
	public void run(ImageProcessor imageProcessor){   apply(); }

	/**
	 * 
	 * Method used to detect the regional minima on an image.
	 * All the regional minima voxels will be output as 1, while
	 * the rest of voxels will be 0 (multi-threaded).
	 * 
	 * @return regional minima binary image
	 */
	public ImagePlus apply()
	{
		final int width = input.getWidth();
		final int height = input.getHeight();
		final int depth = input.getStackSize();

		//final ImagePlus imageOutput = input.duplicate();
		//final ImageStack imageStackOutput = imageOutput.getStack();

		final ImageStack inputStack = input.getStack();
		
		final ImagePlus binaryOutput = input.duplicate();
		final ImageStack binaryStackOutput = binaryOutput.getStack();

		// Initialize binary output image
		// Binarize output image
		for (int k = 0; k < depth; ++k)
			for (int i = 0; i < width; ++i)
				for (int j = 0; j < height; ++j)	        	  
					binaryStackOutput.setVoxel(i, j, k, 1);

		// Apply 3x3x3 minimum filter
		IJ.log("   Minimum filtering...");
		final long t0 = System.currentTimeMillis();
		final double[][][] localMinValues = filterMin3D( input );
		if( null == localMinValues )
			return null;
		final long t1 = System.currentTimeMillis();
		IJ.log("   Filtering took " + (t1-t0) + " ms.");
		
		// find regional minima
		IJ.showStatus( "Finding regional minima..." );
		
		final AtomicInteger ai = new AtomicInteger(0);
        final int n_cpus = Prefs.getThreads();
        
        final int dec = (int) Math.ceil((double) depth / (double) n_cpus);
        
        Thread[] threads = ThreadUtil.createThreadArray(n_cpus);
        for (int ithread = 0; ithread < threads.length; ithread++) 
        {
            threads[ithread] = new Thread() {
                public void run() {
                	for (int k = ai.getAndIncrement(); k < n_cpus; k = ai.getAndIncrement()) 
                	{
                		int zmin = dec * k;
                		int zmax = dec * ( k + 1 );
                		if (zmin<0)
                            zmin = 0;
                        if (zmax > depth)
                            zmax = depth;
                        
                        findMinimaRange( zmin, zmax, inputStack,
                				binaryStackOutput, localMinValues );
                		
                    }
                }
            };
        }
        ThreadUtil.startAndJoin(threads);
		

		IJ.showProgress(1.0);
		
		//(new ImagePlus("valued minima", imageStackOutput)).show();
		
		return new ImagePlus("regional-minima-" + input.getTitle(), binaryStackOutput);
	} //apply
	
	/**
	 * 
	 * Method used to detect the regional minima on an image.
	 * All the regional minima voxels will be output as 1, while
	 * the rest of voxels will be 0 (multi-threaded).
	 * 
	 * @return regional minima binary image
	 */
	public ImagePlus applyWithMask()
	{
		final int width = input.getWidth();
		final int height = input.getHeight();
		final int depth = input.getStackSize();

		//final ImagePlus imageOutput = input.duplicate();
		//final ImageStack imageStackOutput = imageOutput.getStack();

		final ImageStack inputStack = input.getStack();
		
		final ImagePlus binaryOutput = input.duplicate();
		final ImageStack binaryStackOutput = binaryOutput.getStack();

		// Initialize binary output image
		// Binarize output image
		for (int k = 0; k < depth; ++k)
			for (int i = 0; i < width; ++i)
				for (int j = 0; j < height; ++j)	        	  
					binaryStackOutput.setVoxel(i, j, k, 1);

		// Apply 3x3x3 minimum filter
		IJ.log("   Minimum filtering...");
		final long t0 = System.currentTimeMillis();
		final double[][][] localMinValues = filterMin3D( input );
		if( null == localMinValues )
			return null;
		final long t1 = System.currentTimeMillis();
		IJ.log("   Filtering took " + (t1-t0) + " ms.");
		
		// find regional minima
		IJ.showStatus( "Finding regional minima..." );
		
		final AtomicInteger ai = new AtomicInteger(0);
        final int n_cpus = Prefs.getThreads();
        
        final int dec = (int) Math.ceil((double) depth / (double) n_cpus);
        
        Thread[] threads = ThreadUtil.createThreadArray(n_cpus);
        for (int ithread = 0; ithread < threads.length; ithread++) 
        {
            threads[ithread] = new Thread() {
                public void run() {
                	for (int k = ai.getAndIncrement(); k < n_cpus; k = ai.getAndIncrement()) 
                	{
                		int zmin = dec * k;
                		int zmax = dec * ( k + 1 );
                		if (zmin<0)
                            zmin = 0;
                        if (zmax > depth)
                            zmax = depth;
                        
                        findMinimaRange( zmin, zmax, inputStack,
                				binaryStackOutput, localMinValues, tabMask );
                		
                    }
                }
            };
        }
        ThreadUtil.startAndJoin(threads);
		

		IJ.showProgress(1.0);
		
		//(new ImagePlus("valued minima", imageStackOutput)).show();
		
		return new ImagePlus("regional-minima-" + input.getTitle(), binaryStackOutput);
	} //apply
	

	/**
	 * Find regional minima in a range of slices
	 * @param zmin minimum slice to process (zmin >= 0)
	 * @param zmax maximum slice to process (zmax < depth)
	 * @param inputStack original stack
	 * @param binaryStackOutput output stack with binary values (1s for regional minima)
	 * @param localMinValues filtered original values (by a 3x3x3 minimum filter)
	 */
	private void findMinimaRange(
			final int zmin, 
			final int zmax, 
			final ImageStack inputStack,
			final ImageStack binaryStackOutput, 
			double[][][] localMinValues) 
	{
		final int width = inputStack.getWidth();
		final int height = inputStack.getHeight();
		final int depth = inputStack.getSize();
		int kcurrent;
		int icurrent;
		int jcurrent;
		final LinkedList<VoxelRecord> voxelList = new LinkedList<VoxelRecord>();
		
		for (int k = zmin; k < zmax; ++k)
		{
			if (zmin==0) 
				IJ.showProgress(k+1, zmax);
			
			for (int i = 0; i < width; ++i)
				for (int j = 0; j < height; ++j)
				{
					double currentValue = inputStack.getVoxel( i, j, k );
					double currentLabel = binaryStackOutput.getVoxel( i, j, k );
					double currentValueMin = localMinValues[ i ][ j ][ k ];
					if ( currentLabel > 0 && currentValue != currentValueMin )
					{
						//imageStackOutput.setVoxel(i, j, k, 0);
						binaryStackOutput.setVoxel(i, j, k, 0);
						
						voxelList.addLast( new VoxelRecord( i, j, k, 0 ) );
						while ( voxelList.size() > 0 )
						{
							VoxelRecord voxelRecord = voxelList.removeFirst();
							icurrent = voxelRecord.getI();
							jcurrent = voxelRecord.getJ();
							kcurrent = voxelRecord.getK();

							for (int kk = kcurrent - 1; kk <= kcurrent+1; ++kk)
								for (int ii = icurrent - 1; ii <= icurrent+1; ++ii)
									for (int jj = jcurrent - 1; jj <= jcurrent+1; ++jj)
										if ( kk >= 0 && kk < depth && ii >= 0 && ii < width && jj >= 0 && jj < height )
											if ( inputStack.getVoxel( ii, jj, kk ) == currentValue && binaryStackOutput.getVoxel( ii, jj, kk ) > 0)
											{
												binaryStackOutput.setVoxel( ii, jj, kk, 0 );
												voxelList.addLast( new VoxelRecord(ii, jj, kk, 0) );
											}
						}
					}
				}
		}
	}
	
	/**
	 * Find regional minima in a range of slices
	 * @param zmin minimum slice to process (zmin >= 0)
	 * @param zmax maximum slice to process (zmax < depth)
	 * @param inputStack original stack
	 * @param binaryStackOutput output stack with binary values (1s for regional minima)
	 * @param localMinValues filtered original values (by a 3x3x3 minimum filter)
	 * @param tabMask the binary mask to select the are of interest
	 */
	private void findMinimaRange(
			final int zmin, 
			final int zmax, 
			final ImageStack inputStack,
			final ImageStack binaryStackOutput, 
			final double[][][] localMinValues,
			final double[][][] tabMask) 
	{
		final int width = inputStack.getWidth();
		final int height = inputStack.getHeight();
		final int depth = inputStack.getSize();
		int kcurrent;
		int icurrent;
		int jcurrent;
		final LinkedList<VoxelRecord> voxelList = new LinkedList<VoxelRecord>();
		
		for (int k = zmin; k < zmax; ++k)
		{
			if (zmin==0) 
				IJ.showProgress(k+1, zmax);
			
			for (int i = 0; i < width; ++i)
				for (int j = 0; j < height; ++j)
				{
					double currentValue = inputStack.getVoxel( i, j, k );
					double currentLabel = binaryStackOutput.getVoxel( i, j, k );
					double currentValueMin = localMinValues[ i ][ j ][ k ];
					if ( currentLabel > 0 && currentValue != currentValueMin )
					{
						//imageStackOutput.setVoxel(i, j, k, 0);
						binaryStackOutput.setVoxel(i, j, k, 0);
						
						voxelList.addLast( new VoxelRecord( i, j, k, 0 ) );
						while ( voxelList.size() > 0 )
						{
							VoxelRecord voxelRecord = voxelList.removeFirst();
							icurrent = voxelRecord.getI();
							jcurrent = voxelRecord.getJ();
							kcurrent = voxelRecord.getK();

							for (int kk = kcurrent - 1; kk <= kcurrent+1; ++kk)
								for (int ii = icurrent - 1; ii <= icurrent+1; ++ii)
									for (int jj = jcurrent - 1; jj <= jcurrent+1; ++jj)
										if ( kk >= 0 && kk < depth && ii >= 0 && ii < width && jj >= 0 && jj < height && tabMask[ii][jj][kk] > 0 )
											if ( inputStack.getVoxel( ii, jj, kk ) == currentValue && binaryStackOutput.getVoxel( ii, jj, kk ) > 0)
											{
												binaryStackOutput.setVoxel( ii, jj, kk, 0 );
												voxelList.addLast( new VoxelRecord(ii, jj, kk, 0) );
											}
						}
					}
				}
		}
	}
	

	/**
	 * Filter minimum in 3D with a neighboring 3
	 */

	double[][][] filterMin3DWithMask(ImagePlus input)
	{
		int size1 = input.getWidth();
		int size2 = input.getHeight();
		int size3 = input.getStackSize();
		ImageStack imageStack= input.getStack();

		double [][][] localMinValues = new double[size1][size2][size3];

		IJ.showStatus("Minimum filter 3x3x3...");
		
		for (int k=0; k<size3; k++)
		{
			IJ.showProgress(k, size3);		
			for (int i=0; i<size1; i++)
				for (int j=0; j<size2; j++)
				{
					double minValue = imageStack.getVoxel(i, j, k);
					for (int ii = i-1; ii <= i+1; ++ii)             	
						for (int jj = j-1; jj <= j+1; ++jj)
							for (int kk = k-1; kk <= k+1; ++kk)
								if ( ii >= 0 && ii < size1 && jj >= 0 && jj < size2 && kk >= 0 && kk < size3 )
									if (imageStack.getVoxel(ii, jj, kk)<minValue  && tabMask[i][j][k]>0 && tabMask[ii][jj][kk] > 0)
										minValue = imageStack.getVoxel(ii, jj, kk);

					localMinValues[i][j][k] = minValue;
				}
		}
		IJ.showProgress( 1.0 );
		return localMinValues;
	}//filterMin3DWithMask


	/**
	 * Filter minimum in 3D with a neighboring 3
	 */
	double[][][] filterMin3D( final ImagePlus input )
	{
		final int size1 = input.getWidth();
		final int size2 = input.getHeight();
		final int size3 = input.getStackSize();
		final ImageStack stack= input.getStack();

		final double [][][] localMinValues = new double[size1][size2][size3];

		IJ.showStatus("Minimum filter 3x3x3...");
		
		final AtomicInteger ai = new AtomicInteger(0);
        final int n_cpus = Prefs.getThreads();
        
        final int dec = (int) Math.ceil((double) stack.getSize() / (double) n_cpus);
        Thread[] threads = ThreadUtil.createThreadArray(n_cpus);
        for (int ithread = 0; ithread < threads.length; ithread++) 
        {
            threads[ithread] = new Thread() {
                public void run() {
                	for (int k = ai.getAndIncrement(); k < n_cpus; k = ai.getAndIncrement()) 
                	{
                		int zmin = dec * k;
                		int zmax = dec * ( k + 1 );
                		if (zmin<0)
                            zmin = 0;
                        if (zmax>stack.getSize())
                            zmax = stack.getSize();
                        
                        min3D( stack, zmin, zmax, localMinValues );
                		
                    }
                }
            };
        }
        ThreadUtil.startAndJoin(threads);
		
		
		IJ.showProgress( 1.0 );
		
		return localMinValues;
	}//filterMin3D

	/**
	 * Apply minimum 3D filter to a set of slices in a stack
	 * @param imageStack input stack
	 * @param zmin first slice to process (>=0)
	 * @param zmax maximum slice to process (< num of slices)
	 * @param localMinValues 3D matrix to fill with the results
	 */
	void min3D(ImageStack imageStack, int zmin, int zmax, double[][][] localMinValues)
	{
		final int size1 = input.getWidth();
		final int size2 = input.getHeight();
		final int size3 = input.getStackSize();
		
		for( int k = zmin; k<zmax; k ++ )
		{
			if (zmin==0) 
				IJ.showProgress(k+1, zmax);
			final ImageProcessor ip = imageStack.getProcessor( k + 1 );
			for (int i=0; i<size1; i++)
				for (int j=0; j<size2; j++)
				{
					double minValue = ip.getf( i, j );
					for (int ii = i-1; ii <= i+1; ++ii)             	
						for (int jj = j-1; jj <= j+1; ++jj)
							for (int kk = k-1; kk <= k+1; ++kk)
								if ( ii >= 0 && ii < size1 && 
									 jj >= 0 && jj < size2 && 
									 kk >= 0 && kk < size3 ) 
									 {
										final double value = imageStack.getVoxel( ii, jj, kk );
										if( value < minValue )
											minValue = value;
									 }
					localMinValues[i][j][k] = minValue;
				}
		}
	}

	/**
	 * Initialize a matrix of a binary mask to search the minima regions in the mask
	 * @param tab binary mask
	 */

	public void setMask (double tab[][][])
	{
		tabMask=tab;
	} //setMask

	/**
	 * Initialize a matrix of a binary mask to search the minima regions in the mask
	 * @param mask Binary image 
	 */
	public void setMask (ImagePlus mask)
	{
		ImageStack labelPlus = mask.getStack();
		final int size1 = labelPlus.getWidth();
		final int size2 = labelPlus.getHeight();
		final int size3 = labelPlus.getSize();
		tabMask = new double[size1][size2][size3];
		int i, j, k;

		for (i = 0; i < size1; ++i)
			for (j = 0; j < size2; ++j)
				for (k = 0; k < size3; ++k)
					tabMask[i][j][k] = labelPlus.getVoxel(i, j, k);
	} // setMask
}// class
