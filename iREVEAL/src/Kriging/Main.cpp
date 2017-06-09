//Main.cpp
//this is the driver for ROM builder
#include "YROM.h"

int main(int argc, char* argv[])
{
	//argv[1] is option: -i for interpolation, -r for response surface, -v for cross validation, -a for ACM code, -c for CapeOpen output
	//argv[2] is nx, size of input vector
	//argv[3] is ny, size of output vector
	//argv[4] is ncase, number of CFD cases used for building ROM
	//argv[5] is filename of input vectors
	//argv[6] is filename of output vectors
	//argv[7] is filename of output file, input vectors for interpolation, or input for response surface
	//argv[8] is additional input file required if the option is -i and -r
	//execution command example: YROMCmd.exe -i 2 10 20 xvectors.input yvectors.input interploation.output interpolation.input
	//execution command example: YROMCmd.exe -a 2 10 20 xvectors.input yvectors.input rom_script.acmf
	if (argc<8)
	{
		printf("Number of commandline arguments < 8\n");
		return 1;
	}
	if ((!strcmp(argv[1],"-i")) || (!strcmp(argv[1],"-r")))		//interpolation or response surface
	{
		if (argc<9)
		{
			printf("Number of comandline arguments < 9\n");
			return 1;
		}
	}
	bool bsame_input;
	bool* pbremove;
	int i, j, k, icase;
	int nx = atoi(argv[2]);
	int ny = atoi(argv[3]);
	int ncase = atoi(argv[4]);
	int ncase_new;
	CYROM yrom;
	yrom.SetNumberOfCases(ncase);
	yrom.SetNumberOfInputs(nx);
	yrom.SetNumberOfOutputs(ny);
	yrom.AllocateArray();
	yrom.ReadDesignSiteOutputVectors(argv[6]);
	T_REAL* py = new T_REAL[ny];
	T_REAL** ppx = new T_REAL* [ncase];
	T_REAL** ppx_new;
	for (i=0; i<ncase; i++)
		ppx[i] = new T_REAL [nx];
	pbremove = new bool [ncase];
	//read input vector
	char line[500];
	int nerr = 0;
	int iresult;
	double xx;
	FILE* pfin = fopen(argv[5],"r");
	if (pfin==NULL)
		return 1;
	fgets(line,500,pfin);
	for (icase=0; icase<ncase; icase++)
	{
		for (i=0; i<nx; i++)
		{
			iresult = fscanf(pfin,"%lg",&xx);
			if (iresult<1)
			{
				nerr = 2;
				break;
			}
			ppx[icase][i] = xx;
		}
		fgets(line,500,pfin);		//skip line
	}
	fclose(pfin);
	//now filter out any point that has the sample input values
	//a point down in the list overwrite a point above
	pbremove[ncase-1] = false;
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
		}
		delete [] ppx;
		ppx = ppx_new;
		ncase = ncase_new;
	}

	//generate yROM
	yrom.CalcRegression(ppx);
	std::vector<std::string>* pvariable_names = yrom.GetOutputVariableNames();
	int nvaraible_names = pvariable_names->size();
	//open file for output
	FILE* pfout = fopen(argv[7],"w");
	if (pfout==NULL)
		return 1;
	if (!strcmp(argv[1],"-i"))	//interpolation based on provided interpolation input file that contains input vectors
	{
		//now open the input file for interpolation
		pfin = fopen(argv[8],"r");
		if (pfin==NULL)
			return 1;
		//skip first line
		fgets(line,500,pfin);
		for (i=0; i<nvaraible_names-1; i++)
			fprintf(pfout,"%s ",pvariable_names->at(i).c_str());
		fprintf(pfout,"%s\n",pvariable_names->at(nvaraible_names-1).c_str());
		do
		{
			for (i=0; i<nx; i++)
			{
				iresult = fscanf(pfin,"%lg",&xx);
				if (iresult<1)
				{
					nerr = 2;
					break;
				}
				ppx[0][i] = xx;
				printf("%lg\t", xx); 
			}
			printf("\n");
			if (nerr==2) break;
			yrom.Interpolate(ppx[0], py);
			for (i=0; i<ny-1; i++)
				fprintf(pfout,"%lg\t",py[i]);
			fprintf(pfout,"%lg\n",py[ny-1]);
			fgets(line,500,pfin);
		}while (strlen(line)>0 && !feof(pfin));
		fclose(pfin);
		fclose(pfout);
	}
	else
	{
		if (!strcmp(argv[1],"-r"))		//response surface, input file is argv[8]
		{
			int npoints[2];
			int ndim;
			int ix[2];
			T_REAL xmin[2];
			T_REAL xmax[2];
			T_REAL dx[2];
			pfin = fopen(argv[8],"r");
			if (pfin==NULL)
				return 1;
			iresult = fscanf(pfin,"%d",&ndim);
			if (iresult<1)
				return 1;
			fgets(line,500,pfin);
			if (ndim==1)	//1-D response line
			{
				fscanf(pfin,"%d",&npoints[0]);
				fgets(line,500,pfin);
				fscanf(pfin,"%d",&ix[0]);
				if (ix[0]<0 || ix[0]>nx-1)
				{
					printf("Invalid index of input vector\n");
					return 1;
				}
				fgets(line,500,pfin);
				fscanf(pfin,"%lg",&xmin[0]);
				fscanf(pfin,"%lg",&xmax[0]);
				fgets(line,500,pfin);
				for (i=0; i<nx; i++)
					fscanf(pfin,"%lg",&ppx[0][i]);
				dx[0] = (xmax[0]-xmin[0])/(double)(npoints[0]-1);
				for (i=0; i<npoints[0]; i++)
				{
					ppx[0][ix[0]] = xmin[0] + i*dx[0];
					yrom.Interpolate(ppx[0],py);
					//write x
					fprintf(pfout,"%lg\t",ppx[0][ix[0]]);
					//write py out
					for (j=0; j<ny-1; j++)
						fprintf(pfout,"%lg\t",py[j]);
					fprintf(pfout,"%lg\n",py[ny-1]);
				}
			}
			else		//2-D response surface
			{
				fscanf(pfin,"%d",&npoints[0]);
				fscanf(pfin,"%d",&npoints[1]);
				fgets(line,500,pfin);
				fscanf(pfin,"%d",&ix[0]);
				if (ix[0]<0 || ix[0]>nx-1)
				{
					printf("Invalid index of input vector\n");
					return 1;
				}
				fscanf(pfin,"%d",&ix[1]);
				if (ix[1]<0 || ix[1]>nx-1)
				{
					printf("Invalid index of input vector\n");
					return 1;
				}
				fgets(line,500,pfin);
				fscanf(pfin,"%lg",&xmin[0]);
				fscanf(pfin,"%lg",&xmax[0]);
				fgets(line,500,pfin);
				fscanf(pfin,"%lg",&xmin[1]);
				fscanf(pfin,"%lg",&xmax[1]);
				fgets(line,500,pfin);
				for (i=0; i<nx; i++)
					fscanf(pfin,"%lg",&ppx[0][i]);
				dx[0] = (xmax[0]-xmin[0])/(double)(npoints[0]-1);
				dx[1] = (xmax[1]-xmin[1])/(double)(npoints[1]-1);
				for (i=0; i<npoints[0]; i++)
				{
					ppx[0][ix[0]] = xmin[0] + i*dx[0];
					for (j=0; j<npoints[1]; j++)
					{
						ppx[0][ix[1]] = xmin[1] + j*dx[1];
						yrom.Interpolate(ppx[0],py);
						//write x
						fprintf(pfout,"%lg\t%lg\t",ppx[0][ix[0]], ppx[0][ix[1]]);
						//write py out
						for (k=0; k<ny-1; k++)
							fprintf(pfout,"%lg\t",py[k]);
						fprintf(pfout,"%lg\n",py[ny-1]);
					}
					fprintf(pfout,"\n");		//separate rows by a line
				}
			}
			fclose(pfin);
			fclose(pfout);
		}
		else
		{
			if (!strcmp(argv[1],"-v"))		//cross validation
			{
				T_REAL* px_new;
				T_REAL** ppx_new;
				T_REAL** ppy_new;
				T_REAL** ppy_yrom = yrom.GetDesignSiteOutputVectors();
				CYROM yrom_new;
				ppx_new = new T_REAL* [ncase-1];
				ppy_new = new T_REAL* [ncase-1];
				yrom_new.SetNumberOfInputs(nx);
				yrom_new.SetNumberOfCases(ncase-1);
				yrom_new.SetNumberOfOutputs(ny);
				yrom_new.AllocateArray();
				for (i=0; i<nvaraible_names-1; i++)
					fprintf(pfout,"%s ",pvariable_names->at(i).c_str());
				fprintf(pfout,"%s\n",pvariable_names->at(nvaraible_names-1).c_str());
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
					px_new = ppx[i];
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
					yrom_new.Interpolate(px_new,py);
					//write py out
					for (j=0; j<ny-1; j++)
						fprintf(pfout,"%lg\t",py[j]);
					fprintf(pfout,"%lg\n",py[ny-1]);
				}
				delete [] ppx_new;
				delete [] ppy_new;
				fclose(pfout);
			}
			else
			{
				if (!strcmp(argv[1],"-a"))		//export ACM file
				{
					yrom.WriteACMFile(pfout);
					fclose(pfout);
				}
				else
				{
					if (!strcmp(argv[1],"-c"))		//export Cape-Open file
					{
						yrom.WriteCapeOpenFile(pfout);
						fclose(pfout);
					}
				}
			}
		}
	}
	delete [] py;
	for (i=0; i<ncase; i++)
		delete [] ppx[i];
	delete [] ppx;
	return 0;
}