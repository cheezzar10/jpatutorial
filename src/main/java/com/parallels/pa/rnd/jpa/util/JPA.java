package com.parallels.pa.rnd.jpa.util;

import java.util.function.Consumer;
import java.util.function.Function;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JPA {
	private static final Logger log = LoggerFactory.getLogger(JPA.class);

	private final EntityManagerFactory emf;

	public JPA(EntityManagerFactory emf) {
		this.emf = emf;
	}

	public <T> T withEm(Function<EntityManager, T> action) {
		var em = emf.createEntityManager();
		log.debug("entity manager created");

		try {
			var tx = em.getTransaction();
			try {
				tx.begin();
				log.debug("transaction started");
				
				T result = action.apply(em);
				
				tx.commit();
				log.debug("transaction completed");
				
				return result;
			} catch (Exception ex) {
				tx.rollback();
				log.debug("transaction rolledback");

				throw new RuntimeException("failure: ", ex);
			}
		} finally {
			em.close();
			log.debug("entity manager closed");
		}
	}
	
	public void withEmVoid(Consumer<EntityManager> action) {
		withEm(em -> {
			action.accept(em);
			return null;
		});
	}
}
