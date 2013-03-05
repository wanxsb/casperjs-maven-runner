package jp.co.worksap.casperJsRunner.mojo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;

/**
 * the mojo class is used to running casperJs test code in maven environment
 * 
 * @goal test
 * 
 * @phase test
 *
 */
public class CasperJsRunnerMojo extends AbstractMojo {

	/**
	 * Base directory of project/
	 * 
	 * @parameter expression="${basedir}"
	 * @required
	 */
	private File baseDirectory;
	
	/**
	 * Directory containing the build files
	 * 
	 * @parameter expression="${project.build.directory}/casperjs"
	 */
	private File buildDirectory;
	
	/**
	 * Directory of JS test files.
	 * 
	 * @parameter
	 */
	private FileSet testFiles;
	
	/**
	 * Optional command to invoke phantomJs executable (can be space delimited
	 * commands).
	 * 
	 * eg. 'xvfb-run -a /usr/bin/phantomjs'
	 * 
	 * If not set, then defaults to assuming the 'phantomjs' executable is in
	 * system path.
	 * 
	 * @parameter expression="${phantomjs.exec}" default-value="phantomjs"
	 */
	private String phantomJsExec;
	
	/**
	 * @parameter expression="${casperjs.home}
	 * 
	 */
	private String casperJsHome;
	
	/**
	 * Boolean to fail build on failure
	 * 
	 * @parameter expression="${maven.test.failure.ignore}" default-value=false
	 */
	private boolean ignoreFailures;
	
	
	/**
	 * @parameter expression="${phantomjs.test.result.xml}" default-value="test-result.xml"
	 */	
	private String testResultXmlFile;
	
	
	private static FileSetManager fileSetManager = new FileSetManager();
	private static final String casperTesterXmlDirectoryName = "testerxml";
	
	public void execute() throws MojoExecutionException, MojoFailureException {
		printConfigInfo();
		int retCode = 0;
		copyBaseDir();
		for(String filename: fileSetManager.getIncludedFiles(testFiles)){
			getLog().info("Testing js file: "+filename);
			retCode +=runJsFileOnPhantom(filename);
		}
		
		if (!ignoreFailures) {
			if (retCode > 0) {
				throw new MojoFailureException("One or more casperJs tests failed");
			}
		}
	}
	
	private int runJsFileOnPhantom(String filename){
		int exitVal = 255;
		try{
		List<String> paramLists = new ArrayList<String>();
		paramLists.addAll(convertPhantomCmdLine());
		paramLists.add(buildDirectory.toString()+"/"+filename);
		paramLists.add(buildDirectory.toString()+"/"+testResultXmlFile);
		if(casperJsHome!=null){
			paramLists.add(casperJsHome);
		}
		
		getLog().info("casperJs running params:"+paramLists.toString());
		Process process = new ProcessBuilder(paramLists).start();
		exitVal = process.waitFor();
		captureOutput(filename);
		}catch(IOException e){
			getLog().error(e);
		} catch (InterruptedException e) {
			getLog().error(e);
		}		
		return exitVal;
	}
	
	private void captureOutput(String testFile) throws IOException {
		// Grab STDOUT of execution (this is the junit xml output generated
		// by the js), write to file
		BufferedReader input = new BufferedReader(new InputStreamReader(
				new FileInputStream(buildDirectory+"/"+testResultXmlFile)));
		File jUnitXmlOutputPath = new File(buildDirectory + "/"
				+ casperTesterXmlDirectoryName);

		jUnitXmlOutputPath.mkdir();
		File resultsFile = new File(jUnitXmlOutputPath, testFile + ".xml");

		// Write out the stdout from casperJs to the xml file.
		BufferedWriter output = new BufferedWriter(new FileWriter(resultsFile));
		String line = null;
		while ((line = input.readLine()) != null) {
			output.write(line);
		}
		output.close();
	}
	
	private List<String> convertPhantomCmdLine(){
		List<String> phantomParams = new ArrayList<String>();
		for(String param: phantomJsExec.split(" ")){
			phantomParams.add(param);
		}
		return phantomParams;
	}
	
	private void printConfigInfo(){
		getLog().debug("buildDirectory="+buildDirectory);
		if(testFiles!=null){
			String[] filenames = fileSetManager.getIncludedFiles(testFiles);
			getLog().info("testFiles="+createStringArrayLogString(filenames));
		}
		getLog().info("phantomJsExecOrCasperJsExec="+phantomJsExec);
		getLog().info("casperJsHome="+casperJsHome);
		getLog().info("ignoreFailures="+ignoreFailures);
		getLog().info("testResultXmlFile="+testResultXmlFile);
	}
	
	private String createStringArrayLogString(String[] fileNames) {
		String logString = new String();
		if (fileNames != null) {
			for (String libFile : fileNames) {
				logString = logString + libFile;
				logString = logString + ", ";
			}
		}
		return logString;
	}
	
	private InputStream getFileAsStream(String filename) {
		try {
			return new FileInputStream(new File(filename));
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
	
	private void copyBaseDir(){
		for(File file:baseDirectory.listFiles()){
			if(file.isDirectory()){
				copyDir(file.getName(), buildDirectory);
			}else if(file.isFile()){
				copyResourceToDirectory(file.getName(), buildDirectory);
			}
		}
	}
	
	private void copyDir(String dirname, File buildDirectory){
		File newDir = new File(buildDirectory.getPath()+"/"+dirname);
		newDir.mkdir();
	}
	
	private void copyResourceToDirectory(String filename, File buildDirectory) {
		try {
			FileUtils.copyInputStreamToFile(getFileAsStream(baseDirectory.getPath()+"/"+filename),
					new File(buildDirectory + "/" + filename));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
