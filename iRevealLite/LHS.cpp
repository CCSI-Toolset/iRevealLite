//LHS.cpp
#include <stdlib.h>
#include "LHS.h"
#include <stdio.h>
#include <math.h>
#include <climits>

CLHS::CLHS()
{
	ndim = 2;
	npoint = 2;
	pdefault = NULL;
	plower = NULL;
	pupper = NULL;
	ppx = NULL;
}

CLHS::~CLHS()
{
	DeleteLimitArrays();
	DeleteSampleArray();
}

CLHS::CLHS(const CLHS &t)
{
	int i, j;
	ndim = t.ndim;
	npoint = t.npoint;
	DeleteLimitArrays();
	DeleteSampleArray();
	if (t.pdefault!=NULL)
	{
		AllocateLimitArrays();
		for (i=0; i<ndim; i++)
		{
			pdefault[i] = t.pdefault[i];
			plower[i] = t.plower[i];
			pupper[i] = t.pupper[i];
		}
	}
	if (t.ppx!=NULL)
	{
		AllocateSampleArray();
		for (i=0; i<npoint; i++)
		{
			for (j=0; j<ndim; j++)
				ppx[i][j] = t.ppx[i][j];
		}
	}
}

CLHS& CLHS::operator=(const CLHS& t)
{
	if (this==&t)
		return *this;
	int i, j;
	ndim = t.ndim;
	npoint = t.npoint;
	DeleteLimitArrays();
	DeleteSampleArray();
	if (t.pdefault!=NULL)
	{
		AllocateLimitArrays();
		for (i=0; i<ndim; i++)
		{
			pdefault[i] = t.pdefault[i];
			plower[i] = t.plower[i];
			pupper[i] = t.pupper[i];
		}
	}
	if (t.ppx!=NULL)
	{
		AllocateSampleArray();
		for (i=0; i<npoint; i++)
		{
			for (j=0; j<ndim; j++)
				ppx[i][j] = t.ppx[i][j];
		}
	}
	return *this;
}

void CLHS::AllocateLimitArrays()
{
	DeleteLimitArrays();
	pdefault = new T_REAL [ndim];
	plower = new T_REAL [ndim];
	pupper = new T_REAL [ndim];
}

void CLHS::AllocateSampleArray()
{
	DeleteSampleArray();
	ppx = new T_REAL* [npoint];
	for (int i=0; i<npoint; i++)
		ppx[i] = new T_REAL [ndim];
}

void CLHS::DeleteLimitArrays()
{
	if (pdefault!=NULL)
	{
		delete [] pdefault;
		pdefault = NULL;
	}
	if (plower!=NULL)
	{
		delete [] plower;
		plower = NULL;
	}
	if (pupper!=NULL)
	{
		delete [] pupper;
		pupper = NULL;
	}
}

void CLHS::DeleteSampleArray()
{
	if (ppx!=NULL)
	{
		for (int i=0; i<npoint; i++)
			delete [] ppx[i];
		delete [] ppx;
		ppx = NULL;
	}
}

void CLHS::SetDimension(int n)
{
	DeleteLimitArrays();
	DeleteSampleArray();
	ndim = n;
}

void CLHS::SetNumberOfPoints(int n)
{
	DeleteSampleArray();
	npoint = n;
}

void CLHS::SetDefaults(T_REAL* pd)
{
	for (int i=0; i<ndim; i++)
		pdefault[i] = pd[i];
}

void CLHS::SetLowerLimits(T_REAL* pl)
{
	for (int i=0; i<ndim; i++)
		plower[i] = pl[i];
}

void CLHS::SetUpperLimits(T_REAL* pu)
{
	for (int i=0; i<ndim; i++)
		pupper[i] = pu[i];
}

void CLHS::SimpleSampling()
{
	//ppx has npoint rows and ndim columns
	T_REAL dx;			//interval in each dimension
	int i, j, k;
	int isel;
	int** ppleft = new int* [ndim];		//left over points
	int** ppsel = new int* [npoint];
	for (i=0; i<npoint; i++)
		ppsel[i] = new int [ndim];
	for (i=0; i<ndim; i++)
	{
		ppleft[i] = new int [npoint];
		for (j=0; j<npoint; j++)
			ppleft[i][j] = j;
	}
	for (i=npoint-1; i>=0; i--)
	{
		for (j=0; j<ndim; j++)
		{
			isel = rand()%(i+1);
			ppsel[i][j] = ppleft[j][isel];
			//remove the selected point in ppleft[j]
			for (k=isel; k<i; k++)
				ppleft[j][k] = ppleft[j][k+1];
		}
	}
	for (j=0; j<ndim; j++)
	{
		dx = (pupper[j] - plower[j])/(T_REAL)(npoint-1);
		for (i=0; i<npoint; i++)
			ppx[i][j] = plower[j] + dx*ppsel[i][j];
	}
	for (i=0; i<ndim; i++)
		delete [] ppleft[i];
	delete [] ppleft;
	for (i=0; i<npoint; i++)
		delete [] ppsel[i];
	delete [] ppsel;
}

void CLHS::MaxMinDistanceSampling()
{
	//do multiple sampling and pick up the best one with minimum distance of two samples maximized
	T_REAL dx;				//interval in each dimension
	int dd;
	int dist;
	int dist_min;			//minimum distance in each sample
	int nmin;				//count of pairs with the minimum distance
	int nmin_min;			//minimum count of pairs with the minimum distance
	int max_dist_min = 0;	//maximum of minimum distance
	int i, j, k;
	int isel;
	int itry, ntry;
	int** ppleft = new int* [ndim];		//left over points
	int** ppsel = new int* [npoint];	//selected point indices
	int** ppsel_new = new int* [npoint];	//new set of selected point indices
	//determine number of tries
	if      (npoint >= 10000) ntry = 1;
	else if (npoint >= 9000)  ntry = 2;
	else if (npoint >= 4000)  ntry = 5;
	else if (npoint >= 1000)  ntry = 10;
	else if (npoint >= 250)   ntry = 100;
	else ntry = 500;
	for (i=0; i<npoint; i++)
	{
		ppsel[i] = new int [ndim];
		ppsel_new[i] = new int [ndim];
	}
	for (i=0; i<ndim; i++)
		ppleft[i] = new int [npoint];
	for (itry=0; itry<ntry; itry++)
	{
		//reset ppleft[][]
		for (i=0; i<ndim; i++)
		{
			for (j=0; j<npoint; j++)
				ppleft[i][j] = j;
		}
		//new sample
		for (i=npoint-1; i>=0; i--)
		{
			for (j=0; j<ndim; j++)
			{
				isel = rand()%(i+1);
				ppsel_new[i][j] = ppleft[j][isel];
				//remove the selected point in ppleft[j]
				for (k=isel; k<i; k++)
					ppleft[j][k] = ppleft[j][k+1];
			}
		}
		//calculate minimum distance between ppsel[][] and ppsel_new
		dist_min = INT_MAX;
		nmin = 0;
		for (i=0; i<npoint-1; i++)
		{
			for (j=i+1; j<npoint; j++)
			{
				dist = 0;
				for (k=0; k<ndim; k++)
				{
					dd = ppsel_new[i][k] - ppsel_new[j][k];
					dist += dd*dd;
				}
				if (dist<dist_min)
				{
					dist_min = dist;
					nmin = 1;
				}
				else
				{
					if (dist==dist_min)
						nmin++;
				}
			}
		}
		if (dist_min>max_dist_min)
		{
			max_dist_min = dist_min;
			nmin_min = nmin;
			//assign ppsel_new to ppsel
			for (i=0; i<npoint; i++)
			{
				for (j=0; j<ndim; j++)
					ppsel[i][j] = ppsel_new[i][j];
			}
		}
		else
		{
			if (dist_min==max_dist_min && nmin<nmin_min)
			{
				nmin_min = nmin;
				//assign ppsel_new to ppsel
				for (i=0; i<npoint; i++)
				{
					for (j=0; j<ndim; j++)
						ppsel[i][j] = ppsel_new[i][j];
				}
			}
		}
	}
	//calculate samples
	for (j=0; j<ndim; j++)
	{
		dx = (pupper[j] - plower[j])/(T_REAL)(npoint-1);
		for (i=0; i<npoint; i++)
			ppx[i][j] = plower[j] + dx*ppsel[i][j];
	}
	for (i=0; i<ndim; i++)
		delete [] ppleft[i];
	delete [] ppleft;
	for (i=0; i<npoint; i++)
	{
		delete [] ppsel[i];
		delete [] ppsel_new[i];
	}
	delete [] ppsel;
	delete [] ppsel_new;
}

void CLHS::WriteSamples2D(char* filename, int ix, int iy)
{
	int i;
	FILE* pf;
	pf = fopen(filename,"w");
	if (pf==NULL)
		return;
	for (i=0; i<npoint; i++)
	{
			fprintf(pf,"%lg\t%lg\n",ppx[i][ix],ppx[i][iy]);
	}
	fclose(pf);
}

void CLHS::WriteSamples3D(char* filename, int ix, int iy, int iz)
{
	int i;
	FILE* pf;
	pf = fopen(filename,"w");
	if (pf==NULL)
		return;
	for (i=0; i<npoint; i++)
	{
			fprintf(pf,"%lg\t%lg\t%lg\n",ppx[i][ix],ppx[i][iy],ppx[i][iz]);
	}
	fclose(pf);
}

void CLHS::WriteSamples(FILE* pf)
{
	if (pf==NULL)
		return;
	int i, j;
	for (i=0; i<npoint; i++)
	{
		fprintf(pf,"%d",i+1);
		for (j=0; j<ndim; j++)
			fprintf(pf,",%lg",ppx[i][j]);
		fprintf(pf,"\n");
	}
}

double CLHS::CalcQualityPhi(double p)
{
	int i, j, k;
	double phi = 0;
	double dis;
	double sum;
	for (i=0; i<npoint-1; i++)
	{
		for (j=i+1; j<npoint; j++)
		{
			sum = 0;
			for (k=0; k<ndim; k++)
			{
				dis = ppx[i][k] - ppx[j][k];
				if (dis<0)
					dis = -dis;
				sum += dis;
			}
			phi += pow(1/sum,p);
		}
	}
	phi = pow(phi,1/p);
	return phi;
}