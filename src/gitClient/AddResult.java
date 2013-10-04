package gitClient;

import java.util.List;

import org.eclipse.jgit.diff.DiffEntry;

public class AddResult {

	private String lines;
	private List<DiffEntry> diffEntries;
	
	public AddResult() {}
	
	public AddResult(String lines, List<DiffEntry> diffEntries) {
		this.lines = lines;
		this.diffEntries = diffEntries;
	}
	
	public String getLines() {
		return lines;
	}
	public void setLines(String lines) {
		this.lines = lines;
	}
	public List<DiffEntry> getDiffEntries() {
		return diffEntries;
	}
	public void setDiffEntries(List<DiffEntry> diffEntries) {
		this.diffEntries = diffEntries;
	}
	
	public String toString() {
		StringBuilder result = new StringBuilder();
		for(DiffEntry diffEntry : diffEntries)
			result.append(diffEntry.toString() + "\n");
		result.append(lines);
		return result.toString();
	}
}
