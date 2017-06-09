package gov.pnnl.reveal.lite.util;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;


/**
 * Common utility methods for dealing with files
 * in the classpath.
 * @author D3K339
 *
 */
public class ClasspathUtils {
  
  /**
   * Uniform way to get files from the class path that works in both
   * regular Java and Eclipse deployments.
   * 
   * This only works for files in a class folder.  Files inside a jar
   * file cannot be retrieved this way.  Instead you need to call
   * object.getClass().getClassLoader().getResourceAsStream() if it's
   * inside a jar file.
   * 
   * @param clazz - any class in the same classpath as the file you are 
   * looking for 
   * @param path - the path to the file in the classpath
   * @return - null if the resource could not be found in the 
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public static File getFileFromClassFolder(Class clazz, String path) {
    File file = null;    
    URL url = clazz.getClassLoader().getResource(path);
    
    try {
      if(url != null) {
        if(url.getProtocol().equals("bundleresource")) {
          // We are running from eclipse so we need to convert the
          // bundle URL to a file URL using eclipse's FileLocator class

          // Let's use reflection to call it so we don't have to import Eclipse classes
          Class fileLocatorClass = null;
          fileLocatorClass = Class.forName("org.eclipse.core.runtime.FileLocator");
          url = (URL) fileLocatorClass.getMethod("toFileURL", new Class[] {URL.class}).invoke(null, new Object[] {url});

        } 
        // make sure path is decoded
        String filePath = URLDecoder.decode(url.getPath(),  "UTF-8");

        // If this resource is inside a jar file, this will throw an exception
        // need to get as stream instead
        file = new File(filePath);
      }
      
    } catch (Exception e) {
    	e.printStackTrace();
    }

    return file;
  }
  
  /**
   * @param clazz - any class in the class path for which you 
   * would like to get the files
   * @return
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static List<File> getFilesOnClassPath(Class clazz) {

    List<File> files = new ArrayList<File>();
    ClassLoader classLoader = clazz.getClassLoader();

    try {
      // We are running from regular java
      if(classLoader instanceof URLClassLoader) {
        URL[] urls = ((URLClassLoader)classLoader).getURLs();

        for (URL url : urls) {
          if(url.getProtocol().equals("file")) {
            // decode the path
            String path = URLDecoder.decode(url.getPath(), "UTF-8");
            files.add(new File(path));
          }
        }
        
      } else {
        // We are running from eclipse
        // First get the files from the current bundle's classpath
        getBundleClasspathFiles(classLoader, files);
        try{
        // Now get the files from the tools plugin bundle
        Class toolsPluginClass = Class.forName("org.akuna.ui.tools.AkunaToolsPlugin");    
        ClassLoader toolsClassLoader = toolsPluginClass.getClassLoader();
        getBundleClasspathFiles(toolsClassLoader, files);
        } catch (Throwable e) {
          System.out.println("cannot find org.akuna.ui.tools.AkunaToolsPlugin");
        }
      }
    } catch (Throwable e) {
      throw new RuntimeException("Failed to retrieve files from classpath.", e);
    }
    System.out.println(files);
    return files;
  }
  
  private static void getBundleClasspathFiles(ClassLoader classLoader, List<File>files) throws Exception {
    // Let's use reflection so we don't have to import Eclipse classes
    // First get the files from the current bundle's classpath
    Object classPathManager = classLoader.getClass().getMethod("getClasspathManager", new Class[]{}).invoke(classLoader, new Object[]{});
    Class classPathManagerClass = Class.forName("org.eclipse.osgi.baseadaptor.loader.ClasspathManager");
    Object[] entries = (Object[])classPathManagerClass.getMethod("getHostClasspathEntries", new Class[]{}).invoke(classPathManager, new Object[]{});

    Class classPathEntryClass = Class.forName("org.eclipse.osgi.baseadaptor.loader.ClasspathEntry");
    Class bundleFileClass = Class.forName("org.eclipse.osgi.baseadaptor.bundlefile.BundleFile");

    for(Object entry : entries) {
      Object bundleFile = classPathEntryClass.getMethod("getBundleFile", new Class[]{}).invoke(entry, new Object[]{});
      File file = (File)bundleFileClass.getMethod("getBaseFile", new Class[]{}).invoke(bundleFile, new Object[]{});
      if(!files.contains(file)) {
        files.add(file);
      }
    }
  }

  /**
   * @param clazz - any class on the same class path as the jar file
   * you are looking for
   * @param jarFileName
   * @return
   */
  @SuppressWarnings("rawtypes")
  public static File getJarFileFromClassPath(Class clazz, String jarFileName) {
    File file = null;
    
    try {
      List<File> files = getFilesOnClassPath(clazz);
      for (File f : files) {
        if(f.getName().equals(jarFileName)) {
          file = f;
          break;
        }
      }
    } catch (Throwable e) {
    	e.printStackTrace();
    }

    return file;
  }

}

