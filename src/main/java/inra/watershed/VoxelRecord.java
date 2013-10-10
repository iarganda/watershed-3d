package inra.watershed;

public class VoxelRecord implements Comparable<VoxelRecord>{

		int i = 0;
		int j = 0;
		int k = 0;
		double value = 0;
		
		public VoxelRecord(
				final int i,
				final int j,
				final int k,
				final double value)
		{
			this.i = i;
			this.j = j;
			this.k = k;
			this.value = value;
		}
		
		public int getI(){ return this.i; }
		public int getJ(){ return this.j; }
		public int getK(){ return this.k; }
		
		public int[] getCoordinates()
		{
			return new int[]{ i, j, k };
		}
		
		public double getValue()
		{
			return value;
		}

		@Override
		public int compareTo(VoxelRecord voxelRecord) 
		{
			if ( value < voxelRecord.value ) 
		    	return -1;
			if ( value > voxelRecord.value )
				return 1;			  
			return 0;		    		   
		}
}
