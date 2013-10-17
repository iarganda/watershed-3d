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
* Authors: Ignacio Arganda-Carreras
*/

import java.util.LinkedList;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;

/**
 * This class allows to perform geodesic reconstructions
 * of grayscale images in 2D/3D
 */
public class GeodesicReconstruction 
{
	/** mask image */
	ImagePlus mask = null;
	/** marker image */
	ImagePlus marker = null;
	
	/** image width */
	int size1 = 0;
	/** image height */
	int size2 = 0;
	/** image depth */
	int size3 = 0;
	
	/**
	 * Constructs a geodesic reconstruction object
	 * @param marker the marker image
	 * @param mask the mask or reference image
	 */
	public GeodesicReconstruction(
			final ImagePlus marker,
			final ImagePlus mask)
	{
		this.marker = marker;
		this.mask = mask;
		
		this.size1 = mask.getWidth();
		this.size2 = mask.getHeight();
		this.size3 = mask.getImageStackSize();
	}
	
	/**
	 * Reconstruction by dilation using the queue-based method.
	 * Implementation of the algorithm of the same name described
	 * in "Mathematical morphology 2" by Laurent Najman and Hugues Talbot
	 * @return reconstructed image
	 */
	public ImagePlus reconstructionByDilationQueueBased()
	{
		ImagePlus output = marker.duplicate();
		ImageStack outStack = output.getStack();
		ImageStack maskStack = mask.getStack();
		
		LinkedList<int[]> q = new LinkedList<int[]>();
		
		// initialization
		RegionalMaximaFilter rmf = new RegionalMaximaFilter( marker );
		final ImagePlus m = rmf.apply();
		final ImageStack maxStack = m.getStack();
		
		IJ.showStatus("Initialization...");
		for (int k = 0; k < size3; ++k)
		{
			IJ.showProgress(k+1, size3);
			for (int j = 0; j < size2; ++j)
				for (int i = 0; i < size1; ++i)
				{
					for (int u = i-1; u <= i+1; ++u) 
		  				for (int v = j-1; v <= j+1; ++v) 
		  					for (int w = k-1; w <= k+1; ++w) 
		  					{
		  						if ( u >= 0 && u < size1 && v >= 0 && v < size2 && w >= 0 && w < size3 )
		  						{
		  							if( maxStack.getVoxel(u, v, w) < 1 )
		  							{
		  								q.addLast( new int[]{ i, j, k } );
		  								break;
		  							}
		  						}
		  					}
				}
		}

		IJ.showProgress ( 1.0 );
		
		int total = q.size();
		int iter = 1;
		
		// propagation
		IJ.showStatus( "Propagating..." );
		while ( q.isEmpty() == false )
		{
			IJ.showProgress( iter, total );

			final int[] p = q.poll();
			final int i = p[ 0 ];
			final int j = p[ 1 ];
			final int k = p[ 2 ];

			for (int u = i-1; u <= i+1; ++u) 
				for (int v = j-1; v <= j+1; ++v) 
					for (int w = k-1; w <= k+1; ++w) 
					{
						if ( u >= 0 && u < size1 && v >= 0 && v < size2 && w >= 0 && w < size3 )
						{
							final double on = outStack.getVoxel( u, v, w );
							final double op = outStack.getVoxel( i, j, k );
							final double gn = maskStack.getVoxel( u, v, w );
							if( on < op && on != gn )
							{
								final double value = Math.min( op, gn );
								outStack.setVoxel( u, v, w, value );
								q.addLast( new int[]{ u, v, w } );
								total++;
								break;
							}
						}
					}

			iter++;
		}

		IJ.showProgress( 1.0 );
		IJ.showStatus( "Done" );

		output.setTitle( "geodesic-reconstruction-by-dilation" );
		
		return output;
	}
	
	
	/**
	 * Reconstruction by dilation using the hybrid method.
	 * 
	 * The algorithm details are described in:
	 * @inproceedings{vincent1992morphological,
	 * 		title={Morphological grayscale reconstruction: definition, 
	 * 				efficient algorithm and applications in image analysis},
	 * 		author={Vincent, Luc},
	 * 		booktitle={Computer Vision and Pattern Recognition, 1992. 
	 * 					Proceedings CVPR'92., 1992 IEEE Computer Society Conference on},
	 * 		pages={633--635},
	 * 		year={1992},
	 * 		organization={IEEE}
	 * }
	 * @return reconstructed image
	 */
	public ImagePlus reconstructionByDilationHybrid()
	{
		ImagePlus output = marker.duplicate();
		ImageStack outStack = output.getStack();
		ImageStack maskStack = mask.getStack();
		
		LinkedList<int[]> q = new LinkedList<int[]>();
		
		// 2-pass sequence
		
		// forwards
		IJ.showStatus("Forward pass...");
		for (int k = 0; k < size3; ++k)
		{
			IJ.showProgress(k+1, size3);
			for (int j = 0; j < size2; ++j)
				for (int i = 0; i < size1; ++i)
				{

					outStack.setVoxel(i, j, k, 
							Math.min( 
									maxNeighborhoodPlus(i, j, k, outStack),
										maskStack.getVoxel(i, j, k) ));
				}
		}
		// backwards
		IJ.showStatus("Backward pass...");
		for (int k = size3 - 1; k >= 0; --k)
		{
			IJ.showProgress( size3-k, size3 );
			
			for (int j = size2-1; j >= 0; --j)
				for (int i = size1-1; i >= 0; --i)				
				{

					outStack.setVoxel(i, j, k, 
							Math.min( 
									maxNeighborhoodMinus(i, j, k, outStack),
									maskStack.getVoxel(i, j, k)) );
					// push in the queue if neighbor voxel has lower value 
					// in the output than in the mask and it's lower than the 
					// center voxel
					pushQueueNeighborhoodMinus(i, j, k, outStack, maskStack, q );
				}
		}
				
		int total = q.size();
		int iter = 1;

		// propagation
		IJ.showStatus( "Propagating..." );
		while ( q.isEmpty() == false )
		{
			IJ.showProgress( iter, total );

			final int[] p = q.poll();
			final int i = p[ 0 ];
			final int j = p[ 1 ];
			final int k = p[ 2 ];

			for (int u = i-1; u <= i+1; ++u) 
				for (int v = j-1; v <= j+1; ++v) 
					for (int w = k-1; w <= k+1; ++w) 
					{
  						if ( u >= 0 && u < size1 && v >= 0 && v < size2 && w >= 0 && w < size3 )
  						{
  							final double on = outStack.getVoxel( u, v, w );
  							final double op = outStack.getVoxel( i, j, k );
  							final double gn = maskStack.getVoxel( u, v, w );
  							if( on < op && on != gn )
  							{
  								final double value = Math.min( op, gn );
  								outStack.setVoxel( u, v, w, value );
  								q.addLast( new int[]{ u, v, w } );
  								total++;
  								break;
  							}
  						}
  					}
			iter++;
		}
		
		IJ.showProgress( 1.0 );
		IJ.showStatus( "Done" );
		
		output.setTitle( "geodesic-reconstruction-by-dilation" );
		return output;
	}

	/**
	 * Return maximum value in N+ neighborhood
	 * @param x x-coordinate
	 * @param y y-xoordinate
	 * @param z z-coordinate
	 * @param o process image
	 * @return maximum value in N+ neighborhood
	 */
	public double maxNeighborhoodPlus( 
			final int x, 
			final int y, 
			final int z, 
			final ImageStack o )
	{
		double max = o.getVoxel(x, y, z);
		
		for ( int w = -1; w <= 1; ++w)
		{
			for( int v=-1; v<=0; ++v )
			{
				final int maxX = v < 0 ? 1 : (w<0 ? 0 : -1);

				for( int u = -1; u<=maxX; u ++ )
				{
					final int x2 = x + u;
					final int y2 = y + v;
					final int z2 = z + w;
					
					if ( x2 >= 0 && x2 < size1 && y2 >= 0 && y2 < size2 && z2 >= 0 && z2 < size3 )
					{
						double neighborValue = o.getVoxel( x2, y2, z2 );
						if( neighborValue > max )
							max = neighborValue;
					}
				}
					
					
			}
		}
		

		return max;
	}
	
	/**
	 * Push central voxel in queue if any N+ neighbor
	 * has smaller value than the central pixel and
	 * its mask value is larger than its marker value.
	 * 
	 * @param x x-coordinate
	 * @param y y-coordinate
	 * @param z z-coordinate
	 * @param o output image
	 * @param mask mask image
	 * @param q queue of coordinates to be visited
	 */
	public void pushQueueNeighborhoodPlus( 
			final int x, 
			final int y, 
			final int z, 
			final ImageStack o,
			final ImageStack mask,
			final LinkedList<int[]> q)
	{
		
		for ( int w = -1; w <= 1; ++w)
		{
			for( int v=-1; v<=0; ++v )
			{
				final int maxX = v < 0 ? 1 : (w<0 ? 0 : -1);

				for( int u = -1; u<=maxX; u ++ )
				{
					final int x2 = x + u;
					final int y2 = y + v;
					final int z2 = z + w;
					
					if ( x2 >= 0 && x2 < size1 && y2 >= 0 && y2 < size2 && z2 >= 0 && z2 < size3 )
					{
						double neighborValue = o.getVoxel( x2, y2, z2 );
						if( neighborValue < o.getVoxel(x, y, z) && neighborValue < mask.getVoxel( x2, y2, z2 ) )
							q.addLast( new int[]{ x, y, z } );
					}
				}
					
					
			}
		}
	}
	
	/**
	 * Push central voxel in queue if any N- neighbor
	 * has smaller value than the central pixel and
	 * its mask value is larger than its marker value.
	 * 
	 * @param x x-coordinate
	 * @param y y-coordinate
	 * @param z z-coordinate
	 * @param o output image
	 * @param mask mask image
	 * @param q queue of coordinates to be visited
	 */
	public void pushQueueNeighborhoodMinus( 
			final int x, 
			final int y, 
			final int z, 
			final ImageStack o,
			final ImageStack mask,
			final LinkedList<int[]> q)
	{
		
		for ( int w = -1; w <= 1; ++w)
		{
			for( int v=0; v<=1; ++v )
			{
				final int minX = v > 0 ? -1 : (w>0 ? 0 : 1);

				for( int u = minX; u<=1; u ++ )
				{
					final int x2 = x + u;
					final int y2 = y + v;
					final int z2 = z + w;
					
					if ( x2 >= 0 && x2 < size1 && y2 >= 0 && y2 < size2 && z2 >= 0 && z2 < size3 )
					{
						double neighborValue = o.getVoxel( x2, y2, z2 );
						if( neighborValue < o.getVoxel(x, y, z) && neighborValue < mask.getVoxel( x2, y2, z2 ) )
							q.addLast( new int[]{ x, y, z } );
					}
				}
					
					
			}
		}
	}
	
	/**
	 * Return maximum value in N- neighborhood
	 * @param x x-coordinate
	 * @param y y-coordinate
	 * @param z z-coordinate
	 * @param o process image
	 * @return maximum value in N- neighborhood
	 */
	public double maxNeighborhoodMinus( 
			final int x, 
			final int y, 
			final int z, 
			final ImageStack o )
	{
		double max = o.getVoxel(x, y, z);

		for ( int w = -1; w <= 1; ++w)
		{
			for( int v=0; v<=1; ++v )
			{
				final int minX = v > 0 ? -1 : (w>0 ? 0 : 1);

				for( int u = minX; u<=1; u ++ )				
				{
					final int x2 = x + u;
					final int y2 = y + v;
					final int z2 = z + w;

					if ( x2 >= 0 && x2 < size1 && y2 >= 0 && y2 < size2 && z2 >= 0 && z2 < size3 )
					{
						double neighborValue = o.getVoxel( x2, y2, z2 );
						if( neighborValue > max )
							max = neighborValue;
					}
				}


			}
		}


		return max;
	}


}
