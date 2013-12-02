package gitClient;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

public class GitClient implements IGitClient{
	
	private String remoteRepositoryURI;
	private String localRepositoryPath;
	private Git localGit;
	private UsernamePasswordCredentialsProvider credentialsProvider;
	private String authorName;
	private String authorEmail;
	private String committerName;
	private String committerEmail;
	private FileFilter directoryFilter;
	private FileFilter fileOnlyFilter;
	private FileFilter fileAndDirectoryFilter;
	private List<String> branches;
	
	public GitClient(String remoteRepositoryURI, List<String> branches, String localRepositoryPath, 
			String user, String password, String authorName, String authorEmail, String committerName, String committerEmail) {
		this.credentialsProvider = new UsernamePasswordCredentialsProvider(user, password);
		this.remoteRepositoryURI = remoteRepositoryURI;
		this.branches = branches;
		this.localRepositoryPath = localRepositoryPath; 
		this.authorName = authorName;
		this.authorEmail = authorEmail;
		this.committerName = committerName;
		this.committerEmail = committerEmail;
		
		this.directoryFilter = new FileFilter() {
			public boolean accept(File file) {
				return file.isDirectory() && !file.getName().equals(".git");
			}
		};
		
		this.fileOnlyFilter = new FileFilter() {
			public boolean accept(File file) {
				return file.isFile();
			}
		};
		
		this.fileAndDirectoryFilter = new FileFilter() {
			public boolean accept(File file) {
				return true;
			}
		};
	}
	
	@Override
	public AddResult addFile(String sourceFilePath, String repositoryFilePath, String branch, String message) throws Exception {
		switchBranch(branch);
		localGit.fetch().call();
		localGit.pull().call();
		FileUtils.copyFile(new File(sourceFilePath), 
				new File(this.localRepositoryPath + File.separator + repositoryFilePath));
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		List<DiffEntry> diffEntries = localGit.diff().setOutputStream(outputStream).call();
		if(diffEntries.size() > 0) {
			localGit.add().addFilepattern(repositoryFilePath).call();
			localGit.commit().setAuthor(authorName, authorEmail).setCommitter(committerName, committerEmail).setMessage(message).call();
		    localGit.push().setCredentialsProvider(this.credentialsProvider).call();
		}
		AddResult addResult = new AddResult();
		addResult.setDiffEntries(diffEntries);
		addResult.setLines(outputStream.toString(Constants.CHARSET.toString()));
		return addResult;
	}

	@Override
	public File getFile(String repositoryFilePath, String branch) throws Exception {
		switchBranch(branch);
		localGit.fetch().call();
		localGit.pull().call();
		return new File(this.localRepositoryPath + File.separator + repositoryFilePath);
	}
	
	@Override
	public List<File> getDirectories(String repositoryFilePath, String branch) throws Exception {
		return getFilteredFiles(repositoryFilePath, branch, this.directoryFilter);
	}	

	@Override
	public List<File> getFilesOrDirectories(String repositoryFilePath,	String branch) throws Exception {
		return getFilteredFiles(repositoryFilePath, branch, this.fileAndDirectoryFilter);
	}
	
	@Override
	public List<File> getFiles(String repositoryFilePath, String branch) throws Exception {
		return getFilteredFiles(repositoryFilePath, branch, this.fileOnlyFilter);
	}
	
	@Override
	public List<String> getBranches() throws Exception {
		if(!isInitialized())
			initialize(branches.get(0));
		List<String> result = new LinkedList<String>();
		List<Ref> branches = this.localGit.branchList().call();
		for(Ref branchRef : branches) 
			result.add(branchRef.getName());
		return result;
	}
	
	private List<File> getFilteredFiles(String repositoryFilePath, String branch, FileFilter fileFilter) throws Exception {
		switchBranch(branch);
		localGit.fetch().call();
		localGit.pull().call();
		File file = new File(this.localRepositoryPath + File.separator + repositoryFilePath);
		if(file.exists() && file.isDirectory()) {
			return Arrays.asList(file.listFiles(fileFilter)); 
		}
		throw new Exception("The given repositoryFilePath does not exist or does not denote a directory");
	}

	private void switchBranch(String branch) throws InvalidRemoteException, TransportException, IOException, GitAPIException {
		if(!isInitialized())
			initialize(branch);
		createAndCheckout(branch);
	}

	private boolean isInitialized() {
		return this.localGit != null;
	}

	private void initialize(String branch) throws IOException, 
				InvalidRemoteException, TransportException, GitAPIException {
		boolean localRepositoryExists = true;
		try {
			this.localGit = Git.open(new File(this.localRepositoryPath));  
		} catch(IOException e) {
			localRepositoryExists = false;
		}
		
		if(!localRepositoryExists) {
			Git.cloneRepository()
			// set the branches to clone from remote to local repository
			.setBranchesToClone(branches)
			// set the initial branch to check out and where to place HEAD
			.setBranch("refs/heads/" + branch.toString())
			// provide the URI of the remote repository from where to clone
			.setURI(this.remoteRepositoryURI)
			// set local store location of the cloned repository
			.setDirectory(new File(this.localRepositoryPath))
			.call();
			
			this.localGit = Git.open(new File(this.localRepositoryPath));
			for(String aBranch : branches) {
				createAndCheckout(aBranch);
			}
		}
	}
	
	private void createAndCheckout(String branch) throws RefAlreadyExistsException, 
			RefNotFoundException, InvalidRefNameException, CheckoutConflictException, 
			GitAPIException, IOException {
		boolean branchExists = localGit.getRepository().getRef(branch) != null;
		if (!branchExists) {
		    localGit.branchCreate()
		        .setName(branch)
		        .setUpstreamMode(SetupUpstreamMode.TRACK)
		        .setStartPoint("origin/" + branch)
		        .call();
		}
		localGit.checkout().setName(branch).call();
	}

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		String user = "user";
		String password = "password";
		String authorName = "authorName";
		String authorEmail = "authorEmail@gmail.com";
		String committerName = "committerName";
		String committerEmail = "committerEmail@gmail.com";
		String repository = "https://github.com/rodenhausen/test.git";
		
		List<String> branches = new LinkedList<String>();
		branches.add("master");
		//branches.add("development");
		
		GitClient clientA = new GitClient(repository, branches,
				"local", user, password, authorName, authorEmail, committerName, committerEmail);
		AddResult addResult = clientA.addFile("pom.xml", "pom.xml", "master", "message2");
		System.out.println(addResult.toString());
		
		/*GitClient clientB = new GitClient(repository,
				"localB", user, password, authorName, authorEmail, committerName, committerEmail);		
		clientA.addFile("pom.xml", "pom.xml", Branch.master.toString(), "1");
		clientB.addFile("clientB.txt", "clientB.txt", Branch.development.toString(), "clientB");
		clientA.addFile(".project", "test/test/test/project", Branch.development.toString(), "3");
		clientB.addFile("clientB.txt", "clientB.txt", Branch.master.toString(), "clientB");
		clientA.addFile(".classpath", "test/test/test/test/classpath", Branch.development.toString(), "2");
		clientA.addFile(".classpath", "test/classpath", Branch.development.toString(), "2");
		
		File file = clientA.getFile("test/test/test/project", Branch.development.toString());
		System.out.println(file.getAbsolutePath() + " " + file.exists());
		
		clientA.addFile("C://test//fnav19_posedsentences.txt", "fnav19_posedsentences.txt", Branch.master.toString(), "4");
		
		file = clientA.getFile("pom.xml", Branch.master.toString());
		System.out.println(file.getAbsolutePath() + " " + file.exists());
		
		file = clientA.getFile("test/classpath", Branch.development.toString());
		System.out.println(file.getAbsolutePath() + " " + file.exists());
		
		file = clientA.getFile("fnav19_posedsentences.txt", Branch.master.toString());
		System.out.println(file.getAbsolutePath() + " " + file.exists());
		
		file = clientA.getFile("clientB.txt", Branch.master.toString());
		System.out.println(file.getAbsolutePath() + " " + file.exists());
		
		file = clientA.getFile("clientB.txt", Branch.development.toString());
		System.out.println(file.getAbsolutePath() + " " + file.exists()); */
	}
	
}
