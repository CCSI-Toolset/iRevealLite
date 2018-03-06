//DesignSite.h

#ifndef __DESIGNSITE_H__
#define __DESIGNSITE_H__

#include "CCSI.h"

class CDesignSite
{
private:
	int nx;					//number of input parameters, not static in case multiple Kriging models running multi-threaded
	int ny;					//number of output parameters, not static in case multiple Kriging models running multi-threaded
	T_REAL* px;				//input parameter array
	T_REAL* py;				//output parameter array

	void DeleteArrays();	//memory cleanup handled by destructor

public:	
	CDesignSite();
	CDesignSite(int ix, int iy);
	virtual ~CDesignSite();
	void SetNumberOfInputParameters(int ix) {nx = ix;}
	void SetNumberOfOutputParameters(int iy) {ny = iy;}
	void AllocateArrays();
	T_REAL* GetInputData() {return px;}
	T_REAL* GetOutputData() {return py;}
	void SetInputData(T_REAL* pxi);
	void SetOutputData(T_REAL* pyo);
	void SetAndNormalizeInputData(T_REAL* pxi, CDesignSite& mean, CDesignSite& sigma);
	void ConvertToRawOutputData(T_REAL* pyo, CDesignSite& mean, CDesignSite& sigma);
	void WriteTextFile(FILE* pf);
	void Write(FILE* pf);
	void Read(FILE* pf);
	static int Normalize(int n, CDesignSite* pdsr, CDesignSite* pdsn, CDesignSite& mean, CDesignSite& sigma);
};

#endif