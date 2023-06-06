/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2020
//
// Copyright in this software belongs to University of Southampton
// IT Innovation Centre of Gamma House, Enterprise Road,
// Chilworth Science Park, Southampton, SO16 7NS, UK.
//
// This software may not be used, sold, licensed, transferred, copied
// or reproduced in whole or in part in any manner or form or in or
// on any media by any person other than in accordance with the terms
// of the Licence Agreement supplied with the software, or otherwise
// without the prior written consent of the copyright owners.
//
// This software is distributed WITHOUT ANY WARRANTY, without even the
// implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
// PURPOSE, except where stated in the Licence Agreement supplied with
// the software.
//
//      Created By:				Lee Mason
//      Created Date:			2020-08-10
//      Created for Project :   FOGPROTECT
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.modelquerier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.soton.itinnovation.security.modelquerier.dto.EntityDB;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class which allows EntityDB sub-types to be cached. Provides methods of caching, storing, and retrieval.
 * // TODO: This class could do with a refactor
 */
public class EntityCache {
    private static final Logger logger = LoggerFactory.getLogger(EntityCache.class);

    /* Cache data structures. The idea is that when loaded from the triple store, entities are saved
     * in caches (hash maps), one providing easy access to all entities of each type, the other easy
     * access to an entity with a specific URI.
     * 
     * The maps exist for each graph, because the data could be split betwen graphs.
     * 
     * The cached entities are returned to the JenaQuerierDB and thence to consumer processes, so
     * any changes are made directly on the cached entities.
     * 
     * If a call is received to store an entity, the entity is also added to a hashmap containing
     * entities that need to be stored back to the triple store. That is only done when the cache
     * is synchronised.
     * 
     * The cache is valid for a given entity and graph if that entity is cached in that graph.
     * This is tracked using a hash map using the entity's short URI as a key. This contains a
     * true value if the cache is valid for that entity. A false value or no value indicates an
     * invalid entry, which means the calling process should load it from the underlying DB.
     * 
     * The cache entry starts off in an invalid state (the cache is empty), and undergoes the
     * following state changes:
     * - set to true true once the entity has been loaded and cached
     * - remains true if the entity is stored
     * - remains true if the entity is deleted
     * - reset to false when the cache is cleared, which should only be done after a synch
     * 
     * A store operation just tells the cache that the calling process changed the object, so it
     * should be rewritten to the DB at the next synchronisation. The changes are already in the 
     * cache, so a subsequent request for the same entity just still just return the object.
     * 
     * A delete operation tells the cache that the calling process wants the entity deleted. The
     * cache removes the object from the entities cached and to be stored, and adds it to the
     * entities to be deleted at the next sync(). This means a subsequent request for the same
     * entity will return null, which is valid. We don't want the calling process looking for
     * it in the DB (where it still persists) after deletion.
     * 
     * Note that a delete after a store cancels the store, and a store after a delete cancels the
     * delete. However, a store action doesn't need to remove entities from the deleted list, as
     * they can be removed at the next sync().
     * 
     * The cache is valid for a given cache type and graph if all entities of that type from that
     * graph are cached, and hence there is no need to look in the DB for anything else. The cache
     * type is invalid at the start (the cache is empty) and undergoes the following changes:
     * - set to true if all entities of that type are loaded and cached
     * - set to false when the cache is cleared, which should only be done after a synch
     * - not changed if one or more entities are changed, stored or deleted
     * 
     * Because of this, the method for caching multiple entities has a flag to specify whether it
     * is caching all entities of that type. If set to true, the cache type is valid. If not, the
     * cache type is unchanged.
     */

    private Map<String, Map<String, Map<String, EntityDB>>> cacheByTypeByGraph = new HashMap<>();   // Cached entities, get by graph, then type key, then URI
    private Map<String, Map<String, EntityDB>> cacheByUriByGraph = new HashMap<>();                 // Cached entities, get by graph, then URI
    private Map<String, Map<String, Map<String, EntityDB>>> storeByTypeByGraph = new HashMap<>();   // Entities to be stored, get by graph, then type key, then URI
    private Map<String, Map<String, EntityDB>> deleteByType = new HashMap<>();                      // Entities to be deleted, get by type key, then URI
    private Map<String, Map<String, Boolean>> cacheValidByTypeByGraph = new HashMap<>();            // Get by graph, then type key, true if all entities are loaded
    private Map<String, Map<String, Boolean>> cacheValidByUriByGraph = new HashMap<>();             // Get by graph, then URI, true if that entity is loaded

    private static final String[] systemGraphs = {"system", "system-inf", "system-ui"};

    /* Adds a list of entities of a specific type received from a calling process to the cache.
     *
     * The boolean argument validateCache is set to true if we're adding all entities of that type
     * to the cache (i.e. there are no others in the same graph). If set to false, it means there
     * could be more entities of the same type in the DB but not cached.
     * 
     * The assumption seems to be that these entities override whatever may be in the triple store
     * in any graph, but there may be other entities of the same type in other graphs.
     */
    public <T extends EntityDB> void cacheEntities(Map<String, T> entities, String typeKey, boolean validateCache, String graph, String... otherGraphs) {
        // Add them to the cache
        Map<String, Map<String, EntityDB>> cacheByType = cacheByTypeByGraph.computeIfAbsent(graph, k -> new HashMap<>());
        Map<String, EntityDB> cacheThisType = cacheByType.computeIfAbsent(typeKey, k -> new HashMap<>());
        Map<String, EntityDB> cacheThisGraph = cacheByUriByGraph.computeIfAbsent(graph, k -> new HashMap<>());
        cacheThisType.putAll(entities);
        cacheThisGraph.putAll(entities);

        // If these are the last uncached entities of their type, Set validity status for the cache type
        if (validateCache) {
            Map<String, Boolean> cacheValidByType = cacheValidByTypeByGraph.computeIfAbsent(graph, k -> new HashMap<>());
            cacheValidByType.put(typeKey, true);
        }

        // Set validity status for the cached copy of this entity
        // TODO : work out why this is done for all graphs - does it make sense?
        List<String> graphs = new ArrayList<>();
        graphs.add(graph);
        graphs.addAll(Arrays.asList(otherGraphs));
        for (String aGraph : graphs) {
            for (String entityUri : entities.keySet()) {
                Map<String, Boolean> cacheValidByUri = cacheValidByUriByGraph.computeIfAbsent(aGraph, k -> new HashMap<>());
                cacheValidByUri.put(entityUri, true);
            }
        }
    }

    /* Adds a single entity with a specific type received from a calling process to the cache,
     * and specifies if this is adding the last one.
     *
     * The assumption seems to be that this entity overrides whatever may be in the triple
     * store in any graph.
     */
    public <T extends EntityDB> void cacheEntity(T entity, String typeKey, boolean validateCache, String graph, String... otherGraphs) {
        // Add the entity to the cache
        Map<String, Map<String, EntityDB>> cacheByType = cacheByTypeByGraph.computeIfAbsent(graph, k -> new HashMap<>());
        Map<String, EntityDB> cacheThisType = cacheByType.computeIfAbsent(typeKey, k -> new HashMap<>());
        Map<String, EntityDB> cacheThisGraph = cacheByUriByGraph.computeIfAbsent(graph, k -> new HashMap<>());
        cacheThisType.put(entity.getUri(), entity);
        cacheThisGraph.put(entity.getUri(), entity);

        // Set validity status for the cache of entities of this type
        if (validateCache) {
            Map<String, Boolean> cacheValidByType = cacheValidByTypeByGraph.computeIfAbsent(graph, k -> new HashMap<>());
            cacheValidByType.put(typeKey, true);
        }

        // Set validity status for the cached copy of this entity
        // TODO : work out why this is done for all graphs - does it make sense?
        List<String> graphs = new ArrayList<>();
        graphs.add(graph);
        graphs.addAll(Arrays.asList(otherGraphs));
        for (String aGraph : graphs) {
            Map<String, Boolean> cacheValidByUri = cacheValidByUriByGraph.computeIfAbsent(aGraph, k -> new HashMap<>());
            cacheValidByUri.put(entity.getUri(), true);
        }

    }

    /* Adds a single entity with a specific type received from a calling process to the cache,
     * with no guarantee this is the last entity of that type.
     *
     * This is almost always the case, so having a wrapper method for it makes code simpler.
     */
    public <T extends EntityDB> void cacheEntity(T entity, String typeKey, String graph, String... otherGraphs) {
        cacheEntity(entity, typeKey, false, graph, otherGraphs);
    }

    /* Adds a single entity with a specific type received from a calling process to the list
     * of entities that need to be written back to a specific graph.
     *
     * Note that the entity is a pointer to one in the cache, changes to which have been made
     * by the calling process. This just adds it to a list that should be written back to the
     * triple store if the cache is synchronised.
     */
    public <T extends EntityDB> void storeEntity(T entity, String typeKey, String graph) {
        // Add the entity to the cache without changing validity status for the entity type
        cacheEntity(entity, typeKey, false, graph);

        // Add it to the staging area, so we know it must be written to the DB at the next synchronisation 
        Map<String, Map<String, EntityDB>> storeByType = storeByTypeByGraph.computeIfAbsent(graph, k -> new HashMap<>());
        Map<String, EntityDB> store = storeByType.computeIfAbsent(typeKey, k -> new HashMap<>());
        store.put(entity.getUri(), entity);

    }

    /* Deletes a set of entities with a specific type in a specific graph from the cache.
     * 
     * The behaviour after this should be as follows:
     * - an attempt to retrieve any of these entities from the cache returns null, which
     *   is valid (to the calling process, the entities should no longer exist)
     * - an attempt to retrieve all entities of this type doesn't include these entities
     * - if the cache was valid for this type (all entities cached), it still is, since the
     *   only entities removed from the cache are those that should be deleted
     * 
     * Deletion is only permitted on system model entities, and must be done across all the
     * system model graphs.
     */
    public <T extends EntityDB> void deleteEntities(Map<String, T> entities, String typeKey) {
        // Maybe not the most efficient option, but it will do for now
        for(EntityDB entity : entities.values()){
            deleteEntity(entity, typeKey);
        }
    }

    /* Deletes a single entity with a specific type received from a calling process from the
     * cache.
     * 
     * The behaviour after this should be as follows:
     * - an attempt to retrieve this entity from the cache returns null, which is valid (to
     *   the calling process, the entity should no longer exist)
     * - an attempt to retrieve all entities of this type doesn't include this entity
     * - if the cache was valid for this type (all entities cached), it still is, since the
     *   only entities removed from the cache are those that should be deleted
     * 
     * Deletion is only permitted on system model entities, and must be done across all the
     * system model graphs.
     */
    public <T extends EntityDB> void deleteEntity(T entity, String typeKey, Boolean skipDelete) {
        // Get the URI
        String uri = entity.getUri();
        
        for(String graph : systemGraphs) {
            Map<String, Boolean> cacheValidThisGraph = cacheValidByUriByGraph.getOrDefault(graph, new HashMap<>());
            if(cacheValidThisGraph.keySet().contains(uri) && cacheValidThisGraph.get(uri)) {
                // Remove the entity from the cache, if present
                Map<String, Map<String, EntityDB>> cacheByType = cacheByTypeByGraph.computeIfAbsent(graph, k -> new HashMap<>());
                Map<String, EntityDB> cacheThisType = cacheByType.computeIfAbsent(typeKey, k -> new HashMap<>());
                if(cacheThisType.containsKey(uri)) cacheThisType.remove(uri);

                Map<String, EntityDB> cacheThisGraph = cacheByUriByGraph.computeIfAbsent(graph, k -> new HashMap<>());
                if(cacheThisGraph.containsKey(uri)) cacheThisGraph.remove(uri);

                // Remove it from the staging area of entities to be stored, if present
                Map<String, Map<String, EntityDB>> storeByType = storeByTypeByGraph.computeIfAbsent(graph, k -> new HashMap<>());
                Map<String, EntityDB> store = storeByType.computeIfAbsent(typeKey, k -> new HashMap<>());
                if(store.containsKey(uri)) store.remove(uri);
            }
        }

        if(!skipDelete){
            // Add it to the staging area for entities to be deleted, so we know to delete it 
            Map<String, EntityDB> deletions = deleteByType.computeIfAbsent(typeKey, k -> new HashMap<>());
            deletions.put(entity.getUri(), entity);    
        }

        // Do not remove it from the list of valid cached entities - deleted means a null response is valid

    }

    public <T extends EntityDB> void deleteEntity(T entity, String typeKey){
        deleteEntity(entity, typeKey, false);
    }

    /* Gets all entities of a given type from the cache for a specified list of graphs.
     */
    public <T extends EntityDB> Map<String, T> getAll(String typeKey, Class<T> entityClass, String... graphs) {
        Map<String, T> entities = new HashMap<>();
        for (String graph : graphs) {
            Map<String, Map<String, EntityDB>> cacheByType = cacheByTypeByGraph.getOrDefault(graph, new HashMap<>());
            for (EntityDB entity : cacheByType.getOrDefault(typeKey, new HashMap<>()).values()) {
                /* TODO : should we merge rather than replace an entity if one with the same URI
                 * is found in a subsequent graph?
                 * Alternatively, should we return a separate map from each graph?
                 */
                entities.put(entity.getUri(), (T) entity);
            }
        }

        return entities;
    }

    /* Gets a single map containing all entities from the cache for a specified list of graphs.
     */
    public Map<String, EntityDB> getAll(String... graphs) {
        Map<String, EntityDB> entities = new HashMap<>();

        for (String graph : graphs) {
            Map<String, Map<String, EntityDB>> cacheByType = cacheByTypeByGraph.getOrDefault(graph, new HashMap<>());

            for (Map<String, EntityDB> cache : cacheByType.values()) {
                entities.putAll(cache);
            }
        }

        return entities;
    }

    /* Gets a single map of  all entities by type from the cache for a specified graph.
     */
    public Map<String, Map<String, EntityDB>> getAllByType(String graph) {
        return cacheByTypeByGraph.getOrDefault(graph, new HashMap<>());
    }

    /* Gets a single map of entities that should be stored in a specified graph, organised by type.
     */
    public Map<String, Map<String, EntityDB>> getStoreCache(String graph) {
        return storeByTypeByGraph.getOrDefault(graph, new HashMap<>());
    }

    /* Gets a single map of entities that should be deleted from a specified graph, organised by type.
     */
    public Map<String, Map<String, EntityDB>> getDeleteCache() {
        return deleteByType;
    }

    /* Gets a single entity from the cache for a specified graph.
     */
    public <T extends EntityDB> T get(String uri, String typeKey, Class<T> entityClass, String... graphs) {
        for (String graph : graphs) {
            if(typeKey.endsWith("DB")){
                UnsupportedOperationException e = new UnsupportedOperationException(String.format("Getting cached entity {} in graph {} using key {}", uri, graph, typeKey));
                throw e;
            }
            Map<String, EntityDB> cacheByUri = cacheByUriByGraph.get(graph);
            if (cacheByUri == null) {
                return null;
            }

            T entity = (T) cacheByUri.get(uri);
            if (entity != null) {
                return entity;
            }
        }
        return null;
    }

    /* Checks if all entities of a given type are cached over a specified list of graphs.
     */
    public boolean checkTypeValid(String typeKey, String... graphs) {
        boolean valid = true;
        for (String graph : graphs) {
            valid = valid && cacheValidByTypeByGraph.getOrDefault(graph, new HashMap<>()).getOrDefault(typeKey, false);
        }
        return valid;
    }

    /* Checks if a specific entity is cached over a specified list of graphs.
     */
    public boolean checkEntityValid(String uri, String... graphs) {
        boolean valid = true;
        for (String graph : graphs) {
            valid = valid && cacheValidByUriByGraph.getOrDefault(graph, new HashMap<>()).getOrDefault(uri, false);
        }
        return valid;
    }

    /* Clears entities from the delete list if they were subsequently stored */
    public void prepareSync(){
        for(String graph : systemGraphs) {
            Map<String, Map<String, EntityDB>> cacheByType = cacheByTypeByGraph.computeIfAbsent(graph, k -> new HashMap<>());
            for(String typeKey : cacheByType.keySet()){
                Map<String, EntityDB> deletions = deleteByType.computeIfAbsent(typeKey, k -> new HashMap<>());
                Map<String, EntityDB> cacheThisType = cacheByType.computeIfAbsent(typeKey, k -> new HashMap<>());
                for(String uri : cacheThisType.keySet()){
                    if(deletions.keySet().contains(uri)) deletions.remove(uri);
                }
            }
        }
    }

    public void clear() {
        cacheByTypeByGraph.clear();
        cacheByUriByGraph.clear();
        storeByTypeByGraph.clear();
        cacheValidByTypeByGraph.clear();
        cacheValidByUriByGraph.clear();
        deleteByType.clear();
    }

}