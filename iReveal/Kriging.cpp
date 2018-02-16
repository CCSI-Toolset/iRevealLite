//Kriging.cpp
#include <cmath>
#include "Kriging.h"

CKriging::CKriging()
{
	nx = 1;
	ny = 1;
	nds = 1;
	psigma2 = NULL;
	ptheta = NULL;
	pds_raw = NULL;
	pds_norm = NULL;
}

CKriging::~CKriging()
{
	DeleteMemory();
}

void CKriging::SetRegressionModelOption(int i)
{
	iregression = i;
	switch (i)
	{
	case 0:
		nf = 1;
		break;
	case 1:
		nf = nx + 1;
		break;
	case 2:
		nf = (nx + 1)*(nx + 2)/2;
		break;
	}
}

void CKriging::SetDesignSiteInputArray(T_REAL** ppin)
{
	for (int i=0; i<nds; i++)
		pds_raw[i].SetInputData(ppin[i]);
}

void CKriging::SetDesignSiteOutputArray(T_REAL** ppout)
{
	for (int i=0; i<nds; i++)
		pds_raw[i].SetOutputData(ppout[i]);
}

void CKriging::SetThetaVector(T_REAL* pth)
{
	for (int i=0; i<nx; i++)
		ptheta[i] = pth[i];
}

void CKriging::GetDesignSiteInputArrayLimits(T_REAL* pmin, T_REAL* pmax)
{
	int i, j;
	T_REAL* px = pds_raw[0].GetInputData();
	for (i=0; i<nx; i++)
	{
		pmin[i] = px[i];
		pmax[i] = px[i];
	}
	for (j=1; j<nds; j++)
	{
		px = pds_raw[j].GetInputData();
		for (i=0; i<nx; i++)
		{
			if (px[i]<pmin[i])
				pmin[i] = px[i];
			if (px[i]>pmax[i])
				pmax[i] = px[i];
		}
	}
}

void CKriging::AllocateMemory()
{
	//dimensions nx, ny and nds have to be set before this call
	//SetRegressionModelOption() should be called before calling this function
	int i;
	DeleteMemory();
	psigma2 = new T_REAL [ny];
	ptheta = new T_REAL [nx];
	for (i=0; i<nx; i++)
		ptheta[i] = 1;		//default is 1
	pds_raw = new CDesignSite [nds];
	pds_norm = new CDesignSite [nds];
	for (int i=0; i<nds; i++)
	{
		pds_raw[i].SetNumberOfInputParameters(nx);
		pds_raw[i].SetNumberOfOutputParameters(ny);
		pds_raw[i].AllocateArrays();
		pds_norm[i].SetNumberOfInputParameters(nx);
		pds_norm[i].SetNumberOfOutputParameters(ny);
		pds_norm[i].AllocateArrays();
	}
	ds_mean.SetNumberOfInputParameters(nx);
	ds_mean.SetNumberOfOutputParameters(ny);
	ds_mean.AllocateArrays();
	ds_sigma.SetNumberOfInputParameters(nx);
	ds_sigma.SetNumberOfOutputParameters(ny);
	ds_sigma.AllocateArrays();
	Y.SetDimensions(nds, ny);
	Y.AllocateMemory();
	Ytilde.SetDimensions(nds, ny);
	Ytilde.AllocateMemory();
	F.SetDimensions(nds, nf);
	F.AllocateMemory();
	Ftilde.SetDimensions(nds, nf);
	Ftilde.AllocateMemory();
	R.SetDimensions(nds, nds);
	R.AllocateMemory();
	C.SetDimensions(nds,nds);
	C.AllocateMemory();
	G.SetDimensions(nf,nf);
	G.AllocateMemory();
	Beta.SetDimensions(nf, ny);
	Beta.AllocateMemory();
	Gamma.SetDimensions(nds, ny);
	Gamma.AllocateMemory();
}

void CKriging::DeleteMemory()
{
	if (psigma2!=NULL)
	{
		delete [] psigma2;
		psigma2 = NULL;
	}
	if (ptheta!=NULL)
	{
		delete [] ptheta;
		ptheta = NULL;
	}
	if (pds_raw!=NULL)
	{
		delete [] pds_raw;
		pds_raw = NULL;
	}
	if (pds_norm!=NULL)
	{
		delete [] pds_norm;
		pds_norm = NULL;
	}
}

int CKriging::NormalizeRawDesignSites()
{
	return CDesignSite::Normalize(nds, pds_raw, pds_norm, ds_mean, ds_sigma);
}

void CKriging::CalcFunctionArray(T_REAL* px, T_REAL* pf)
{
	//px is the given design site input array
	//pf is the calculated function array
	int i, j, k;
	switch (iregression)
	{
	case 0:		//const
		pf[0] = 1;
		break;
	case 1:		//linear
		pf[0] = 1;
		for (i=0; i<nx; i++)
			pf[i+1] = px[i];
		break;
	case 2:		//quadratic
		pf[0] = 1;
		for (i=0; i<nx; i++)
			pf[i+1] = px[i];
		k = nx + 1;
		for (i=0; i<nx; i++)
		{
			for (j=i; j<nx; j++)
			{
				pf[k] = px[i]*px[j];
				k++;
			}
		}
		break;
	}
}

T_REAL CKriging::CalcCorrelation(T_REAL* px1, T_REAL* px2)
{
	//pth is the theta array
	//px1 is the first design site input array
	//px2 is the second design site input array
	int i;
	T_REAL dx;
	T_REAL tmp;
	T_REAL r = 1;
	switch (icorrelation)
	{
	case 0:		//gauss
		for (i=0; i<nx; i++)
		{
			dx = px1[i] - px2[i];
			r *= exp(-ptheta[i]*dx*dx);
		}
		break;
	case 1:		//exponential
		for (i=0; i<nx; i++)
		{
			dx = fabs(px1[i] - px2[i]);
			r *= exp(-ptheta[i]*dx);
		}
		break;
	case 2:		//linear
		for (i=0; i<nx; i++)
		{
			dx = fabs(px1[i] - px2[i]);
			tmp = 1-ptheta[i]*dx;
			r *= (tmp>0 ? tmp : 0);
		}
		break;
	case 3:		//spherical
		for (i=0; i<nx; i++)
		{
			dx = fabs(px1[i] - px2[i]);
			tmp = ptheta[i]*dx;
			tmp = (tmp<1 ? tmp : 1);
			r *= 1 -1.5*tmp + 0.5*tmp*tmp*tmp;
		}
		break;
	case 4:		//cubic
		for (i=0; i<nx; i++)
		{
			dx = fabs(px1[i] - px2[i]);
			tmp = ptheta[i]*dx;
			tmp = (tmp<1 ? tmp : 1);
			r *= 1 -3*tmp*tmp + 2*tmp*tmp*tmp;
		}
		break;
	case 5:		//spline
		for (i=0; i<nx; i++)
		{
			dx = fabs(px1[i] - px2[i]);
			tmp = ptheta[i]*dx;
			if (tmp < 0)
				tmp = 0;
			if (tmp<=0.2)
				r *= 1 -15*tmp*tmp + 30*tmp*tmp*tmp;
			else if (tmp<1)
			{
				tmp = 1 - tmp;
				r *= 1.25*tmp*tmp*tmp;
			}
			else
			{
				r = 0;
				break;
			}
		}
		break;
	}
	return r;
}

void CKriging::CalcFMatrix()
{
	int i;
	T_REAL** ppa_f = F.ppa;
	for (i=0; i<nds; i++)
		CalcFunctionArray(pds_norm[i].GetInputData(), ppa_f[i]);
}

void CKriging::CalcYMatrix()
{
	int i, j;
	T_REAL* py;
	T_REAL** ppa_y = Y.ppa;
	for (i=0; i<nds; i++)
	{
		py = pds_norm[i].GetOutputData();
		for (j=0; j<ny; j++)
			ppa_y[i][j] = py[j];
	}
}

void CKriging::CalcFYMatrices()
{
	CalcFMatrix();
	CalcYMatrix();
}

void CKriging::CalcAllMatrices()
{
	CalcFMatrix();
	CalcYMatrix();
	CalcRMatrix();
	CalcCMatrices();
	CalcDetR1m();
	CalcFtildeMatrix();
	CalcYtildeMatrix();
	CalcBetaMatrix();
	CalcGammaMatrix();
}

void CKriging::CalcRMatrix()
{
	int i, j;
	T_REAL** ppa_r = R.ppa;
	for (i=0; i<nds; i++)
	{
		ppa_r[i][i] = 1 + (10+nds)*pow(2.0,-52.0);		//add a tiny number to be diagonal dorminant
		for (j=i+1; j<nds; j++)
			ppa_r[i][j] = CalcCorrelation(pds_norm[i].GetInputData(), pds_norm[j].GetInputData());
		//since R is symmetric, calculate lower part from upper part
		for (j=0; j<i; j++)
			ppa_r[i][j] = ppa_r[j][i];
	}
}

void CKriging::CalcCMatrices()
{
	R.CholeskyDecompose(&C);		//seems better than my own version
}

void CKriging::CalcDetR1m()
{
	//use C matrix to calculate |R|
	T_REAL rm = 2/(T_REAL)nds;
	T_REAL** ppac = C.ppa;
	detR1m = 1;
	for (int i=0; i<nds; i++)
		detR1m *= pow(ppac[i][i],rm);
}

void CKriging::CalcFtildeMatrix()
{
	C.LMatrixSolveMatrix(&F,&Ftilde);
}

void CKriging::CalcYtildeMatrix()
{
	C.LMatrixSolveMatrix(&Y,&Ytilde);
}

void CKriging::CalcBetaMatrix()
{
	//also calculates G matrix
	CMatrix Q(nds,nf);
	CMatrix QTY(nf,ny);		//Q^TxYtilde
	CMatrix Gtran(nf,nf);	//Ftilde = QxGtran
	Ftilde.QRDecompose(&Q, &Gtran);
	Q.TransposeMultiplyByMatrixOnRight(&Ytilde,&QTY);
	Gtran.Transpose(&G);
	//back substitution, maybe better then Ginv x QTY
	Gtran.UMatrixSolveMatrix(&QTY, &Beta);
}

void CKriging::CalcGammaMatrix()
{
	//also calculate variance
	//Beta has been calculated
	CMatrix FBeta(nds,ny);		//Ftidle x Beta
	CMatrix YFB(nds,ny);		//Ytilde - Ftidle x Beta
	Ftilde.MultiplyByMatrixOnRight(&Beta,&FBeta);
	Ytilde.SubtractMatrix(&FBeta,&YFB);
	//From testing, solving Ctran x Gamma = Ytilde - Ftilde x Beta gives much lower error than Cinv x (Ytilde - Ftilde x Beta)
	//Cinv.TransposeMultiplyByMatrixOnRight(&YFB,&Gamma);
	CMatrix Ctran(nds,nds);
	C.Transpose(&Ctran);
	Ctran.UMatrixSolveMatrix(&YFB,&Gamma);
	//calculate variance
	T_REAL** ppa = YFB.ppa;
	for (int j=0; j<ny; j++)
	{
		psigma2[j] = 0;
		for (int i=0; i<nds; i++)
			psigma2[j] += ppa[i][j]*ppa[i][j];
		psigma2[j] /= (T_REAL)nds;
	}
}

void CKriging::Interpolate(T_REAL* px, T_REAL* py)
{
	int i, j;
	T_REAL* pxn;		//normalized input array
	T_REAL* pyn;		//normalized output array
	T_REAL* pf = new T_REAL [nf];
	T_REAL* pr = new T_REAL [nds];
	T_REAL** ppa_beta = Beta.ppa;
	T_REAL** ppa_gamma = Gamma.ppa;
	CDesignSite ds(nx, ny);
	ds.SetAndNormalizeInputData(px, ds_mean, ds_sigma);
	pxn = ds.GetInputData();
	pyn = ds.GetOutputData();
	CalcFunctionArray(pxn, pf);
	for (i=0; i<nds; i++)
		pr[i] = CalcCorrelation(pxn, pds_norm[i].GetInputData());
	for (j=0; j<ny; j++)
	{
		pyn[j] = 0;
		for (i=0; i<nf; i++)
			pyn[j] += pf[i]*ppa_beta[i][j];
		for (i=0; i<nds; i++)
			pyn[j] += pr[i]*ppa_gamma[i][j];
	}
	ds.ConvertToRawOutputData(py, ds_mean, ds_sigma);
	delete [] pf;
	delete [] pr;
}

void CKriging::InterpolateWithErrorEstimate(T_REAL* px, T_REAL* py, T_REAL* pe)
{
	//returned pe is the standard deviation of unnormalized value
	int i, j;
	T_REAL  term = 1;	//term to calculate estimated error
	T_REAL* pxn;		//normalized input array
	T_REAL* pyn;		//normalized output array
	T_REAL* pf = new T_REAL [nf];		//f vector
	T_REAL* pr = new T_REAL [nds];		//r vector
	T_REAL* prtilde = new T_REAL [nds];	//r tilde, Cinv x r
	T_REAL* pu = new T_REAL [nf];		//u vector
	T_REAL** ppa_beta = Beta.ppa;
	T_REAL** ppa_gamma = Gamma.ppa;
	CDesignSite ds(nx, ny);
	ds.SetAndNormalizeInputData(px, ds_mean, ds_sigma);
	pxn = ds.GetInputData();
	pyn = ds.GetOutputData();
	CalcFunctionArray(pxn, pf);
	for (i=0; i<nds; i++)
		pr[i] = CalcCorrelation(pxn, pds_norm[i].GetInputData());
	for (j=0; j<ny; j++)
	{
		pyn[j] = 0;
		for (i=0; i<nf; i++)
			pyn[j] += pf[i]*ppa_beta[i][j];
		for (i=0; i<nds; i++)
			pyn[j] += pr[i]*ppa_gamma[i][j];
	}
	ds.ConvertToRawOutputData(py, ds_mean, ds_sigma);
	//calculate estimated error
	//based on testing, back substitution performs much better than matrix multiplication
	//Cinv.MultiplyByVectorOnRight(pr,prtilde);
	C.LMatrixSolveVector(pr,prtilde);
	Ftilde.TransposeMultiplyByVectorOnRight(prtilde,pu);
	for (i=0; i<nf; i++)
		pu[i] -= pf[i];
	//based on testing, back substitution does not help much
	//Ginv.MultiplyByVectorOnRight(pu,pf);
	G.LMatrixSolveVector(pu,pf);	//use pf to hold Ginv x u
	for (i=0; i<nf; i++)
		term += pf[i]*pf[i];
	for (i=0; i<nds; i++)
		term -= prtilde[i]*prtilde[i];
	//use pyn to pointer to ds_sigma output array
	pyn = ds_sigma.GetOutputData();
	for (i=0; i<ny; i++)
	{
		pe[i] = term*psigma2[i]*pyn[i]*pyn[i];
		pe[i] = sqrt(fabs(pe[i]));		//avoid tiny negative variance
	}
	delete [] pf;
	delete [] pr;
	delete [] prtilde;
	delete [] pu;
}

T_REAL CKriging::CalcObjectiveFunction(T_REAL* pln)
{
	//given natural log of theta array, calculate objective function
	//pln: array of theta (size of nx)
	//F and Y matrices must have been calculated
	int i;
	T_REAL sum_sigma2 = 0;
	T_REAL fun;
	for (i=0; i<nx; i++)
		ptheta[i] = exp(pln[i]);
	CalcRMatrix();
	CalcCMatrices();
	CalcDetR1m();
	CalcFtildeMatrix();
	CalcYtildeMatrix();
	CalcBetaMatrix();
	CalcGammaMatrix();
	for (i=0; i<ny; i++)
		sum_sigma2 += psigma2[i];
	fun = sum_sigma2*detR1m;
	return fun;
}

void CKriging::WriteACMFile(FILE* pf)
{
	int i, j;
	fprintf(pf, "//Variables and equations from Kriging regression\n");
	fprintf(pf, "vNormRomInput([1:%d]) as RealVariable;\n", nx);
	fprintf(pf, "vNormRomOutput([1:%d]) as RealVariable;\n", ny);
	fprintf(pf, "vMeanInput([1:%d]) as RealVariable(Fixed);\n", nx);
	fprintf(pf, "vSigmaInput([1:%d]) as HIDDEN RealVariable(Fixed);\n", nx);
	fprintf(pf, "vMeanOutput([1:%d]) as RealVariable(Fixed);\n", ny);
	fprintf(pf, "vSigmaOutput([1:%d]) as HIDDEN RealVariable(Fixed);\n", ny);
	fprintf(pf, "vvDSInput([1:%d],[1:%d]) as HIDDEN RealVariable(Fixed);\n", nds, nx);
	fprintf(pf, "vTheta([1:%d]) as HIDDEN RealVariable(Fixed);\n", nx);
	fprintf(pf, "vvBeta([1:%d],[1:%d]) as HIDDEN RealVariable(Fixed);\n", nx+1, ny);
	fprintf(pf, "vvGamma([1:%d],[1:%d]) as HIDDEN RealVariable(Fixed);\n", nds, ny);
	fprintf(pf, "vF([1:%d]) as HIDDEN RealVariable;\n", nx+1);
	fprintf(pf, "vR([1:%d]) as HIDDEN RealVariable;\n", nds);
	for (i=0; i<nx; i++)
		fprintf(pf, "vMeanInput(%d): %lg;\n", i+1, ds_mean.GetInputData()[i]);
	for (i=0; i<nx; i++)
		fprintf(pf, "vSigmaInput(%d): %lg;\n", i+1, ds_sigma.GetInputData()[i]);
	for (i=0; i<ny; i++)
		fprintf(pf, "vMeanOutput(%d): %lg;\n", i+1, ds_mean.GetOutputData()[i]);
	for (i=0; i<ny; i++)
		fprintf(pf, "vSigmaOutput(%d): %lg;\n", i+1, ds_sigma.GetOutputData()[i]);
	for (i=0; i<nds; i++)
	{
		for (j=0; j<nx; j++)
			fprintf(pf, "vvDSInput(%d,%d): %lg;\n", i+1, j+1, pds_norm[i].GetInputData()[j]);
	}
	for (i=0; i<nx; i++)
		fprintf(pf, "vTheta(%d): %lg;\n", i+1, ptheta[i]);
	for (i=0; i<=nx; i++)
	{
		for (j=0; j<ny; j++)
			fprintf(pf, "vvBeta(%d,%d): %lg;\n", i+1, j+1, Beta.ppa[i][j]);
	}
	for (i=0; i<nds; i++)
	{
		for (j=0; j<ny; j++)
			fprintf(pf, "vvGamma(%d,%d): %lg;\n", i+1, j+1, Gamma.ppa[i][j]);
	}
	//added to handle ACM to Aspen Plus solid substreams
	fprintf(pf, "IF containsAllSolidNames AND containsGasSpecies AND containsSolidSpecies THEN\n");
	//normalize input
	fprintf(pf, "FOR i IN [1:%d] DO\n", nx);
	fprintf(pf, "\tvNormRomInput(i) = (vRomInput(i) - vMeanInput(i))/vSigmaInput(i);\n");
	fprintf(pf, "ENDFOR\n");
	//calculate linear regression functions
	fprintf(pf, "vF(1) : 1, Fixed;\n");
	for (i=0; i<nx; i++)
		fprintf(pf, "vF(%d) = vNormRomInput(%d);\n", i+2, i+1);
	//calculate correlation functions
	fprintf(pf, "FOR i IN [1:%d] DO\n", nds);
	fprintf(pf, "\tvR(i) = exp(-sigma(foreach (j in [1:%d]) vTheta(j)*(vvDSInput(i,j)-vNormRomInput(j))^2));\n", nx);
	fprintf(pf, "ENDFOR\n");
	//calculate normalized output vector
	fprintf(pf, "FOR j IN [1:%d] DO\n", ny);
	fprintf(pf, "\tvNormRomOutput(j) = sigma(foreach (i in [1:%d]) vF(i)*vvBeta(i,j)) + sigma(foreach (i in [1:%d]) vR(i)*vvGamma(i,j));\n", nx+1, nds);
	fprintf(pf, "ENDFOR\n");
	//calculate unnormalized output vector
	fprintf(pf, "FOR i IN [1:%d] DO\n", ny);
	fprintf(pf, "\tvRomOutput(i) = vMeanOutput(i) + vNormRomOutput(i)*vSigmaOutput(i);\n");
	fprintf(pf, "ENDFOR\n");
	//added to handle ACM to Aspen Plus solid substreams
	fprintf(pf, "ENDIF\n");
}

void CKriging::WriteCapeOpenFile(FILE* pf)
{
	int i, j;
	T_REAL* preal;
	T_REAL** ppa;
	fprintf(pf,"//Kriging regression data\n");
	//integers
	fprintf(pf,"%d\t//size of input vector\n",nx);
	fprintf(pf,"%d\t//size of output vector\n",ny);
	fprintf(pf,"%d\t//iregression\n",iregression);
	fprintf(pf,"%d\t//icorrelation\n",icorrelation);
	fprintf(pf,"%d\t//number of design sites\n",nds);
	//theta vector
	fprintf(pf,"//theta vector\n");
	for (i=0; i<nx; i++)
		fprintf(pf,"%lg\t",ptheta[i]);
	fprintf(pf,"\n");
	//mean of input vector
	fprintf(pf,"//mean of input vector\n");
	preal = ds_mean.GetInputData();
	for (i=0; i<nx; i++)
		fprintf(pf,"%lg\t",preal[i]);
	fprintf(pf,"\n");
	//mean of output vector
	fprintf(pf,"//mean of output vector\n");
	preal = ds_mean.GetOutputData();
	for (i=0; i<ny; i++)
		fprintf(pf,"%lg\t",preal[i]);
	fprintf(pf,"\n");
	//standard deviation of input vector
	fprintf(pf,"//sigma of input vector\n");
	preal = ds_sigma.GetInputData();
	for (i=0; i<nx; i++)
		fprintf(pf,"%lg\t",preal[i]);
	fprintf(pf,"\n");
	//standard deviation of output vector
	fprintf(pf,"//sigma of output vector\n");
	preal = ds_sigma.GetOutputData();
	for (i=0; i<ny; i++)
		fprintf(pf,"%lg\t",preal[i]);
	fprintf(pf,"\n");
	//normalized design site input
	fprintf(pf,"//normalized design site input vectors\n");
	for (i=0; i<nds; i++)
	{
		preal = pds_norm[i].GetInputData();
		for (j=0; j<nx; j++)
			fprintf(pf,"%lg\t",preal[j]);
		fprintf(pf,"\n");
	}
	//beta matrix
	ppa = Beta.ppa;
	fprintf(pf, "//Beta Matrix: %d rows by %d columns\n",nf,ny);
	for (i=0; i<nf; i++)
	{
		for (j=0; j<ny; j++)
			fprintf(pf, "%lg\t", ppa[i][j]);
		fprintf(pf,"\n");
	}
	//gamma matrix
	ppa = Gamma.ppa;
	fprintf(pf, "//Gamma Matrix: %d rows by %d columns\n",nds,ny);
	for (i=0; i<nds; i++)
	{
		for (j=0; j<ny; j++)
				fprintf(pf, "%lg\t", ppa[i][j]);
		fprintf(pf,"\n");
	}
}

void CKriging::Write(FILE* pf)
{
	int iversion = 0;
	bool ballocated = psigma2!=NULL && ptheta!=NULL && pds_raw!=NULL && pds_norm!=NULL;
	fwrite(&iversion,sizeof(int),1,pf);
	fwrite(&iregression,sizeof(int),1,pf);
	fwrite(&icorrelation,sizeof(int),1,pf);
	fwrite(&nx,sizeof(int),1,pf);
	fwrite(&ny,sizeof(int),1,pf);
	fwrite(&nf,sizeof(int),1,pf);
	fwrite(&nds,sizeof(int),1,pf);
	fwrite(&detR1m,sizeof(T_REAL),1,pf);
	fwrite(&ballocated,sizeof(bool),1,pf);
	if (ballocated)
	{
		fwrite(psigma2,sizeof(T_REAL),ny,pf);
		fwrite(ptheta,sizeof(T_REAL),nx,pf);
		for (int i=0; i<nds; i++)
		{
			pds_raw[i].Write(pf);
			pds_norm[i].Write(pf);
		}
		ds_mean.Write(pf);
		ds_sigma.Write(pf);
		Y.Write(pf);
		Ytilde.Write(pf);
		F.Write(pf);
		Ftilde.Write(pf);
		R.Write(pf);
		C.Write(pf);
		G.Write(pf);
		Beta.Write(pf);
		Gamma.Write(pf);
	}
}

void CKriging::Read(FILE* pf)
{
	int iversion;
	bool ballocated;
	fread(&iversion,sizeof(int),1,pf);
	fread(&iregression,sizeof(int),1,pf);
	fread(&icorrelation,sizeof(int),1,pf);
	fread(&nx,sizeof(int),1,pf);
	fread(&ny,sizeof(int),1,pf);
	fread(&nf,sizeof(int),1,pf);
	fread(&nds,sizeof(int),1,pf);
	fread(&detR1m,sizeof(T_REAL),1,pf);
	fread(&ballocated,sizeof(bool),1,pf);
	if (ballocated)
	{
		AllocateMemory();
		fread(psigma2,sizeof(T_REAL),ny,pf);
		fread(ptheta,sizeof(T_REAL),nx,pf);
		for (int i=0; i<nds; i++)
		{
			pds_raw[i].Read(pf);
			pds_norm[i].Read(pf);
		}
		ds_mean.Read(pf);
		ds_sigma.Read(pf);
		Y.Read(pf);
		Ytilde.Read(pf);
		F.Read(pf);
		Ftilde.Read(pf);
		R.Read(pf);
		C.Read(pf);
		G.Read(pf);
		Beta.Read(pf);
		Gamma.Read(pf);
	}
}
