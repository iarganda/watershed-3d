package inra.watershed.process;

import java.util.LinkedList;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;

public class GeodesicReconstruction 
{
	ImagePlus mask = null;
	ImagePlus marker = null;
	
	int size1 = 0;
	int size2 = 0;
	int size3 = 0;
	
	public GeodesicReconstruction(
			final ImagePlus mask,
			final ImagePlus marker)
	{
		this.marker = marker;
		this.mask = mask;
		
		this.size1 = mask.getWidth();
		this.size2 = mask.getHeight();
		this.size3 = mask.getImageStackSize();
	}
	
	public ImagePlus reconstructionByDilationHybrid()
	{
		ImagePlus output = marker.duplicate();
		ImageStack outStack = output.getStack();
		ImageStack maskStack = mask.getStack();
		
		LinkedList<VoxelRecord> q = new LinkedList<VoxelRecord>();
		
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
										maxNeighborhoodMinus(i, j, k, outStack),  
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
									maxNeighborhoodPlus(i, j, k, outStack),
									maskStack.getVoxel(i, j, k)) );
					// push in the queue if neighbor voxel has lower value 
					// in the output than in the mask and it's lower than the 
					// center voxel
					pushQueueNeighborhoodPlus(i, j, k, outStack, maskStack, q );
							
				}
		}
		
		final int numVoxels = size1 * size2 * size3;
		
		// propagation
		IJ.showStatus( "Propagating..." );
		while ( q.isEmpty() == false )
		{
			IJ.showProgress( numVoxels-q.size(), numVoxels );
			
			final VoxelRecord p = q.poll();
			final int[] coord = p.getCoordinates();
			final int i = coord[ 0 ];
			final int j = coord[ 1 ];
			final int k = coord[ 2 ];
			
			for (int u = i-1; u <= i+1; ++u) 
  				for (int v = j-1; v <= j+1; ++v) 
  					for (int w = k-1; w <= k+1; ++w) 
  					{
  						if ( u >= 0 && u < size1 && v >= 0 && v < size2 && w >= 0 && w < size3 )
  						{
  							final double on = outStack.getVoxel( u, v, w );
  							final double op = p.getValue();
  							final double gn = maskStack.getVoxel( u, v, w );
  							if( on < op && on != gn)
  							{
  								final double value = Math.min( op, gn );
  								outStack.setVoxel( u, v, w, value );
  								q.addLast( new VoxelRecord( u, v, w, value ));
  							}
  						}
  					}
			
		}
		
		IJ.showProgress( 1.0 );
		IJ.showStatus( "Done" );
		
		output.setTitle( "geodesic-reconstruction-by-dilation" );
		return output;
	}

	
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
	
	public double pushQueueNeighborhoodPlus( 
			final int x, 
			final int y, 
			final int z, 
			final ImageStack o,
			final ImageStack mask,
			final LinkedList<VoxelRecord> q)
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
						if( neighborValue < o.getVoxel(x, y, z) && neighborValue < mask.getVoxel( x2, y2, z2 ) )
							q.addLast(new VoxelRecord( x, y, z, neighborValue));
					}
				}
					
					
			}
		}
		

		return max;
	}
	
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
