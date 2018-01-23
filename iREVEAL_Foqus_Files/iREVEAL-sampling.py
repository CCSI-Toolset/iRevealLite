#
#FOQUS_SURROGATE_PLUGIN
#
# Surrogate plugins need to have FOQUS_SURROGATE_PLUGIN in the first
# 150 characters of text.  They also need to hav a .py extention and
# inherit the surrogate class.
#
#
# iREVEAL.py
#
# * This is an example of a surrogate model builder plugin for FOQUS, 
#   it uses the iREVEAL surrogate model builder program ref:
#
# [TODO: reference]
#
# * A setting of this plugin is the location of the iREVEAL executable 
#
#
# John Eslick, Carnegie Mellon University, 2014
#

import numpy as np
import threading
import Queue
import logging
import subprocess
import os
import copy
import traceback
import time
from foqus_lib.framework.surrogate.surrogate import surrogate
from foqus_lib.framework.uq.SurrogateParser import SurrogateParser
import subprocess
import csv

def checkAvailable():
    '''
        Plug-ins should have this function to check availability of any
        additional required software.  If requirements are not available
        plug-in will not be available.
    '''
    return True

class surrogateMethod(surrogate):
    def __init__(self, dat=None):
        '''
            iREVEAL interface constructor
        '''
        surrogate.__init__(self, dat)
        self.name = "iREVEAL-sampling"
        #still working on hanging indent for refernces
        self.methodDescription = \
            ("<html>\n<head>"
             ".hangingindent {\n"
             "    margin-left: 22px ;\n"
             "    text-indent: -22px ;\n"
             "}\n"
             "</head>\n"
             "<b>iREVEAL: Reduced Order Model Builder </b>"
             "<p class=\"hangingindent\">TODO: refer paper </p></html>")
        self.adaptiveCapable = False
        self.adaptive = False
        self.iREVEALDir = 'iREVEAL'
        self.options.add(
            name="Exec Path", 
            default="iREVEAL.jar",
            dtype=str,
            desc="Path to the iREVEAL executable jar file")
        self.options.add(
            name="User Input File", 
            default="elec.json",
            dtype=str,
            desc="User Input File in JSON Format to read")
        self.options.add(
            name="Sampling Method", 
            default="LHS",
            dtype=str,
            desc="Method to create data samples")
        self.options.add(
            name="Sampling File", 
            default="rom.in",
            desc="Output File Name After Sampling Values")
        self.options.add(
            name="Results File", 
            default="results",
            desc="Results File After Running Simulations")
        self.options.add(
            name="Regression Method", 
            default="Kriging",
            desc="Regression Method To Create Surrogate ROM")
        
    def run(self):
        '''
            This function overloads the Thread class function,
            and is called when you run start() to start a new thread.
            
            a.    To create samples:
                    java -jar iREVEAL.jar -s LHS -i userInputFile.json
            
            b.    To run regressing analysis:
                    java -jar iREVEAL.jar -r resultsFile -d workingDir
            
            c.    To export model
                    java -jar iREVEAL.jar -e exportDir
            
        '''
        #Create Samples
        iREVEALDir = self.iREVEALDir
        iREVEALExec = self.options["Exec Path"].value
        iREVEALUserInput = self.options["User Input File"].value
        iREVEALSamplingMethod = self.options["Sampling Method"].value
        iREVEALSamplingFile = self.options["Sampling File"].value
            
        self.msgQueue.put("------------------------------------")
        self.msgQueue.put("Starting iREVEAL Sampling\n")
        self.msgQueue.put("Exec File Path:    " + iREVEALExec)
        self.msgQueue.put("Sub-directory:     " + iREVEALDir)
        self.msgQueue.put("User Input File Name:   " + iREVEALUserInput)
        self.msgQueue.put("Sampling Method:   " + iREVEALSamplingMethod)
        self.msgQueue.put("Output File Name:  " + iREVEALSamplingFile)
        self.msgQueue.put("------------------------------------")
            
        try:
            process = subprocess.Popen([
                "java",
                "-jar",
                iREVEALExec,
                "-s",
                iREVEALSamplingMethod,
                "-i",
                iREVEALUserInput],
                cwd=iREVEALDir,
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT)
            line = process.stdout.readline()
            while process.poll() == None or line != '':
                if line == '': time.sleep(0.2)
                if line != '':
                    self.msgQueue.put(line.rstrip())
                line = process.stdout.readline()
                if self.stop.isSet():
                    self.msgQueue.put("**terminated by user**")
                    process.kill()
                    break
            self.msgQueue.put("Output Sampling File Name:  " + iREVEALSamplingFile)
        except Exception as e:
            logging.getLogger("foqus." + __name__).\
            error("Problem running iREVEAL:\n" + traceback.format_exc())
            #should raise an exception here
        
        with open(iREVEALDir+'/'+iREVEALSamplingFile, 'r') as f:
            reader = csv.reader(f, dialect='excel', delimiter='\t')
            output = ''
            for row in reader:
                output = output + ' '.join(row) + '\n'
            
        self.result = {'outputEqns': output}
        
    def nInput(self):
        n = 0
        for v in self.input:
            n += self.graph.input.get(v).nElements()
        return n
            
    def nOutput(self):
        n = 0
        for v in self.output:
            n += self.graph.output.get(v).nElements()
        return n
