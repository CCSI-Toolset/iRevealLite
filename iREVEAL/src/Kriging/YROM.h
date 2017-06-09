//YROM.h

#ifndef __YROM_H__
#define __YROM_H__

#include <string>
#include <vector>
#include "CCSI.h"
#include "Kriging.h"

class CYROM
{
	private:
	int nx;									//number of input variables used for ROM
	int ny;									//number of output variables used for ROM
	int ncase;								//number of CFD simulation cases
	T_REAL** ppy;							//array of y vectors
	std::vector<std::string> outputnames;	//array of output variable name, dimension is ny
	std::vector<std::string> filenames;		//post mfix filenames, dimension is ncase

	CKriging krig;							//Ckriging object with input data and regression matrices
	
	void DeleteArray();

public:
	CYROM();
	virtual ~CYROM();
	void AllocateArray();
	void SetNumberOfInputs(int n) {nx=n;}
	void SetNumberOfOutputs(int n) {ny=n;}
	void SetNumberOfCases(int n) {ncase=n;}
	void SetDesignSiteOutputVectors(T_REAL** pp);
	int GetNumberOfInputVariables() {return nx;}
	int GetNumberOfOutputVariables() {return ny;}
	T_REAL** GetDesignSiteOutputVectors() {return ppy;}
	std::vector<std::string>* GetOutputVariableNames() {return &outputnames;}
	std::vector<std::string>* GetResultFileNames() {return &filenames;}
	void SetCaseFiles(const char* foldername, const char* basename, const char* rstfilename);
	int ReadSimulationResults();
	int ReadPostProcessingResultFile(char* fname, T_REAL* py);
	int ReadDesignSiteOutputVectors(char* fname);
	int CalcRegression(T_REAL** ppin);
	void RemoveDuplicateDesignSites(bool* pbremove);
	void Interpolate(T_REAL* px, T_REAL* py);
	void WriteCapeOpenFile(FILE* pf);
	void WriteACMFile(FILE* pf);
	void Write(FILE* pf);
	void Read(FILE* pf);
};

#endif
