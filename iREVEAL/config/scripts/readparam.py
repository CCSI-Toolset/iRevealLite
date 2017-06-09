
#----------------Read user provided parameters like model, I/Oparameters, ranges etc --------------------

from model import *
import  logging
import subprocess 


#---------- Generate output parameter names from data in param.in ---
def gen_output_param_name(port_id, arr_list):

    if(port_id == -1):
        print(" MFIX output sould not have negative port id")
        exit()

    if(len(arr_list) != 3):
        print(" MFIX output parameters not in corrcte format")
        exit()

    phase_id = arr_list[0]
    species_id = arr_list[1]
    output_name = arr_list[2]

    gas_var_list = ["P_g", "V_g", "T_g"]
    solid_var_list = ["P_s", "V_s", "T_s"]
    # gas is single phase, PVT of gas is not associated with species too
    if(output_name in gas_var_list):
        outvar = output_name + "(" + str(port_id) +")"

    # Mass frcation of gas, will have no phase, but species
    elif(output_name == "X_g"):
        outvar = "X_g(" + str(port_id) + "," + species_id + ")"

    #
    elif(output_name in solid_var_list):
        outvar = output_name + "(" + str(port_id) + "," + phase_id + ")"

    #
    elif(output_name == "X_s"):
       outvar = "X_s(" + str(port_id) + "," + phase_id + "," + species_id + ")"

    else:
        print(" New output variable, did not recogbnize", output_name)
        exit()

    return outvar


def readUserInput(paramfile):
    if(paramfile):
        try:
            f = open(paramfile, "r+")

            m = model() ;
            num_sims = []
            num_iter = 0
            iter_names = []

            # handle SOFC constraints
            num_fuel_cons = 0
            list_fuel_cons = []
            
            # handle MFIX constraints
            num_mfix_cons = 0
            list_mfix_cons = []

            exepath = ''
            for line in f:
                line = line.strip("\n")
                arr = line.split('=')
                if((arr[0] == "Input_Model")):
                    m.modelType = arr[1].strip()
                elif(arr[0] == "RUN_NAME"):
                    m.runName = arr[1].strip()
                elif(arr[0] == "TSTART"):
                    m.tstart = float(arr[1].strip())
                elif(arr[0] == "TSTOP"):
                    m.tstop = float(arr[1].strip())
                elif(arr[0] == "DatFilePath"):
					m.modelInputFile = arr[1].strip()
                elif((arr[0] == "No_Of_Simulations")):
                    #m.numSim = [int(s) for s in arr[1].strip().split(',')]
                    m.numSim = int(arr[1].strip())
                elif((arr[0] == "No_Of_Iterations")):
                    num_iter = int(arr[1].strip())
                elif(arr[0].strip() == "Iterations_Name_Starts"):
                    for line in f:
                        line = line.rstrip("\n")
                        arr = line.split()
                        if(arr[0] == "Iterations_Name_Ends"):
                            break;
                        else:
							iter_names.append(arr[0])
                elif(arr[0].strip() == "Input_Parameter_Starts"):
                    for line in f:
                        line = line.rstrip("\n")
                        arr = line.split()
                        m.set_fuel_vs_cell_flag(arr[0])
                        m.set_oxidant_vs_cell_flag(arr[0])
                        if(arr[0] == "Input_Parameter_Ends"):
                            break;
                        else:
							m.inputNames.append(arr[0])
							m.numIn += 1
							m.minsList.append(float(arr[1]))
							m.maxsList.append(float(arr[2]))
                
                #elif(arr[0].strip() == "Mass_Fraction_Constraint_Starts"):
                #    for line in f:
                #        line = line.rstrip("\n")
                #        arr = line.split()
                #        if(arr[0] == "Mass_Fraction_Constraint_Ends"):
                #            break;
                #        else:
                #            m.numCons += int(arr[1])
                #            m.constraintList.append("MFIX_COMP")
                #            num_mfix_cons = int(arr[1])
                #            for i in range(0, num_mfix_cons):
                #                constraint = mfix_cons(f)
                #                #print("MFIX Constraint ", i+1, ":")
                #                #constraint.print_cons()
                #                list_mfix_cons.append(constraint)

                #elif(arr[0].strip() == "Begin_Constraint_FuelComposition"):
                #    m.num_cons += 1
                #    m.constraint_list.append("FUEL_COMP")
                    
                #    fuelcell_cons fuelcomp;
			    #	 fuelcomp.cons_type= "FUEL_COMP"
                #    num_fuel_cons += 1
                #    for line in f:
                #        line = line.rstrip("\n")
                #        arr = line.split()
                #        if(arr[0] == "End_Constraint_FuelComposition"):
                #            list_fuel_cons.append(fuelcomp)
                #            break;
                #        else:
				#			 fuelcomp.num_in += 1
				#			 fuelcomp.input_names.append(arr[0]))
			    #			 fuelcomp.input_mins.append(float(arr[1]))
                #            fuelcomp.input_maxs.append(float(arr[2]))
                #
                #
                #elif(arr[0].strip() == "Begin_Constraint_OxidantComposition"):
                #    m.num_cons += 1
                #    m.constraint_list.append("OXIDANT_COMP")
                #    
                #    fuelcell_cons oxidantcomp;
                #    num_fuel_cons += 1
                     
				#	 oxidant_comp.cons_type= "OXIDANT_COMP"
                #    for line in f:
                #        line = line.rstrip("\n")
                #        arr = line.split()
                #        if(arr[0] == "End_Constraint_OxidantComposition"):
                #            list_fuel_cons.append(oxidantcomp)
                #            break;
                #        else:
				#			 oxidant_comp.num_in += 1
                #            oxidant_comp.input_names.append(arr[0])
				#			 oxidant_comp.input_mins.append(float(arr[1]))
				#			 oxidant_comp.input_maxs.append(float(arr[2]))
                #
                #elif(arr[0].strip() == "Begin_Constraint_Steam_Carbon_Ratio"):
                #    m.num_cons += 1 
                #    m.constraint_list.append("SC_RATIO")
                #    m.is_fuelflowrate = True
                #    
                #    sc_ratio = fuelcell_cons();
                #    num_fuel_cons += 1
                #    sc_ratio.cons_type = "SC_RATIO"
                #    for line in f:
                #        line = line.rstrip("\n")
                #        arr = line.split()
                #        if(arr[0] == "End_Constraint_Steam_Carbon_Ratio"):
                #            list_fuel_cons.append(sc_ratio)
                #            break;
                #        elif(len(arr) == 1):
                #            sc_ratio.sc_ratio_val = float(arr[0])
                #        elif(len(arr) == 3):
                #            sc_ratio.num_in += 1
                #            sc_ratio.input_names.append(arr[0])
                #            sc_ratio.mins_list.append(float(arr[1]))
                #            sc_ratio.maxs_list.append(float(arr[2]))
                #        else:
                #            print("Unrecognized format in SC_RATIO spec")
                            
                elif(arr[0].strip() == "Output_Parameter_Starts"):
                    for line in f:
                        line = line.rstrip("\n")
                        arr = line.split()
                        if(arr[0] == "Output_Parameter_Ends"):
                            break;
                        else:
                            outvar = arr[0]
                            [m.outputList.append(outvar)]
                elif(arr[0].strip() == "Dependent_Parameter_Starts"):
                    for line in f:
                        line = line.rstrip("\n")
                        arr = line.split()
                        if(arr[0] == "Dependent_Parameter_Ends"):
                            break;
                        else:
							m.depList.append(arr)
							m.numDep += 1
                else:
                    print("Could not recognize this user configuration option")
                    #logging.info('%',arr,'%')
                    print("Exiting With Error, chcek pyhton.log for details")
                    exit()
                    

        except IOError:
            logging.info("Could not open " + paramfile + "\n Please check filepath")
            print("Exiting With Error, chcek pyhton.log for details")
            exit()
        m.numOut = len(m.outputList)
        print("Model Data from " + paramfile + ":")
        m.printdata()
        #return(m, num_sims, num_iter, iter_names, num_fuel_cons, list_fuel_cons, num_mfix_cons, list_mfix_cons)
        return(m)





        

    


        
