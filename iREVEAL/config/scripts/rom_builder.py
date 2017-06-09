from config import *
from include import *
from crossvalidation import *
from readparam import *

def __main__(options):

    jobDir = options.dir

    (paramInFile, romInFile, resultsFile, predictedResultsFile, psuadeConfigFile, samplingFile) = configure(jobDir, options)

    logging.info("\nReading user configuration :" + psuadeConfigFile +"\n")
    
    paramFileDataModel =  readUserInput(paramInFile) 

    romModel = getROMConfig(psuadeConfigFile)

    print("Model data from : " + psuadeConfigFile + ":")
    romModel.printdata()

    if(options.buildROM):
        logging.info("\n Mode = Analyze ROM (Assumtion : all simulation runs completed)\n")
        logging.info("\nReading input parameter set :" + samplingFile)

        #Read data from PSUADE format and convert to space seperated csv format
        #NOte the order of input and output parameters in "Psuade.in" file and
        #"param.in" dont match. SInce Kriging requires order to be in param.in
        #format : the samples are reorederd data to be in same order as in
        # paramFileDataModel
        sampledData = getSampledData(samplingFile, romModel)
        paramFileOrderSampledData = orderByParamFile(sampledData, romModel,
                                                          paramFileDataModel)

        writeROMFile(romInFile, paramFileDataModel.get_input_names(), paramFileOrderSampledData,
                     paramFileDataModel.get_num_sim(), paramFileDataModel.get_num_in())

        #writeROMFile(romInFile, paramFileDataModel.get_input_names(), sampledData,
        #             paramFileDataModel.get_num_sim(), paramFileDataModel.get_num_in())
        print(str(paramFileOrderSampledData))

        # generate results file from simulation if needed
        #if(options.genResults):
        #    model_type = "MFIX"
        #    num_iter = 1
        #    iter_names = "Iteration_1"
        #    results = getSimResults(resultsFile, model_type, num_iter,
        #                            iter_names, romModel.get_output_list(), jobDir)
        #    writeResultsFile(resultsFile, results)


        # perform regression/cross validation to generate Errors  and Predicted
        # results file
        regType = options.regression
        regPath = options.regPath
        regDir = jobDir +"/"+ regType
        if(os.path.exists(regDir) == False):
            os.makedirs(regDir)
        crossValidate(regType, regPath, paramFileDataModel, romInFile, resultsFile, predictedResultsFile, jobDir)

    elif(options.exportROM):
        logging.info("\n Mode = Export ROM (Assumtion : all simulation runs completed)\n")
        regType = options.regression
        regPath = options.regPath
        regDir = jobDir +"/"+ regType
        if(os.path.exists(regDir) == False):
            os.makedirs(regDir)
        
        acmFile = jobDir+"/"+str(regType) + ".acmf"
        coFile = jobDir+"/"+str(regType) + ".co"

        runExport(regType, regPath, paramFileDataModel, romInFile, resultsFile, acmFile, coFile, jobDir)

        print("Completed Export. Check Output File :" + acmFile + " and " + coFile)

    else:
        print("Unsupported mode currently, please contact iREVEAL team for any feature requests")
        logging.info("Unsupported mode currently, please contact iREVEAL team for any feature requests")

if (__name__ == "__main__"):
    print("Use -h or --help to list the command line options.\n")
    (options,args) = get_arg()
    logging.basicConfig(filename='python.log', filemode='w', level=logging.INFO)
    logging.info('Starting python main()')
    sys.exit(__main__(options) )











            

