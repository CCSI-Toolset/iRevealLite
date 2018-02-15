//Simplex.cpp
#include <cmath>
#include "Simplex.h"

CSimplex::CSimplex()
{
	nvar = 2;
	nite_max = 150;
	cref = 1;
	cexp = 2;
	ccon = 0.5;
	cshr = 0.5;
	ftolr = 0.0001;
	ftola = 0;
	psum = NULL;
	pnew = NULL;
	psave = NULL;
	pmax = NULL;
	pmin = NULL;
	pps = NULL;
	pkg = NULL;			//calling program take care of memory allocation and deletion
}

CSimplex::~CSimplex()
{
	DeleteMemory();
}

void CSimplex::AllocateMemory()
{
	DeleteMemory();
	psum = new T_REAL [nvar];
	pnew = new T_REAL [nvar+1];
	psave = new T_REAL [nvar+1];
	pmax = new T_REAL [nvar];
	pmin = new T_REAL [nvar];
	pps = new T_REAL* [nvar+1];
	for (int i=0; i<=nvar; i++)
		pps[i] = new T_REAL [nvar+1];
}

void CSimplex::DeleteMemory()
{
	if (psum!=NULL)
	{
		delete [] psum;
		psum = NULL;
	}
	if (pnew!=NULL)
	{
		delete [] pnew;
		pnew = NULL;
	}
	if (psave!=NULL)
	{
		delete [] psave;
		psave = NULL;
	}
	if (pmax!=NULL)
	{
		delete [] pmax;
		pmax = NULL;
	}
	if (pmin!=NULL)
	{
		delete [] pmin;
		pmin = NULL;
	}
	if (pps!=NULL)
	{
		for (int i=0; i<=nvar; i++)
			delete [] pps[i];
		delete [] pps;
		pps = NULL;
	}
}

void CSimplex::InitSimplex(T_REAL* p0, T_REAL* plen, T_REAL* pmx, T_REAL* pmn)
{
	//p0 is the first point and plen is the offset length in each direction
	//nvar elements in both p0 and plen
	int i, j;
	for (i=0; i<nvar; i++)
	{
		pps[0][i] = p0[i];
		pmax[i] = pmx[i];
		pmin[i] = pmn[i];
	}
	for (j=1; j<=nvar; j++)
	{
		for (i=0; i<nvar; i++)
			pps[j][i] = p0[i];
		pps[j][j-1] += plen[j-1];
	}
	for (j=0; j<=nvar; j++)
		CalcObjectiveFunction(pps[j]);
	CalcPsum();
}

void CSimplex::CalcPsum()
{
	int i, j;
	for (i=0; i<nvar; i++)
	{
		psum[i] = 0;
		for (j=0; j<=nvar; j++)
			psum[i] += pps[j][i];
	}
}

void CSimplex::SortSimplex()
{
	//bubble sort the simplex, form lowest to highest
	int i, j;
	T_REAL* ptmp;
	for (i=0; i<nvar; i++)
	{
		for (j=i+1; j<=nvar; j++)
		{
			if (pps[j][nvar]<pps[i][nvar])
			{
				ptmp = pps[j];
				pps[j] = pps[i];
				pps[i] = ptmp;
			}
		}
	}
}

void CSimplex::CalcNewPoint(T_REAL coef)
{
	int i;
	T_REAL cnew = (coef+1)/nvar;
	T_REAL cnew1 = cnew + coef;
	for (i=0; i<nvar; i++)
	{
		pnew[i] = cnew*psum[i] - cnew1*pps[nvar][i];
		//check if inside limit, currently hard wired
		if (pnew[i]<pmin[i])
			pnew[i] = pmin[i];
		if (pnew[i]>pmax[i])
			pnew[i] = pmax[i];
	}
}

void CSimplex::ShrinkSimplex()
{
	int i, j;
	for (i=1; i<=nvar; i++)
	{
		for (j=0; j<nvar; j++)
			pps[i][j] = pps[0][j] + cshr*(pps[i][j]-pps[0][j]);
		CalcObjectiveFunction(pps[i]);
	}
}

int CSimplex::Optimize()
{
	//return 0 if converges with nite_max
	//otherwise return number of iterations performed
	int i;
	int nite = 0;			//iteration count
	T_REAL ferra;			//absolute error of objective function
	T_REAL ferrr;			//relative error of objective function
	T_REAL fref;			//objective function evaluated at reflected point
	T_REAL fexp;			//objective function evaluated at expension point
	T_REAL fcon;			//objective function evaluated at contraction point
	T_REAL* ptmp;
	do
	{
		SortSimplex();
		ferra = fabs(pps[nvar][nvar]-pps[0][nvar]);
		ferrr = ferra/(fabs(pps[0][nvar])+TINY);
		if (ferrr<ftolr || ferra<ftola)
		{
			CalcObjectiveFunction(pps[0]);
			return nite;
		}
		nite++;
		//reflection
		CalcNewPoint(cref);
		fref = CalcObjectiveFunction(pnew);
		if (fref<pps[nvar-1][nvar] && fref>pps[0][nvar])	//swap reflection point with worst point
		{
			//update psum
			for (i=0; i<nvar; i++)
				psum[i] += pnew[i] - pps[nvar][i];
			ptmp = pnew;
			pnew = pps[nvar];
			pps[nvar] = ptmp;
		}
		else if (fref<pps[0][nvar])		//expansion
		{
			//save the reflection point
			ptmp = psave;
			psave = pnew;
			pnew = ptmp;
			CalcNewPoint(cexp);
			fexp = CalcObjectiveFunction(pnew);
			if (fexp<fref)		//swap expansion point with worst point
			{
				//update psum
				for (i=0; i<nvar; i++)
					psum[i] += pnew[i] - pps[nvar][i];
				ptmp = pnew;
				pnew = pps[nvar];
				pps[nvar] = ptmp;
			}
			else				//swap reflection point with worst point
			{
				//update psum
				for (i=0; i<nvar; i++)
					psum[i] += psave[i] - pps[nvar][i];
				ptmp = psave;
				psave = pps[nvar];
				pps[nvar] = ptmp;
			}
		}
		else		//contraction
		{
			CalcNewPoint(-ccon);
			fcon = CalcObjectiveFunction(pnew);
			if (fcon<pps[nvar][nvar])		//swap contraction point with worst point
			{
				//update psum
				for (i=0; i<nvar; i++)
					psum[i] += pnew[i] - pps[nvar][i];
				ptmp = pnew;
				pnew = pps[nvar];
				pps[nvar] = ptmp;
			}
			else			//shrinking
			{
				ShrinkSimplex();
				CalcPsum();
			}
		}
	}while (nite<nite_max);
	//make sure the best input parameters are used
	CalcObjectiveFunction(pps[0]);
	return nite;
}

T_REAL CSimplex::CalcObjectiveFunction(T_REAL* pvar)
{
	//assign the result to the last element in pvar and also return it
	T_REAL fun = 0;
	fun = pkg->CalcObjectiveFunction(pvar);
	pvar[nvar] = fun;
	return fun;
}