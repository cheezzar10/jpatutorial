package com.parallels.pa.rnd.jpa;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

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
		root.setCreated(2016, 10, 1, 9, 0);
		em.persist(root);
		
		Project parent = new Project(GROUP_ID, "parent", VERSION);
		parent.setCreated(2016, 10, 2, 9, 0);
		parent.setParent(root);
		em.persist(parent);
		
		Project webProject = new Project(GROUP_ID, "module-web", VERSION);
		webProject.setParent(parent);
		webProject.setCreated(2016, 10, 3, 9, 0);
		em.persist(webProject);
		
		Project ejbProject = new Project(GROUP_ID, "module-ejb", VERSION);
		ejbProject.setParent(parent);
		ejbProject.setCreated(2016, 10, 4, 9, 0);
		em.persist(ejbProject);
		
		Project actProj = new Project(GROUP_ID, "actuator-project", VERSION);
		actProj.setCreated(2016, 10, 5, 9, 0);
		em.persist(actProj);
		
		Project actCliProj = new Project(GROUP_ID, "actuator-cli", VERSION);
		actCliProj.setParent(actProj);
		actCliProj.setCreated(2016, 10, 6, 9, 0);
		em.persist(actCliProj);
		
		tx.commit();
		em.close();
		
		performProjectFind(webProject.getId());
		listProjectsInChronoOrder();
	}

	private void listProjectsInChronoOrder() {
		EntityManager em = emf.createEntityManager();
		EntityTransaction tx = em.getTransaction();
		tx.begin();
		
		TypedQuery<Project> query = em.createQuery("select p from Project p order by p.created desc", Project.class);
		List<Project> projects = query.getResultList();
		assertEquals(6, projects.size());
		
		Project recentProject = query.setMaxResults(1).getSingleResult();
		boolean mostRecent = projects.stream().allMatch(p -> p.getCreated().compareTo(recentProject.getCreated()) <= 0);
		assertTrue(mostRecent);
		
		tx.commit();
		em.close();
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
