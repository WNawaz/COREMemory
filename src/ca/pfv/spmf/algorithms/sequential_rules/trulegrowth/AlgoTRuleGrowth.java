package ca.pfv.spmf.algorithms.sequential_rules.trulegrowth;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import ca.pfv.spmf.input.sequence_database_list_integers.Sequence;
import ca.pfv.spmf.input.sequence_database_list_integers.SequenceDatabase;
import ca.pfv.spmf.tools.MemoryLogger;

/**
 * Implementation of the TRULEGROWTH algorithm.
 * Written by Philippe Fournier-Viger
 * 
 * For more information about this algorithm, you can read the paper
 * published at Canadian AI 2012 describing this algorithm.
 *
 * Copyright (c) 2008-2012 Philippe Fournier-Viger
 * 
 * This file is part of the SPMF DATA MINING SOFTWARE
 * (http://www.philippe-fournier-viger.com/spmf).
 *
 * SPMF is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SPMF is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SPMF.  If not, see <http://www.gnu.org/licenses/>.
 */
public class AlgoTRuleGrowth {
	
	// *** for statistics ***
	long timeStart = 0; // start time of latest execution
	long timeEnd = 0;  // end time of latest execution
	
	
	//*** internal variables ***/
	// A map to record the occurences of each item in each sequence
	// KEY: an item
	// VALUE:  a map of  key: sequence ID  value: occurences of the item in that sequence.
	// (note: an occurence is an itemset position)
	Map<Integer,  Map<Integer, Occurence>> mapItemCount;
	
	 // minimum support which will be raised dynamically
	int minsuppRelative; 
	
	// The number of patterns found
	int ruleCount;
	
	// object to write the output file
	BufferedWriter writer = null; 
	
	
	// *** parameters ***
	SequenceDatabase database; // a sequence database
	double minconf;   // minimum confidence
	int windowSize =0;  // window size


	/**
	 * Default constructor
	 */
	public AlgoTRuleGrowth() {
	}
	
	/**
	 * Run the algorithm.  
	 * @param minSupport  Minsup as a percentage (ex: 0.05 = 5 %)
	 * @param minConfidence minimum confidence (a value between 0 and 1).
	 * @param input  the input file path
	 * @param output the output file path
	 * @param windowSize a window size
	 * @throws IOException exception if there is an error reading/writing files
	 */
	public void runAlgorithm(double minSupport, double minConfidence, String input, String output, int windowSize ) throws IOException{
		
		// load the input file into memory
		try {
			this.database = new SequenceDatabase();
			database.loadFile(input);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// convert minimum support to an absolute minimum support (integer)
		this.minsuppRelative = (int) Math.ceil(minSupport * database.size());
		// run the algorithm
		runAlgorithm(input, output, minsuppRelative, minConfidence, windowSize);
	}
	
	/**
	 * Run the algorithm.
	 * @param minSupport  Minsup as a a relative value (integer)
	 * @param minConfidence minimum confidence (a value between 0 and 1).
	 * @param input  the input file path
	 * @param output the output file path
	 * @param windowSize a window size
	 * @throws IOException exception if there is an error reading/writing files
	 */
	public void runAlgorithm(String input, String output, int relativeMinSupport, double minConfidence, int windowSize 
			) throws IOException{
		// save the minconf parameter
		this.minconf = minConfidence;
		
		// read the database into memory
		if(database == null){
			try {
				this.database = new SequenceDatabase();
				database.loadFile(input);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		// We add 1 to the window size to that it follows
		// the same definition as in the published article.
		this.windowSize = windowSize + 1; 
		
		// if minsup is 0, set it to 1 to avoid generating
		// rules not in the database
		this.minsuppRelative = relativeMinSupport;
		if(this.minsuppRelative == 0){ // protection
			this.minsuppRelative = 1;
		}

		// reset the stats for memory usage
		MemoryLogger.getInstance().reset();
		
		// prepare the object for writing the output file
		writer = new BufferedWriter(new FileWriter(output)); 

		// save the start time
		timeStart = System.currentTimeMillis(); // for stats
		
		// Remove infrequent items from the database in one database scan.
		// Then perform another database scan to count the
		// the support of each item in the same database scan 
		// and their occurrences.
		removeItemsThatAreNotFrequent(database);	
		
		// Put frequent items in a list.
		List<Integer> listFrequents = new ArrayList<Integer>();
		// for each item
		for(Entry<Integer,Map<Integer, Occurence>> entry : mapItemCount.entrySet()){
			// if it is frequent
			if(entry.getValue().size() >= minsuppRelative){
				// add it to the list
				listFrequents.add(entry.getKey());
			}
		}
		
		// We will now try to generate rules with one item in the
		// antecedent and one item in the consequent using
		// the frequent items.

		// For each pair of frequent items i  and j such that i != j
		for(int i=0; i< listFrequents.size(); i++){
			// get the item i and its map of occurences
			Integer intI = listFrequents.get(i);
			Map<Integer,Occurence> occurencesI = mapItemCount.get(intI);
			
			for(int j=i+1; j< listFrequents.size(); j++){
				// get the item j and its map of occurences
				Integer intJ = listFrequents.get(j);
				Map<Integer,Occurence> occurencesJ = mapItemCount.get(intJ);
				
				// (1) We will now calculate the tidsets
				// of itemset I,  itemset J,   the rule I -->J 
				// and the rule J-->I
				Set<Integer> tidsI = new HashSet<Integer>();
				Set<Integer> tidsJ = null;
				Set<Integer> tidsIJ = new HashSet<Integer>();
				Set<Integer> tidsJI= new HashSet<Integer>();

				// for each occurence of I
	looptid:	for(Occurence occI : occurencesI.values()){
					
					// add the sequenceID of that occurence to tidsI
					tidsI.add(occI.sequenceID);
					
					// if J does not appear in that sequence continue loop
					Occurence occJ = occurencesJ.get(occI.sequenceID);
					if(occJ == null){
						continue looptid;
					}
					
					// make a big loop to compare if I appears before
					// J in that sequence and
					// if J appears before I
					boolean addedIJ= false;
					boolean addedJI= false;
					// for each occurence of I in that sequence
			loopIJ:	for(Short posI : occI.occurences){
						// for each occurence of J in that sequence
						for(Short posJ : occJ.occurences){
							if(!posI.equals(posJ) && Math.abs(posI - posJ) <= windowSize){
								if(posI <= posJ){
									// if I is before J
									tidsIJ.add(occI.sequenceID);
									addedIJ = true;
								}else{
									// if J is before I
									tidsJI.add(occI.sequenceID);
									addedJI = true;
								}
								// if we have found that I is before J and J is before I
								// we don't need to continue.
								if(addedIJ && addedJI){
									break loopIJ;
								}
							}
						}
					}
				}
				// END
				
				// (2) check if I ==> J has enough common tids
				// If yes, we create the rule I ==> J
				if(tidsIJ.size() >= minsuppRelative){
					// calculate the confidence of I ==> J
					double confIJ = ((double)tidsIJ.size()) / occurencesI.size();
					
					// create itemset of the rule I ==> J
					int[] itemset1 = new int[]{intI};
					int[] itemset2 = new int[]{intJ};
					
					// if the confidence is high enough, save the rule
					if(confIJ >= minConfidence){
						saveRule(tidsIJ, confIJ, itemset1, itemset2);
					}
					// Calculate tidsJ.
					tidsJ = new HashSet<Integer>();
					for(Occurence occJ : occurencesJ.values()){
						tidsJ.add(occJ.sequenceID);
					}
					
					// recursive call to try to expand the rule on the left and
					// right sides
					expandLeft(itemset1, itemset2, tidsI, tidsIJ);
					expandRight(itemset1, itemset2, tidsI, tidsJ, tidsIJ);
				}
					
				// check if J ==> I has enough common tids
				// If yes, we create the rule J ==> I
				if(tidsJI.size() >= minsuppRelative){
						double confJI = ((double)tidsJI.size()) / occurencesJ.size();
//						
						// create itemsets for that rule
						int[] itemset1 = new int[]{intI};
						int[] itemset2 = new int[]{intJ};
						
						// if the rule has enough confidence, save it!
						if(confJI >= minConfidence){
							saveRule(tidsJI, confJI, itemset2, itemset1);
//							rules.addRule(ruleJI);
						}
						
						// Calculate tidsJ.
						if(tidsJ == null){
							tidsJ = new HashSet<Integer>();
							for(Occurence occJ : occurencesJ.values()){
								tidsJ.add(occJ.sequenceID);
							}
						}
						// recursive call to try to expand the rule
						expandRight(itemset2, itemset1, tidsJ,  tidsI, tidsJI /*, occurencesJ, occurencesI*/);
						expandLeft(itemset2, itemset1, tidsJ, tidsJI /*, occurencesI*/);
					}
				}
		}
		// save the end time for the execution of the algorithm
		timeEnd = System.currentTimeMillis(); // for stats
		
		// close the file
		writer.close();
		database = null;
	}

	/**
	 * This method search for items for expanding left side of a rule I --> J 
	 * with any item c. This results in rules of the form I U{c} --> J. The method makes sure that:
	 *   - c  is not already included in I or J
	 *   - c appear at least minsup time in tidsIJ before last occurence of J
	 *   - c is lexically bigger than all items in I
	 * @param itemsetI the left side of a rule (see paper)
	 * @param itemestJ the right side of a rule (see paper)
	 * @param tidsI the tids set of I
	 * @param tidsJ the tids set of J
	 * @throws IOException  exception if error while writing output file
	 */
    private void expandLeft(int[] itemsetI, int[] itemsetJ,
    						Collection<Integer> tidsI, 
    						Collection<Integer> tidsIJ // ,
//    						Map<Integer, Occurence> mapOccurencesJ
    						) throws IOException {    	
    	
    	// The following map will be used to count the support of each item
    	// c that could potentially extend the rule.
    	// The map associated a set of tids (value) to an item (key).
    	Map<Integer, Set<Integer>> frequentItemsC  = new HashMap<Integer, Set<Integer>>();  

    	// We scan the sequence where I-->J appear to search for items c 
    	// that we could add to generate a larger rule  IU{c} --> J
    	
    	// For each tid of  sequence containing I-->J
    	for(Integer tid : tidsIJ){
    		Sequence sequence = database.getSequences().get(tid);
    		
    		//  there maps are used when scanning the sequence to determine
    		// what is currently inside the window and what fall out of the window.
    		// We use linkedhashMap to as to keep the order of what is inserted into the maps
    		// There maps are key: item      value: position of an itemset
    		LinkedHashMap<Integer, Integer> mapMostLeftFromI = new LinkedHashMap<Integer, Integer>();
    		LinkedHashMap<Integer, Integer> mapMostLeftFromJ = new LinkedHashMap<Integer, Integer>();
    		//  key: item   value:  list of positions of itemsets
    		LinkedHashMap<Integer, LinkedList<Integer>> mapMostRightFromJ = new LinkedHashMap<Integer, LinkedList<Integer>>();

    		//
        	int lastItemsetScannedForC = Integer.MAX_VALUE;
        	
    		// For each itemset starting from the last in this sequence
        	int k= sequence.size()-1;
        	do{
    			final int firstElementOfWindow = k;    //  - windowSize +1
    			final int lastElementOfWindow = k + windowSize -1; 
    			
    			// remove items from J that fall outside the time window
    			int previousJSize = mapMostLeftFromJ.size();
    			removeElementOutsideWindow(mapMostLeftFromJ, lastElementOfWindow);
    			// important: if J was all there, but become smaller we need to clear the
    			// hashmap for items of I.
    			int currentJSize = mapMostLeftFromJ.size();
    			if(previousJSize == itemsetJ.length && previousJSize != currentJSize){
    				mapMostLeftFromI.clear();
    			}
				
    			// remove items from I that fall outside the time window
    			removeElementOutsideWindow(mapMostLeftFromI, lastElementOfWindow);
    			
    			// For each item of the current itemset
    			for(Integer item : sequence.get(k)){
    				// record the first position until now of each item in I or J
    				
    				// if we saw J completely already,  and the current item is in I
    				if(mapMostLeftFromJ.size() == itemsetJ.length  && contains(itemsetI, item)){ 
    					// then we add its position to the map for items from I
    					addToLinked(mapMostLeftFromI, item, k);
    				// otherwise, if it is an item from J
    				}else if(contains(itemsetJ, item)){ 
    					// add its position to  the map of positions for J
    					addToLinked(mapMostLeftFromJ, item, k);
    					LinkedList<Integer> list = mapMostRightFromJ.get(item);
    					if(list == null){
    						list = new LinkedList<Integer>();
    						addToLinked(mapMostRightFromJ, item, list);
    					}
    					list.add(k);
    				}
    			}
    			
    			// if all the items of I ==> J are in the current window
    			if(mapMostLeftFromI.size() == itemsetI.length && mapMostLeftFromJ.size() == itemsetJ.length){
    				
    				//remove items from mostRight that fall outside the time window.
        			// at the same time, calculate the minimum index for items of J.
        			int minimum = Integer.MAX_VALUE;
        			
        			// for each position in the map of positions of J
        			for(LinkedList<Integer> list: mapMostRightFromJ.values()){
        				while(true){
        					// get the last position
        					Integer last = list.getLast();
        					// if it is oustide the window remove it
        					if(last > lastElementOfWindow){
        						list.removeLast();
        					}else{ 
        						// ottherwise update the minimum  and break
        						if(last < minimum){
                					minimum = last -1;
                				}
        						break;
    						}
    					}
    				}
    				
	    			// we need to scan for items C to extend the rule...	
	    		    // Such item c has to appear in the window before the last occurence of J (before "minimum")
	    		    // and if it was scanned before, it should not be scanned again.
    				int itemsetC = minimum;
    				if(itemsetC >= lastItemsetScannedForC){
    					itemsetC = lastItemsetScannedForC -1;
    				}
    				
    				// for each item c after the first element of the window, starting from the
    				// last itemset just before the last occurence of J
    				for(; itemsetC >= firstElementOfWindow; itemsetC--){
    					for(Integer itemC : sequence.get(itemsetC)){
//    						if lexical order is not respected or c is included in the rule already.			
							if(containsLEXPlus(itemsetI, itemC)  
						   || containsLEX(itemsetJ, itemC)){
								continue;  // skip it
							}	
							// otherwise, get the tidset of "c"
							Set<Integer> tidsItemC = frequentItemsC.get(itemC);
							// if there is no tidset, create one
							if(tidsItemC == null){
								tidsItemC = new HashSet<Integer>();
								frequentItemsC.put(itemC, tidsItemC);
							}
							// add the tid to the tidset of c
							tidsItemC.add(tid);	
    					}
    				}
    				// update the last item scanned
    				lastItemsetScannedForC = firstElementOfWindow;
    			}
    			
    			k--;  // go to previous itemset in the sequence (we scan the sequence bacward)
        	}while(k >= 0  && lastItemsetScannedForC >0);
 		}

    	

    	// For each item c found, we create a rule	IU{c} ==> J
    	for(Entry<Integer, Set<Integer>> entry : frequentItemsC.entrySet()){
    		Set<Integer> tidsIC_J = entry.getValue();

    		// if the support of IU{c} ==> J is enough 
    		if(tidsIC_J.size() >= minsuppRelative){ 
        		Integer itemC = entry.getKey();
    			int [] itemsetIC = new int[itemsetI.length+1];
				System.arraycopy(itemsetI, 0, itemsetIC, 0, itemsetI.length);
				itemsetIC[itemsetI.length] = itemC;
				
				// Calculate tids containing IU{c} within the time window which is necessary
    			// to calculate the confidence
				
    			Set<Integer> tidsIC = new HashSet<Integer>();
    			// for each sequence containing I 
   loop1:	    for(Integer tid: tidsI){
	   				// get the sequence
    	    		Sequence sequence = database.getSequences().get(tid);
    	    		// To check if IU{c} is contained in that sequence we will use a map
    	    		// such that the key : item    value: position of an itemset
    	    		LinkedHashMap<Integer, Integer> mapAlreadySeenFromIC = new LinkedHashMap<Integer, Integer>();
    	    		
    	    		// For each itemset
    	    		for(int k=0; k< sequence.size(); k++){
    					// For each item
    	    			for(Integer item : sequence.get(k)){
    	    				// record the last position of each item in IU{C}
    	    				if(contains(itemsetIC, item)){ 
    	    					addToLinked(mapAlreadySeenFromIC, item, k);
    	    				}
    	    			}
    	    			// as we are moving through the sequence, 
    	    			// remove items that fall outside the time window
    	    			Iterator<Entry<Integer, Integer>> iter = mapAlreadySeenFromIC.entrySet().iterator();
    	    			while(iter.hasNext()){
    	    				// if falling outside of the window
    	    				Entry<Integer, Integer> entryMap = iter.next();
    	    				if(entryMap.getValue() < k - windowSize +1){
    	    					// remove the item
    	    					iter.remove();
    	    				}else{
    	    					// otherwise break
    	    					break;
    	    				}
    	    			}
    	    			// if all the items of I are inside the current window, then record the tid
    	    			if(mapAlreadySeenFromIC.keySet().size() == itemsetIC.length){
    	    				tidsIC.add(tid);
    	    				continue loop1;
    	    			}
    	    		}
    	    	}
    			// ----  ----
//    			
//    			if(itemC == 6){
//    				System.out.println();
//    			}

    			// Create rule and calculate its confidence of IU{c} ==> J 
    	    	// defined as:  sup(IU{c} -->J) /  sup(IU{c})						
				double confIC_J = ((double)tidsIC_J.size()) / tidsIC.size();

				// if the confidence is high enough, then it is a valid rule
				if(confIC_J >= minconf){
					// save the rule
					saveRule(tidsIC_J, confIC_J, itemsetIC, itemsetJ);
				}
				// recursive call to expand left side of the rule
				expandLeft(itemsetIC, itemsetJ, tidsIC, tidsIC_J );
    		}
    	}
    	// check the memory usage
    	MemoryLogger.getInstance().checkMemory();
	}

    /**
     * This method insert a key and a value in a hashmap while making sure that
     * the insertion order is preserved. It was necessary to do that because
     * when an element is re-inserted in a linked list, the access order remain 
     * the one of the first insertion. 
     * @param mapMostLeftFromI   the map
     * @param key     a key
     * @param value   a value
     */
	private void addToLinked(LinkedHashMap mapMostLeftFromI,
			Object key, Object value) {
		// if the map contain the key already
		if(mapMostLeftFromI.containsKey(key)){
			// remove it
			mapMostLeftFromI.remove(key);
		}
		// then put it
		mapMostLeftFromI.put(key, value);
	}

	/**
	 * This method removes elements out of the current window from a  hashmap containing
	 * the position of items at the left of an itemset.
	 *  key: item   value : a itemset position
	 * @param mapMostLeftFromItemset  the map
	 * @param lastElementOfWindow  the last itemset of the window in terms of itemset position
	 *                             in the sequence.
	 */
	private void removeElementOutsideWindow(
			LinkedHashMap<Integer, Integer> mapMostLeftFromItemset,
			final int lastElementOfWindow) {
		// iterate over elements of the map
		Iterator<Entry<Integer, Integer>> iter = mapMostLeftFromItemset.entrySet().iterator();
		while(iter.hasNext()){
			// if the position is outside the window, remove it
			if(iter.next().getValue() > lastElementOfWindow){
				iter.remove();
			}else{
				// otherwise, we break
				break;
			}
		}
	}
	
	/**
	 * This method removes elements out of the current window from a  hashmap containing
	 * the position of items at the right of an itemset.
	 *  key: item   value : a itemset position
	 * @param mapMostLeftFromItemset  the map
	 * @param lastElementOfWindow  the last itemset of the window in terms of itemset position
	 *                             in the sequence.
	 */
	private void removeElementOutsideWindowER(
			LinkedHashMap<Integer, Integer> mapMostRightfromI,
			final int firstElementOfWindow) {
		// iterate over elements of the map
		Iterator<Entry<Integer, Integer>> iter = mapMostRightfromI.entrySet().iterator();
		while(iter.hasNext()){
			Entry<Integer, Integer> entry = iter.next();
			// if the position is outside the window, remove it
			if(entry.getValue() < firstElementOfWindow){
				iter.remove();
			}else{
				// otherwise, we break
				break;
			}
		}
	}
    
	/**
	 * This method search for items for expanding left side of a rule I --> J 
	 * with any item c. This results in rules of the form I --> J U{c}. The method makes sure that:
	 *   - c  is not already included in I or J
	 *   - c appear at least minsup time in tidsIJ after the first occurence of I
	 *   - c is lexically bigger than all items in J
	 * @param mapWindowsJI 
	 * @throws IOException 
	 */
    private void expandRight(int[] itemsetI, int[] itemsetJ, 
							Set<Integer> tidsI, 
    						Collection<Integer> tidsJ, 
    						Collection<Integer> tidsIJ //,
//    						Map<Integer, Occurence> occurencesI,
//    						Map<Integer, Occurence> occurencesJ
    						) throws IOException {

    	// The following map will be used to count the support of each item
    	// c that could potentially extend the rule.
    	// The map associated a set of tids (value) to an item (key).
    	Map<Integer, Set<Integer>> frequentItemsC  = new HashMap<Integer, Set<Integer>>();  
    	
    	// For each tid of sequence containing I-->J
    	 for(Integer tid : tidsIJ){
    		// get the sequence
    		Sequence sequence = database.getSequences().get(tid);
    		
    		//  there maps are used when scanning the sequence to determine
    		// what is currently inside the window and what fall out of the window.
    		// We use linkedhashMap so as to keep the order of what is inserted into the maps
    		// There maps are key: item      value: position of an itemset
    		LinkedHashMap<Integer, Integer> mapMostRightFromI = new LinkedHashMap<Integer, Integer>();
    		LinkedHashMap<Integer, Integer> mapMostRightFromJ = new LinkedHashMap<Integer, Integer>();
    		//  key: item   value:  list of positions of itemsets
    		LinkedHashMap<Integer, LinkedList<Integer>> mapMostLeftFromI = new LinkedHashMap<Integer, LinkedList<Integer>>();

        	int lastItemsetScannedForC = Integer.MIN_VALUE;

    		// For each itemset starting from the last in this sequence
        	int k= 0;
        	do{
    			final int firstElementOfWindow = k - windowSize +1;   
    			int lastElementOfWindow = k; 
				
    			// remove items from I that fall outside the time window
    			int previousISize = mapMostRightFromI.size();
    			removeElementOutsideWindowER(mapMostRightFromI, firstElementOfWindow);
    			// important: if I was all there, but become smaller we need to clear the
    			// hashmap for items of J.
    			int currentISize = mapMostRightFromI.size();
    			if(previousISize == itemsetJ.length && previousISize != currentISize){
    				mapMostRightFromJ.clear();
    			}

    			// remove items from J that fall outside the time window
    			removeElementOutsideWindowER(mapMostRightFromJ, firstElementOfWindow);
    			
    			// For each item of the current itemset
    			for(Integer item : sequence.get(k)){
    				// record the first position until now of each item in I or J
    				
    				// if we saw I completely already,  and the current item is in J
    				if(mapMostRightFromI.size() == itemsetI.length && 	contains(itemsetJ, item)){
    					// then we add its position to the map for items most right from J
    					addToLinked(mapMostRightFromJ, item, k);
        				// otherwise, if it is an item from I
    				}else if(contains(itemsetI, item)){ 
    					// add its position to  the map of positions most left for I
    					addToLinked(mapMostRightFromI, item, k);
    					LinkedList<Integer> list = mapMostLeftFromI.get(item);
    					if(list == null){
    						list = new LinkedList<Integer>();
    						addToLinked(mapMostLeftFromI, item, list);
    					}
    					list.add(k);
    				}
    			}
 
    			// if all the items of IJ are in the current window
    			if(mapMostRightFromI.size() == itemsetI.length && mapMostRightFromJ.size() == itemsetJ.length){
    				
    				//remove items from mostLeft that fall outside the time window.
        			// at the same time, calculate the minimum index for items of I.
        			int minimum = 1;
        			
        			// for each position from I in mostLeft
        			for(LinkedList<Integer> list: mapMostLeftFromI.values()){
        				while(true){
        					// get the last position
        					Integer last = list.getLast();
        					// if outside the window
        					if(last < firstElementOfWindow){
        						// remove the position
        						list.removeLast();
        					}else{ 
        						// otherwise update the minimum
        						if(last > minimum){
                					minimum = last + 1;  
                				}
        						// then break 
        						break;
    						}
    					}
    				}
    				
	    			// we need to scan for items C to extend the rule...	
	    		    // Such item c has to appear in the window before the last occurence of J (before "minimum")
	    		    // and if it was scanned before, it should not be scanned again.
    				int itemsetC = minimum;
    				if(itemsetC < lastItemsetScannedForC){
    					itemsetC = lastItemsetScannedForC +1;
    				}
    				
    				// for each item c before the lst element of the window, starting from the
    				// first itemset just after the first occurence of I
    				for(; itemsetC <= lastElementOfWindow; itemsetC++){
    					for(Integer itemC : sequence.get(itemsetC)){
    						// We will consider if we could create a rule I --> J U{c}
    						// If lexical order is not respected or c is included in the rule already,
    						// then we cannot so the algorithm return.
    						if(containsLEX(itemsetI, itemC) 
						   ||  containsLEXPlus(itemsetJ, itemC)){
								continue;
							}	
							// otherwise, get the tidset of "c"
							Set<Integer> tidsItemC = frequentItemsC.get(itemC);
							// if there is no tidset, create one
							if(tidsItemC == null){
								//if we did not see "c" yet, create a new tidset for "c"
								tidsItemC = new HashSet<Integer>();
								frequentItemsC.put(itemC, tidsItemC);
							}
							// add the current tid to the tidset of "c"
							tidsItemC.add(tid);	
    					}
    				}
    				// update last itemset scanned
    				lastItemsetScannedForC = lastElementOfWindow;
    			}
    			k++;  // to go next itemset
        	}while(k < sequence.size() && lastItemsetScannedForC < sequence.size()-1);
 		}  	
    	 
    	// For each item c found, we create a rule	I ==> JU {c} 	
     	for(Entry<Integer, Set<Integer>> entry : frequentItemsC.entrySet()){
    		// get the tidset of I ==> JU {c}
     		Set<Integer> tidsI_JC = entry.getValue();

    		// if the support of I ==> JU{c} is enough 
     		if(tidsI_JC.size() >= minsuppRelative){ 
         		Integer itemC = entry.getKey();

    			// create the itemset JU{c}
         		int[] itemsetJC = new int[itemsetJ.length+1];
				System.arraycopy(itemsetJ, 0, itemsetJC, 0, itemsetJ.length);
				itemsetJC[itemsetJ.length]= itemC;
 				
     			//  calculate the occurences of JU{c} within the time window
     			Set<Integer> tidsJC = new HashSet<Integer>();
     			// for each sequence containing J
    loop1:	    for(Integer tid: tidsJ){
    				// get the sequence
     	    		Sequence sequence = database.getSequences().get(tid);
     	    		
     	    		// To check if JU{c} is contained in that sequence we will use a map
    	    		// such that the key : item    value: position of an itemset
     	    		LinkedHashMap<Integer, Integer> mapAlreadySeenFromJC = new LinkedHashMap<Integer, Integer>();
     	    		
     	    		// For each itemset
     	    		for(int k=0; k< sequence.size(); k++){
     					// For each item
     	    			for(Integer item : sequence.get(k)){
     	    				// if the item is in JU{C}, then
     	    				 // record the last position of the item for the map of JU{C}
     	    				if(contains(itemsetJC, item)){
     	    					addToLinked(mapAlreadySeenFromJC, item, k);
     	    				}
     	    			}
     	    			// remove items that fall outside the time window
     	    			
     	    			// For each position in the map
     	    			Iterator<Entry<Integer, Integer>> iter = mapAlreadySeenFromJC.entrySet().iterator();
     	    			while(iter.hasNext()){
     	    				// if position is outside the map
     	    				Entry<Integer, Integer> entryMap = iter.next();
     	    				if(entryMap.getValue() < k - windowSize +1){
     	    					// remove the position
     	    					iter.remove();
     	    				}else{
     	    					// otherwise break
     	    					break;
     	    				}
     	    			}
     	    			// if all the items of I are inside the current window, then record the tid
     	    			if(mapAlreadySeenFromJC.keySet().size() == itemsetJC.length){
     	    				tidsJC.add(tid);
     	    				// then continue the loop!
     	    				continue loop1; 
     	    			}
     	    		}
     	    	}
     			// ----  ----
     			

    			// Create rule and calculate its confidence of I ==> J U{c}
    	    	// defined as:  sup(I -->J U{c}) /  sup(I)	
     			double confI_JC = ((double)tidsI_JC.size()) / tidsI.size();
//				Rule ruleI_JC = new Rule(ruleIJ.getItemset1(), itemsetJC, confI_JC, tidsI_JC.size());
				
				// if the confidence is enough
				if(confI_JC >= minconf){
					// then it is a valid rule so save it
					saveRule(tidsI_JC, confI_JC, itemsetI, itemsetJC);
				}

				// recursively try to expand the left and right side
				// of the rule
				expandRight(itemsetI, itemsetJC, tidsI, tidsJC, tidsI_JC);  
				expandLeft(itemsetI, itemsetJC, tidsI, tidsI_JC);  // occurencesJ
     		}
     	}
    	// check the memory usage
     	MemoryLogger.getInstance().checkMemory();
	}


	/**
	 * This method calculate the frequency of each item in one database pass.
	 * Then it remove all items that are not frequent in another database pass.
	 * @param database : a sequence database 
	 * @return A map such that key = item
	 *                         value = a map  where a key = tid  and a value = Occurence
	 * This map allows knowing the frequency of each item and their first and last occurence in each sequence.
	 */
	private Map<Integer, Map<Integer, Occurence>> removeItemsThatAreNotFrequent(SequenceDatabase database) {
		// (1) Count the support of each item in the database in one database pass
		mapItemCount = new HashMap<Integer, Map<Integer, Occurence>>(); // <item, Map<tid, occurence>>
		
		// for each sequence in the database
		for(Sequence sequence : database.getSequences()){
			// for each itemset in that sequence
			for(short j=0; j< sequence.getItemsets().size(); j++){
				List<Integer> itemset = sequence.get(j);
				// for each item in that sequence
				for(int i=0; i< itemset.size(); i++){
					Integer itemI = itemset.get(i);
					
					// get the map of occurences of that item
					Map<Integer, Occurence> occurences = mapItemCount.get(itemI);
					// if the map of occurences of that item is null, create a new one
					if(occurences == null){
						occurences = new HashMap<Integer, Occurence>();
						mapItemCount.put(itemI, occurences);
					}
					// add this sequence id to the occurences of that item
					Occurence occurence = occurences.get(sequence.getId());
					if(occurence == null){
						occurence = new Occurence(sequence.getId());
						occurences.put(sequence.getId(), occurence);
					}
					// add the current itemset position to the occurences of that item
					occurence.add(j);
				}
			}
		}

		// (2) remove all items that are not frequent from the database
		
		// for each sequence
		for(Sequence sequence : database.getSequences()){
			int i=0;
			
			// for each itemset
			while(i < sequence.getItemsets().size()){
				List<Integer> itemset = sequence.getItemsets().get(i);
				int j=0;
				
				// for each item
				while(j < itemset.size()){
					double count = mapItemCount.get(itemset.get(j)).size();
					
					// if the item is not frequent remove it
					if(count < minsuppRelative){
						itemset.remove(j);
					}else{
						// otherwise go to next item
						j++;
					}
				}
				i++;  // go to next itemset
			}
		}
		// return the map of occurences of items
		return mapItemCount;
	}
	
	/**
	 * Save a rule I ==> J to the output file
	 * @param tidsIJ the tids containing the rule
	 * @param confIJ the confidence
	 * @param itemsetI the left part of the rule
	 * @param itemsetJ the right part of the rule
	 * @throws IOException exception if error writing the file
	 */
	private void saveRule(Set<Integer> tidsIJ, double confIJ, int[] itemsetI, int[] itemsetJ) throws IOException {
		// increase the number of rule found
		ruleCount++;
		
		// create a string buffer
		StringBuffer buffer = new StringBuffer();
		
		// write itemset 1 (antecedent)
		for(int i=0; i<itemsetI.length; i++){
			buffer.append(itemsetI[i]);
			if(i != itemsetI.length -1){
				buffer.append(",");
			}
		}
		
		// write separator
		buffer.append(" ==> ");
		
		// write itemset 2  (consequent)
		for(int i=0; i<itemsetJ.length; i++){
			buffer.append(itemsetJ[i]);
			if(i != itemsetJ.length -1){
				buffer.append(",");
			}
		}
		// write support
		buffer.append(" #SUP: ");
		buffer.append(tidsIJ.size());
		// write confidence
		buffer.append(" #CONF: ");
		buffer.append(confIJ);
		writer.write(buffer.toString());
		writer.newLine();
	}
	
	/**
	 * Check if an itemset contains an item.
	 * @param itemset the itemset
	 * @param item an item
	 * @return true if the item appears in the itemset, false otherwise
	 */
	public boolean contains(int[] itemset, int item) {
		// for each item in the itemset
		for(int i=0; i<itemset.length; i++){
			// if the item is found, return true
			if(itemset[i] == item){
				return true;
			// if the current item is larger than the item that is searched,
			// then return false because of the lexical order.
			}else if(itemset[i] > item){
				return false;
			}
		}
		// not found, return false
		return false;
	}
	
	/**

	 * This method checks if the item "item" is in the itemset.
	 * It asumes that items in the itemset are sorted in lexical order
	 * This version also checks that if the item "item" was added it would be the largest one
	 * according to the lexical order.
	 * @param itemset an itemset
	 * @param item  the item
	 * @return return true if the above conditions are met, otherwise false
	 */
	public boolean containsLEXPlus(int[] itemset, int item) {
		// for each item in itemset
		for(int i=0; i< itemset.length; i++){
			// check if the current item is equal to the one that is searched
			if(itemset[i] == item){
				// if yes return true
				return true;
			// if the current item is larger than the item that is searched,
			// then return true because if if the item "item" was added it would be the largest one
			// according to the lexical order.  
			}else if(itemset[i] > item){
				return true; // <-- XXXX
			}
		}
		// if the searched item was not found, return false.
		return false;
	}
	
	/**
	 * This method checks if the item "item" is in the itemset.
	 * It assumes that items in the itemset are sorted in lexical order
	 * @param itemset an itemset
	 * @param item  the item
	 * @return return true if the item
	 */
	public boolean containsLEX(int[] itemset, int item) {
		// for each item in itemset
		for(int i=0; i< itemset.length; i++){
			// check if the current item is equal to the one that is searched
			if(itemset[i] == item){
				// if yes return true
				return true;
			// if the current item is larger than the item that is searched,
			// then return false because of the lexical order.
			}else if(itemset[i] > item){
				return false;  // <-- xxxx
			}
		}
		// if the searched item was not found, return false.
		return false;
	}

	
	/**
	 * Print statistics about the last algorithm execution to System.out.
	 */
	public void printStats() {
		System.out
				.println("=============  TRULEGROWTH - STATS =============");
//		System.out.println("minsup: " + minsuppRelative);
		System.out.println("Sequential rules count: " + ruleCount);
		System.out.println("Total time : " + (timeEnd - timeStart) + " ms");
		System.out.println("Max memory (mb)" + MemoryLogger.getInstance().getMaxMemory());
		System.out.println("=====================================");
	}

	/**
	 * Get the total runtime of the last execution.
	 * @return the time as a double.
	 */
	public double getTotalTime(){
		return timeEnd - timeStart;
	}
}
