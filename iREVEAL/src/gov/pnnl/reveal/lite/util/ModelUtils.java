package gov.pnnl.reveal.lite.util;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import DataModel.UnitOperation;

public class ModelUtils {
	
	//TODO: Should be configurable through some properties file.
	private static String tempCOFileName = "CapeOpen.rom";
	private static String tempACMFFileName = "ACM_code.acmf";
	
	/**
	 * 2) java DataModel.UnitOperation -c myROM example_for_alstom_gasifier_setup.input myROM.rom param.in
	 * This will create param.in file and first part of ROM file
	 */
	public static void createParamAndROM(File modelInputFile, String runDirPath) throws IOException{
		String tempCOFilePath = runDirPath+File.separatorChar+tempCOFileName;
		String tempACMFFilePath = runDirPath+File.separatorChar+tempACMFFileName;
		String paramInPath = runDirPath+File.separatorChar+"param.in";
		if(Utilities.isDebugging())
			System.out.println("Calling UnitOperation with "+ " -c, " + modelInputFile.getAbsolutePath()+", "+tempCOFilePath+", "+paramInPath);
		UnitOperation.main(new String[]{"-c",modelInputFile.getAbsolutePath(),tempCOFilePath,paramInPath});
		if(Utilities.isDebugging())
			System.out.println("Calling UnitOperation with "+ " -a, " + modelInputFile.getAbsolutePath()+", "+tempACMFFilePath+", "+paramInPath);
		UnitOperation.main(new String[]{"-a",modelInputFile.getAbsolutePath(),tempACMFFilePath,paramInPath});
		if(Utilities.isDebugging())
			System.out.println("UnitOperation Completed");
	}
	
	
	/**
	 * 3) Run Python code to create “rom.in” file
	 * Python rom_builder.py –d $dir –s $sampling_type
	 */
	public static void createSamplingFile(String runDirPath, String samplingMethod){
		String scripts = Utilities.getScriptsPath();
		String regressionCommand = "cmd /c python \""+ new File(scripts, "rom_builder.py") + "\" -s "+ samplingMethod+" -d " + runDirPath  ;
		if(Utilities.isDebugging()){
			System.out.println("Executing python script: ");
			System.out.println(regressionCommand);
		}
		runCommand(regressionCommand);
		System.out.println("Process Complete");
	
	}
	
	
	/**
	 * 4)	Save “results” file in the same folder 
	 * And run python code like this to generate “Kriging/errors” file
	 * Python rom_builder.py –d $dir –b –p “pathtoKriging/rom.exe”
	 */
	public static void runRegression(File resultsFile, String runDirPath){
			String scripts = Utilities.getScriptsPath();
			String krigingPath = Utilities.getConfigPath()+File.separatorChar+"Kriging"+File.separatorChar+"rom.exe";
			String regressionCommand = "cmd /c python \""+ new File(scripts, "rom_builder.py") + "\" -d \"" + runDirPath +"\" -b -p \""+krigingPath+"\"";
			if(Utilities.isDebugging())
				System.out.println(regressionCommand);
			runCommand(regressionCommand);
			
		/*try{	
			ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "python26", new File(scripts, "rom_builder.py").getAbsolutePath(), "-d", runDirPath, "-b", "-p", krigingPath);
			File input = new File("iREVEAL_in.log");
			if(!input.exists())
				input.createNewFile();
			File output = new File("iREVEAL_out.log");
			if(!output.exists())
				output.createNewFile();
			File errors = new File("iREVEAL_error.log");
			if(!errors.exists())
				errors.createNewFile();
			pb.redirectInput(input);
			pb.redirectError(errors);
			pb.redirectOutput(output);
			Process p = pb.start();
			p.waitFor();
			System.out.println(pb.redirectInput());
			System.out.println(pb.redirectOutput());
			System.out.println(pb.redirectError());
			
			
		} catch (Exception e) {
				e.printStackTrace();
			}*/
			
			
			System.out.println("Process Complete");
	}
	
	/**
	 * 5)	run python code like this to generate “Kriging.acmf and Kriging.co file
	 * Python rom_builder.py –d $dir –e –p “pathtoKriging/rom.exe”
	 */
	public static void createExportFile(String runDirPath) throws IOException{
		String scripts = Utilities.getScriptsPath();
		String krigingPath = Utilities.getConfigPath()+File.separatorChar+"Kriging"+File.separatorChar+"rom.exe";
		//String regressionCommand = "cmd /c python \""+ new File(scripts, "rom_builder.py") + "\" -s "+ samplingMethod+" -d \"" + runDirPath + "\"" ;
		String regressionCommand = "cmd /c python \""+ new File(scripts, "rom_builder.py") + "\" -d \"" + runDirPath +"\" -e -p \""+krigingPath+"\"";
		System.out.println(regressionCommand);
		runCommand(regressionCommand);
		
		
		File exportFile = new File(runDirPath,"model.acmf");
		exportFile.createNewFile();
		File modelACMFTempFile = new File(runDirPath, tempACMFFileName);
		if(!modelACMFTempFile.exists())
			System.out.print("modelACMF.temp not found");
		File krigingExportFile = new File(runDirPath,"Kriging.acmf");
		if(!krigingExportFile.exists())
			System.out.print("Kriging.acmf not found");
		
		List<File> files= new ArrayList<File>();
		files.add(exportFile);
		files.add(modelACMFTempFile);
		files.add(krigingExportFile);
		FileUtils.concatenate(files);
		//krigingExportFile.deleteOnExit();
		//modelACMFTempFile.deleteOnExit();
		
		exportFile = new File(runDirPath,"model.rom");
		exportFile.createNewFile();
		File modelCOTempFile = new File(runDirPath, tempCOFileName);
		if(!modelCOTempFile.exists())
			System.out.print("modelCO.temp not found");
		krigingExportFile = new File(runDirPath,"Kriging.co");
		if(!krigingExportFile.exists())
			System.out.print("Kriging.co not found");
		
		files= new ArrayList<File>();
		files.add(exportFile);
		files.add(modelCOTempFile);
		files.add(krigingExportFile);
		FileUtils.concatenate(files);
		//krigingExportFile.deleteOnExit();
		//modelCOTempFile.deleteOnExit();
		

		System.out.println("Process Complete");
	}
	
	
	public static void runCommand(String command) {
		try {
			
			String line = null;
			String error = null;
			String output = "";
			String errorString = "";
			
			Runtime r = Runtime.getRuntime();
			Process p = r.exec(command);
			
			BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			BufferedReader br1 = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			
			while ((line = br.readLine()) != null) {
				
				output=line.trim();
				if(Utilities.isDebugging())
					System.out.println(output);
				if(output.contains("Exiting With Error")){
					System.out.println("Exiting with error: Please check python.log for more details: "+System.getProperty("user.dir")+File.separatorChar+"python.log");
					System.exit(0);
				}
			}
			
			while ((error = br1.readLine()) != null) {
				errorString=errorString+"\n"+error.trim();
			}
			br.close();
			br1.close();
		
		} catch (Exception e) {
			System.out.println("Exception @RunPythonFromCommandLine:runCommand");
			System.out.println(e.toString());
			e.printStackTrace();
		}
	}
	

}
