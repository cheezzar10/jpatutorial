package com.parallels.pa.rnd.jpa;

import java.nio.file.Files;
import java.nio.file.Paths;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.hibernate.Session;
import org.hibernate.stat.SessionStatistics;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class ScrapTest {
	private static EntityManagerFactory emf;

	@BeforeClass
	public static void createEntityManagerFactory() {
		emf = Persistence.createEntityManagerFactory("car");
		
		EntityManager em = emf.createEntityManager();
		EntityTransaction tx = em.getTransaction();
		tx.begin();
		
		// engine.dynoGraph ConstraintMode.NO_CONSTRAINT doesn't work
		Query query = em.createNativeQuery("alter table engine drop constraint fk_d9fc0wgwhjxbnn4e1mhyrh9vo");
		query.executeUpdate();
		
		tx.commit();
		em.close();
	}

	@AfterClass
	public static void closeEntityManagerFactory() {
		emf.close();
	}
	
	@Test
	public void testCarPersisting() throws Exception {
		EntityManager em = emf.createEntityManager();
		EntityTransaction tx = em.getTransaction();
		tx.begin();
		
		System.out.println("creating production stat");
		
		Car car = new Car("Alfa Romeo", "Giulia QV");
		Engine engine = new Engine("Ferrari", "F154", 510);
		car.setEngine(engine);
		em.persist(car);
		
		EngineDynoGraph dynoGraph = new EngineDynoGraph(engine.getId());
		dynoGraph.setDynoGraph(Files.readAllBytes(Paths.get("src/main/resources/giulia_qv_dyno.jpg")));
		em.persist(dynoGraph);
		
		engine.setDynoGraph(dynoGraph);
		System.out.printf("new car id: %d%n", car.getId());
		
		TypedQuery<Car> query = em.createQuery("select c from Car c where c.id != :carId", Car.class);
		query.setParameter("carId", car.getId());
		
		System.out.printf("%d car found%n", query.getResultList().size());
		
		Owner owner = new Owner("Alessandro", "Del Piero");
		owner.getCars().add(car);
		car.setOwner(owner);
		owner.getAddresses().add(new Address("Italia", "Milan"));
		
		Garage garage = new Garage("5th Avenue", 2);
		owner.getGarages().add(garage);
		
		em.persist(garage);
		em.persist(owner);
		
		tx.commit();
		em.close();
		
		testLinkedEntitiesManagement(car.getId());
	}
	
	private void testLinkedEntitiesManagement(Integer carId) {
		EntityManager em = emf.createEntityManager();
		EntityTransaction tx = em.getTransaction();
		tx.begin();
		
		Car car = em.find(Car.class, carId);
		Owner owner = car.getOwner();
		
		Garage mosGarage = new Garage("Moscow", 1);
		em.persist(mosGarage);
		// owner.getGarages().add(mosGarage);
		
		for (Garage garage : owner.getGarages()) {
			owner.getGarages().remove(garage);
		}
		
		tx.commit();
		em.close();
	}
	
	@Test
	@Ignore("concenrating on associations")
	public void testProductionStatPersisting() {
		EntityManager em = emf.createEntityManager();
		EntityTransaction tx = em.getTransaction();
		tx.begin();
		
		ProductionStatistics prodStats = new ProductionStatistics(1, 256);
		em.persist(prodStats);
		System.out.println("production statistics saved");
		
		printSessionStat(em, "after persist");
		
		TypedQuery<ProductionStatistics> query = em.createQuery("from ProductionStatistics", ProductionStatistics.class);
		System.out.printf("count: %d%n", query.getResultList().size());
		
		prodStats.setUnitsMade(512);
		
		tx.commit();
		em.close();
	}
	
	private void printSessionStat(EntityManager em, String description) {
		System.out.printf("hibernate session stat: %s%n", description);
		
		Session session = em.unwrap(Session.class);
		SessionStatistics sessionStat = session.getStatistics();
		
		for (Object key : sessionStat.getEntityKeys()) {
			System.out.printf("%s%n", key);
		}
	}
}
