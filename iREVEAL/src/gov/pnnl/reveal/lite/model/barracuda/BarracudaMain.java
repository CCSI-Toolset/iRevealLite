package gov.pnnl.reveal.lite.model.barracuda;

import gov.pnnl.reveal.lite.util.FileUtils;
import gov.pnnl.reveal.lite.util.ModelUtils;
import gov.pnnl.reveal.lite.util.Utilities;

import java.io.File;

public class BarracudaMain {
	
	static String usage  = "For sampling: -s [sampling method] -i [model input file] \n"+	
						"For regression: -r [results file] -d [working directory path] \n"+
						"For ROM export: -e [working directory path]";
	
	public static void main(String[] args){
		try{
			
			Utilities.getInstance();
			
			int i=1;
			for(String str: args){
				System.out.println("Input "+i+": "+str);
				i++;
			}
			
			if(args.length<2){
				System.out.println("Error: Not enough input arguments.");
				System.out.println("Please see Usage");
				System.out.println(usage);
				return;
			}
				
			//-c modelInputFile -d runDir
			if(args[0].equals("-c")){
				File inputFile = new File(args[1]);
				String runDirPath = args[3];
				File modelInputFile = new File(runDirPath,inputFile.getName());
				FileUtils.copy(inputFile, modelInputFile);
				ModelUtils.createParamAndROM(modelInputFile, runDirPath);
			}
			
			//-s samplingMethod -d runDir
			if(args[0].equals("-s")){
				
				if(args.length!=4){
					System.out.println("Error: Incorrect input arguments");
					System.out.println("Please see Usage");
					System.out.println(usage);
					return;
				}
				
				File modelInputFile = new File(args[3]);
				if((!modelInputFile.exists()) || (!modelInputFile.isFile())){
					System.out.println("Error: model input file does not exist. Path is incorrect: "+modelInputFile.getAbsolutePath());
					System.out.println("Please see Usage");
					System.out.println(usage);
					return;
				}
				else{
					System.out.println(modelInputFile.getAbsolutePath()+" exists");
				}
				
				File runDir  = new File(modelInputFile.getAbsolutePath());
				runDir = runDir.getParentFile();
				//String samplingMethod = args[1];
				
				ModelUtils.createParamAndROM(modelInputFile, runDir.getAbsolutePath());
				//ModelUtils.createSamplingFile(runDir.getAbsolutePath(), samplingMethod);
				
				
			}
			
			else if(args[0].equals("-r")){
				
				if(args.length!=4){
					System.out.println("Error: Incorrect input arguments");
					System.out.println("Please see Usage");
					System.out.println(usage);
					return;
				}
				
				String runDirPath = args[3];
				if(!new File(runDirPath).exists()){
					System.out.println("Error: Run directory path does not exist.");
					System.out.println("Please see Usage");
					System.out.println(usage);
					return;
				}
				File inputResultsFile =new File(args[1]);
				if(!inputResultsFile.exists()){
					System.out.println("Error: Results file does not exist.Path is incorrect");
					System.out.println("Please see Usage");
					System.out.println(usage);
					return;
				}
				
				/*if(!new File(runDirPath,"param.in").exists()){
					System.out.println("Error: pram.in file does not exist in working direcotry. Please run using '-s' option to generate it first.");
					System.out.println("Please see Usage");
					System.out.println(usage);
					return;
				}
				
				if(!new File(runDirPath,"rom.in").exists()){
					System.out.println("Error: rom.in file does not exist in working direcotry. Please run using '-s' option to generate it first.");
					System.out.println("Please see Usage");
					System.out.println(usage);
					return;
				}*/
				
				if(runDirPath.equals("."))
					runDirPath = new File(runDirPath).getAbsoluteFile().getParent();
				else
					runDirPath = new File(runDirPath).getAbsolutePath();
				if(!runDirPath.equals(inputResultsFile.getAbsoluteFile().getParent())){
					File resultsFileInRunDir = new File(runDirPath,inputResultsFile.getName());
					FileUtils.copy(inputResultsFile, resultsFileInRunDir);
				}
				ModelUtils.runRegression(inputResultsFile, runDirPath);
				
				
				
			}
			
			else if(args[0].equals("-e")){
				
				if(args.length!=2){
					System.out.println("Error: Incorrect input arguments");
					System.out.println("Please see Usage");
					System.out.println(usage);
					return;
				}
				
				String runDirPath = args[1];
				if(!new File(runDirPath).exists()){
					System.out.println("Error: Run directory path does not exist.");
					System.out.println("Please see Usage");
					System.out.println(usage);
					return;
				}
				ModelUtils.createExportFile(runDirPath);
			}
			
			else{
				System.out.println("Error: Incorrect input argument");
				System.out.println("Please see Usage");
				System.out.println(usage);
				return;
			}
				
		}
		catch(Exception e){
			System.out.println("Error: Invalid input. Please see Usage:");
			System.out.println(usage);
			if(Utilities.isDebugging())
				e.printStackTrace();
			
		}
	}	
	
}
