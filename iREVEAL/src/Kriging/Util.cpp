//Util.cpp
#include "Util.h"

void CUtil::ChangeEndian(short& x)
{
	//assume short is 2 bytes
	char temp;
	char* pc = (char*)&x;
	temp = pc[0];
	pc[0] = pc[1];
	pc[1] = temp;
}

void CUtil::ChangeEndian(int& x)
{
	//assume int is 4 bytes
	char temp;
	char* pc = (char*)&x;
	temp = pc[0];
	pc[0] = pc[3];
	pc[3] = temp;
	temp = pc[1];
	pc[1] = pc[2];
	pc[2] = temp;
}

void CUtil::ChangeEndian(float& x)
{
	//assume float is 4 bytes
	char temp;
	char* pc = (char*)&x;
	temp = pc[0];
	pc[0] = pc[3];
	pc[3] = temp;
	temp = pc[1];
	pc[1] = pc[2];
	pc[2] = temp;
}

void CUtil::ChangeEndian(double& x)
{
	//assume double is 8 bytes
	char temp;
	char* pc = (char*)&x;
	temp = pc[0];
	pc[0] = pc[7];
	pc[7] = temp;
	temp = pc[1];
	pc[1] = pc[6];
	pc[6] = temp;
	temp = pc[2];
	pc[2] = pc[5];
	pc[5] = temp;
	temp = pc[3];
	pc[3] = pc[4];
	pc[4] = temp;
}

void CUtil::WriteString(std::string& str, FILE* pf)
{
	int strlen = str.size();
	fwrite(&strlen,sizeof(int),1,pf);
	fwrite(str.c_str(),sizeof(char),strlen,pf);
}

void CUtil::ReadString(std::string& str, FILE* pf)
{
	int strlen;
	char* pbuffer;
	fread(&strlen,sizeof(int),1,pf);
	pbuffer = new char [strlen+1];
	pbuffer[strlen] = '\0';
	fread(pbuffer,sizeof(char),strlen,pf);
	str = pbuffer;
	delete [] pbuffer;
}

void CUtil::WriteStringText(std::string& str, FILE* pf)
{
	fprintf(pf,"%s",str.c_str());
}