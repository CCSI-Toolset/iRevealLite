//Main.cpp
//this is the driver for ROM builder
#include "CCSI.h"
#include "Util.h"


int main(int argc, char* argv[])
{
	int i;
	string str;
	if (argc<2)
	{
		printf("iReveal command takes at least one argument\n");
		PrintCommandUsage();
		return 1;
	}
	if (strcmp(argv[1],"-s") && strcmp(argv[1],"-b") && strcmp(argv[1],"-v"))
	{
		printf("iReveal command's 1st argument must be -s, -b or -v\n");
		PrintCommandUsage();
		return 1;
	}
	if (!strcmp(argv[1],"-v"))	//sampling option
	{
		printf("iReveal version 2.0, 2018\n");
		return 0;
	}
	if (!strcmp(argv[1],"-s"))	//sampling option
	{
		//check if the extension is json
		str = argv[2];
		i = str.find('.');
		if (i<1)
		{
			printf("You should provide a json file to run the command!\n");
			PrintCommandUsage();
			return 1;
		}
		str = str.substr(i+1);
		if (str.compare("json") && str.compare("JSON"))
		{
			printf("You should provide a json file to run the command!\n");
			PrintCommandUsage();
			return 1;
		}
		ProcessJsonAndSampleInputSpace(argv[2]);
		return 0;
	}
	//-b argument
	if (argc>2)
	{
		printf("Too many argument for -b option!\n");
		PrintCommandUsage();
		return 1;
	}
	if (BuildKrigingRom())
	{
		printf("Failed to build reduced order model!\n");
		return 1;
	}
	return 0;
}