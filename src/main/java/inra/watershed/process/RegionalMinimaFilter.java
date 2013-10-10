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

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;



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
	 * Method used to detect the regional minima on an image.
	 * All the regional minima voxels will be output as 1, while
	 * the rest of voxels will be 0.
	 * 
	 * @param imagePlus Image to be processed
	 * @return regional minima binary image
	 */
	public ImagePlus applyWithMask()
	{
		final double width = input.getWidth();
		final double height = input.getHeight();
		final double depth = input.getStackSize();
		int kcurrent, icurrent, jcurrent;

		LinkedList<VoxelRecord> voxelList = new LinkedList<VoxelRecord>();

		final ImagePlus imageOutput = input.duplicate();
		final ImageStack imageStackOutput = imageOutput.getStack();

		final ImagePlus binaryOutput = input.duplicate();
		final ImageStack binaryStackOutput = imageOutput.getStack();

		// Initialize binary output image
		// Binarize output image
		for (int k = 0; k < depth; ++k)
			for (int i = 0; i < width; ++i)
				for (int j = 0; j < height; ++j)	        	  
					binaryStackOutput.setVoxel(i, j, k, 1);

		// Apply 3x3x3 minimum filter
		double[][][] localMinValues = filterMin3DWithMask( input );
		if( null == localMinValues )
			return null;

		// find regional minima
		for (int k = 0; k < depth; ++k)
			for (int i = 0; i < width; ++i)
				for (int j = 0; j < height; ++j)
				{
					double currentValue = imageStackOutput.getVoxel(i,j,k);
					double currentValueMin = localMinValues[i][j][k];
					if ( currentValue > 0 &&  currentValue != currentValueMin && tabMask[i][j][k]>0)
					{
						imageStackOutput.setVoxel(i, j, k, 0);
						voxelList.addLast( new VoxelRecord(i, j, k, 0) );
						while ( voxelList.size() > 0 )
						{
							VoxelRecord voxelRecord = voxelList.removeFirst();
							icurrent = voxelRecord.getI();
							jcurrent = voxelRecord.getJ();
							kcurrent = voxelRecord.getK();

							for (int kk = kcurrent - 1; kk <= kcurrent+1; ++kk)
								for (int ii = icurrent - 1; ii <= icurrent+1; ++ii)
									for (int jj = jcurrent - 1; jj <= jcurrent+1; ++jj)
										if ( kk >= 0 && kk < depth && ii >= 0 && ii < width && jj >= 0 && jj < height  && tabMask[ii][jj][kk]>0)
											if ( imageStackOutput.getVoxel(ii,jj,kk) == currentValue )
											{
												imageStackOutput.setVoxel(ii, jj, kk, 0);
												binaryStackOutput.setVoxel(ii, jj, kk, 0);
												voxelList.addLast( new VoxelRecord(ii, jj, kk, 0) );
											}
						}
					}
				}
		
		binaryOutput.setTitle("regional-minima-" + input.getTitle());
		return binaryOutput;
	} //applywithMask



	/**
	 * 
	 * Method used to detect the regional minima on an image.
	 * All the regional minima voxels will be output as 1, while
	 * the rest of voxels will be 0.
	 * 
	 * @return regional minima binary image
	 */
	public ImagePlus apply()
	{
		final int width = input.getWidth();
		final int height = input.getHeight();
		final int depth = input.getStackSize();
		int kcurrent, icurrent, jcurrent;

		LinkedList<VoxelRecord> voxelList = new LinkedList<VoxelRecord>();

		final ImagePlus imageOutput = input.duplicate();
		final ImageStack imageStackOutput = imageOutput.getStack();

		final ImagePlus binaryOutput = input.duplicate();
		final ImageStack binaryStackOutput = binaryOutput.getStack();

		// Initialize binary output image
		// Binarize output image
		for (int k = 0; k < depth; ++k)
			for (int i = 0; i < width; ++i)
				for (int j = 0; j < height; ++j)	        	  
					binaryStackOutput.setVoxel(i, j, k, 1);

		// Apply 3x3x3 minimum filter
		double[][][] localMinValues = filterMin3D( input );
		if( null == localMinValues )
			return null;

		// find regional minima
		IJ.showStatus( "Finding regional minima..." );
		
		for (int k = 0; k < depth; ++k)
		{
			IJ.showProgress(k, depth);
			for (int i = 0; i < width; ++i)
				for (int j = 0; j < height; ++j)
				{
					double currentValue = imageStackOutput.getVoxel(i,j,k);
					double currentValueMin = localMinValues[i][j][k];
					if ( currentValue > 0 &&  currentValue != currentValueMin )
					{
						imageStackOutput.setVoxel(i, j, k, 0);
						binaryStackOutput.setVoxel(i, j, k, 0);
						
						voxelList.addLast( new VoxelRecord(i, j, k, 0) );
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
											if ( imageStackOutput.getVoxel(ii,jj,kk) == currentValue )
											{
												imageStackOutput.setVoxel(ii, jj, kk, 0);
												binaryStackOutput.setVoxel(ii, jj, kk, 0);
												voxelList.addLast( new VoxelRecord(ii, jj, kk, 0) );
											}
						}
					}
				}
		}
		
		IJ.showProgress(1.0);
		
		//(new ImagePlus("valued minima", imageStackOutput)).show();
		
		return new ImagePlus("regional-minima-" + input.getTitle(), binaryStackOutput);
	} //apply



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
		final ImageStack imageStack= input.getStack();

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
									if ( imageStack.getVoxel(ii, jj, kk)<minValue )
										minValue = imageStack.getVoxel(ii, jj, kk);

					localMinValues[i][j][k] = minValue;
				}
		}
		
		IJ.showProgress( 1.0 );
		
		return localMinValues;
	}//filterMin3D


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
