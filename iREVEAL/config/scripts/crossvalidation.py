
import subprocess
import os
import math
from include import *

class crossValidate:
    def __init__(self, regType, regPath, romModel, infile, resultsfile, predicted_resultsfile, jobdir):
        
        self.jobdir = jobdir
        self.infile = infile
        self.results = resultsfile
        self.predicted_results = predicted_resultsfile
        self.numIn = romModel.get_num_in()
        self.numOut = romModel.get_num_out()
        self.numSim = romModel.get_num_sim()
        self.input_names = romModel.get_input_names()
        self.output_names = romModel.get_output_list()
        self.mins_list = romModel.get_mins_list()
        self.maxs_list = romModel.get_maxs_list()

        self.mydir = jobdir + "/crossValidate"
        self.cvInfile = self.mydir + "/rom.in."
        self.cvOutfile = self.mydir + "/results."
        self.cvTestfile = self.mydir + "/test."
        self.cvTestOutfile = self.mydir + "/test.out."
        self.errors = self.jobdir + "/" + regType  +"/errors"
        self.relative_errors = self.jobdir + "/" + regType  + "/relative_errors"

        self.ORIG_VAL_TOLERANCE = 1.0e-6

        if(os.path.exists(self.mydir) == False):
            os.mkdir(self.mydir)

        #change this  to define ratio of no of testCases/numTraining
        self.k_fold_percent = 0.2
        self.num_sim_per_test = int(math.ceil(self.k_fold_percent * self.numSim))
        self.num_iter = int(math.ceil(float(self.numSim)/self.num_sim_per_test))

        self.num_sim_curr_iter = []

        self.calc_num_sim_iter()
    
        print("Nuber fo simulation per test data, number of iterations: ", self.num_sim_curr_iter, self.num_iter)
        
        print("Running k-fold cross validation,  k= " + str(self.k_fold_percent) + ",for " +jobdir)
        self.bootstrap(regType, regPath)

        print("Calculating Mean Errors for each O/P parameter")
        mse = self.calcMeanAbsErrors()
        return

    def calc_num_sim_iter(self):

        total = 0
        for i in range(self.num_iter):
            if((total+self.num_sim_per_test) > self.numSim):
                rem = self.numSim-total
                self.num_sim_curr_iter.append(self.numSim - rem)
            else:
                self.num_sim_curr_iter.append(self.numSim - self.num_sim_per_test)
                total = total +  self.num_sim_per_test

        return

     
    def bootstrap(self, regType, regPath):
        # creates files for 1 off cross validation 
        # and call run Regression on each
        self.create_inoutfiles()
        self.create_tests()
        ntest = 1
        numSim = self.numSim
        for i in range(self.num_iter):
            
            ifile = self.cvInfile + str(i)
            ofile = self.cvOutfile  + str(i)
            tfile = self.cvTestfile +  str(i)
            toutfile = self.cvTestOutfile + str(i)

            (inParVals) = readROMFile(ifile)
            (outParVals) = readROMFile(ofile)
            (testCases) = readROMFile(tfile)

            runregression(regType, regPath, self.numIn, self.numOut, self.num_sim_curr_iter[i], 
                          ntest, self.input_names, inParVals, self.mins_list, self.maxs_list, 
                          self.output_names, outParVals, testCases,
                          ifile, ofile, tfile, toutfile, self.mydir)
            if(os.path.exists(toutfile) == False):
                print (" Looks like normalization failed")
                exit()
            print("\n")

        #self.outVarList = outVars
        return
    
    def calcMeanAbsErrors(self):

        (header, origResults) = self.readResults()
        #print("Results=", origResults)
        
        # NOte thsi method also writes the predicted results file
        predResults = self.getPredictedResults(header)
        #print("Predicted values - ", predResults)

        #NOte this methids also writes  the relative_errors file 
        (relative_errors, r_squared_errors) =  self.calcErrors(origResults, predResults, header)

        self.calc_write_mean_errors(relative_errors, r_squared_errors)

    def readResults(self):
        try:
            fresults = open(self.results, "r+")
            header = fresults.readline()
            origResults = []
            print(header)
            
            for i in range(self.numSim):
                arr = fresults.readline().strip().split()
                origResults.append(arr)
            #print("Results=", origResults)
        except IOError:
            print("Fcould not open", self.results)
            exit()
        return (header,origResults)

    def getPredictedResults(self, header):

        predictResult=[]
        
        try:
            fpred = open(self.predicted_results, "w+")
            fpred.write(header)

            for i in range(self.num_iter):
                fcvTestOut = open(self.cvTestOutfile + str(i) , "r+")
                line = fcvTestOut.readline()

                for j in range(self.num_sim_per_test):
                    id_curr_sim = (i*self.num_sim_per_test) + j
                    if(id_curr_sim < self.numSim):
                        line = fcvTestOut.readline()
                        fpred.write(line)
                        arr = line.strip().split()
                        predictResult.append(arr)
            return predictResult
        except IOError:
             print("Fcould not open test.out.* files or " + self.predicted_results + "\n")
             exit()

    def calcErrors(self, origResult,  predictResult, header):
        
        # note that results and redResults are both are list of strings ( not
        # floats) at this point
        all_relative_errors = []
        all_sq_errors = []

        frelative_errors = open(self.relative_errors, "w+")
        frelative_errors.write(header)

        for i in range(self.numSim):
            relative_errors = []
            sq_errors = []
            for j in range(self.numOut):
                err_val = (float(origResult[i][j]) - float(predictResult[i][j]))
                sq_err = err_val * err_val
                if(abs(float(origResult[i][j])) > self.ORIG_VAL_TOLERANCE):
                    err = abs(err_val/float(origResult[i][j]))
                else:
                    err = "nan"

                frelative_errors.write(str(err) + "\t")
                relative_errors.append(err)
                sq_errors.append(sq_err)
            frelative_errors.write("\n")
            #print("Absolute Relative Errors(bootstrap case)", i , "=",relative_errors)
            all_relative_errors.append(relative_errors)
            all_sq_errors.append(sq_errors)
        
        frelative_errors.close()
        #print(all_sq_errors)
        return (all_relative_errors, all_sq_errors)

    def calc_write_mean_errors(self, relative_errors, sq_errors):
        
        try:
            ferr = open(self.errors, "w")
        except IOError:
            print_and_log("Could not open"+ self.errors +"\n")
            return
        
        for j in range(self.numOut):
            meanError = 0
            mean_sqError = 0
            valid_num_sim = 0
            sumOutput = 0
            for i in range(self.numSim):
                if(relative_errors[i][j] != "nan" and meanError!= "nan"):
                    meanError = relative_errors[i][j] + meanError
                    valid_num_sim = valid_num_sim + 1
                else:
                    meanError = "nan"
                mean_sqError = sq_errors[i][j] + mean_sqError
 
            if(meanError != "nan"):
                meanError = (meanError*100) / valid_num_sim

            mean_sqError = float(mean_sqError/self.numSim)
            ferr.write(self.output_names[j] + "," + str(meanError) + "," +
                       str(mean_sqError) + "\n")
            print(mean_sqError) 
        
        ferr.close()
        return

    
    def create_inoutfiles(self):

        inputfiles = [self.infile, self.results] 
        outputfiles = [self.cvInfile,  self.cvOutfile]

        num_sim_test = self.num_sim_per_test

        for (inputfile, outputfile) in zip(inputfiles, outputfiles):
            for i in range(self.num_iter):
                try:
                    name = outputfile + str(i)
                    fout = open(name, "w+")
                    # to copy header
                    j = -1
                
                    f = open(inputfile, "r+")
                    #range_min is inclusive and range_max is exclusive
                    range_min = i*num_sim_test
                    range_max = (i+1) * num_sim_test
                    for line in f:
                        if((j==-1) or (j < range_min) or (j>= range_max)):
                            fout.write(line)
                        j=j+1
                    fout.close()
                    f.close()
                except IOError:
                    print("Could not open file", inputfile, "or ", outputfile)
        return

    def create_tests(self):

        num_sim_test = self.num_sim_per_test
        
        for i in range(self.num_iter):
            try:
                name = self.cvTestfile + str(i)
                fout = open(name, "w+")
                # to copy header
                j = -1
                range_min = i*num_sim_test
                range_max = (i+1) * num_sim_test

                f = open(self.infile, "r+")
                for line in f:
                    if(j==-1 or ((range_min <= j ) and (j < range_max))):
                        fout.write(line)
                    j=j+1
                fout.close()
                f.close()
            except IOError:
                print("Could not open file")
        return


#print("Average Error associated with the ROM = " + str(avg_error))
#if(avg_error >= threshold):
#   print("Since current samples do not meet threshold criteria. lets put more samples")
#    curriter = raw_input("Enter the iteration you are at")
#    (inValues, num) = adaptive_sample(infile, numin, numsim, threshold)
#    print(" Putting " + str(num) + " more samples")

#    filename = jobdir + "/rom.in." + str(curriter+1)
#    print("Saving resulst to " + filename)
#    writeROMfile(filename, invarlist, inValues, num, numInParam)
