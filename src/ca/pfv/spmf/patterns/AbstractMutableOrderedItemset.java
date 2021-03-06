package ca.pfv.spmf.patterns;

import java.text.DecimalFormat;
import java.util.Collections;

import ca.pfv.spmf.patterns.itemset_list_integers_without_support.Itemset;

/**
 * This is an abstract class indicating general methods 
 * that an itemset should have no matter how it is implemented.
 * 
 * This class is designed for ordered itemsets where items are sorted
 * by lexical order and no item can appear twice.
 * 
 * Copyright (c) 2008-2013 Philippe Fournier-Viger
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
public abstract class AbstractMutableOrderedItemset extends AbstractOrderedItemset{

	public AbstractMutableOrderedItemset() {
		super();
	}
	
	/**
	 * Add an item to this itemset
	 * @param value the item
	 */
	public abstract void addItem(Integer value);
	
	/**
	 * This method create a new empty itemset and return it
	 * @return
	 */
	protected abstract AbstractMutableOrderedItemset createNewEmptyItemset();


	/**
	 * Make a copy of this itemset except that any item of a given itemset
	 * are not included in the copy.
	 * @param itemsetToNotKeep  the items that should not be included
	 * @return the copy
	 */
	public AbstractMutableOrderedItemset cloneItemSetMinusAnItemset(AbstractMutableOrderedItemset itemsetToNotKeep){
		// create a new itemset
		AbstractMutableOrderedItemset itemset = createNewEmptyItemset();
		// Make a loop to copy each item 
		for(int i=0; i< size(); i++){
			Integer item = this.get(i);
			// If the current item  should  be included, we add it.
			if(!itemsetToNotKeep.contains(item)){
				itemset.addItem(item);
			}
		}
		return itemset; // return the new itemset
	}
	
	/**
	 * Make a copy of this itemset but exclude a given item from the copy.
	 * @param itemToNotInclude  the item that should not be included
	 * @return the copy
	 */
	public AbstractMutableOrderedItemset cloneItemSetMinusOneItem(Integer itemToNotInclude){
		// create a new itemset
		AbstractMutableOrderedItemset itemset = createNewEmptyItemset();
		// Make a loop to copy each item 
		for(int i=0; i< size(); i++){
			Integer item = this.get(i);
			// If the current item  should  be included, we add it.
			if(!itemToNotInclude.equals(item)){
				itemset.addItem(item);
			}
		}
		return itemset; // return the new itemset
	}
	

	/**
	 * This method return an itemset containing items that are included
	 * in this itemset and in a given itemset
	 * @param itemset2 the given itemset
	 * @return the new itemset
	 */
	public AbstractMutableOrderedItemset intersection(AbstractMutableOrderedItemset itemset2) {
		AbstractMutableOrderedItemset intersection = createNewEmptyItemset();
		for(int i=0; i< size(); i++){
			Integer item = this.get(i);
			
			if (itemset2.contains(item)) {
				intersection.addItem(item);
			}
		}
		return intersection;
	}

}