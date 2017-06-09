package gov.pnnl.reveal.lite.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;


public class Utilities {
	
	private static String configPath;
	private static String scriptsPath;
	private static Utilities instance;
	private static boolean debugging=true;

	public static Utilities getInstance() throws FileNotFoundException, UnsupportedEncodingException{
		if(instance == null)  {
			Utilities util = new Utilities();
			
			if(ClasspathUtils.getFileFromClassFolder(util.getClass(), "config")!=null)
				setConfigPath(ClasspathUtils.getFileFromClassFolder(util.getClass(), "config").getAbsolutePath());
			else 
				setConfigPath(new File("config").getAbsolutePath());
			
			if(ClasspathUtils.getFileFromClassFolder(util.getClass(), "config/scripts")!=null)
				setScriptsPath(ClasspathUtils.getFileFromClassFolder(util.getClass(), "config/scripts").getAbsolutePath());
			else 
				setScriptsPath(new File("config/scripts").getAbsolutePath());
			
			if(debugging){
				System.out.println(getConfigPath());
				System.out.println(getScriptsPath());
			}
			
			instance = util;
		}
		return instance;
	}
	
	public static boolean isDebugging(){
		return debugging;
	}
	
	public static String getConfigPath() {
		return configPath;
	}

	public static void setConfigPath(String path) {
		configPath = path;
	}
	
	public static String getScriptsPath() {
		return scriptsPath;
	}

	public static void setScriptsPath(String scriptsPath) {
		Utilities.scriptsPath = scriptsPath;
	}


}
