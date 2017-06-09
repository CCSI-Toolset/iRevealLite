//Util.h

#ifndef __UTIL_H__
#define __UTIL_H__

#include <string>
#include <stdio.h>

class CUtil
{
public:
	CUtil() {}
	virtual ~CUtil() {}
	static void ChangeEndian(short& x);
	static void ChangeEndian(int& x);
	static void ChangeEndian(float& x);
	static void ChangeEndian(double& x);
	static void WriteString(std::string& str, FILE* pf);
	static void ReadString(std::string& str, FILE* pf);
	static void WriteStringText(std::string& str, FILE* pf);
};

#endif
