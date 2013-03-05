package jp.co.worksap.casperJsRunner.mojo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

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
	private String phantomJsExecOrCasperJsExec;
	
	/**
	 * Boolean to fail build on failure
	 * 
	 * @parameter expression="${maven.test.failure.ignore}" default-value=false
	 */
	private boolean ignoreFailures;
	
	
	private static FileSetManager fileSetManager = new FileSetManager();
	
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		getLog().debug("start");
		printConfigInfo();
		copyBaseDir();
		for(String filename: fileSetManager.getIncludedFiles(testFiles)){
			getLog().debug("filename="+filename);
		}
	}
	
	private void printConfigInfo(){
		getLog().debug("buildDirectory="+buildDirectory);
		if(testFiles!=null){
			String[] filenames = fileSetManager.getIncludedFiles(testFiles);
			getLog().info("testFiles="+createStringArrayLogString(filenames));
		}
		getLog().info("phantomJsExecOrCasperJsExec="+phantomJsExecOrCasperJsExec);
		getLog().info("ignoreFailures="+ignoreFailures);
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
