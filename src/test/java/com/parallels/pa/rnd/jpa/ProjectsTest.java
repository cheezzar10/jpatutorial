package com.parallels.pa.rnd.jpa;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class ProjectsTest {
	private static final String GROUP_ID = "com.odin";
	private static final String VERSION = "1.0";
	
	private static EntityManagerFactory emf;

	@BeforeClass
	public static void createEntityManagerFactory() {
		emf = Persistence.createEntityManagerFactory("car");
	}

	@AfterClass
	public static void closeEntityManagerFactory() {
		emf.close();
	}
	
	@Test
	public void testProjectOperations() {
		EntityManager em = emf.createEntityManager();
		EntityTransaction tx = em.getTransaction();
		tx.begin();
		
		Project root = new Project(GROUP_ID, "root", VERSION);
		em.persist(root);
		
		Project parent = new Project(GROUP_ID, "parent", VERSION);
		parent.setParent(root);
		em.persist(parent);
		
		Project webProject = new Project(GROUP_ID, "module-web", VERSION);
		webProject.setParent(parent);
		em.persist(webProject);
		
		Project ejbProject = new Project(GROUP_ID, "module-ejb", VERSION);
		ejbProject.setParent(parent);
		em.persist(ejbProject);
		
		Project actProj = new Project(GROUP_ID, "actuator-project", VERSION);
		em.persist(actProj);
		
		Project actCliProj = new Project(GROUP_ID, "actuator-cli", VERSION);
		actCliProj.setParent(actProj);
		em.persist(actCliProj);
		
		tx.commit();
		em.close();
		
		performProjectFind(webProject.getId());
	}

	private void performProjectFind(Integer id) {
		EntityManager em = emf.createEntityManager();
		EntityTransaction tx = em.getTransaction();
		tx.begin();
		
		Project project = em.find(Project.class, id);
		System.out.println(project.toString());
		
		tx.commit();
		em.close();
	}
}
