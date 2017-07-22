package com.parallels.pa.rnd.jpa;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.hibernate.Session;
import org.hibernate.cfg.Environment;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.stat.SessionStatistics;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.parallels.pa.rnd.jpa.boot.EntityManagerFactoryEnvironment;
import com.parallels.pa.rnd.jpa.boot.PersistenceUnitDescriptorBuilder;

import org.junit.Assert;

public class ScrapTest {
	private static EntityManagerFactory emf;

	@BeforeClass
	public static void createEntityManagerFactory() {
		PersistenceUnitDescriptorBuilder descriptorBuilder = new PersistenceUnitDescriptorBuilder();
		descriptorBuilder.setPersistentUnitName("car");
		
		descriptorBuilder.addManagedClass(Car.class);
		descriptorBuilder.addManagedClass(Engine.class);
		descriptorBuilder.addManagedClass(Owner.class);
		descriptorBuilder.addManagedClass(Garage.class);
		descriptorBuilder.addManagedClass(Address.class);
		descriptorBuilder.addManagedClass(Country.class);
		descriptorBuilder.addManagedClass(EngineProperty.class);
		descriptorBuilder.addManagedClass(EngineDynoGraph.class);
		descriptorBuilder.addManagedClass(ProductionStatistics.class);
		
		PersistenceUnitDescriptor descriptor = descriptorBuilder.build();
		EntityManagerFactoryBuilder emfBuilder = Bootstrap.getEntityManagerFactoryBuilder(descriptor, EntityManagerFactoryEnvironment.newEnv(
				Environment.DRIVER, "org.postgresql.Driver",
				Environment.URL, "jdbc:postgresql://10.0.1.202/cardb",
				Environment.USER, "cardb",
				Environment.PASS, "1q2w3e",
				Environment.DIALECT, "org.hibernate.dialect.PostgreSQLDialect",
				Environment.HBM2DDL_AUTO, "create",
				Environment.CACHE_REGION_FACTORY, "org.hibernate.cache.ehcache.EhCacheRegionFactory",
				Environment.USE_SECOND_LEVEL_CACHE, "true",
				Environment.USE_QUERY_CACHE, "true",
				Environment.SHOW_SQL, "true"
		));
		
		emf = emfBuilder.build();
		
		EntityManager em = emf.createEntityManager();
		EntityTransaction tx = em.getTransaction();
		tx.begin();
		
		// engine.dynoGraph ConstraintMode.NO_CONSTRAINT doesn't work
		Query query = em.createNativeQuery("alter table engine drop constraint fknwr60acu6ln8s1iqurabqskf8");
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
		
		Country country = new Country("IT", "Italian Republic");
		em.persist(country);
		
		Owner owner = new Owner("Alessandro", "Del Piero");
		owner.getCars().add(car);
		car.setOwner(owner);
		
		Address addr = new Address(country, "Milan");
		em.persist(addr);
		
		owner.getAddresses().add(addr);
		
		Garage garage = new Garage("5th Avenue", 2);
		owner.getGarages().add(garage);
		
		em.persist(garage);
		em.persist(owner);
		
		tx.commit();
		em.close();
		
		createPoorOwner();
		testLinkedEntitiesManagement(car.getId());
	}
	
	private void createPoorOwner() {
		EntityManager em = emf.createEntityManager();
		EntityTransaction tx = em.getTransaction();
		tx.begin();
		
		Owner owner = new Owner("Billy", "Bones");
		em.persist(owner);
		
		tx.commit();
		em.close();
	}

	private void testLinkedEntitiesManagement(Integer carId) {
		EntityManager em = emf.createEntityManager();
		EntityTransaction tx = em.getTransaction();
		tx.begin();
		
		Car car = em.find(Car.class, carId);
		Owner owner = car.getOwner();
		
		Garage mosGarage = new Garage("Moscow", 1);
		em.persist(mosGarage);
		owner.getGarages().add(mosGarage);
		
		// for (Garage garage : owner.getGarages()) owner.getGarages().remove(garage);
		
		System.out.println("loading owner using fetch graph");
		TypedQuery<Owner> query = em.createQuery("select o from Owner o left join fetch o.garages left join fetch o.addresses where o.id = :ownerId", Owner.class);
		query.setHint("javax.persistence.fetchgraph", em.getEntityGraph("Owner.withAddresses"));
		query.setParameter("ownerId", owner.getId());
		
		try {
			Owner fetchedOwner = query.getSingleResult();
			Assert.assertSame(owner, fetchedOwner);
		} catch (NoResultException ownerNotFoundEx) {
			Assert.fail();
		}
		
		tx.commit();
		em.close();
		
		Set<Address> addresses = owner.getAddresses();
		for (Address addr : addresses) {
			Assert.assertNotNull(addr.getCity());
			Assert.assertNotNull(addr.getCountry().getId());
		}
	}
	
	@Test
	@Ignore("concentrating on associations")
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
