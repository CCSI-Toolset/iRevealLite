//DesignSite.cpp
#include "DesignSite.h"
#include <cmath>

CDesignSite::CDesignSite()
{
	nx = 1;
	ny = 1;
	px = NULL;
	py = NULL;
}

CDesignSite::CDesignSite(int ix, int iy)
{
	nx = ix;
	ny = iy;
	px = NULL;
	py = NULL;
	AllocateArrays();
}

CDesignSite::~CDesignSite()
{
	DeleteArrays();
}

void CDesignSite::AllocateArrays()
{
	//delete arrays before allocate
	DeleteArrays();
	px = new T_REAL [nx];
	py = new T_REAL [ny];
}

void CDesignSite::DeleteArrays()
{
	if (px!=NULL)
	{
		delete [] px;
		px = NULL;
	}
	if (py!=NULL)
	{
		delete [] py;
		py = NULL;
	}
}

void CDesignSite::WriteTextFile(FILE* pf)
{
	int i;
	fprintf(pf,"%d input variables: \n",nx);
	for (i=0; i<nx; i++)
	{
		if (i<nx-1)
			fprintf(pf,"%g\t",px[i]);
		else
			fprintf(pf,"%g\n",px[i]);
	}
	fprintf(pf,"%d output variables: \n",ny);
	for (i=0; i<ny; i++)
	{
		if (i<ny-1)
			fprintf(pf,"%g\t",py[i]);
		else
			fprintf(pf,"%g\n",py[i]);
	}
}

void CDesignSite::Write(FILE* pf)
{
	int iversion = 0;
	bool ballocated = px!=NULL && py!=NULL;
	fwrite(&iversion,sizeof(int),1,pf);
	fwrite(&nx,sizeof(int),1,pf);
	fwrite(&ny,sizeof(int),1,pf);
	fwrite(&ballocated,sizeof(bool),1,pf);
	if (ballocated)
	{
		fwrite(px,sizeof(T_REAL),nx,pf);
		fwrite(py,sizeof(T_REAL),ny,pf);
	}
}

void CDesignSite::Read(FILE* pf)
{
	int iversion;
	bool ballocated;
	fread(&iversion,sizeof(int),1,pf);
	fread(&nx,sizeof(int),1,pf);
	fread(&ny,sizeof(int),1,pf);
	fread(&ballocated,sizeof(bool),1,pf);
	if (ballocated)
	{
		AllocateArrays();
		fread(px,sizeof(T_REAL),nx,pf);
		fread(py,sizeof(T_REAL),ny,pf);
	}
}

int CDesignSite::Normalize(int n, CDesignSite* pdsr, CDesignSite* pdsn, CDesignSite& mean, CDesignSite& sigma)
{
	//return non-zero value if error
	//n is the size of the design site set, n>1
	//pdsr is the array of design site raw data
	//pdsn is the returned array of the normalized design site data
	//mean is the returned mean of the design sites
	//sigma is hte standard deviation of the design sites
	//all design sites in the set should have the same numbers of input and output parameters
	int i, j;
	int nnx = pdsr[0].nx;
	int nny = pdsr[0].ny;
	T_REAL dxy;
	if (n<2)		//sigma will be zero
		return 1;
	//calculate mean
	for (i=0; i<nnx; i++)
	{
		mean.px[i] = 0;
		for (j=0; j<n; j++)
			mean.px[i] += pdsr[j].px[i];
		mean.px[i] /= (T_REAL)n;
	}
	for (i=0; i<nny; i++)
	{
		mean.py[i] = 0;
		for (j=0; j<n; j++)
			mean.py[i] += pdsr[j].py[i];
		mean.py[i] /= (T_REAL)n;
	}
	//calculate standard deviation
	for (i=0; i<nnx; i++)
	{
		sigma.px[i] = 0;
		for (j=0; j<n; j++)
		{
			dxy = pdsr[j].px[i] - mean.px[i];
			sigma.px[i] += dxy*dxy;
		}
		sigma.px[i] = sqrt(sigma.px[i]/((T_REAL)n-1));
	}
	for (i=0; i<nny; i++)
	{
		sigma.py[i] = 0;
		for (j=0; j<n; j++)
		{
			dxy = pdsr[j].py[i] - mean.py[i];
			sigma.py[i] += dxy*dxy;
		}
		sigma.py[i] = sqrt(sigma.py[i]/((T_REAL)n-1));
	}
	//check if all sigmas are non-zero
	for (i=0; i<nnx; i++)
	{
		if (sigma.px[i] <=0)	//input sigma zero
			return 2;
	}
	for (i=0; i<nny; i++)
	{
		if (sigma.py[i] <=0)	//output sigma zero, allow this happen
			sigma.py[i] = 1;
	}
	//normalize
	for (j=0; j<n; j++)
	{
		for (i=0; i<nnx; i++)
			pdsn[j].px[i] = (pdsr[j].px[i] - mean.px[i])/sigma.px[i];
		for (i=0; i<nny; i++)
			pdsn[j].py[i] = (pdsr[j].py[i] - mean.py[i])/sigma.py[i];
	}
	return 0;
}

void CDesignSite::SetInputData(T_REAL* pxi)
{
	for (int i=0; i<nx; i++)
		px[i] = pxi[i];
}

void CDesignSite::SetOutputData(T_REAL* pyo)
{
	for (int i=0; i<ny; i++)
		py[i] = pyo[i];
}

void CDesignSite::SetAndNormalizeInputData(T_REAL* pxi, CDesignSite& mean, CDesignSite& sigma)
{
	//convert input pxi to the normalized design site px
	//pxi is the array of raw input data
	for (int i=0; i<nx; i++)
		px[i] = (pxi[i] - mean.px[i])/sigma.px[i];
}

void CDesignSite::ConvertToRawOutputData(T_REAL* pyo, CDesignSite& mean, CDesignSite& sigma)
{
	//convert output py inside the normalized design site to raw pyo
	//pyo is the array of raw output data
	for (int i=0; i<ny; i++)
		pyo[i] = py[i]*sigma.py[i] + mean.py[i];
}