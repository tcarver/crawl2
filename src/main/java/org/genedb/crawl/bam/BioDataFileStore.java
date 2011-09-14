package org.genedb.crawl.bam;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.sf.samtools.SAMSequenceRecord;

import org.apache.log4j.Logger;
import org.genedb.crawl.model.Alignment;
import org.genedb.crawl.model.BioDataFile;
import org.genedb.crawl.model.MappedSAMSequence;
import org.genedb.crawl.model.Variant;
import org.springframework.util.StringUtils;

/**
 * A store of data files that facilitates querying by sequence, organism and fileID. 
 * 
 * @author gv1
 *
 * @param <T>
 */
public class BioDataFileStore <T extends BioDataFile> {
	
	private static Logger logger = Logger.getLogger(BioDataFileStore.class);
	
	private Integer fileID = 0;
	private List<T> files;
	private Map<String, String> sequences;
	
	/**
	 * Setup empty arrays if nothing passed.
	 */
	BioDataFileStore () {
		files = new ArrayList<T>();
		sequences = new HashMap<String, String>();
	}
	
	BioDataFileStore (List<T> files, Map<String, String> sequences) throws IOException {
		this.files = files;
		this.sequences = sequences;
		
		generateMetaFields();
		assignFileIDs();
		//getReaders();
	}
	
	
	public Map<String, String> getSequences() {
		return sequences;
	}
	
	
	void generateMetaFields() {
		
		Set<String> found = new HashSet<String>();
		Set<String> uniques = new HashSet<String>();
		
		for (BioDataFile file : files) {
			
			String[] metas = file.file.split("/");
			
			for (String meta : metas) {
				if (found.contains(meta)) {
					
					if (uniques.contains(meta)) {
						uniques.remove(meta);
					}
					
					continue;
				}
				
				found.add(meta);
				uniques.add(meta);
				
			}
			
		}
		
		for (BioDataFile file : files) {
			
			// do not bother if a meta has been supplied
			if (file.meta != null) {
				continue;
			}
			
			String[] metas = file.file.split("/");
			
			List<String> path_elements = new ArrayList<String>();
			
			for (String meta : metas) {
				if (uniques.contains(meta)) {
					path_elements.add(meta);
				}
			}
			
			file.meta = StringUtils.collectionToDelimitedString(path_elements, " > "); 
			
		}
		
	}

	
	void assignFileIDs() {
		for (BioDataFile file : files) {
			file.fileID = fileID++;
			logger.info(String.format("Assigning file %d (%s) :: %s", 
					file.fileID, file.getClass() , file.file));
		}
	}
	
//	void getReaders() throws IOException {
//		for (BioDataFile file : files) {
//			file.init();
//		}
//	}
	
	public T getFile(int fileID) {
		if (fileID < files.size()) {
			return files.get(fileID);
		}
		return null;
	}
	
	public List<T> getFiles() {
		return files;
	}
	
	public List<T> listfororganism(String organism) {
		
		List<T> list = new ArrayList<T>();
		
		for (T file : getFiles()) {
			
			if (file.organism.equals(organism)) {
				list.add(file);
				logger.info("added!");
			}
		}
		
		return list;
	}
	
	
	public String getActualSequenceName(int fileID, String sequenceName) throws Exception {
		
		for (MappedSAMSequence sequence : getSequences(fileID)) {
			String currentName = sequence.name;
			
			//logger.info(String.format("%s = %s", currentName, sequenceName));
			
			if (currentName.equals(sequenceName)) {
				return currentName;
			}
			
			if (sequences.containsKey(sequenceName)) {
				return sequences.get(sequenceName);			
			}
		}
		
		return null;
		
	}
	
	
	public String getAlignmentFromName(String sequenceName) {
		if (sequences.containsKey(sequenceName)) {
			return sequences.get(sequenceName);
		}
		return sequenceName;
	}
	
	public String getReferenceFromName(String sequenceName) {
		for (Entry<String, String> entry : sequences.entrySet()) {
			if (entry.getValue().equals(sequenceName)) {
				return entry.getKey();
			}
		}
		return sequenceName;
	}
	
	
	
	public List<MappedSAMSequence> getSequences(int fileID) throws IOException {
		BioDataFile file = getFile(fileID);
		
		List<MappedSAMSequence> sequences = new ArrayList<MappedSAMSequence>();
		
		if (file instanceof Alignment) {
			
			Alignment alignment = (Alignment) file;
			for (SAMSequenceRecord ssr : alignment.getReader().getFileHeader().getSequenceDictionary().getSequences()) {
				MappedSAMSequence mss = new MappedSAMSequence();
				mss.length = ssr.getSequenceLength();
				mss.name = ssr.getSequenceName();
				mss.index = ssr.getSequenceIndex();
				
				sequences.add(mss);
			}
			
		} else if (file instanceof Variant) {
			Variant variant = (Variant) file;
			
			for (String seqname : variant.getReader().getSeqNames()) {
				MappedSAMSequence mss = new MappedSAMSequence();
				mss.name = seqname;
				sequences.add(mss);
			}
		}
		
		return sequences;
	}
	
	public List<T> listforsequence(String sequence) throws Exception {
		
		
		Map<Integer, T> map = new HashMap<Integer, T>();
		
		for (T file : files) {
			
			Integer fileID = file.fileID;
			if (map.containsKey(fileID)) {
				continue;
			}
			
			String actualSequenceName = getActualSequenceName(fileID, sequence);
			if (actualSequenceName == null) {
				continue;
			}
			
			
			for (MappedSAMSequence fileSequence : getSequences(fileID)) {
				
				if (actualSequenceName.equals(fileSequence.name)) {
					map.put(fileID, file);
				}
				
			}
			
		}
		
		return new ArrayList<T>(map.values());
	}
	
}