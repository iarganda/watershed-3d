package inra.watershed.process;

import java.util.LinkedList;

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
		for (int k = 0; k < size3; ++k)
			for (int j = 0; j < size2; ++j)
				for (int i = 0; i < size1; ++i)
				{

					outStack.setVoxel(i, j, k, 
							Math.min( 
									Math.max( 	maxNeighborhoodMinus(i, j, k, outStack), outStack.getVoxel(i, j, k) ), 
									maskStack.getVoxel(i, j, k)));
				}

		// backwards
		for (int i = size1-1; i >= 0; --i)
			for (int j = size2-1; j >= 0; --j)
				for (int k = size3 - 1; k >= 0; --k)
				{

					outStack.setVoxel(i, j, k, 
							Math.min( 
									Math.max( 	maxNeighborhoodPlus(i, j, k, outStack), outStack.getVoxel(i, j, k) ), 
									maskStack.getVoxel(i, j, k)));
				}

		
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
	
	public double maxNeighborhoodMinus( 
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
	
	
}
