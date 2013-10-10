package inra.watershed.process;

import java.util.ArrayList;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;

public class ComponentLabelling 
{
	ImagePlus inputImage = null;

	int numLabels = 0;
	ArrayList<Integer> labelTable = null;

	public ComponentLabelling( ImagePlus inputImage )
	{
		this.inputImage = inputImage;
	}

	int newLabel()
	{
		++numLabels;
		
		labelTable.add( numLabels );

		return numLabels;
	}
	
	int equivLabel( int l1, int l2, int l3 )
	{
	  int min = 0;

	  if ( l1 < l2 )
	  {
	    min = l1;
	    labelTable.set( l2, l1 );
	    labelTable.set( l3, l1 );
	  }
	  else
	  {
	    min = l2;
	    labelTable.set( l1, l2 );
	    labelTable.set( l3, l2 );
	  }

	  if ( l3 < min )
	  {
	    min = l3;
	    labelTable.set( l1, l3 );
	    labelTable.set( l2, l3 );
	    labelTable.set( l3, l3 );
	  }

	  return min;
	}

	public ImagePlus apply()
	{
		final ImagePlus imageOutput = inputImage.duplicate();
		final ImageStack outputStack = imageOutput.getStack();
		
		final ImageStack inputStack = inputImage.getStack();
	    final int size1 = inputStack.getWidth();
	    final int size2 = inputStack.getHeight();
	    final int size3 = inputStack.getSize();
	    
		int v1, v2, v3;  // previous voxels along dimension 1, 2, 3
		int i, j, k;

		labelTable = new ArrayList<Integer>( );
		
		labelTable.add( 0 );
		
		numLabels = 0;

		IJ.showStatus( "Calculated connected components..." );
		

		// premier plan du volume, premier voxel
		if ( outputStack.getVoxel( 0,0,0 ) != 0 )
		{
			outputStack.setVoxel( 0,0,0, newLabel() );
		}

		// premier plan, première ligne
		k = i = 0;
		for (j = 1 ; j < size2; ++j)
		{
			if ( outputStack.getVoxel(i,j,k) != 0 )
			{
				v2 = (int) outputStack.getVoxel(i,j-1,k) ;
				outputStack.setVoxel( i,j,k, v2 != 0 ? v2: newLabel() );
			}
		}

		// reste du premier plan
		for (k = 0, i = 1; i < size1; ++i)
		{
			// premier pixel de la ligne
			if ( outputStack.getVoxel(i, 0, 0) != 0 )
			{
				v1 = (int)( outputStack.getVoxel(i-1,0,0) );
				outputStack.setVoxel( i,0,0, v1 != 0 ? v1: newLabel() );
			}

			// autres pixels de la ligne
			for (j = 1; j < size2; ++j)
			{
				if ( outputStack.getVoxel( i,j,k ) != 0 )
				{
					v1 = (int)( outputStack.getVoxel(i-1,j,k) );
					v2 = (int)( outputStack.getVoxel(i,j-1,k) );

					if ( v1 != 0 )
					{
						if ( v2 != 0 )
						{
							while ( v1 != labelTable.get( v1 ) ) 
								v1 = labelTable.get( v1 );
							while ( v2 != labelTable.get( v2 ) ) 
								v2 = labelTable.get( v2 );
							if( v1 < v2 )
							{
								labelTable.set( v2, v1 );
								outputStack.setVoxel( i,j,k, v1 );
							}
							else
							{
								labelTable.set( v1, v2 );
								outputStack.setVoxel( i,j,k, v2 );
							}
							 
						}
						else
						{
							outputStack.setVoxel( i,j,k, v1 );
						}
					}
					else
					{
						outputStack.setVoxel( i,j,k, v2 != 0 ? v2: newLabel() );
					}
				}
			}
		}



		// reste des plans
		// première ligne des autres plans du volume
		for (k = 1; k < size3; ++k)
		{
			for (i = 0, j = 1; j < size2; ++j)
			{
				if ( outputStack.getVoxel( i,j,k ) != 0 )
				{
					v2 = (int)( outputStack.getVoxel( i, j-1, k   ) );
					v3 = (int)( outputStack.getVoxel( i,   j, k-1 ) );

					if ( v2 != 0 )
					{
						if ( v3 != 0 )
						{
							while ( v2 != labelTable.get( v2 ) ) 
								v2 = labelTable.get( v2 );
							while ( v3 != labelTable.get( v3 ) ) v3 = labelTable.get( v3 );
							if( v2 < v3 )
							{
								outputStack.setVoxel( i,j,k, v2 );
								labelTable.set( v3, v2 );
							}
							else
							{
								outputStack.setVoxel( i,j,k, v3 );
								labelTable.set( v2, v3 );
							}
						}
						else
						{
							outputStack.setVoxel( i,j,k, v2 );							
						}
					}
					else
					{
						outputStack.setVoxel( i,j,k, v3 != 0  ? v3: newLabel() );
					}
				}
			}
		}

		// reste des plans: autres lignes que la première
		for (k = 1; k < size3; ++k)
		{
			for (i = 1; i < size1; ++i)
			{
				// premier voxel de la ligne
				if ( outputStack.getVoxel(i,0,k) != 0 )
				{
					v1 = (int)( outputStack.getVoxel(i-1,0,k) );
					v3 = (int)( outputStack.getVoxel(i,0,k-1) );

					if ( v1 != 0 )
					{
						if ( v3 != 0 )
						{
							while ( v1 != labelTable.get( v1 ) ) 
								v1 = labelTable.get( v1 );
							while ( v3 != labelTable.get( v3 ) ) 
								v3 = labelTable.get( v3 );
							
							if( v1 < v3 )
							{
								outputStack.setVoxel( i,0,k, v1);
								labelTable.set( v3, v1 );	
							}
							else
							{
								outputStack.setVoxel( i,0,k, v3);
								labelTable.set( v1, v3 );
							}																					
						}
						else
						{
							outputStack.setVoxel( i,0,k, v1);
						}
					}
					else
					{
						outputStack.setVoxel( i,0,k, v3 != 0 ? v3: newLabel() );
					}
				}

				// autres voxels de la ligne
				for (j = 1; j < size2; ++j)
				{
					if ( outputStack.getVoxel(i,j,k) != 0 )
					{
						v1 = (int)( outputStack.getVoxel(i-1,j,k) );
						v2 = (int)( outputStack.getVoxel(i,j-1,k) );
						v3 = (int)( outputStack.getVoxel(i,j,k-1) );

						if ( v2 != 0 )
						{
							if ( v1 != 0 )
							{
								if ( v3 != 0 )
								{
									while ( v1 != labelTable.get( v1 ) ) v1 = labelTable.get( v1 );
									while ( v2 != labelTable.get( v2 ) ) v2 = labelTable.get( v2 );
									while ( v3 != labelTable.get( v3 ) ) v3 = labelTable.get( v3 );
									outputStack.setVoxel( i,j,k, equivLabel( v1, v2, v3 ) );
								}
								else
								{
									while ( v1 != labelTable.get( v1 ) ) v1 = labelTable.get( v1 );
									while ( v2 != labelTable.get( v2 ) ) v2 = labelTable.get( v2 );
									if( v1 < v2 )
									{
										outputStack.setVoxel( i,j,k, v1 );
										labelTable.set( v2, v1 );
									}
									else
									{
										outputStack.setVoxel( i,j,k, v2 );
										labelTable.set( v1, v2 );
									}
								}
							}
							else
							{
								if ( v3 != 0 )
								{
									while ( v2 != labelTable.get( v2 ) ) v2 = labelTable.get( v2 );
									while ( v3 != labelTable.get( v3 ) ) v3 = labelTable.get( v3 );
									if( v2 < v3 )
									{
										outputStack.setVoxel( i,j,k, v2 );
										labelTable.set( v3, v2 );
									}
									else
									{
										outputStack.setVoxel( i,j,k, v3 );
										labelTable.set( v2, v3 );
									}
								}
								else
								{
									outputStack.setVoxel( i,j,k, v2 );
								}
							}
						}
						else
						{
							if ( v1 != 0 )
							{
								if ( v3 != 0 )
								{
									while ( v1 != labelTable.get( v1 ) ) v1 = labelTable.get( v1 );
									while ( v3 != labelTable.get( v3 ) ) v3 = labelTable.get( v3 );
									
									if( v1 < v3 )
									{
										outputStack.setVoxel( i,j,k, v1 );
										labelTable.set( v3, v1 );
									}
									else
									{
										outputStack.setVoxel( i,j,k, v3 );
										labelTable.set( v1, v3 );
									}									
								}
								else
								{
									outputStack.setVoxel( i,j,k, v1 );
								}
							}
							else
							{
								outputStack.setVoxel( i,j,k, v3 != 0 ? v3: newLabel() );
							}
						}
					}
				}
			}
/*
			this->notifyProgressed( k+1, size3 );
			if ( this->isCanceled() )
			{
				return;
			}*/
			
		}

		// actualisation de la table d'équivalence
		int numEquivalences = 0;
		
		for (int v = 1; v <= numLabels; v++)
		{
			if ( labelTable.get(v) == v )
			{
				labelTable.set( v, v - numEquivalences );
			}
			else
			{
				labelTable.set( v, labelTable.get( labelTable.get( v ) ) );
				++numEquivalences;
			}
		}
		numLabels -= numEquivalences;


		// deuxième balayage du volume matrice:
		// actualisation par la table d'équivalence
		for (k = 0; k < size3; k++)
			for (i = 0; i < size1; i++)
				for (j = 0; j < size2; j++)
					outputStack.setVoxel( i,j,k, labelTable.get( (int) outputStack.getVoxel(i,j,k) ) );

		imageOutput.setTitle("connected-components-" + inputImage.getTitle() );
		return imageOutput;		
	}

}