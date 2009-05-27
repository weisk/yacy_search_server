// AbstractIndex.java
// -----------------------------
// (C) 2009 by Michael Peter Christen; mc@yacy.net, Frankfurt a. M., Germany
// first published 15.3.2009 on http://yacy.net
//
// This is a part of YaCy, a peer-to-peer based web search engine
//
// $LastChangedDate: 2006-04-02 22:40:07 +0200 (So, 02 Apr 2006) $
// $LastChangedRevision: 1986 $
// $LastChangedBy: orbiter $
//
// LICENSE
// 
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

package de.anomic.kelondro.text;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import de.anomic.kelondro.order.Order;

public abstract class AbstractIndex <ReferenceType extends Reference> implements Index<ReferenceType> {
    
    final protected ReferenceFactory<ReferenceType> factory;

    public AbstractIndex(final ReferenceFactory<ReferenceType> factory) {
        this.factory = factory;
    }
    
    public int remove(final TreeSet<byte[]> termHashes, final String urlHash) throws IOException {
        // remove the same url hashes for multiple words
        // this is mainly used when correcting a index after a search
        final Iterator<byte[]> i = termHashes.iterator();
        int c = 0;
        while (i.hasNext()) {
            if (remove(i.next(), urlHash)) c++;
        }
        return c;
    }
    
    public void remove(final TreeSet<byte[]> termHashes, final Set<String> urlHashes) throws IOException {
        // remove the same url hashes for multiple words
        // this is mainly used when correcting a index after a search
        final Iterator<byte[]> i = termHashes.iterator();
        while (i.hasNext()) {
            remove(i.next(), urlHashes);
        }
    }
    
    public synchronized TreeSet<ReferenceContainer<ReferenceType>> references(final byte[] startHash, final boolean rot, int count) throws IOException {
        // creates a set of indexContainers
        // this does not use the cache
        final Order<ReferenceContainer<ReferenceType>> containerOrder = new ReferenceContainerOrder<ReferenceType>(factory, this.ordering().clone());
        ReferenceContainer<ReferenceType> emptyContainer = ReferenceContainer.emptyContainer(factory, startHash, 0);
        containerOrder.rotate(emptyContainer);
        final TreeSet<ReferenceContainer<ReferenceType>> containers = new TreeSet<ReferenceContainer<ReferenceType>>(containerOrder);
        final Iterator<ReferenceContainer<ReferenceType>> i = references(startHash, rot);
        //if (ram) count = Math.min(size(), count);
        ReferenceContainer<ReferenceType> container;
        // this loop does not terminate using the i.hasNex() predicate when rot == true
        // because then the underlying iterator is a rotating iterator without termination
        // in this case a termination must be ensured with a counter
        // It must also be ensured that the counter is in/decreased every loop
        while ((count > 0) && (i.hasNext())) {
            container = i.next();
            if ((container != null) && (container.size() > 0)) {
                containers.add(container);
            }
            count--; // decrease counter even if the container was null or empty to ensure termination
        }
        return containers; // this may return less containers as demanded
    }
    
    
    // methods to search in the index
    
    /**
     * collect containers for given word hashes. This collection stops if a single container does not contain any references.
     * In that case only a empty result is returned.
     * @param wordHashes
     * @param urlselection
     * @return map of wordhash:indexContainer
     */
    public HashMap<byte[], ReferenceContainer<ReferenceType>> searchConjunction(final TreeSet<byte[]> wordHashes, final Set<String> urlselection) {
    	// first check if there is any entry that has no match; this uses only operations in ram
    	/*
    	Iterator<byte[]> i = wordHashes.iterator();
        while (i.hasNext()) {
            if (!this.has(i.next())); return new HashMap<byte[], ReferenceContainer<ReferenceType>>(0);
        }
        */
    	// retrieve entities that belong to the hashes
        final HashMap<byte[], ReferenceContainer<ReferenceType>> containers = new HashMap<byte[], ReferenceContainer<ReferenceType>>(wordHashes.size());
        byte[] singleHash;
        ReferenceContainer<ReferenceType> singleContainer;
        Iterator<byte[]> i = wordHashes.iterator();
        while (i.hasNext()) {
        
            // get next word hash:
            singleHash = i.next();
        
            // retrieve index
            try {
                singleContainer = this.get(singleHash, urlselection);
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }
        
            // check result
            if ((singleContainer == null || singleContainer.size() == 0)) return new HashMap<byte[], ReferenceContainer<ReferenceType>>(0);
        
            containers.put(singleHash, singleContainer);
        }
        return containers;
    }
    
    @SuppressWarnings("unchecked")
	public HashMap<byte[], ReferenceContainer<ReferenceType>>[] searchTerm(
            final TreeSet<byte[]> queryHashes, 
            final TreeSet<byte[]> excludeHashes, 
            final Set<String> urlselection) {
		// search for the set of hashes and return a map of of wordhash:indexContainer containing the seach result
		
		// retrieve entities that belong to the hashes
		HashMap<byte[], ReferenceContainer<ReferenceType>> inclusionContainers =
		(queryHashes.size() == 0) ?
		    new HashMap<byte[], ReferenceContainer<ReferenceType>>(0) :
		    this.searchConjunction(queryHashes, urlselection);
		if ((inclusionContainers.size() != 0) && (inclusionContainers.size() < queryHashes.size())) inclusionContainers = new HashMap<byte[], ReferenceContainer<ReferenceType>>(0); // prevent that only a subset is returned
		final HashMap<byte[], ReferenceContainer<ReferenceType>> exclusionContainers =
		(inclusionContainers.size() == 0) ?
		    new HashMap<byte[], ReferenceContainer<ReferenceType>>(0) :
		    this.searchConjunction(excludeHashes, urlselection);
		return new HashMap[]{inclusionContainers, exclusionContainers};
    }
    
}
