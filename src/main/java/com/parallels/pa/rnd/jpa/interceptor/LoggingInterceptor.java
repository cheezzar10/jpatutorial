package com.parallels.pa.rnd.jpa.interceptor;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.EmptyInterceptor;
import org.hibernate.type.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingInterceptor extends EmptyInterceptor {
	private static final long serialVersionUID = -598715229206238435L;

	private static final Logger log = LoggerFactory.getLogger(LoggingInterceptor.class);
	
	public static final Map<EntityKey, Long> entityCache = new LinkedHashMap<>(128, 0.75f, true);

	public LoggingInterceptor() {
		log.debug("logging interceptor instance created");
	}
	
	public static record EntityKey(String entityClassName, Serializable entityId) {}

	@Override
	public boolean onFlushDirty(
			Object entity, 
			Serializable id, 
			Object[] currentState, 
			Object[] previousState, 
			String[] propertyNames, 
			Type[] types) {
		
		log.debug("onFlushDirty(entity = {}, id = {})", entity, id);
		
		addEntityCacheEntry(entity, id);

		return false;
	}

	private void addEntityCacheEntry(Object entity, Serializable id) {
		entityCache.put(new EntityKey(entity.getClass().getName(), id), System.currentTimeMillis());
	}

	@Override
	public boolean onLoad(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
		log.debug("onLoad(entity = {}, id = {})", entity, id);
		
		addEntityCacheEntry(entity, id);

		return false;
	}

	@Override
	public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
		log.debug("onSave(entity = {}, id = {})", entity, id);
		
		addEntityCacheEntry(entity, id);

		return false;
	}
	
	@Override
	public void onDelete(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
		log.debug("onDelete(entity = {}, id = {})", entity, id);
		
		entityCache.remove(new EntityKey(entity.getClass().getName(), id));
	}

	@Override
	public void postFlush(Iterator entities) {
		log.debug("postFlush()");
	}
}
