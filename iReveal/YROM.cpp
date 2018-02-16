//YROM.cpp
#include "YROM.h"
#include "Simplex.h"

CYROM::CYROM()
{
	nx = 1;
	ny = 1;
	ncase = 1;
	ppy = NULL;
}

CYROM::~CYROM()
{
	DeleteArray();
}

void CYROM::AllocateArray()
{
	DeleteArray();
	ppy = new T_REAL* [ncase];
	for (int i=0; i<ncase; i++)
		ppy[i] = new T_REAL [ny];
}

void CYROM::DeleteArray()
{
	if (ppy!=NULL)
	{
		for (int i=0; i<ncase; i++)
			delete [] ppy[i];
		delete [] ppy;
		ppy = NULL;
	}
}

void CYROM::SetDesignSiteOutputVectors(T_REAL** pp)
{
	int i, j;
	for (i=0; i<ncase; i++)
	{
		for (j=0; j<ny; j++)
			ppy[i][j] = pp[i][j];
	}
}

int CYROM::ReadDesignSiteOutputVectors(char* fname)
{
	int i;
	int icase;
	int iresult;		//fscanf return value, -1 if error
	int nerr = 0;
	char line[500];
	char name_var[100];
	std::string str;
	double yy;
	FILE* pf = fopen(fname,"r");
	if (pf==NULL)
		return 1;
	outputnames.clear();
	for (i=0; i<ny; i++)
	{
		fscanf(pf,"%s",name_var);
		str = name_var;
		outputnames.push_back(str);
	}
	fgets(line,500,pf);
	for (icase=0; icase<ncase; icase++)
	{
		for (i=0; i<ny; i++)
		{
			iresult = fscanf(pf,"%lg",&yy);
			if (iresult<1)
			{
				nerr = 2;
				break;
			}
			ppy[icase][i] = yy;
		}
		fgets(line,500,pf);		//skip line
	}
	fclose(pf);
	return nerr;
}

int CYROM::CalcRegression(T_REAL** ppin)
{
	krig.SetDimensions(nx,ny,ncase);
	krig.SetCorrelationOption(0);			//Gaussian function
	krig.SetRegressionModelOption(1);		//first order, linear
	krig.AllocateMemory();
	krig.SetDesignSiteInputArray(ppin);
	krig.SetDesignSiteOutputArray(ppy);
	if (krig.NormalizeRawDesignSites())		//normalization failure
		return 1;
	krig.CalcFYMatrices();					//optimizing theta vector does not affect F and Y matrices
	//optimize theta vector
	T_REAL* plntheta = new T_REAL [nx];
	T_REAL* plen = new T_REAL [nx];
	T_REAL* pmax = new T_REAL [nx];
	T_REAL* pmin = new T_REAL [nx];
	CSimplex sp;
	sp.SetKrigingPointer(&krig);
	sp.SetNvar(nx);
	sp.AllocateMemory();
	for (int i=0; i<nx; i++)
	{
		plntheta[i] = 0;
		plen[i] = 1;
		pmax[i] = 5;
		pmin[i] = -3;
	}
	sp.InitSimplex(plntheta,plen,pmax,pmin);
	sp.Optimize();
	delete [] plntheta;
	delete [] plen;
	delete [] pmax;
	delete [] pmin;
	return 0;
}

void CYROM::RemoveDuplicateDesignSites(bool* pbremove)
{
	int i, j;
	int ncase_new;
	//calculate new ncase
	ncase_new = 0;
	for (i=0; i<ncase; i++)
	{
		if (!pbremove[i])
			ncase_new++;
	}
	T_REAL** ppy_new = new T_REAL* [ncase_new];
	j = 0;
	for (i=0; i<ncase; i++)
	{
		if (!pbremove[i])
		{
			ppy_new[j] = ppy[i];
			j++;
		}
	}
	delete [] ppy;
	ppy = ppy_new;
	ncase = ncase_new;
}

void CYROM::Interpolate(T_REAL* px, T_REAL* py)
{
	krig.Interpolate(px, py);
}

void CYROM::WriteCapeOpenFile(FILE* pf)
{
	krig.WriteCapeOpenFile(pf);
}

void CYROM::WriteACMFile(FILE* pf)
{
	krig.WriteACMFile(pf);
	//assume the ROM ACM file is appended to the Java ACM file, add "END" to indicate the end of file
	fprintf(pf,"END\n");
}
