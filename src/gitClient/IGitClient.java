package gitClient;
import java.io.File;
import java.util.List;


public interface IGitClient {

	/**
	 * Copies file at sourceFilePath to repositoryFilePath in the respective branch
	 * File is pushed with the given message
	 * @param sourceFilePath
	 * @param repositoryFilePath
	 * @param branch
	 * @param message
	 * @return 
	 * @throws Exception
	 */
	public AddResult addFile(String sourceFilePath, String repositoryFilePath,
			String branch, String message) throws Exception;
	
	/**
	 * Return 
	 * @param filename
	 * @return the most current file at repositoryFilePath in the given branch
	 */
	public File getFile(String repositoryFilePath, String branch) throws Exception;

	/**
	 * @param repositoryFilePath
	 * @param branch
	 * @return
	 * @throws Exception 
	 */
	public List<File> getDirectories(String repositoryFilePath, String branch) throws Exception;
	
	/**
	 * @param repositoryFilePath
	 * @param branch
	 * @return
	 */
	public List<File> getFilesOrDirectories(String repositoryFilePath, String branch) throws Exception;
	
	/**
	 * @param repositoryFilePath
	 * @param branch
	 * @return
	 */
	public List<File> getFiles(String repositoryFilePath, String branch) throws Exception;
	
	/**
	 * @return
	 * @throws Exception
	 */
	public List<String> getBranches() throws Exception;
	
}
