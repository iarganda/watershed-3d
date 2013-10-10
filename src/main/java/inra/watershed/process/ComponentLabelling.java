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

import java.util.ArrayList;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.StackConverter;

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
		ImagePlus imageOutput = inputImage.duplicate();
		// Make sure output image is 32-bit
		if( imageOutput.getType() != ImagePlus.GRAY32 )
		{
			if( imageOutput.getImageStackSize() > 1 )
				(new StackConverter( imageOutput )).convertToGray32();
			else
				imageOutput = new ImagePlus( imageOutput.getTitle(), 
						imageOutput.getProcessor().convertToFloat() );
					
		}
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
