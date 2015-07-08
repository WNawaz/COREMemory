/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.pfv.spmf.algorithms.sequentialpatterns.gsp_AGP.items.patterns;

import ca.pfv.spmf.algorithms.sequentialpatterns.gsp_AGP.items.abstractions.ItemAbstractionPair;
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
public class Pattern implements Comparable<Pattern> {

    /**
     * List of elements of a patterns. Concretely it is a list of pairs <abstraction, item>
     */
    private List<ItemAbstractionPair> elements;
    /**
     * Bitset when we keep the sequence IDs where the pattern it appears
     */
    private BitSet appearingIn;
    //private Boolean frequent = null;

    /**
     * Standard constructor that sets all the attributes to empty values
     */
    public Pattern() {
        this.elements = new ArrayList<ItemAbstractionPair>();
        this.appearingIn = new BitSet();
    }

    /**
     * Constructor that creates a pattern with the given element list
     * @param elements element list
     */
    public Pattern(List<ItemAbstractionPair> elements) {
        this.elements = elements;
        this.appearingIn = new BitSet();
    }

    /**
     * Constructor that creates a pattern with the only element passed as a parameter
     * @param pair 
     */
    public Pattern(ItemAbstractionPair pair) {
        this.elements = new ArrayList<ItemAbstractionPair>();
        this.elements.add(pair);
        this.appearingIn = new BitSet();
    }

    /**
     * String representation of itemset
     * @return 
     */
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        BitSet sequenceIdsList = appearingIn;
        for (int i = 0; i < elements.size(); i++) {
            result.append(elements.get(i).toString());
        }
        result.append("\t[");
        //for (int i = 0; i < listaEntradasPresentes.size(); i++) {
        for(int i = sequenceIdsList.nextSetBit(0); i >= 0; i = sequenceIdsList.nextSetBit(i+1)){
            result.append(i).append(", ");
        }
        result.deleteCharAt(result.length() - 1);
        result.deleteCharAt(result.length() - 1);
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
        result.append(appearingIn.cardinality());
        return result.toString();
    }

    /**
     * Method that clones a pattern
     * @return the clone of the pattern
     */
    public Pattern clonePattern() {
        List<ItemAbstractionPair> elementsCopy = new ArrayList<ItemAbstractionPair>(elements);
        Pattern clone = new Pattern(elementsCopy);
        return clone;
    }

    /**
     * It gets the components of the patterns in a list of pairs
     * @return 
     */
    public List<ItemAbstractionPair> getElements() {
        return elements;
    }

    /**
     * It gets the ith component from the pattern
     * @param i ith index of the element to return
     * @return 
     */
    public ItemAbstractionPair getIthElement(int i) {
        return elements.get(i);
    }

    /**
     * It returns the last  but one element of the pattern
     * @return 
     */
    public ItemAbstractionPair getLastButOneElement() {
        if (size() > 1) {
            return getIthElement(size() - 2);
        }
        return null;
    }

    /**
     * It returns the last element of the pattern
     * @return 
     */
    public ItemAbstractionPair getLastElement() {
        if (size() > 0) {
            return getIthElement(size() - 1);
        }
        return null;
    }

    /**
     * It returns the first n elements of the pattern
     * @param n
     * @return 
     */
    public List<ItemAbstractionPair> getNElements(int n) {
        if (n <= elements.size()) {
            return elements.subList(0, n - 1);
        }
        return null;
    }

    /**
     * Setter for the pattern components
     * @param elements 
     */
    public void setElements(List<ItemAbstractionPair> elements) {
        this.elements = elements;
    }

    /**
     * Add a pair in the last position of the pattern
     * @param pair 
     */
    public void add(ItemAbstractionPair pair) {
        this.elements.add(pair);
    }

    /**
     * return the pattern size
     * @return 
     */
    public int size() {
        return elements.size();
    }

    /**
     * For comparing with another pattern
     * @param arg
     * @return 
     */
    public int compareTo(Pattern arg) {
        List<ItemAbstractionPair> elementsOfBiggerPattern, elementsOfSmallerPattern;
        if (getElements().size() >= arg.getElements().size()) {
            elementsOfBiggerPattern = getElements();
            elementsOfSmallerPattern = arg.getElements();
        } else {
            elementsOfSmallerPattern = getElements();
            elementsOfBiggerPattern = arg.getElements();
        }
        for (int i = 0; i < elementsOfSmallerPattern.size(); i++) {
            int comparison = elementsOfSmallerPattern.get(i).compareTo(elementsOfBiggerPattern.get(i));
            if (comparison != 0) {
                return comparison;
            }
        }
        if (elementsOfBiggerPattern.size() == elementsOfSmallerPattern.size()) {
            return 0;
        }
        if (getElements().size() < arg.getElements().size()) {
            return -1;
        }
        return 1;
    }

    @Override
    public boolean equals(Object arg) {
        if (arg instanceof Pattern) {
            Pattern p = (Pattern) arg;
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
     * It answer to the question of if the current pattern is a prefix of the 
     * given pattern
     * @param p pattern where we want to check if our current pattern is a prefix
     * @return true if is a positive answer, false otherwise
     */
    public boolean isPrefix(Pattern p) {
        boolean output = false;
        List<ItemAbstractionPair> pElements = new ArrayList<ItemAbstractionPair>(p.getElements());
        pElements.remove(pElements.size() - 1);
        Pattern pTemp = new Pattern(pElements);
        if (this.equals(pTemp)) {
            output = true;
        }
        return output;
    }

    /**
     * return the set of sequence IDs where the pattern appears
     * @return 
     */
    public BitSet getAppearingIn() {
        return appearingIn;
    }

    public void setAppearingIn(BitSet appearingIn) {
        this.appearingIn = appearingIn;
    }

    public void clear() {
        elements.clear();
        appearingIn.clear();
    }

    /**
     * Add a sequence ID in the sequence Id set
     * @param sequenceId 
     */
    public void addAppearance(Integer sequenceId) {
        appearingIn.set(sequenceId);
    }

    /**
     * It returns the support of a pattern
     * @return 
     */
    public double getSupport() {
        return appearingIn.cardinality();
    }
}
