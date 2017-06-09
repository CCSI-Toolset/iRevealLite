from kriging import *
from model import *
import logging

#import pylab as pl 
#--------------------#Read PSUADE  configuration input file#------------------------
def getROMConfig(filename):

    try:
        f = open(filename, "r+")
        psuadeEnd = False
        m = model()
        inputVars = []
        outputVars = []
        mins = []
        maxs = []

        arr = f.readline().strip().split()
        if(arr[0] != "PSUADE"):
            print("first line ins psuade input file should be PSUADE header",
                  filename)
            sys.exit()

        while(psuadeEnd == False):
            line = f.readline()
            arr = line.strip().split()
            if(len(arr) == 0):
                continue;
            
            elif(len(arr) == 1 and arr[0] == "INPUT"):
                while(arr[0] != "END"):
                    line = f.readline()
                    arr = line.strip().split()

                    if(arr[0] == "dimension"):
                        m.set_num_in(int(arr[2]))
                    if(arr[0] == "variable"):
                        #print("found variable", str(arr))
                        if(arr[2].find("iREVEAL.") == 0):
                            alltext = arr[2].split(".")
                            varName = alltext[1]
                        else:
                            varName = arr[2]
                        inputVars.append(varName)
                        mins.append(float(arr[4]))
                        maxs.append(float(arr[5]))
            
            elif(len(arr) == 1 and arr[0] == "OUTPUT"):
                while(arr[0] != "END"):
                    line = f.readline()
                    arr = line.strip().split()
                    if(arr[0] == "dimension"):
                        m.set_num_out(int(arr[2]))
                    if(arr[0] == "variable"):
                        if(arr[2].find("iREVEAL.") == 0):
                            alltext = arr[2].split(".")
                            varName = alltext[1]
                        else:
                            varName = arr[2]
                        outputVars.append(varName)
            
            elif(len(arr) == 1 and arr[0] == "METHOD"):
                while(arr[0] != "END"):
                    line = f.readline()
                    arr = line.strip().split()
                    if(arr[0] == "num_samples"):
                        m.set_num_sim(int(arr[2]))

            elif(len(arr) == 1 and (arr[0] == "APPLICATION" or arr[0] ==
                                    "ANALYSIS")):
                while(arr[0] != "END"):
                    line = f.readline()
                    arr = line.strip().split()

            elif(len(arr) == 1 and arr[0] == "END"):
                psuadeEnd = True

            else:
                print("unknown format in psuade input file ", filename)
                print line
                exit()

        m.set_input_names(inputVars)
        m.set_output_list(outputVars)
        m.set_mins_list(mins)
        m.set_maxs_list(maxs)

        return m
    
    except IOError:
        print("Could not open file", filename)
        exit()


def getSampledData(filename, romModel):
    
    sampledData = []

    try:
        f = open(filename, "r+")
        arr = f.readline().strip().split()
        while(arr[0] != "PSUADE_IO"):
            arr = f.readline().strip().split()

        # assuming first line after "PSUADE_IO" is in this format:
        # num_in num_out num_sim
        arr = f.readline().strip().split()
        if(int(arr[0]) != romModel.get_num_in() or int(arr[1]) !=
           romModel.get_num_out() or int(arr[2]) != romModel.get_num_sim()):
            print("mismtach between psuade input file and psuade sampling data file")
            print("please check " + filename + "line = ", str(arr))
            sys.exit()

        for i in range(romModel.get_num_sim()):
            line = f.readline()
            arr= line.strip().split()
            if(len(arr) != 2 and int(arr[0]) !=  i+1):
                print("could not parse line psuade data file, unknown format", line)
                sys.exit()

            invalues = []
            outvalues= []
            for j in range(romModel.get_num_in()):
                arr = f.readline().strip().split()
                invalues.append(float(arr[0]))
            for j in range(romModel.get_num_out()):
                arr = f.readline().strip().split()
                outvalues.append(float(arr[0]))
            sampledData.append(invalues)
        return sampledData
    
    except IOError:
        print("Could not open file", filename)
        sys.exit()


#-------------------Reorder SampledData to be in same order as in--------------
#-------------------paramFileDataModel-----------------------------------------

def orderByParamFile(sampledData, romModel,  paramFileDataModel):

    newSampledData = []

    for simid in range(0, paramFileDataModel.get_num_sim()):
        simDataList = []
        for paramName in paramFileDataModel.get_input_names():
            indexInROMModel = romModel.get_input_names().index(paramName)
            #print("Index in ROM model =", indexInROMModel)
            
            simData = sampledData[simid][indexInROMModel]
            #print("Simid, indexinROMMOdel, varData", simid, indexInROMModel ,
            #      simData);
            simDataList.append(simData)
        newSampledData.append(simDataList)

    #print("original Data =", sampledData)
    #print("reoredred Data  =", newSampledData)
    return newSampledData


#--------------------#Write input parameter set into a file#------------------------
# The data is saved to be used later while contruting ROM for these simulations

def writeROMFile(filename, invarlist, parValues, num_sim, num_in_param):
    f = open(filename, "w+")
    varid=0
    for var in invarlist:
        f.write(var.rjust(12) + " ")
    f.write('\n')
    for simid in range(num_sim):
        for param_id in range(num_in_param):
            f.write( str(parValues[simid][param_id]).rjust(12) + "  ")
        f.write("\n")
    f.close()

#--------------#Read input parameter set to precisdt values for#------
def readTestFile(infile, num_in):
    try:
        f = open(infile, "r+")
        #print("opening ", infile)
        parValues = []

        # first line contains parameter names. The parameter names are to be read from 
        # param.in to avoid " space realted issues in parameter names"
        line = f.readline()
        if(len(line.strip().split()) != num_in):
            print("The no of input parametrs in test file does not match actual number of input parameters with constraints")
            exit()

        for line in f:
            line=line.strip()
            val = line.split()
            parValues.append(val)
        
        f.close()
        #print parValues
        return(parValues)
    except IOError:
        print("Could not open " + infile + "\nExiting..")


#--------------#Read input parameter set(used for MFIX input contruction earlier)#------
def readROMFile(infile):
    try:
        f = open(infile, "r+")
        #print("opening ", infile)
        parValues = []

        # first line contains parameter names. The parameter names are to be read from 
        # param.in to avoid " space realted issues in parameter names"
        line = f.readline()
        for line in f:
            line=line.strip()
            val = line.split()
            parValues.append(val)
        
        f.close()
        #print parValues
        return(parValues)
    except IOError:
        print("Could not open " + infile + "\nExiting..")

#----------------------CReate testout file iwth comma seperarted output parameter names----#

def generateModTestOut(infile, outfile, num_out, output_list):
    try:
        f = open(infile, "r+")
        fout = open(outfile, "w+")

        line = f.readline()

        out_names = ""
        for name in output_list:
            out_names = out_names + str(name)+ ","
        out_names = out_names + "\n"
        
        print("output with comma seperated=", out_names)

        fout.write(str(out_names))

        for line in f:
            fout.write(line)
            
        f.close()
        fout.close()
    except IOError:
        print("Could not open " + infile + " or " + outfile + "\nExiting..")

              
#----------------------RUN SELECTED REGRESSION METHOD--------------------------#
def getParentDir(currdir):
    # Remove ending '/' in directory name
    l = len(currdir)
    #print currdir
    #print( "l = ", l)
    while(currdir.endswith('/') or currdir.endswith('\\')):
        currdir = currdir[0:l-1]
        l = len(currdir)
    
    pos = currdir.rfind('\\')
    topdir = currdir[0 : pos]
    print("For directory = " + currdir + ", topdir = " + topdir)
    return topdir



#----------------------RUN SELECTED REGRESSION METHOD--------------------------#
def runregression(regType, regPath, numInParam, numOutParam, numSim, numTest,
                  inVarList, inParValues, mins, maxs, 
                  outVarList, outParValues, testCases,
                  rominfile, resultsfile, tfile, toutfile, jobdir):

    print  ("Regression method to be used = " + regType)
    #print  ("Test cases = " , testCases)
    mode = "-i"
    if(regType == "Kriging"):
        kriging(numInParam, numOutParam, numSim, numTest, rominfile , resultsfile , tfile, toutfile, mode, regPath)
    
    elif(regType == "MARS"):

        mars(numInParam, inVarList, inParValues, mins, maxs, numOutParam, outVarList, outParValues, numSim, jobdir, testCases, regPath)
    

    elif(regType == "Linear"):
        print("using Linear")
        order = 1
        polynomial(inVarList, inParValues, outVarList, outParValues, testCases, order, toutfile)
    
    elif(regType == "Quadratic"):
        print("using Quadratic")
        order = 2
        polynomial(inVarList, inParValues, outVarList, outParValues, testCases, order, toutfile)
    
    elif(regType == "Cubic"):
        print("using Cubic")
        order = 3
        polynomial(inVarList, inParValues, outVarList, outParValues, testCases, order, toutfile)
    
    elif(regType == "Poly4"):
        print("using Order 4 Polynomial")
        order = 4
        polynomial(inVarList, inParValues, outVarList, outParValues, testCases, order, toutfile)
    
    elif(regType == "ANOVA"):
        print("using ANOVA")
        method = 1
        sst(inVarList, inParValues, outVarList, outParValues, numSim, jobdir, method)

    elif(regType == "SRC"):
        print("using SRC")
        method = 2
        sst(inVarList, inParValues, outVarList, outParValues, numSim, jobdir,method)

    elif(regType == "ANN"):
        #print("using ANN")
        ann(numInParam, inVarList, inParValues, numOutParam,
            outVarList, outParValues, numSim, jobdir, testCases, regPath, toutfile)
    
    elif(regType == "SVM"):
        print("using SVM")
        svm_rom(numInParam, inVarList, inParValues, numOutParam,
                outVarList, outParValues, numSim, jobdir, testCases, regPath, toutfile)

    else:
        print(" unrecognized option ")

    return

#----------------------EXPORT ROM FOR SELECTED REGRESSION METHOD--------------------------#
def runExport(regType, regPath, romModel,rominfile, resultsfile, acmFile, coFile, jobdir):
              
    print("Regression method to be used = " + regType)
    numInParam = romModel.get_num_in()
    numOutParam = romModel.get_num_out()
    numSim = romModel.get_num_sim()

    numTest = 0
    tfile = "None"

    if(regType == "Kriging"):
        mode = "-c"
        kriging(numInParam, numOutParam, numSim, numTest, rominfile , resultsfile, tfile, coFile, mode, regPath)

        mode = "-a"
        kriging(numInParam, numOutParam, numSim, numTest, rominfile , resultsfile, tfile, acmFile, mode, regPath)
    
    #elif(options.regression == "MARS"):

        #mars(numInParam, inVarList, inParValues, mins, maxs, numOutParam, outVarList, outParValues, numSim, jobdir, testCases, regPath)
    
    #elif(options.regression == "ANN"):
        #print("using ANN")
    
    else:
        print("Unsupporetd option for export ROM ")


#------------Normalize LHS output within parameter ranges-------#
def align(base, mins, maxs, num_param, num_sim):
	for i in range(num_sim):
		for j in range(num_param):
			base[i,j] = mins[j] + ((maxs[j] - mins[j]) * base[i,j])
	return base


def reAlign(parValues, mins, maxs, num_param, num_sim):
	for i in range(num_sim):
		for j in range(num_param):
			parValues[i][j] = (float(parValues[i][j]) - mins[j])/(maxs[j] - mins[j])
	return parValues

def alignQMC(base, mins, maxs, num_param, num_sim):
	if(num_param == 1):
		tmp = [ [] for k1 in range(num_sim)]
		for k2 in range(num_sim):
			tmp[k2].append(base[k2])
		base = tmp
	for i in range(num_sim):
		for j in range(num_param):
			base[i][j] = mins[j] + ((maxs[j] - mins[j]) * base[i][j])
	return base

#---------------------------------Create Input to plot file ---------
def createTestFile(output_file, invars, mins, maxs, nsteps):
    try:
        f = open(output_file, "w+")
        for var in invars:
            f.write(var.rjust(12) + "\t")
        f.write('\n')
        
        nParam = len(invars)
        allParamVals = []
        testCases = []

		# For each input parameter 
		# create list of values based on mins and maxs and nsteps
		# for e.f if min=1, max =2, nsteps=4
		# create 1, 1.25, 1.5, 1.75, 2.0
		# Note: Total number of values of a parameter = nsteps+1
        for i in range(0, nParam):
            
            paramVals = []
            stepval = (maxs[i] - mins[i])/nsteps
            for j in range(0, nsteps):
                val = mins[i]  + j*stepval
                paramVals.append(val)
            
            paramVals.append(maxs[i])
            print("Test data for param " , invars[i] , "=")
            print(str(paramVals)  +"\n")
            allParamVals.append(paramVals)

			# to account for storing "max" value
        nTest = nsteps+1 

		# now total no of test cases = pow(nTest, numParam)
        nTotalCase = pow(nTest, nParam)

		# For each test case, find indices of each paramValue
        for icase in range(0, nTotalCase):
            param_indexs = []
            for iparam in range(0, nParam):
                
                pos = (nParam - (iparam+1))
                changeafter = pow(nTest, pos)
                
                param_indexs.append((icase / changeafter) % nTest)
            
            line = [allParamVals[param][param_indexs[param]] for param in range(0, nParam)]
            #print line
            testCases.append(line)
            for param_id in range(nParam):
                f.write( str(line[param_id]).rjust(12) + "  ")
            f.write("\n")

        return testCases
    
    except(IOError):
        print(" Could not open " + output_file)
        exit()

#------------------------READ/PROCESS SIMULATION  RESULTS-----------------------#
def getSimResults(resultsfile, model, num_iter, iter_names, outvarlist, jobDir):
    
    # read or create an output parameter file, based on simulation results
    num_sims = model.get_num_sim()
    if(os.path.exists(resultsfile)):
        print (" Found " + resultsfile +  " file\n")
        outParValues = readROMFile(resultsfile)
    else:
        outdir = jobDir + "/"
        #print ("Creating results file : " + resultsfile)
      	if(model == "MFIX"):
      		print("Assuming simulation results available as: "+ outdir + "JobOut/sim.out.$i")
      		outParValues = process_mfix(outdir, num_sims, num_iter,
                                             iter_names, outvarlist,
                                             resultsfile)
      	elif(model == "FUELCELL"):
            	print("Assuming simulation results available as: "+ outdir + "JobOut/SOFC_MP_ROM.*.dat")
             	outParValues = process_FC(outdir, num_sims, num_iter, 
                                               iter_names, outvarlist, resultsfile)
        else:
       		print "Unrecognized Model, cannot process output. Exiting.."
       		exit()

    return(outParValues)

    #print ("")
    
def handleDep(model_type, invarlist, parValues, depVar, depVarList,
              num_sim, num_mfix_cons, list_mfix_cons):


    if(model_type == "MFIX" and depVar > 0 ):
        # data is in terms of mass fractions, while dpendencies might be
        # speciefied in terms of flow rates
        (invar_withDep, parValues_withDep) = handleMFIXDep(invarlist, parValues, 
                                                           depVar, depVarList,
                                                           num_sim, num_mfix_cons, 
                                                           list_mfix_cons)
    elif(model_type == "FUELCELL" and depVar > 0):
        (invar_withDep, parValues_withDep) = handleFCDep(invarlist, parValues, depVar, depVarList,num_sim)
    else:
        logging.info("In depnedency handling, no dependent parameters")
        invar_withDep = invarlist
        parValues_withDep =  parValues

    return(invar_withDep, parValues_withDep)


##---------------------------------Create Multiple Input Models---------------------#
def createSimIns(model, jobdir, invarlist, parValues, startid, num_sim, numPrevSucc, num_mfix_cons, list_mfix_cons):
    
    print("\nUsing : " + model.model_input_file + " as template input file")
    print(" calling handle dependencies ")
    (invar_withDep, parValues_withDep) = handleDep(model.model_type, invarlist, 
                                                   parValues, model.num_dep,
                                                   model.dep_list, num_sim,
                                                   num_mfix_cons, list_mfix_cons)
    if(model.model_type == "MFIX"):
		 for simid in range(startid, num_sim):
			createMfixIns(model.model_input_file, jobdir, invar_withDep, parValues_withDep, simid)

    elif(model.model_type == "FUELCELL"):
		count=0
		for simid in range(startid, num_sim):
			createFCIns(model.model_input_file, jobdir, invar_withDep, parValues_withDep, simid, numPrevSucc, count, model.is_fuelflowrate, model.is_cellflowrate, model.is_ox_stackflowrate, model.is_ox_cellflowrate)
			count=count+1
	
    else:
		print(" Unrecognized Simulator")
		exit()

##-------------- Add data  about constraints to the model -----------#########
def  add_cons_data(model, num_mfix_cons, list_mfix_cons, num_fuel_cons,
                   list_fuel_cons):

    if(model.model_type == "MFIX"):
        (num_in, input_names, mins, maxs)  = add_MFIX_Species_Flowrate(model,
                                                                       num_mfix_cons,
                                                                       list_mfix_cons)
    elif(model.model_type == "FUELCELL"):
        (num_in, input_names, mins, maxs) = add_fuelcell_cons_data(model, num_fuel_cons, list_fuel_cons)
    
    else:
        print("no constarint data available for the model")
        return(model.num_in, model.input_names, model.mins_list,
               model.maxs_list)

    return(num_in, input_names, mins, maxs)

######--------------------Draw Plots using PYLAB -------------------#####
#def drawplots(samples, invarlist, mins, maxs, num_param, num_sim):

#        pl.subplot(311)
#        pl.xlabel(invarlist[0])
#        pl.ylabel("Frequency")
#        pl.axis([mins[0], maxs[0], 0, 25])
#        pl.hist(samples[0], facecolor='g')
#        pl.subplot(312)
#        pl.xlabel(invarlist[1])
#        pl.axis([mins[1], maxs[1], 0, 25])
#        pl.ylabel("Frequency")
#        pl.hist(samples[1], facecolor='g')
#        pl.subplot(313)
#        pl.plot(samples[0], samples[1], "ro")
#        pl.show()
