package com.parallels.pa.rnd.jpa;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.persistence.Cache;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.LockModeType;
import javax.persistence.NoResultException;
import javax.persistence.Persistence;
import javax.persistence.PersistenceException;
import javax.persistence.PersistenceUnitUtil;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.MapJoin;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class AppTest {
	private static EntityManagerFactory emf;

	@BeforeClass
	public static void createEntityManagerFactory() {
		emf = Persistence.createEntityManagerFactory("car");
		
		EntityManager em = emf.createEntityManager();
		EntityTransaction tx = em.getTransaction();
		tx.begin();
		
		Query query = em.createNativeQuery("alter table engine drop constraint fknwr60acu6ln8s1iqurabqskf8");
		query.executeUpdate();
		
		tx.commit();
		em.close();
	}

	@AfterClass
	public static void closeEntityManagerFactory() {
		emf.close();
	}
	
	private byte[] generateRandomBytes(EntityManager em, int size) {
		Random rand = new Random();
		byte[] bytes = new byte[size];
		rand.nextBytes(bytes);
		return bytes;
	}

	@Test
	public void testCreateCars() {
		EntityManager em = emf.createEntityManager();
		EntityTransaction tx = em.getTransaction();
		tx.begin();

		Car car = new Car("BMW", "320d");
		Map<String, String> options = new HashMap<>();
		options.put("interiour trim", "aluminium");
		options.put("rims style", "double spoke");
		car.setOptions(options);

		Engine engine = new Engine("BMW", "N47", 184);
		car.setEngine(engine);
		em.persist(car);
		
		EngineDynoGraph dynoGraph = new EngineDynoGraph(engine.getId());
		dynoGraph.setDynoGraph(generateRandomBytes(em, 1024 * 16));
		em.persist(dynoGraph);
		
		engine.setDynoGraph(dynoGraph);
		
		System.out.printf("Car id: %d%n", car.getId());

		String[] opts = { "interiour trim" };
		// TypedQuery<String> query = em.createQuery("select o from Car c join
		// c.options o where c.maker = :maker and c.model = :model and index(o)
		// in ('interiour trim')", String.class);

		// using map join MapJoin<Car, String, String> options =
		// root.joinMap("options") criteriaQuery.multiselect(options.key(),
		// options.value())
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Object[]> cq = cb.createQuery(Object[].class);
		Root<Car> car1 = cq.from(Car.class);
		MapJoin<Car, String, String> options1 = car1.joinMap("options");
		cq.multiselect(options1.value(), options1.value());
		cq.where(cb.equal(car1.get("maker"), cb.parameter(String.class, "maker")),
				cb.equal(car1.get("model"), cb.parameter(String.class, "model")), options1.key().in((Object[])opts));

		TypedQuery<Object[]> query = em.createQuery(cq);
		query.setParameter("maker", "BMW");
		query.setParameter("model", "320d");

		List<Object[]> result = query.getResultList();
		assertEquals("Incorrect number of options", 1, result.size());
		assertEquals("Incorrect option value", "aluminium", result.get(0)[1]);

		tx.commit();
		em.close();

		testRemoveEngine();
		testCarArchival();
		testLoadCars(new Integer[] { car.getId() });
		testLoadCarUsingNativeQuery(car.getId());
		testAdHocNativeQueries();
	}

	@Test
	public void testFindCarsWithEngines() {
		EntityManager em = emf.createEntityManager();
		EntityTransaction tx = em.getTransaction();
		tx.begin();

		Integer[] carIds = { createCarWithEngine(em, "BMW", "530d", "BMW", "N57D30O1", 258),
				createCarWithEngine(em, "BMW", "535d", "BMW", "N57D30T1", 313) };
		System.out.printf("car ids: %s%n", Arrays.toString(carIds));

		Car car = em.find(Car.class, carIds[1]);

		// sharing engine between cars
		Car newCar = new Car("BMW", "335d");
		newCar.setEngine(car.getEngine());
		em.persist(newCar);

		tx.commit();
		em.close();

		testLoadCars(carIds);
		testFindCarsWithEngine();
		testFindEnginesWithPowerInRange(220, 350);
		testFindCarsWithUniqueEngine(carIds[1]);
		testCountEngineMakersQuery();
		testSelectAllObjects();

		System.out.println("test cars with engines test completed");
	}
	
	private void testSelectAllObjects() {
		EntityManager em = emf.createEntityManager();
		EntityTransaction tx = em.getTransaction();
		tx.begin();
		
		Query query = em.createQuery("from java.io.Serializable");
		List<?> objects = query.getResultList();
		System.out.printf("objects count = %d%n", objects.size());
		
		tx.commit();
		em.close();
	}

	private void testCountEngineMakersQuery() {
		EntityManager em = emf.createEntityManager();
		EntityTransaction tx = em.getTransaction();
		tx.begin();

		TypedQuery<Object[]> query = em.createQuery("select e.maker, count(e.maker) from Engine e group by e.maker",
				Object[].class);
		List<Object[]> stats = query.getResultList();
		for (Object[] makerStat : stats) {
			assertEquals("BMW", makerStat[0]);
			assertTrue(((Long) makerStat[1]) > 0);
		}

		tx.commit();
		em.close();
	}

	private void testFindCarsWithUniqueEngine(Integer carId) {
		EntityManager em = emf.createEntityManager();
		EntityTransaction tx = em.getTransaction();
		tx.begin();

		TypedQuery<Car> query = em.createQuery(
				"select c from Car c where c.id = :carId and not exists (select ic from Car ic where ic.id <> :carId and ic.engine = c.engine)",
				Car.class);
		query.setParameter("carId", carId);
		List<Car> cars = query.getResultList();
		assertTrue(cars.isEmpty());

		tx.commit();
		em.close();
	}

	private void testFindCarsWithEngine() {
		EntityManager em = emf.createEntityManager();
		EntityTransaction tx = em.getTransaction();
		tx.begin();

		TypedQuery<Car> query = em.createQuery(
				"select c from Car c where c.engine.id = ("
						+ " select e.id from Engine e where e.maker = :maker and e.model = :model and e.diesel = true)",
				Car.class);
		query.setParameter("maker", "BM");
		query.setParameter("model", "N57D30O1");
		query.getResultList();

		tx.commit();
		em.close();
	}

	private void testFindEnginesWithPowerInRange(int powerMin, int powerMax) {
		System.out.printf("searching for engines with power in range %d-%d%n", powerMin, powerMax);

		EntityManager em = emf.createEntityManager();
		EntityTransaction tx = em.getTransaction();
		tx.begin();

		TypedQuery<Engine> query = em
				.createQuery("select e from Engine e where e.power between :powerMin and :powerMax and e.diesel = true "
						+ " or e.power between :powerMin and :powerMax and e.diesel = false", Engine.class);

		query.setParameter("powerMin", powerMin);
		query.setParameter("powerMax", powerMax);

		List<Engine> engines = query.getResultList();
		System.out.printf("%d engines found%n", engines.size());

		tx.commit();
		em.close();
	}

	private void testLoadCars(Integer[] carIds) {
		EntityManager em = emf.createEntityManager();
		EntityTransaction tx = em.getTransaction();
		tx.begin();

		Cache cache = em.getEntityManagerFactory().getCache();
		if (cache.contains(Car.class, carIds[0])) {
			System.out.printf("Car#%d is cached.%n", carIds[0]);
		} else {
			System.out.printf("Car#%d is not cached.%n", carIds[0]);
		}

		System.out.println("test cars with engines test completed");

		// select c.engine.id from Car c where c.id = :carId
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Object[]> cq = cb.createQuery(Object[].class);
		Root<Car> car = cq.from(Car.class);
		Join<Car, Engine> engine = car.join("engine");
		// cq.select(car.<Engine>get("engine").<Integer>get("id"));
		cq.multiselect(car, engine.get("id"));
		cq.where(car.get("id").in((Object[])carIds));
		TypedQuery<Object[]> query = em.createQuery(cq);

		List<Object[]> carsAndEngineIds = query.getResultList();

		assertEquals("incorrect amount of used engines", carIds.length, carsAndEngineIds.size());
		for (Object[] carAndEngineId : carsAndEngineIds) {
			System.out.println("persisting already persisted car");
			em.persist(carAndEngineId[0]);
			System.out.printf("engine id: %d%n", carAndEngineId[1]);
			// ((Car)carAndEngineId[0]).getModel();
		}

		tx.commit();
		em.close();
	}

	private void testLoadCarUsingNativeQuery(Integer carId) {
		EntityManager em = emf.createEntityManager();
		EntityTransaction tx = em.getTransaction();
		tx.begin();

		Query query = em.createNativeQuery(
				"select c.id as car_id, c.maker, c.model, c.engine_id, c.owner_id as car_owner_id, c.bodystyle, o.id as owner_id, o.first_name, o.last_name from car c left join owner o on o.id = c.owner_id where c.id = :carId",
				"Car.carAndOwner");
		query.setParameter("carId", carId);
		Object[] result = (Object[]) query.getSingleResult();
		Car car = (Car) result[0];
		Owner owner = (Owner) result[1];

		assertEquals(carId, car.getId());
		assertEquals("320d", car.getModel());
		car.setModel("321d");
		assertNull(owner);

		tx.commit();
		em.close();
	}

	private void testAdHocNativeQueries() {
		EntityManager em = emf.createEntityManager();
		EntityTransaction tx = em.getTransaction();
		tx.begin();

		Query query = em.createNativeQuery("select id as car_id from car");

		@SuppressWarnings("unchecked")
		List<Integer> result = (List<Integer>) query.getResultList();
		
		Integer maxId = 0;
		for (Integer row : result) {
			Integer col = row;
			maxId = Math.max(col, maxId);
		}

		tx.commit();
		em.close();
	}

	private Integer createCarWithEngine(EntityManager em, String carMaker, String carModel, String engineMaker,
			String engineModel, int power) {
		Car car = new Car(carMaker, carModel);
		Engine engine = new Engine(engineMaker, engineModel, power);
		car.setEngine(engine);
		em.persist(car);
		
		EngineDynoGraph dynoGraph = new EngineDynoGraph(engine.getId());
		dynoGraph.setDynoGraph(generateRandomBytes(em, 32 * 1024));
		em.persist(dynoGraph);
		
		engine.setDynoGraph(dynoGraph);
		
		return car.getId();
	}

	@Test
	public void testFindCars() {
		EntityManager em = emf.createEntityManager();
		EntityTransaction tx = em.getTransaction();
		tx.begin();

		TypedQuery<Car> query = em.createQuery("from Car c where c.model = :model", Car.class);
		query.setParameter("model", "320d");

		List<Car> cars = query.getResultList();
		System.out.printf("Cars found %d%n", cars.size());

		tx.commit();
		em.close();
	}

	@Test
	public void testGetCarsCount() {
		EntityManager em = emf.createEntityManager();
		EntityTransaction tx = em.getTransaction();
		tx.begin();

		TypedQuery<Long> query = em.createQuery("select count(c) from Car c", Long.class);

		System.out.printf("total number of cars: %d%n", query.getSingleResult());

		tx.commit();
		em.close();
	}

	@Test
	public void testCreateCarWithProductinStatistics() {
		EntityManager em = emf.createEntityManager();
		EntityTransaction tx = em.getTransaction();

		tx.begin();

		Car car = new Car("BMW", "335i");
		Engine engine = new Engine("BMW", "N55", 306);
		engine.setDiesel(true);
		car.setEngine(engine);
		em.persist(car);
		
		EngineDynoGraph dynoGraph = new EngineDynoGraph(engine.getId());
		dynoGraph.setDynoGraph(generateRandomBytes(em, 8 * 1024));
		em.persist(dynoGraph);
		
		engine.setDynoGraph(dynoGraph);
		
		System.out.printf("Car id: %s%n", car.getId());

		ProductionStatistics prodStats = new ProductionStatistics(1000);
		car.setProductionStats(prodStats);
		System.out.println("persisting production statistics");
		em.persist(prodStats);

		em.flush();

		TypedQuery<Car> carByModelQuery = em.createQuery("from Car c where c.model = :model", Car.class);
		carByModelQuery.setParameter("model", "335i");

		List<Car> cars = carByModelQuery.getResultList();
		System.out.printf("Cars found %d%n", cars.size());

		for (Car loadedCar : cars) {
			System.out.printf("Loaded car %d model: %s%n", loadedCar.getId(), loadedCar.getModel());
			ProductionStatistics loadedProdStats = loadedCar.getProductionStats();
			System.out.printf("Total produced %d%n", loadedProdStats.getUnitsMade());
		}

		TypedQuery<ProductionStatistics> prodStatsQuery = em.createQuery(
				"from ProductionStatistics ps where ps.unitsMade = :unitsMade", ProductionStatistics.class);
		prodStatsQuery.setParameter("unitsMade", 1000);

		for (ProductionStatistics loadedProdStats : prodStatsQuery.getResultList()) {
			System.out.printf("Units made: %d car: %d%n", loadedProdStats.getUnitsMade(), loadedProdStats.getId());
		}

		// car.setProductionStats(null);
		em.remove(prodStats);

		// em.persist(prodStats);

		Plant plant = new Plant();
		plant.setId(1);
		plant.setName("Leipzig");
		em.persist(plant);

		tx.commit();
		em.close();

		// Cache cache = emf.getCache();
		// cache.evictAll();

		tryingToPersistDetachedPlant(emf, plant);
		tryToMergeTransientPlant(emf);
	}

	private void tryingToPersistDetachedPlant(EntityManagerFactory emf, Plant plant) {
		EntityManager em = emf.createEntityManager();
		EntityTransaction tx = em.getTransaction();
		tx.begin();

		// fixing id to persist detached entity one more time
		plant.setId(2);
		em.persist(plant);

		tx.commit();
		em.close();
	}
	
	private void tryToMergeTransientPlant(EntityManagerFactory emf) {
		EntityManager em = emf.createEntityManager();
		EntityTransaction tx = em.getTransaction();
		tx.begin();

		Plant plant = new Plant();
		plant.setId(1);
		plant.setName("Dingolfing");
		em.merge(plant);

		tx.commit();
		em.close();
	}

	private void testRemoveEngine() {
		EntityManager em = emf.createEntityManager();
		EntityTransaction tx = em.getTransaction();

		tx.begin();

		TypedQuery<Engine> query = em.createQuery("from Engine e where e.maker = :maker and e.model = :model",
				Engine.class);
		query.setParameter("maker", "BMW");
		query.setParameter("model", "N47");
		Engine engine = query.getSingleResult();

		try {
			System.out.printf("removing engine #%d%n", engine.getId());
			// check constraints
			em.remove(engine);

			assertFalse(em.contains(engine));

			em.flush();
			tx.commit();
		} catch (PersistenceException pEx) {
			System.out.println("Catched");
			if (pEx.getCause() instanceof ConstraintViolationException) {
				ConstraintViolationException cvEx = (ConstraintViolationException) pEx.getCause();
				String message = cvEx.getMessage();
				System.out.printf("JDBC exception message: %s %n", message);
				if (message.contains("unique")) {
					System.out.println("Unique constraint violation");
				} else if (message.contains("foreign key")) {
					System.out.println("Foreign key constraint violation");
				}
			}
			tx.rollback();
		}

		em.close();
	}

	private void testCarArchival() {
		EntityManager em = emf.createEntityManager();
		EntityTransaction tx = em.getTransaction();
		tx.begin();

		Session session = (Session) em.getDelegate();

		// required for H2 database only
		// session.createSQLQuery("create table removed_car (id integer, maker
		// varchar(32))").executeUpdate();

		SQLQuery query = session.createSQLQuery("insert into removed_car select id, maker from car where id = ?");
		System.out.printf("sql query class: %s%n", query.getClass());
		// query.addSynchronizedQuerySpace("sql");
		// Query query = em.createNativeQuery("insert into removed_car select
		// id, maker from car where id = ?");
		query.setParameter(1, 1);
		query.executeUpdate();

		tx.commit();
		em.close();
	}

	@Test
	public void testSetEngineProperties() {
		Runtime runtime = Runtime.getRuntime();
		System.out.printf("mem (before): total = %d free = %d%n", runtime.totalMemory(), runtime.freeMemory());
		
		EntityManager em = emf.createEntityManager();
		EntityTransaction tx = em.getTransaction();
		tx.begin();

		Engine engine = new Engine("BMW", "N54", 302);
		engine.addProperty("type", "four stroke bi-turbo");
		engine.addProperty("engine block", "aluminium with cast iron liners");
		engine.addProperty("cylinder head", "dual camshaft 4-valves per cylynder");
		engine.addProperty("displacement", "2979 cc");
		engine.addProperty("redline", "7000");
		engine.addProperty("compression ratio", "10.2:1");
		engine.addProperty("bore", "84 mm");
		engine.addProperty("stroke", "89.6 mm");
		engine.addProperty("production start", "2007");
		engine.addProperty("production end", "2013");
		engine.addProperty("weight", "195 kg");
		engine.addProperty("low powerband", "1500");
		engine.addProperty("high powerband", "4500");

		em.persist(engine);
		
		EngineDynoGraph dynoGraph = new EngineDynoGraph(engine.getId());
		dynoGraph.setDynoGraph(generateRandomBytes(em, 1024 * 1024));
		em.persist(dynoGraph);
		
		engine.setDynoGraph(dynoGraph);

		for (Iterator<EngineProperty> enginePropsIter = engine.getProperties().iterator(); enginePropsIter.hasNext();) {
			EngineProperty engineProp = enginePropsIter.next();
			EnginePropertyChangeRec changeRec = new EnginePropertyChangeRec(engineProp);
			em.persist(changeRec);
			enginePropsIter.remove();
		}

		tx.commit();
		em.close();

		testGetEnginePropertiesChangeRecs(engine.getId());
		testGetEnginePropertyUsingQuery(engine.getId());
		testGetEnginePropertyUsingCriteriaAPI();
		testGetEnginePropertyByEngineAndName(engine.getId(), "type");
		testGetEngineWithFilteredProps(engine.getId());
		// testRemoveEngineWithFilteredProperties(engine.getId());
		
		System.out.printf("mem (after): total = %d free = %d%n", runtime.totalMemory(), runtime.freeMemory());
	}

	private void testGetEngineWithFilteredProps(Integer engineId) {
		EntityManager em = emf.createEntityManager();
		EntityTransaction tx = em.getTransaction();
		tx.begin();
		
		TypedQuery<Engine> query = em.createQuery("select distinct e from Engine e left join fetch e.properties ep where e.id = :engineId", Engine.class);
		query.setParameter("engineId", engineId);
		
		List<Engine> engines = query.getResultList();
		assertTrue(engines.size() == 1);
		Engine engine = engines.iterator().next();
		em.detach(engine);
		for (Iterator<EngineProperty> enginePropsIter = engine.getProperties().iterator(); enginePropsIter.hasNext();) {
			EngineProperty prop = enginePropsIter.next();
			if (prop.getName().equals("type")) {
				enginePropsIter.remove();
			}
		}
		
		assertEquals(12, engine.getProperties().size());
		// em.clear();
		// em.refresh(engine);
		assertEquals(13, query.getSingleResult().getProperties().size());
		
		tx.commit();
		em.close();
	}

	private void testGetEnginePropertyByEngineAndName(Integer engineId, String name) {
		EntityManager em = emf.createEntityManager();
		EntityTransaction tx = em.getTransaction();
		tx.begin();

		Engine engine = em.find(Engine.class, engineId);
		EngineDynoGraph dynoGraph = engine.getDynoGraph();
		
		System.out.printf("dyno graph size = %d%n", dynoGraph.getDynoGraph().length);
		
		Engine engineProxy = em.getReference(Engine.class, engineId);

		System.out.printf("engine proxy class: %s%n", engineProxy.getClass());

		System.out.printf("loading engine #%d property %s%n", engineId, name);

		EngineProperty engineProp = em.find(EngineProperty.class, new EnginePropertyId(engine.getId(), name));
		assertNotNull(engineProp);

		tx.commit();
		em.close();
	}

	private void testGetEnginePropertiesChangeRecs(Integer engineId) {
		EntityManager em = emf.createEntityManager();
		EntityTransaction tx = em.getTransaction();
		tx.begin();

		TypedQuery<EnginePropertyChangeRec> query = em.createQuery(
				"select epcr from EnginePropertyChangeRec epcr join epcr.property ep join ep.engine e where e.id = :engineId",
				EnginePropertyChangeRec.class);
		query.setParameter("engineId", engineId);
		List<EnginePropertyChangeRec> recs = query.getResultList();

		EnginePropertyChangeRecId firstRecId = null;
		for (EnginePropertyChangeRec rec : recs) {
			EntityManagerFactory emFactory = em.getEntityManagerFactory();
			PersistenceUnitUtil pu = emFactory.getPersistenceUnitUtil();
			// firstRecId = (EnginePropertyChangeRecId)pu.getIdentifier(rec);
			break;
		}

		// em.find(EnginePropertyChangeRec.class, firstRecId);

		assertEquals(13, recs.size());

		tx.commit();
		em.close();
	}

	private void testGetEnginePropertyUsingQuery(Integer engineId) {
		EntityManager em = emf.createEntityManager();
		EntityTransaction tx = em.getTransaction();
		tx.begin();

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<EngineProperty> cq = cb.createQuery(EngineProperty.class);
		Root<EngineProperty> engineProp = cq.from(EngineProperty.class);
		engineProp.fetch("engine");
		cq.select(engineProp);
		Predicate p = cb.conjunction();
		p = cb.and(p, engineProp.get("engine").get("id").in(new Object[] { engineId }));
		p = cb.and(p, engineProp.get("name").in(new Object[] { "engine block", "type" }));
		cq.where(p);

		TypedQuery<EngineProperty> query = em.createQuery(cq);
		List<EngineProperty> result = query.getResultList();

		assertEquals("Incorrect number of rows", 2, result.size());
		assertEquals("Incorrect model", "N54", ((EngineProperty) result.get(0)).getEngine().getModel());

		tx.commit();
		em.close();
	}

	private void testGetEnginePropertyUsingCriteriaAPI() {
		EntityManager em = emf.createEntityManager();
		EntityTransaction tx = em.getTransaction();
		tx.begin();

		// select ep.name, ep.value from Engine e join e.properties ep where
		// e.maker = :maker and e.model = :model and ep.name in (...)

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<EngineProperty> cq = cb.createQuery(EngineProperty.class);
		Root<Engine> engine = cq.from(Engine.class);
		Join<Engine, EngineProperty> properties = engine.join("properties");
		cq.select(properties);
		Predicate p = cb.conjunction();
		p = cb.and(p, cb.equal(engine.get("maker"), cb.parameter(String.class, "maker")));
		p = cb.and(p, cb.equal(engine.get("model"), cb.parameter(String.class, "model")));
		p = cb.and(p, properties.get("name").in(new Object[] { "engine block" }));
		cq.where(p);

		TypedQuery<EngineProperty> query = em.createQuery(cq);
		query.setParameter("maker", "BMW");
		query.setParameter("model", "N54");

		List<EngineProperty> result = query.getResultList();
		assertEquals("Incorrect number of properties", 1, result.size());
		assertEquals("Incorrect property value", "aluminium with cast iron liners", result.get(0).getValue());

		tx.commit();
		em.close();
	}

	private void testRemoveEngineWithFilteredProperties(Integer engineId) {
		EntityManager em = emf.createEntityManager();
		EntityTransaction tx = em.getTransaction();
		tx.begin();

		Engine engine = em.find(Engine.class, engineId);
		System.out.printf("engine %s loaded%n", engine);
		List<EngineProperty> engineProps = engine.getProperties();
		for (Iterator<EngineProperty> enginePropsIter = engineProps.iterator(); enginePropsIter.hasNext();) {
			EngineProperty prop = enginePropsIter.next();
			if (prop.getName().equals("type")) {
				System.out.println("property 'type' removed");
				enginePropsIter.remove();
			}
		}

		System.out.printf("removing engine with id: %d%n", engineId);
		if (em.contains(engine)) {
			System.out.printf("engine %s already loaded%n", engine);
			em.detach(engine);
			engine = em.find(Engine.class, engineId);
			System.out.printf("engine %s reloaded%n", engine);
		}

		em.remove(engine);
		em.flush();

		tx.commit();
		em.close();
	}

	@Test
	public void testCreateOwnerWithCars() {
		EntityManager em = emf.createEntityManager();
		EntityTransaction tx = em.getTransaction();
		tx.begin();

		Owner owner = new Owner("Billy", "Bones");
		Integer[] carIds = { createCarWithEngine(em, "BMW", "M3", "BMW", "S55B30", 421),
				createCarWithEngine(em, "BMW", "M5", "BMW", "S63B44T0", 560) };

		for (Integer carId : carIds) {
			Car car = em.find(Car.class, carId);
			car.setOwner(owner);
		}

		em.persist(owner);

		Integer carId = createCarWithEngine(em, "BMW", "1M Coupe", "BMW", "N54B30TO", 340);

		em.flush();

		Car car = em.find(Car.class, carIds[1]);
		assertNotNull(car);

		tx.commit();
		em.close();

		tryToLoadOwner(owner.getId(), carId);
		tryToLoadCarAndAccessEngine(carId);
		tryToRemoveCarsOwnerBy(owner, carIds[0]);
	}

	private void tryToLoadOwner(Integer ownerId, Integer carId) {
		EntityManager em = emf.createEntityManager();
		EntityTransaction tx = em.getTransaction();
		tx.begin();
		
		System.out.printf("loading owner #%d%n", ownerId);
		// TypedQuery<Owner> query = em.createQuery("select o from Owner o join o.cars c where o.id = :id", Owner.class);
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Owner> cq = cb.createQuery(Owner.class);
		Root<Owner> root = cq.from(Owner.class);
		Join<Owner, Car> carJoin = root.join("cars");
		cq.select(root);
		cq.where(cb.equal(root.get("id"), cb.parameter(Integer.class, "id")));
		TypedQuery<Owner> query = em.createQuery(cq);
		
		query.setParameter("id", ownerId);
		// query.setHint("org.hibernate.readOnly", Boolean.TRUE.toString());
		
		int rowsCount = 0;
		for (Owner row : query.getResultList()) {
			assertEquals(2, row.getCars().size());
			rowsCount++;
		}
		assertEquals(2, rowsCount);
		assertEquals(1, new LinkedHashSet<>(query.getResultList()).size());

		Car car = em.find(Car.class, carId);

		Owner owner = em.find(Owner.class, ownerId);
		car.setOwner(owner);
		owner.getCars().add(car);

		tx.commit();
		em.close();
	}

	private void tryToLoadCarAndAccessEngine(Integer carId) {
		EntityManager em = emf.createEntityManager();

		System.out.println("loading car engine id");

		TypedQuery<Object[]> query = em.createQuery("select c.id, c.engine.id from Car c where c.id = :carId",
				Object[].class);
		query.setParameter("carId", carId);
		Object[] result = query.getSingleResult();

		em.close();

		// Engine engine = car.getEngine();
		// Integer engineId = engine.getId();

		System.out.println("car engine id loading completed");
	}

	private void tryToRemoveCarsOwnerBy(Owner owner, Integer carId) {
		EntityManager em = emf.createEntityManager();
		EntityTransaction tx = em.getTransaction();
		tx.begin();

		Car car = em.find(Car.class, carId);

		Query query = em.createQuery("delete Car c where c.owner = :owner");
		query.setParameter("owner", owner);
		query.executeUpdate();

		// em.refresh(car, LockModeType.PESSIMISTIC_WRITE);
		// em.detach(car);

		TypedQuery<Car> checkQuery = em.createQuery("from Car c where c.id = :cid", Car.class);
		// checkQuery.setLockMode(LockModeType.PESSIMISTIC_WRITE);
		checkQuery.setParameter("cid", car.getId());

		try {
			Car car1 = checkQuery.getSingleResult();
		} catch (NoResultException noResEx) {
			System.out.println("car not found");
		}

		// car = em.find(Car.class, carId, LockModeType.PESSIMISTIC_WRITE);
		// assertNull(car);

		em.clear();

		assertFalse(em.contains(car));

		car = em.find(Car.class, carId, LockModeType.PESSIMISTIC_WRITE);
		assertNull(car);

		tx.commit();
		em.close();
	}

	@Test
	public void testCreateOwnerWithGarages() {
		EntityManager em = emf.createEntityManager();
		EntityTransaction tx = em.getTransaction();
		tx.begin();

		Owner owner = new Owner("Blind", "Pew");
		Garage garage = new Garage("Treasure Island", 1);
		owner.getGarages().add(garage);

		em.persist(owner);
		em.persist(garage);

		tx.commit();
		em.close();

		tryToAddGarageToOwner(owner.getId());
	}

	public void tryToAddGarageToOwner(Integer ownerId) {
		EntityManager em = emf.createEntityManager();
		EntityTransaction tx = em.getTransaction();
		tx.begin();

		TypedQuery<Owner> query = em.createQuery("from Owner o where o.id = :id", Owner.class);
		query.setParameter("id", ownerId);
		query.setHint("org.hibernate.readOnly", "true");
		Owner owner = query.getSingleResult();

		owner.setFirstName("sailor");
		em.flush();
		// em.detach(owner);

		Garage garage = new Garage("Carribean Sea", 2);

		owner.setGarages(new HashSet<>(Arrays.asList(garage)));
		em.persist(garage);

		garage = new Garage("Pacific Ocean", 3);
		owner.getGarages().add(garage);
		em.persist(garage);

		owner = em.find(Owner.class, ownerId);
		owner.getGarages().add(garage);

		tx.commit();
		em.close();
	}
}
