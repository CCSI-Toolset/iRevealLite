//Kriging.h

#ifndef __KRIGING_H__
#define __KRIGING_H__

#include <stdio.h>
#include "Matrix.h"
#include "DesignSite.h"

//Typical procedure to call functions in CKriging
//SetRegressionModelOption()
//SetCorrelationOption()
//SetDimensions()
//AllocateMemory();
//SetDesignSiteInputArray()
//SetDesignSiteOutputArray()
//NormalizeRawDesignSites()
//SetThetaVector()
//CalcAllMatrices()
//Interpolate() or InterpolateWithErrorEstimate()

//If theta vector needs optimized
//SetRegressionModelOption()
//SetCorrelationOption()
//SetDimensions()
//AllocateMemory();
//SetDesignSiteInputArray()
//SetDesignSiteOutputArray()
//NormalizeRawDesignSites()
//CalcFYMatrices()
//CalcObjectiveFunction() by optimization code
//Interpolate() or InterpolateWithErrorEstimate()

class CKriging
{
private:
	int iregression;		//regression model option, 0=const, 1=linear, 2 = quadratic
	int icorrelation;		//correlation model option, 0=gauss, 1=exponential, 2=linear, 3=spherical, 4=cubic, 5=spline
	int nx;					//number of input parameters
	int ny;					//number of output parameters
	int nf;					//number of regression functions
	int nds;				//number of design sites (known points)
	CDesignSite ds_mean;	//mean of design sites, used for normalization
	CDesignSite ds_sigma;	//standard deviation of design sites, used for normalization
	CDesignSite* pds_raw;	//raw design site array
	CDesignSite* pds_norm;	//normalized design site array
	T_REAL	detR1m;			//determinate of R matrix raised to power of 1/nds, pow(|R|,1/m)
	T_REAL* psigma2;		//normalized sigma^2 array, size is ny
	T_REAL* ptheta;			//correlation parameter array, size is nx, as user input or optimized by optimization code
	CMatrix Y;				//Y matrix, nds row by ny column
	CMatrix Ytilde;			//Y tilde matrix, nds row by ny column
	CMatrix F;				//F matrix, nds row by nf column
	CMatrix Ftilde;			//F tilde matrix, nds row by nf column
	CMatrix R;				//correlation matrix, nds row by nds column
	CMatrix C;				//R=C*C^T, nds row by nds column
	CMatrix G;				//G matrix, nf row by nf column, Ftilde = Q x G^T
	CMatrix Beta;			//beta matrix, nf row by ny column
	CMatrix Gamma;			//gamma matrix, nds row by ny column

//private functions
	void CalcFunctionArray(T_REAL* px, T_REAL* pf);
	T_REAL CalcCorrelation(T_REAL* px1, T_REAL* px2);
	void CalcFMatrix();
	void CalcYMatrix();
	void CalcRMatrix();
	void CalcCMatrices();
	void CalcDetR1m();
	void CalcFtildeMatrix();
	void CalcYtildeMatrix();
	void CalcBetaMatrix();
	void CalcGammaMatrix();
	void DeleteMemory();	//memory cleanup handled by destructor

public:
	CKriging();
	virtual ~CKriging();
	void SetRegressionModelOption(int i);
	void SetCorrelationOption(int i) {icorrelation = i;}
	void SetDimensions(int ix, int iy, int ids) {nx=ix; ny=iy; nds=ids;}
	void SetDesignSiteInputArray(T_REAL** ppin);
	void SetDesignSiteOutputArray(T_REAL** ppout);
	void SetThetaVector(T_REAL* pth);
	void GetDesignSiteInputArrayLimits(T_REAL* pmin, T_REAL* pmax);
	void AllocateMemory();
	int NormalizeRawDesignSites();
	void CalcFYMatrices();
	void CalcAllMatrices();
	void Interpolate(T_REAL* px, T_REAL* py);
	void InterpolateWithErrorEstimate(T_REAL* px, T_REAL* py, T_REAL* pe);
	T_REAL CalcObjectiveFunction(T_REAL* pln);
	void WriteACMFile(FILE* pf);
	void WriteCapeOpenFile(FILE* pf);
	void Write(FILE* pf);
	void Read(FILE* pf);
};

#endif