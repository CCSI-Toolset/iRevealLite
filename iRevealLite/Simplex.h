//Simplex.h
//Nelder-Mead method
#ifndef __SIMPLEX_H__
#define __SIMPLEX_H__

#include "CCSI.h"
#include "Kriging.h"

class CSimplex
{
private:
	int nvar;				//number of input variables
	int nite_max;			//maximum number of iterations allowed
	T_REAL   cref;			//reflection coefficient
	T_REAL   cexp;			//expansion coefficient
	T_REAL   ccon;			//contraction coefficient
	T_REAL   cshr;			//shrinking coefficient
	T_REAL   ftolr;			//relative error of objective function for convergence
	T_REAL   ftola;			//absolute error of objective function for convergence
	T_REAL*  psum;			//sum of all simplex points, for base face centriod calculation, nvar elements
	T_REAL*  pnew;			//new point vector, nvar+1 elements
	T_REAL*  psave;			//saved point vector, nvar+1 elements
	T_REAL*  pmax;			//array of maximum allowed values
	T_REAL*  pmin;			//array of minimum allowed values
	T_REAL** pps;			//simplex with nvar+1 vectors, each vector has nvar+1 element with last element as objective function

	CKriging* pkg;			//pointer to a Kriging object

	void DeleteMemory();	//memory cleanup handled by destructor
	
public:
	CSimplex();
	virtual ~CSimplex();
	void AllocateMemory();
	void SetNvar(int n) {nvar = n; nite_max=n*50;}
	void SetKrigingPointer(CKriging* pk) {pkg=pk;}
	void InitSimplex(T_REAL* p0, T_REAL* plen, T_REAL* pmx, T_REAL* pmn);
	void CalcPsum();
	void SortSimplex();
	void CalcNewPoint(T_REAL coef);
	void ShrinkSimplex();
	int Optimize();
	T_REAL CalcObjectiveFunction(T_REAL* pvar);
};

#endif