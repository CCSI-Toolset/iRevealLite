//YROM.cpp
#include "YROM.h"
#include "Simplex.h"
#include "Util.h"

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

void CYROM::SetCaseFiles(const char* foldername, const char* basename, const char* rstfilename)
{
	//add ncase file names to the filenames vector
	//assume basename is the baseline case folder name and parametric case folder names are indexed from 1
	int i;
	char fname[500];
	filenames.clear();
	for (i=0; i<ncase; i++)
	{ 
		sprintf(fname,"%s\\%s_%d\\%s",foldername,basename,i+1,rstfilename);
		filenames.push_back(fname);
	}
}

int CYROM::ReadSimulationResults()
{
	int i;
	int nerr = 0;
	char fname[500];
	for (i=0; i<ncase; i++)
	{
		strcpy(fname,filenames[i].c_str());
		if (ReadPostProcessingResultFile(fname, ppy[i]))
			nerr++;
	}
	return nerr;
}

int CYROM::ReadPostProcessingResultFile(char* fname, T_REAL* py)
{
	//assume the result file is ascii file data arranged line by line in the order of y vector
	int iresult;		//fscanf return value, -1 if error
	int nerr = 0;
	char line[500];
	float yy;
	FILE* pf = fopen(fname,"r");
	if (pf==NULL)
		return 1;
	for (int i=0; i<ny; i++)
	{
		iresult = fscanf(pf,"%f",&yy);
		if (iresult<1)
		{
			nerr = 2;
			break;
		}
		fgets(line,500,pf);		//skip line
		py[i] = yy;
	}
	fclose(pf);
	return nerr;
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

void CYROM::Write(FILE* pf)
{
	int i;
	int iversion = 0;
	int noutputname = outputnames.size();
	int nfilename = filenames.size();
	bool ballocated = ppy!=NULL;
	fwrite(&iversion,sizeof(int),1,pf);
	fwrite(&nx,sizeof(int),1,pf);
	fwrite(&ny,sizeof(int),1,pf);
	fwrite(&ncase,sizeof(int),1,pf);
	fwrite(&ballocated,sizeof(bool),1,pf);
	if (ballocated)
	{
		for (i=0; i<ncase; i++)
			fwrite(ppy[i],sizeof(T_REAL),ny,pf);
	}
	fwrite(&noutputname,sizeof(int),1,pf);
	for (i=0; i<noutputname; i++)
		CUtil::WriteString(outputnames[i],pf);
	fwrite(&nfilename,sizeof(int),1,pf);
	for (i=0; i<nfilename; i++)
		CUtil::WriteString(filenames[i],pf);
	krig.Write(pf);
}

void CYROM::Read(FILE* pf)
{
	int i;
	int iversion;
	int noutputname;
	int nfilename;
	bool ballocated;
	std::string str;
	fread(&iversion,sizeof(int),1,pf);
	fread(&nx,sizeof(int),1,pf);
	fread(&ny,sizeof(int),1,pf);
	fread(&ncase,sizeof(int),1,pf);
	fread(&ballocated,sizeof(bool),1,pf);
	if (ballocated)
	{
		AllocateArray();
		for (i=0; i<ncase; i++)
			fread(ppy[i],sizeof(T_REAL),ny,pf);
	}
	fread(&noutputname,sizeof(int),1,pf);
	outputnames.clear();
	for (i=0; i<noutputname; i++)
	{
		CUtil::ReadString(str,pf);
		outputnames.push_back(str);
	}
	fread(&nfilename,sizeof(int),1,pf);
	filenames.clear();
	for (i=0; i<nfilename; i++)
	{
		CUtil::ReadString(str,pf);
		filenames.push_back(str);
	}
	krig.Read(pf);
}