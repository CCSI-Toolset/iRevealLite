
import subprocess

class model:

    def __init__(self):
        self.modelType = "CFD"
        self.modelInputFile = "default"
        self.runName = "default"
        self.tstart = 0
        self.tstop = 0
        self.numSim = 0
        self.numIn = 0
        self.inputNames = []
        self.minsList = []
        self.maxsList = []
        self.numOut = 0
        self.outputList = []
        self.numCons = 0
        self.constraintList = []
        self.numDep = 0
        self.depList = []
        self.isFuelFlowrate = False
        self.isCellFlowrate = False
        self.isOxStackfFlowrate = False
        self.isOxCellFlowrate = False
    
    def set_model_type(self, modelType):
        self.modelType = modelType

    def set_model_input_file(self, modelInputFile):
        self.modelInputFile = modelInputFile
    
    def set_run_name(self, runName):
        self.runName = runName
    
    def set_tstart(self, tstart):
        self.tstart = tstart
    
    def set_tstop(self, tstop):
        self.tstop = tstop
    
    def set_num_sim(self, num):
        self.numSim = num
    
    def set_num_in(self, numIn):
        self.numIn = numIn
    
    def set_input_names(self, inputNames):
        self.inputNames = inputNames
    
    def set_mins_list(self, minsList):
        self.minsList = minsList
    
    def set_maxs_list(self, maxsList):
        self.maxsList = maxsList
    
    def set_num_out(self, numOut):
        self.numOut = numOut
    
    def set_output_list(self, outputList):
        self.outputList=outputList

    def set_num_cons(self, numCons):
        self.numCons = numCons
    
    def set_constraint_list(self, constraintList):
        self.constraintList = constraintList
   
    def set_num_dep(self, numDep):
        self.numDep = numDep

    def set_dep_list(self, depList):
        self.depList = depList
   
    def set_fuel_vs_cell_flag(self, param_name):
        if(param_name == "CellFuelFlowRate"):
            self.isCellFlowrate = True
        elif(param_name == "StackFuelFlowRateCO" or param_name ==
             "StackFuelFlowRateCO2" or param_name == "StackFuelFlowRateCH4" or
             param_name ==  "StackFuelFlowRateH2O" or param_name ==
             "StackFuelFlowRateH2" or param_name == "StackFuelFlowRateN2"):
            self.isFuelFlowrate = True
        else:
            return
        
        return
    
    def set_oxidant_vs_cell_flag(self, param_name):
        if(param_name == "CellOxidantFlowRate"):
            self.isOxCellFlowrate = True
        elif(param_name == "StackOxidantFlowRateO2" or param_name == 
             "StackOxidantFlowRateN2"):
            self.isOxStackFlowrate = True
        else:
            return
        return
    
    ##------GET METHODS-------------#####
    
    
    def get_model_type(self):
        return(self.modelType)

    def get_model_input_file(self):
        return(self.modelInputFile)
    
    def get_run_name(self):
        return(self.runName)
    
    def get_tstart(self):
        return(self.tstart)
    
    def get_tstop(self):
        return(self.tstop)
    
    def get_num_sim(self):
        return(self.numSim)
    
    def get_num_in(self):
        return(self.numIn)
    
    def get_input_names(self):
        return(self.inputNames)
    
    def get_mins_list(self):
        return(self.minsList)
    
    def get_maxs_list(self):
        return(self.maxsList)
    
    def get_num_out(self):
        return(self.numOut)
    
    def get_output_list(self):
        return(self.outputList)

    def get_num_cons(self):
        return(self.numCons)
    
    def get_constraint_list(self):
        return(self.constraintList)
   
    def get_num_dep(self):
        return(self.numDep)

    def get_dep_list(self):
        return(self.depList)


    def printdata(self):
        print("Model Type= ", self.modelType)
        print("Template input file=", self.modelInputFile)
        #print("Run Name", self.runName)
        #print("TStart =", self.tstart)
        #print("Tstop", self.tstop)
        print("Number of Simulations", self.numSim)
        print("num input", self.numIn)
        print("Input names", self.inputNames)
        print(" Mins list", self.minsList)
        print("Maxs list ",self.maxsList)
        print("Number of output", self.numOut)
        print("Outoput list", self.outputList)
        #print("#Constraint ", self.numCons)
        #print("Constaerint list", self.constraintList)
        #print("Number of dependenet", self.numDep)
        #print("Dep list ", self.depList)




