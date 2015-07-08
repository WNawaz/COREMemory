package ca.pfv.spmf.algorithms.sequentialpatterns.clospan_AGP.items.patterns;

import ca.pfv.spmf.algorithms.sequentialpatterns.clospan_AGP.items.abstractions.ItemAbstractionPair;
import ca.pfv.spmf.algorithms.sequentialpatterns.clospan_AGP.items.creators.AbstractionCreator;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * Implementation of pattern structure. We define it as a list of pairs <abstraction, item>.
 * Besides, a bitSet appearingIn denotes the sequences where the pattern appears.
 * 
 * Copyright Antonio Gomariz Peñalver 2013
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
 * 
 * @author agomariz
 */
public class Pattern implements Comparable<Pattern>{

    /**
     * List of pairs <abstraction, item> that define the pattern
     */
    private List<ItemAbstractionPair> elements;
    /**
     * Set of sequence IDs indicating where the pattern appears
     */
    //private List<Integer> appearingIn;
    private BitSet appearingIn;
    /**
     * Appearance frequency of this concrete pattern
     */
    private int support;
    
    /**
     * Addition of all the different sequence identifiers
     */
    private int sumIdSequences=-1;

    /**
     * Standard constructor
     */
    public Pattern() {
        this.elements = new ArrayList<ItemAbstractionPair>();
        //this.appearingIn = new ArrayList<Integer>();
        this.appearingIn=new BitSet();
    }

    /**
     * New pattern from a list of pairs <abstraction, item>
     * @param elements 
     */
    public Pattern(List<ItemAbstractionPair> elements) {
        this.elements = elements;
        //this.appearingIn = new ArrayList<Integer>();
        this.appearingIn=new BitSet();
    }

    /**
     * New pattern from a single pair <abstraction, item>
     * @param pair 
     */
    public Pattern(ItemAbstractionPair pair) {
        this.elements = new ArrayList<ItemAbstractionPair>();
        this.elements.add(pair);
        //this.appearingIn = new ArrayList<Integer>();
        this.appearingIn=new BitSet();
    }


    /**
     * String representation of a pattern
     * @return 
     */
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < elements.size(); i++) {
            result.append(elements.get(i).toString());
        }
        result.append("\t(").append(appearingIn.size()).append(')');
        result.append("\t[");
        result.append(getSupport());
        result.append("]");
        return result.toString();
    }
    
    /**
     * String representation of itemset. Adjusted to SPMF format.
     * @return 
     */
    public String toStringToFile() {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < elements.size(); i++) {
            if(i==elements.size()-1){
                if(i!=0)
                    result.append(elements.get(i).toStringToFile());
                else
                    result.append(elements.get(i).getItem());
                result.append(" -1");
            }
            else if(i==0){
                result.append(elements.get(i).getItem());
            }else{
                result.append(elements.get(i).toStringToFile());
            }
            
        }
        result.append(" #SUP: ");
        result.append(getSupport());
        return result.toString();
    }

    /**
     * It clones a pattern
     * @return 
     */
    public Pattern clonePatron() {
        PatternCreator patternCreator = PatternCreator.getInstance();
        List<ItemAbstractionPair> elementsCopy = new ArrayList<ItemAbstractionPair>(elements);
        Pattern clone = patternCreator.createPattern(elementsCopy);
        return clone;
    }

    /**
     * It obtains the elements of the pattern
     * @return 
     */
    public List<ItemAbstractionPair> getElements() {
        return elements;
    }

    /**
     * It obtains the Ith element of the pattern
     * @param i
     * @return 
     */
    public ItemAbstractionPair getIthElement(int i) {
        return elements.get(i);
    }

    /**
     * It obtains the last element of the pattern
     * @return 
     */
    public ItemAbstractionPair getLastElement() {
        if (size() > 0) {
            return getIthElement(size() - 1);
        }
        return null;
    }

    /**
     * Setter method to set the elements of the pattern
     * @param elements 
     */
    public void setElements(List<ItemAbstractionPair> elements) {
        this.elements = elements;
    }

    /**
     * It adds an item with its abstraction in the pattern. The new pair is 
     * added in the last position of the pattern.
     * @param pair 
     */
    public void add(ItemAbstractionPair pair) {
        this.elements.add(pair);
    }

    /**
     * It returns the number of items contained in the pattern
     * @return 
     */
    public int size() {
        return elements.size();
    }

    /**
     * We use a lexicographic order.
     * @param arg
     * @return 
     */
    @Override
    public int compareTo(Pattern o) {
        List<ItemAbstractionPair> elementsOfGreaterPattern, elementOfSmallerPattern;
        if (getElements().size() >= o.getElements().size()) {
            elementsOfGreaterPattern = getElements();
            elementOfSmallerPattern = o.getElements();
        } else {
            elementOfSmallerPattern = getElements();
            elementsOfGreaterPattern = o.getElements();
        }
        for (int i = 0; i < elementOfSmallerPattern.size(); i++) {
            int comparison = elementOfSmallerPattern.get(i).compareTo(elementsOfGreaterPattern.get(i));
            if (comparison != 0) {
                return comparison;
            }
        }
        if (elementsOfGreaterPattern.size() == elementOfSmallerPattern.size()) {
            return 0;
        }
        if (getElements().size() < o.getElements().size()) {
            return -1;
        }
        return 1;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Pattern) {
            Pattern p = (Pattern) o;
            if (this.compareTo(p) == 0) {
                return true;
            }
            return false;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 67 * hash + (this.elements != null ? this.elements.hashCode() : 0);
        return hash;
    }

    /**
     * It returns the list of sequence IDs where the pattern appears.
     * @return 
     */
    /*public List<Integer> getAppearingIn() {
        return appearingIn;
    }*/
    public BitSet getAppearingIn() {
        return appearingIn;
    }
    
    
    /**
     * it set the list of sequence IDs where the pattern appears.
     * @param appearingIn 
     */
    public void setAppearingIn(BitSet ids){
        //this.appearingIn=new ArrayList<Integer>(ids);
        this.appearingIn=ids;
        setSupport(this.appearingIn.cardinality());
    }
    
    /**
     * It answers if the current Pattern is a subpattern of another one
     * given as parameter.
     * @param abstractionCreator Abstraction creator
     * @param p Pattern to check if it is a superpattern of the current one
     * @return 
     */
    public boolean isSubpattern(AbstractionCreator abstractionCreator, Pattern p){
       //We initialize all the positions values to 0
       List<Integer> positions=new ArrayList<Integer>(p.size());
       for(int i=0;i<size();i++){
           positions.add(0);
       }
       //And we call to the method of abstractionCreator
       return abstractionCreator.isSubpattern(this,p,0,positions);
    }

    /**
     * Method to obtain the support of a pattern
     * 
     * @return 
     */
    public int getSupport() {
        return support;
    }

    /**
     * Method to establish the support of this pattern
     * @param support 
     */
    public void setSupport(int support) {
        this.support = support;
    }

    /**
     * Getter method to obtain the addition of all the sequence 
     * identifiers that are present in the support of this pattern
     * @return 
     */
    public int getSumIdSequences() {
        if(sumIdSequences<0)
            sumIdSequences=calculateSumIdSequences();
        return sumIdSequences;
    }

    /**
     * Setter method to establish the value for the addition of all the sequence 
     * identifiers that are present in the support of this pattern
     * 
     * @param sumIdSequences value for the addition
     */
    public void setSumIdSequences(int sumIdSequences) {
        this.sumIdSequences = sumIdSequences;
    }

    /**
     * Method for the real calculation of the addition of sequence identifiers
     * @return 
     */
    private int calculateSumIdSequences() {
        int sum=0;
        for(int i=appearingIn.nextSetBit(0);i>=0;i=appearingIn.nextSetBit(i+1))
            sum+=i;
        return sum;
    }
    
    /**
     * It concatenates the given pair as a last element of the pattern
     * @param pair
     * @return 
     */
    public Pattern concatenate(ItemAbstractionPair pair) {
        Pattern result = clonePatron();
        result.add(pair);
        return result;
    }
    
    /**
     * It concatenates the given pattern to the current pattern
     * @param pattern
     * @return 
     */
    public Pattern concatenate(Pattern pattern) {
        Pattern result = clonePatron();
        result.getElements().addAll(pattern.getElements());
        return result;
    }
    
}
