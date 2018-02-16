
//LHS.h

#ifndef __LHS_H__
#define __LHS_H__

#include "CCSI.h"

class CLHS
{
private:
	int	ndim;				//number of dimension
	int npoint;				//number of points
	T_REAL* pdefault;		//default value of baseline case
	T_REAL* plower;			//lower limit
	T_REAL* pupper;			//upper limit
	T_REAL** ppx;			//LSH samples
	void DeleteLimitArrays();
	void DeleteSampleArray();

public:
	CLHS();
	virtual ~CLHS();
	CLHS(const CLHS &t);
	CLHS& operator=(const CLHS& t);
	void AllocateLimitArrays();
	void AllocateSampleArray();
	void SetDimension(int n);
	void SetNumberOfPoints(int n);
	void SetDefaults(T_REAL* pd);
	void SetLowerLimits(T_REAL* pl);
	void SetUpperLimits(T_REAL* pu);
	void SimpleSampling();
	void MaxMinDistanceSampling();
	int GetDimension() {return ndim;}
	int GetNumberOfPoints() {return npoint;}
	T_REAL* GetDefaults() {return pdefault;}
	T_REAL* GetLowerLimits() {return plower;}
	T_REAL* GetUpperLimits() {return pupper;}
	T_REAL** GetLHSPoints() {return ppx;}
	bool AreLimitArraysAailable() {return pdefault!=NULL;}
	bool IsSampleArrayAailable() {return ppx!=NULL;}
	void WriteSamples2D(char* filename, int ix, int iy);
	void WriteSamples3D(char* filename, int ix, int iy, int iz);
	void WriteSamples(FILE* pf);
	double CalcQualityPhi(double p);
};

#endif