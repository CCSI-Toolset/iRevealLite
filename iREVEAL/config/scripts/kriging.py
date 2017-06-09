import os.path
import sys
import subprocess
import config

#----------------BUILD ROM provided input and output parameter set-------------------	
class kriging:
    
    def __init__(self, nx, ny, nds, ntest, ifile, ofile, tfile, outfile,
                 mode, path=''):
        if(path == ''):
            path = config.pathKriging
        
        #print(" Kriging executable = " + path)
        #command = [path, str(nx),  str(ny), str(nds), str(ntest), ifile, ofile, tfile, toutfile]
        if(mode == "-i"):
            command = [path, mode, str(nx),  str(ny), str(nds), ifile, ofile, outfile, tfile]
        if(mode == "-a"):
            command = [path, mode, str(nx),  str(ny), str(nds), ifile, ofile, outfile]
        if(mode == "-c"):
            command = [path, mode, str(nx),  str(ny), str(nds), ifile, ofile, outfile]
        print command
        proc = subprocess.Popen(command)
        retval = proc.wait()
        return
