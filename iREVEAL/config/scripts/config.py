# file config.py contains all global variables definitions required by python 
# setup to execute
import os.path
import sys
import subprocess
from optparse import OptionParser

pathKriging = "../plugins/Kriging/rom.exe"
pathANN = '../plugins/ANN/'

def configure(jobDir, options):
    regType = options.regression
    paramfile = jobDir + "/param.in"
    rominfile = jobDir + "/rom.in"
    resultsfile = jobDir + "/results"
    predicted_resultsfile = jobDir + "/" + regType + "/predicted_results"
    testfile = jobDir + "/test"
    testoutfile = jobDir + "/" + regType + "/test.out"
    psuadeConfigFile = jobDir + "/psuade.in"
    psuadeSamplingFile = jobDir + "/psuadeData"


    #return(paramfile, rominfile, resultsfile, predicted_resultsfile, testfile, testoutfile)
    return(paramfile, rominfile, resultsfile, predicted_resultsfile, psuadeConfigFile,
           psuadeSamplingFile)


def get_arg():
    parser = OptionParser()
##    parser.add_option("-g", "--generate", 
##                      help = "Generate input files for N simulations",
##                      action="store_true", default=False, dest="generateSim");
    
    parser.add_option("-b", "--build", 
                      help = "Build Reduced Order Model",
                      action="store_true", default=False, dest="buildROM");
    
##    parser.add_option("-t", "--test", 
##                      help = "Gnerate Predicted Values from Reduced Order Model",
##                      action="store_true", default=False, dest="testROM");
    
##    parser.add_option("-a", "--analyze", 
##                      help = "Do sensitivity Analysis",
##                      action="store_true", default=False, dest="sensitivity");
    
    parser.add_option("-e", "--export", 
                      help = "Export Reduced Order Model",
                      action="store_true", default=False, dest="exportROM");
    
    
    parser.add_option("-p", "--path", 
                      help = "Path to use for Kriging/ANN/MARS Executable", 
                      dest="regPath", default = '');
    
    parser.add_option("-r", "--reg", 
                      help="Regression Method : Kriging/MARS/ANN/SVM/Linear/Quadratic/Cubic/Poly4/ANOVA/SRC" , 
                      dest="regression", default="Kriging")
    
    # NOte by default sampling option is enabled
#    parser.add_option("--sample", 
#                      help="Enable Sampling?" , 
#                      dest="sample", default="True")

#    parser.add_option("-s", "--sampling", 
#                      help="Sampling Method : LHS/NORM/QMC" , 
#                      dest="sampling", default="LHS")

    parser.add_option("-d", "--dir", 
                      help="ROM project directory: Contain all files for a ROM", 
                      dest="dir", default=".")
    
#    parser.add_option("-n", "--numSamples", 
#                      help="Number of Test DataSet to use during plotting", 
#                      type="int",  dest="numSteps", default = 3)

    
#    parser.add_option("--sid", 
#                      help="Starting id of simulation, during sample generation", 
#                      type="int",  dest="gen_from_sid", default = 0)

#    parser.add_option("--eid", 
#                      help="End id of simulation, during sample generation", 
#                      type="int",  dest="gen_till_eid", default = -1)

    (options, args) = parser.parse_args()
    return(options, args);

