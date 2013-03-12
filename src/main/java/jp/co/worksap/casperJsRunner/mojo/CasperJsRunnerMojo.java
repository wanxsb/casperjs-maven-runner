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
	private String phantomJsOrCasperJsExec;
	
	/**
	 * you can inject the casperJs home directory at this places
	 * @parameter expression="${jsParams}
	 * 
	 */
	private String jsParams;
	
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
		paramLists.addAll(convertPhantomCmdLine(phantomJsOrCasperJsExec));
		paramLists.add(buildDirectory.toString()+"/"+filename);
		if(jsParams!=null){
			paramLists.addAll(convertPhantomCmdLine(jsParams));
		}
		
		getLog().info("casperJs running params:"+paramLists.toString());
		Process process = new ProcessBuilder(paramLists).redirectErrorStream(true).start();
		captureStdOutput(process);
		exitVal = process.waitFor();
		captureOutput(filename);
		}catch(IOException e){
			getLog().error(e);
		} catch (InterruptedException e) {
			getLog().error(e);
		}		
		return exitVal;
	}
	
	private void captureStdOutput(Process pr) throws IOException {
		// Grab STDOUT of execution (this is the junit xml output generated
		// by the js), write to file
		BufferedReader input = new BufferedReader(new InputStreamReader(
				pr.getInputStream()));
		
		String line = null;
		while ((line = input.readLine()) != null) {
			System.out.println(line);
		}
	}
	
	private String getFileName(String filePath){
		if(filePath==null||filePath.length()==0){
			throw new IllegalArgumentException("invalid file name path:"+filePath);
		}
		File file = new File(filePath);
		
		return file.getName();
	}
	
	private void captureOutput(String testFile) throws IOException {
		// Grab STDOUT of execution (this is the junit xml output generated
		// by the js), write to file
		BufferedReader input = new BufferedReader(new InputStreamReader(
				new FileInputStream(buildDirectory+"/"+testResultXmlFile)));
		File jUnitXmlOutputPath = new File(buildDirectory + "/"
				+ casperTesterXmlDirectoryName);

		jUnitXmlOutputPath.mkdir();
		String filename = getFileName(testFile+".xml");
		File resultsFile = new File(jUnitXmlOutputPath, filename);
		getLog().info("start to create output result file:"+resultsFile.getPath());

		// Write out the stdout from casperJs to the xml file.
		BufferedWriter output = new BufferedWriter(new FileWriter(resultsFile));
		String line = null;
		while ((line = input.readLine()) != null) {
			output.write(line);
		}
		output.close();
	}
	
	private List<String> convertPhantomCmdLine(String cmd){
		List<String> phantomParams = new ArrayList<String>();
		for(String param: cmd.split(" ")){
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
		getLog().info("phantomJsExecOrCasperJsExec="+phantomJsOrCasperJsExec);
		getLog().info("jsParams="+jsParams);
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
	
	private String getRelativeFileName(String baseDir, String filePath){
		if(filePath.length()<baseDir.length()){
			throw new IllegalArgumentException("file path should be longer than baseDir");
		}
		
		if(!filePath.substring(0, baseDir.length()).equals(baseDir)){
			throw new IllegalArgumentException("file path should file under the baseDir");
		}
		
		String rightPart = filePath.substring(baseDir.length());
		while(rightPart!=null&& rightPart.length()>0 && rightPart.charAt(0) == '/'){
			rightPart = rightPart.substring(1);
		}
		return rightPart;
	}
	
	private void copyBaseDir(){
		copyDirRecursively(baseDirectory, baseDirectory);
	}
	
	private void copyDirRecursively(File baseDir, File dir){
		for(File file:dir.listFiles()){
			String filename = getRelativeFileName(baseDir.getPath(), file.getPath());
			if(file.isDirectory()){
				copyDir(filename, buildDirectory);
				copyDirRecursively(baseDir, file);
			}else if(file.isFile()){
				copyResourceToDirectory(filename, buildDirectory);
			}else{
				throw new IllegalStateException("unknown state of file " + file.getPath());
			}
		}
	}
	
	private void copyDir(String dirname, File buildDirectory){
		getLog().info("copy directory "+dirname+" to target directory " + buildDirectory.getPath()+"/"+dirname);
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
