//Util.cpp
#include <math.h>
#include "Util.h"
#include "LHS.h"
#include "YROM.h"
#ifdef WIN32
#include <Windows.h>
#else
#include <unistd.h>
#endif

string GetExecutableDirectory()
{
	std::string path = "";
#ifdef WIN32
	char szAppPath[MAX_PATH];
	GetModuleFileNameA(0, szAppPath, MAX_PATH);
	path = szAppPath;
	string::size_type t = path.find_last_of("\\");
	path = path.substr(0,t+1);
#else
	pid_t pid = getpid();
	char buf[10];
	sprintf(buf,"%d",pid);
	std::string _link = "/proc/";
	_link.append( buf );
	_link.append( "/exe");
	char proc[512];
	int ch = readlink(_link.c_str(),proc,512);
	if (ch != -1)
	{ 
		proc[ch] = 0; 
		path = proc; 
	}
	string::size_type t = path.find_last_of("/");
	path = path.substr(0,t+1);
#endif
	return path;
}

void PrintCommandUsage()
{
	printf("iRevealLite command usage:\n");
	printf("\t-s *.json\tprovide a JSON configuration file to sample input space and create iRevealLite.csv and a temporary ACM file.\n");
	printf("\t-b\t\tuse iRevealLite.csv in current working directory to build ROM, finalize ACM file and do cross validation.\n");
}

int ProcessJsonAndSampleInputSpace(char* filename)
{
	string str = "java -cp \"";
	str += GetExecutableDirectory();
	str += "iRevealLite.jar\" DataModel.UnitOperation -a ";
	str += filename;
	printf("%s\n",str.c_str());
	//call java code to create iRevealLite.io and 1st part of acmf file
	system(str.c_str());
	FILE* pfin = fopen("iRevealLite.io","r");
	FILE* pfout = fopen("iRevealLite.csv","w");
	if (pfin==NULL)
	{
		printf("Unable to open iRevealLite.io file for reading!\n");
		return -1;
	}
	if (pfout==NULL)
	{
		printf("Unable to open iRevealLite.csv file for writing!\n");
		return -1;
	}
	//sample input space and write csv file for input and output
	char line[500];		//a line in iRevealLite.io file
	int i;				//counter
	int ncase;			//number of high-fidelity model cases
	int nx;				//number of input variables
	int ny;				//number of output variables
	double xl, xu;		//lower and upper limit of input variable
	string rom_name;	//reduced order model name, also used for file name
	vector<string> vname;		//input variable name list
	vector<double> vxl;
	vector<double> vxu;
	fscanf(pfin, "%s", line);
	rom_name = line;
	fgets(line, 499, pfin);
	fscanf(pfin, "%d", &ncase);
	fgets(line, 499, pfin);
	fscanf(pfin, "%d", &nx);
	fgets(line, 499, pfin);
	for (i=0; i<nx; i++)
	{
		fscanf(pfin,"%s %lg %lg", line, &xl, &xu);
		str = line;
		vname.push_back(str);
		vxl.push_back(xl);
		vxu.push_back(xu);
		fgets(line, 499, pfin);
	}
	fscanf(pfin,"%d", &ny);
	fgets(line, 499, pfin);
	for (i=0; i<ny; i++)
	{
		fscanf(pfin,"%s", line);
		str = line;
		vname.push_back(str);
		fgets(line,499,pfin);
	}
	fclose(pfin);
	//always use the same seed
	srand(0);
	CLHS lhs;
	lhs.SetDimension(nx);
	lhs.SetNumberOfPoints(ncase);
	lhs.AllocateLimitArrays();
	lhs.AllocateSampleArray();
	lhs.SetLowerLimits(&vxl[0]);
	lhs.SetUpperLimits(&vxu[0]);
	lhs.MaxMinDistanceSampling();
	fprintf(pfout, "I/O data for %s", rom_name.c_str());
	for (i=0; i<nx; i++)
		fprintf(pfout,",Input");
	for (i=0; i<ny; i++)
		fprintf(pfout,",Output");
	fprintf(pfout,"\nCase");
	for (i=0; i<nx+ny; i++)
		fprintf(pfout,",%s",vname[i].c_str());
	fprintf(pfout,"\n");
	lhs.WriteSamples(pfout);
	fclose(pfout);
	printf("Input space is sampled successfully.\nPlease use input data in iRevealLite.csv to run high-fidelity models and then fill in the output data.\n");
	return 0;
}

int BuildKrigingRom()
{
	int i, j, k;
	int icase;
	int nx;
	int ny;
	int ncase;
	int ncase_new;
	int iresult;
	double xx;
	char line[2001];
	string rom_name;
	string str;
	string str1;
	CYROM yrom;
	vector<string> inputnames;
	vector<string>* poutputnames = yrom.GetOutputVariableNames();
	FILE* pfio = fopen("iRevealLite.io","r");
	if (pfio==NULL)
	{
		printf("Unable to open iRevealLite.io file for reading!\n");
		return 1;
	}
	FILE* pfcsv = fopen("iRevealLite.csv","r");
	if (pfcsv==NULL)
	{
		printf("Unable to open iRevealLite.csv file for reading!\n");
		return 1;
	}
	//read iRevealLite.io file
	//rom name
	fscanf(pfio, "%s", line);
	rom_name = line;
	fgets(line, 499, pfio);
	//number of cases
	fscanf(pfio, "%d", &ncase);
	fgets(line, 499, pfio);
	//number of input variables
	fscanf(pfio, "%d", &nx);
	fgets(line, 499, pfio);
	for (i=0; i<nx; i++)
	{
		fscanf(pfio, "%s", line);
		str = line;
		inputnames.push_back(str);
		fgets(line, 499, pfio);
	}
	//number of output variables
	fscanf(pfio, "%d", &ny);
	fgets(line, 499, pfio);
	for (i=0; i<ny; i++)
	{
		fscanf(pfio, "%s", line);
		str = line;
		poutputnames->push_back(str);
		fgets(line, 499, pfio);
	}
	fclose(pfio);
	//open acmf file
	str = rom_name;
	str.append(".acmf");
	FILE* pfacm = fopen(str.c_str(),"r");
	if (pfacm==NULL)
	{
		printf("Unable to open acmf file for appending!\n");
		return 1;
	}
	//check if the last line is a comment line like this: //Regression variables and equations need to be appended
	while (!feof(pfacm))
	{
		fgets(line, 2000, pfacm);
	}
	fclose(pfacm);
	str = line;
	if (str.compare("//Regression variables and equations need to be appended\n"))
	{
		printf("The acmf file is not valid for appending regression results!\n");
		return 1;
	}
	yrom.SetNumberOfCases(ncase);
	yrom.SetNumberOfInputs(nx);
	yrom.SetNumberOfOutputs(ny);
	yrom.AllocateArray();
	//now read the input/output data and remove any case with same input vector
	bool bsame_input;
	bool* pbremove;
	T_REAL** ppy = yrom.GetDesignSiteOutputVectors();
	T_REAL** ppx = new T_REAL* [ncase];
	T_REAL** ppx_new;
	for (i=0; i<ncase; i++)
		ppx[i] = new T_REAL [nx];
	pbremove = new bool [ncase];
	//read iRevealLite.csv file
	fgets(line,2000,pfcsv);		//skip 1st line
	fgets(line,2000,pfcsv);		//skip 2nd line
	for (icase=0; icase<ncase; icase++)
	{
		fgets(line,2000,pfcsv);
		str = line;
		j = str.find(',');
		str = str.substr(j+1);	//skip case number
		for (i=0; i<nx; i++)
		{
			j = str.find(',');
			if (j<1)		//',' not found or empty between two ','
			{
				printf("Error in reading iRevealLite.csv file!\n");
				return 1;
			}
			str1 = str.substr(0,j);
			str = str.substr(j+1);
			iresult = sscanf(str1.c_str(),"%lg",&xx);
			if (iresult==1)
				ppx[icase][i] = xx;
			else
			{
				printf("Data missing in the csv file!\n");
				return 1;
			}
		}
		for (i=0; i<ny-1; i++)
		{
			j = str.find(',');
			if (j<1)		//',' not found or empty between two ','
			{
				printf("Error in reading iRevealLite.csv file!\n");
				return 1;
			}
			str1 = str.substr(0,j);
			str = str.substr(j+1);
			iresult = sscanf(str1.c_str(),"%lg",&xx);
			if (iresult==1)
				ppy[icase][i] = xx;
			else
			{
				printf("Data missing in the csv file!\n");
				return 1;
			}
		}
		//last output variable
		iresult = sscanf(str.c_str(),"%lg",&xx);
		if (iresult==1)
			ppy[icase][ny-1] = xx;
		else
		{
			printf("Data missing in the csv file!\n");
			return 1;
		}
	}
	fclose(pfcsv);
	//now filter out any point that has the same input values
	pbremove[ncase-1] = false;		//last one never removed
	for (i=0; i<ncase-1; i++)
	{
		pbremove[i] = false;
		for (j=i+1; j<ncase; j++)
		{
			bsame_input = true;
			for (k=0; k<nx; k++)
			{
				if (ppx[i][k]!=ppx[j][k])
				{
					bsame_input = false;
					break;
				}
			}
			if (bsame_input)
			{
				printf("Point %d and Point %d have the same input values!\nPoint %d is removed.\n", i+1, j+1, i+1);
				break;
			}
		}
		if (bsame_input)
		{
			pbremove[i] = true;
			continue;
		}
	}
	ncase_new = 0;
	for (i=0; i<ncase; i++)
	{
		if (!pbremove[i])
			ncase_new++;
	}
	if (ncase_new!=ncase)		//assign new set of design sites
	{
		//remove duplicate output data
		yrom.RemoveDuplicateDesignSites(pbremove);
		//remove duplicate output data
		ppx_new = new T_REAL* [ncase_new];
		j = 0;
		for (i=0; i<ncase; i++)
		{
			if (!pbremove[i])
			{
				ppx_new[j] = ppx[i];
				j++;
			}
			else
				delete [] ppx[i];
		}
		delete [] ppx;
		ppx = ppx_new;
		ncase = ncase_new;
	}
	delete [] pbremove;
	//generate yROM
	yrom.CalcRegression(ppx);
	//reopen acmf file for appending
	str = rom_name;
	str.append(".acmf");
	pfacm = fopen(str.c_str(),"a");
	yrom.WriteACMFile(pfacm);
	fclose(pfacm);
	//do cross validation
	T_REAL* px_int;			//input for interpolation
	T_REAL* py_int;			//output for inlerpolation
	T_REAL** ppy_new;
	T_REAL** ppy_yrom = yrom.GetDesignSiteOutputVectors();
	FILE* pfcvd = fopen("iRevealLite_cross_validation.csv","w");
	if (pfcvd==NULL)
	{
		printf("Unable to open iRevealLite_cross_validation.csv for writting!\n");
		return 1;
	}
	CYROM yrom_new;
	py_int = new T_REAL [ny];
	ppx_new = new T_REAL* [ncase-1];
	ppy_new = new T_REAL* [ncase-1];
	yrom_new.SetNumberOfInputs(nx);
	yrom_new.SetNumberOfCases(ncase-1);
	yrom_new.SetNumberOfOutputs(ny);
	yrom_new.AllocateArray();
	//headers
	fprintf(pfcvd,"Cross validation for %s", rom_name.c_str());
	for (i=0; i<nx; i++)
		fprintf(pfcvd,",Input");
	for (i=0; i<ny; i++)
		fprintf(pfcvd,",Output");
	for (i=0; i<ny; i++)
		fprintf(pfcvd,",ROM");
	for (i=0; i<ny; i++)
		fprintf(pfcvd,",Error");
	fprintf(pfcvd,"\nCase");
	for (i=0; i<nx; i++)
		fprintf(pfcvd,",%s",inputnames[i].c_str());
	for (i=0; i<ny; i++)
		fprintf(pfcvd,",%s",poutputnames->at(i).c_str());
	for (i=0; i<ny; i++)
		fprintf(pfcvd,",%s",poutputnames->at(i).c_str());
	for (i=0; i<ny; i++)
		fprintf(pfcvd,",%s",poutputnames->at(i).c_str());
	fprintf(pfcvd,"\n");
	for (i=0; i<ncase; i++)		//loop over each removed data point
	{
		//get input design sites from LHS
		for (j=0; j<ncase-1; j++)
		{
			if (j<i)
				ppx_new[j] = ppx[j];
			else
				ppx_new[j] = ppx[j+1];
		}
		px_int = ppx[i];
		//get output design sites from yrom
		for (j=0; j<ncase-1; j++)
		{
			if (j<i)
				ppy_new[j] = ppy_yrom[j];
			else
				ppy_new[j] = ppy_yrom[j+1];
		}
		yrom_new.SetDesignSiteOutputVectors(ppy_new);
		//do regression
		yrom_new.CalcRegression(ppx_new);
		//interpolate
		yrom_new.Interpolate(px_int,py_int);
		//write results
		fprintf(pfcvd,"%d",i+1);
		for (j=0; j<nx; j++)
			fprintf(pfcvd,",%lg",px_int[j]);
		for (j=0; j<ny; j++)
			fprintf(pfcvd,",%lg",ppy_yrom[i][j]);
		for (j=0; j<ny; j++)
			fprintf(pfcvd,",%lg",py_int[j]);
		for (j=0; j<ny; j++)
		{
			xx = py_int[j] - ppy_yrom[i][j];
			if (fabs(ppy_yrom[i][j])>TINY)
				xx /= ppy_yrom[i][j];
			else
				xx = 0;
			fprintf(pfcvd,",%lg",xx);
		}
		fprintf(pfcvd,"\n");
	}
	fclose(pfcvd);
	//clean up memory
	delete [] py_int;
	delete [] ppx_new;
	delete [] ppy_new;
	for (i=0; i<ncase; i++)
		delete [] ppx[i];
	delete [] ppx;
	printf("%s.acmf is updated successfully and is a valid ACM file.\n",rom_name.c_str());
	return 0;
}