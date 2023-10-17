package com.parallels.pa.rnd.jpa;

import static org.junit.jupiter.api.Assertions.*;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.Cache;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.PersistenceException;
import javax.persistence.PersistenceUnitUtil;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.MapJoin;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.parallels.pa.rnd.jpa.util.JPA;
import com.parallels.pa.rnd.jpa.util.RND;

@Testcontainers
public class AppTest {
	@Container
	private static PostgreSQLContainer databaseContainer = (PostgreSQLContainer) new PostgreSQLContainer("postgres:11")
			.withInitScript("init.sql");

	private static EntityManagerFactory emf;
	
	private static JPA jpa;
	
	private static RND rnd = new RND();

	@BeforeAll
	public static void createEntityManagerFactory() {
		emf = Persistence.createEntityManagerFactory(
				"car",
				Map.of(
						"javax.persistence.jdbc.url",
						databaseContainer.getJdbcUrl(),
						"javax.persistence.jdbc.user",
						databaseContainer.getUsername(),
						"javax.persistence.jdbc.password",
						databaseContainer.getPassword()));
		
		jpa = new JPA(emf);
	}

	@AfterAll
	public static void closeEntityManagerFactory() {
		emf.close();
	}

	@Test
	public void testCreateCars() {
		System.out.println("create cars test started");
		
		var newCar = jpa.withEm(em -> {
			Car car = new Car("BMW", "320d");

			Map<String, String> options = new HashMap<>();
			options.put("interiour trim", "aluminium");
			options.put("rims style", "double spoke");
			car.setOptions(options);

			Engine engine = new Engine("BMW", "N47", 184);
			car.setEngine(engine);
			engine.setDynoGraph(rnd.randomBytes(1024 * 16));
			em.persist(car);
			
			System.out.printf("Car id: %d%n", car.getId());

			Object[] opts = { "interiour trim" };
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
					cb.equal(car1.get("model"), cb.parameter(String.class, "model")), options1.key().in(opts));

			TypedQuery<Object[]> query = em.createQuery(cq);
			query.setParameter("maker", "BMW");
			query.setParameter("model", "320d");

			List<Object[]> result = query.getResultList();
			assertEquals(1, result.size(), "Incorrect number of options");
			assertEquals("aluminium", result.get(0)[1], "Incorrect option value");
			
			return car;
		});

//		testRemoveEngine();
		testCarArchival();
		testLoadCars(new Integer[] { newCar.getId() });
		testLoadCars(new Integer[] { 2 } );
		testLoadCarUsingNativeQuery(newCar.getId());
		testCarRemovalAndCreationUsingAssignedId(newCar.getId());
		testAdHocNativeQueries();

		System.out.println("create cars test completed");
	}

	private void testCarRemovalAndCreationUsingAssignedId(Integer carId) {
		jpa.withEmVoid(em -> {
			Car car = em.find(Car.class, carId);
			em.remove(car);

//			Car newCar = new Car(carId);
//			newCar.setMaker(car.getMaker());
//			newCar.setModel(car.getModel());
//			em.persist(newCar);
		});
	}

	@Test
	public void testFindCarsWithEngines() {
		System.out.println(">>>>> find cars with engines test started >>>>>");

		var newCarIds = jpa.withEm(em -> {
			Integer[] carIds = { createCarWithEngine(em, "BMW", "530d", "BMW", "N57D30O1", 258),
					createCarWithEngine(em, "BMW", "535d", "BMW", "N57D30T1", 313) };
			System.out.printf("car ids: %s%n", Arrays.toString(carIds));

			em.find(Car.class, carIds[0]);
			Car car = em.find(Car.class, carIds[1]);

			// sharing engine between cars
			Car newCar = new Car("BMW", "335d");
			newCar.setEngine(car.getEngine());
			em.persist(newCar);
			
			return carIds;
		});

		testLoadCars(newCarIds);
		testFindCarsWithEngine();
		testFindEnginesWithPowerInRange(220, 350);
		testFindCarsWithUniqueEngine(newCarIds[1]);
		testCountEngineMakersQuery();

		System.out.println("<<<<< find cars with engines test completed <<<<<");
	}

	private void testCountEngineMakersQuery() {
		jpa.withEmVoid(em -> {
			TypedQuery<Object[]> query = em.createQuery("select e.maker, count(e.maker) from Engine e group by e.maker",
					Object[].class);
			List<Object[]> stats = query.getResultList();
			for (Object[] makerStat : stats) {
				assertEquals("BMW", makerStat[0]);
				assertTrue(((Long) makerStat[1]) > 0);
			}
		});
	}

	private void testFindCarsWithUniqueEngine(Integer carId) {
		jpa.withEmVoid(em -> {
			TypedQuery<Car> query = em.createQuery(
					"select c from Car c where c.id = :carId and not exists (select ic from Car ic where ic.id <> :carId and ic.engine = c.engine)",
					Car.class);
			query.setParameter("carId", carId);
			List<Car> cars = query.getResultList();
			assertTrue(cars.isEmpty());
		});
	}

	private void testFindCarsWithEngine() {
		jpa.withEmVoid(em -> {
			TypedQuery<Car> query = em.createQuery(
					"select c from Car c where c.engine.id = ("
							+ " select e.id from Engine e where e.maker = :maker and e.model = :model and e.diesel = true)",
					Car.class);
			query.setParameter("maker", "BM");
			query.setParameter("model", "N57D30O1");
			query.getResultList();
		});
	}

	private void testFindEnginesWithPowerInRange(int powerMin, int powerMax) {
		System.out.printf("searching for engines with power in range %d-%d%n", powerMin, powerMax);
		
		jpa.withEmVoid(em -> {
			TypedQuery<Engine> query = em
					.createQuery("select e from Engine e where e.power between :powerMin and :powerMax and e.diesel = true "
							+ " or e.power between :powerMin and :powerMax and e.diesel = false", Engine.class);

			query.setParameter("powerMin", powerMin);
			query.setParameter("powerMax", powerMax);

			List<Engine> engines = query.getResultList();
			System.out.printf("%d engines found%n", engines.size());
		});
	}

	private void testLoadCars(Integer[] carIds) {
		jpa.withEmVoid(em -> {
			Cache cache = em.getEntityManagerFactory().getCache();
			Integer carId = carIds[0];
			if (cache.contains(Car.class, carId)) {
				System.out.printf("Car#%d is cached.%n", carId);
			} else {
				System.out.printf("Car#%d is not cached.%n", carId);
			}

			Car loadedCar = em.find(Car.class, carId);
			System.out.printf("car with id %d loaded: %s%n", carId, loadedCar);

			// select c.engine.id from Car c where c.id = :carId
			CriteriaBuilder cb = em.getCriteriaBuilder();
			CriteriaQuery<Object[]> cq = cb.createQuery(Object[].class);
			Root<Car> car = cq.from(Car.class);
			Join<Car, Engine> engine = car.join("engine");
			// cq.select(car.<Engine>get("engine").<Integer>get("id"));
			cq.multiselect(car, engine.get("id"));
			cq.where(car.get("id").in(carIds));
			TypedQuery<Object[]> query = em.createQuery(cq);

			List<Object[]> carsAndEngineIds = query.getResultList();

			assertEquals(carIds.length, carsAndEngineIds.size(), "incorrect amount of used engines");
			for (Object[] carAndEngineId : carsAndEngineIds) {
				System.out.println("persisting already persisted car");
				em.persist(carAndEngineId[0]);
				System.out.printf("engine id: %d%n", carAndEngineId[1]);
				// ((Car)carAndEngineId[0]).getModel();
			}
		});
	}

	private void testLoadCarUsingNativeQuery(Integer carId) {
		jpa.withEmVoid(em -> {
			Query query = em.createNativeQuery(
					"select c.id as car_id, c.maker, c.model, c.engine_id, c.owner_id as car_owner_id, o.id as owner_id, o.first_name, o.last_name from car c left join owner o on o.id = c.owner_id where c.id = :carId",
					"Car.carAndOwner");
			query.setParameter("carId", carId);
			Object[] result = (Object[]) query.getSingleResult();
			Car car = (Car) result[0];
			Owner owner = (Owner) result[1];

			assertEquals(carId, car.getId());
			assertEquals("320d", car.getModel());
			car.setModel("321d");
			assertNull(owner);
		});
	}

	private void testAdHocNativeQueries() {
		jpa.withEmVoid(em -> {
			Query query = em.createNativeQuery("select id as car_id from car");

			@SuppressWarnings("unchecked")
			List<Integer> result = (List<Integer>) query.getResultList();
			for (Integer row : result) {
				Integer col = row;
			}
		});
	}

	private Integer createCarWithEngine(EntityManager em, String carMaker, String carModel, String engineMaker,
			String engineModel, int power) {
		Car car = new Car(carMaker, carModel);
		Engine engine = new Engine(engineMaker, engineModel, power);
		car.setEngine(engine);
		engine.setDynoGraph(rnd.randomBytes(32 * 1024));
		em.persist(car);
		return car.getId();
	}

	@Test
	public void testFindCars() {
		jpa.withEmVoid(em -> {
			TypedQuery<Car> query = em.createQuery("from Car c where c.model = :model", Car.class);
			query.setParameter("model", "320d");

			List<Car> cars = query.getResultList();
			System.out.printf("Cars found %d%n", cars.size());
		});
	}

	@Test
	public void testGetCarsCount() {
		jpa.withEmVoid(em -> {
			TypedQuery<Long> query = em.createQuery("select count(c) from Car c", Long.class);

			System.out.printf("total number of cars: %d%n", query.getSingleResult());
		});
	}

	@Test
	public void testCreateCarWithProductinStatistics() {
		var newPlant = jpa.withEm(em -> {
			Car car = new Car("BMW", "335i");
			Engine engine = new Engine("BMW", "N55", 306);
			engine.setDiesel(true);
			car.setEngine(engine);
			engine.setDynoGraph(rnd.randomBytes(8 * 1024));
			em.persist(car);
			System.out.printf("Car id: %s%n", car.getId());

			ProductionStatistics prodStats = new ProductionStatistics(1000);
			car.setProductionStats(prodStats);
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
			
			return plant;
		});

		// Cache cache = emf.getCache();
		// cache.evictAll();

		tryingToPersistDetachedPlant(newPlant);
	}

	private void tryingToPersistDetachedPlant(Plant plant) {
		jpa.withEmVoid(em -> {
			// fixing id to persist detached entity one more time
			plant.setId(2);
			System.out.println("persisting detached plant");
			em.persist(plant);
		});
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
		query.addSynchronizedQuerySpace("sql");
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
		
		var newEngine = jpa.withEm(em -> {
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

			engine.setDynoGraph(rnd.randomBytes(1024 * 1024));
			em.persist(engine);

			for (Iterator<EngineProperty> enginePropsIter = engine.getProperties().iterator(); enginePropsIter.hasNext();) {
				EngineProperty engineProp = enginePropsIter.next();
				EnginePropertyChangeRec changeRec = new EnginePropertyChangeRec(engineProp);
				em.persist(changeRec);
				enginePropsIter.remove();
			}

			Map<Serializable, Long> entitiesCount =
					((Set<EntityKey>)em.unwrap(Session.class).getStatistics().getEntityKeys())
							.stream()
							.collect(Collectors.groupingBy(EntityKey::getEntityName, Collectors.counting()));

			System.out.printf("entity stats: %s%n", entitiesCount);
			
			return engine;
		});


		testGetEnginePropertiesChangeRecs(newEngine.getId());
		testGetEnginePropertyUsingQuery(newEngine.getId());
		testGetEnginePropertyUsingCriteriaAPI();
		testGetEnginePropertyByEngineAndName(newEngine.getId(), "type");
		testGetEngineWithFilteredProps(newEngine.getId());
		// testRemoveEngineWithFilteredProperties(engine.getId());
		
		System.out.printf("mem (after): total = %d free = %d%n", runtime.totalMemory(), runtime.freeMemory());
	}

	private void testGetEngineWithFilteredProps(Integer engineId) {
		jpa.withEmVoid(em -> {
			TypedQuery<Engine> query = em.createQuery("select e from Engine e left join fetch e.properties ep where e.id = :engineId", Engine.class);
			query.setParameter("engineId", engineId);
			
			Engine engine = query.getSingleResult();
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
		});
	}

	private void testGetEnginePropertyByEngineAndName(Integer engineId, String name) {
		jpa.withEmVoid(em -> {
			Engine engine = em.find(Engine.class, engineId);
			Engine engineProxy = em.getReference(Engine.class, engineId);

			System.out.printf("engine proxy class: %s%n", engineProxy.getClass());

			System.out.printf("loading engine #%d property %s%n", engineId, name);

			TypedQuery<EngineProperty> query = em.createQuery(
					"select ep from EngineProperty ep where ep.engine = :engine and ep.name = :name", EngineProperty.class);
			query.setParameter("engine", engine);
			query.setParameter("name", name);

			EngineProperty engineProp = query.getSingleResult();
		});
	}

	private void testGetEnginePropertiesChangeRecs(Integer engineId) {
		jpa.withEmVoid(em -> {
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
		});
	}

	private void testGetEnginePropertyUsingQuery(Integer engineId) {
		jpa.withEmVoid(em -> {
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

			assertEquals(2, result.size(), "Incorrect number of rows");
			assertEquals("N54", ((EngineProperty) result.get(0)).getEngine().getModel(), "Incorrect model");
		});
	}

	private void testGetEnginePropertyUsingCriteriaAPI() {
		jpa.withEmVoid(em -> {
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
			assertEquals(1, result.size(), "Incorrect number of properties");
			assertEquals("aluminium with cast iron liners", result.get(0).getValue(), "Incorrect property value");
		});
	}

	private void testRemoveEngineWithFilteredProperties(Integer engineId) {
		jpa.withEmVoid(em -> {
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
		});
	}

	@Test
	public void testCreateOwnerWithCars() {
		final String initialOwnerFirstName = "Billy";
		
		record Result(Integer[] carIds, Owner owner, Integer carId) {};

		var result = jpa.withEm(em -> {
			Integer[] carIds = { 
					createCarWithEngine(em, "BMW", "M3", "BMW", "S55B30", 421), 
					createCarWithEngine(em, "BMW", "M5", "BMW", "S63B44T0", 560) 
			};

			Owner owner = new Owner(initialOwnerFirstName, "Bones", LocalDate.of(1950, 1, 1));
			for (Integer carId : carIds) {
				Car car = em.find(Car.class, carId);
				car.setOwner(owner);
			}

			em.persist(owner);

			Integer carId = createCarWithEngine(em, "BMW", "1M Coupe", "BMW", "N54B30TO", 340);

			em.flush();

			Car car = em.find(Car.class, carIds[1]);
			
			return new Result(carIds, owner, carId);
		});
		

		tryToLoadOwner(result.owner().getId(), result.carId());
		tryToLoadCarAndAccessEngine(result.carId());
		tryToRemoveCarsOwnerBy(result.owner(), result.carIds()[0]);

		// checking persistence context repeatable read semantics
		var newOwner = result.owner();
		jpa.withEmVoid(em -> {
			var owner = em.find(Owner.class, newOwner.getId());
			
			tryUpdateOwner(owner.getId(), "William");

			var query = em.createQuery("select o from Owner o where o.id = :ownerId", Owner.class);
			query.setParameter("ownerId", owner.getId());
			
			var fetchedOwner = query.getSingleResult();
			assertEquals(initialOwnerFirstName, fetchedOwner.getFirstName());
		});
	}
	
	private void tryUpdateOwner(int ownerId, String newFirstName) {
		jpa.withEmVoid(em -> {
			var owner = em.find(Owner.class, ownerId);
			
			owner.setFirstName(newFirstName);
		});
	}

	private void tryToLoadOwner(Integer ownerId, Integer carId) {
		jpa.withEmVoid(em -> {
			TypedQuery<Owner> query = em.createQuery("select o from Owner o join o.cars c where o.id = :id", Owner.class);
			query.setParameter("id", ownerId);
			// query.setHint("org.hibernate.readOnly", Boolean.TRUE.toString());
			Owner owner = query.getSingleResult();

			int rowsCount = 0;
			for (Owner row : query.getResultList()) {
				assertEquals(2, row.getCars().size());
				rowsCount++;
			}
			assertEquals(2, rowsCount);
			assertEquals(1, new LinkedHashSet<>(query.getResultList()).size());

			Car car = em.find(Car.class, carId);

			car.setOwner(owner);
			owner.getCars().add(car);
		});
	}

	private void tryToLoadCarAndAccessEngine(Integer carId) {
		System.out.println("loading car engine id");

		jpa.withEmVoid(em -> {
			TypedQuery<Object[]> query = em.createQuery("select c.id, c.engine.id from Car c where c.id = :carId",
					Object[].class);
			query.setParameter("carId", carId);
			Object[] result = query.getSingleResult();

			// Engine engine = car.getEngine();
			// Integer engineId = engine.getId();
		});

		System.out.println("car engine id loading completed");
	}

	private void tryToRemoveCarsOwnerBy(Owner owner, Integer carId) {
		jpa.withEmVoid(em -> {
			Car car = em.find(Car.class, carId);

//			Query query = em.createQuery("delete Car c where c.owner = :owner");
//			query.setParameter("owner", owner);
//			query.executeUpdate();

			em.clear();

			assertFalse(em.contains(car));

			Car reloadedCar = em.find(Car.class, carId);
			assertNotNull(reloadedCar);

			assertFalse(car == reloadedCar);
		});
	}

	@Test
	public void testCreateOwnerWithGarages() {
		var createdOwner = jpa.withEm(em -> {
			Owner newOwner = new Owner("Blind", "Pew", LocalDate.of(1753, 5, 5));
			Garage garage = new Garage("Treasure Island", 1);
			newOwner.getGarages().add(garage);

			em.persist(newOwner);
			em.persist(garage);

			CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
			CriteriaQuery<Owner> criteriaQuery = criteriaBuilder.createQuery(Owner.class);
			Root<Owner> owner = criteriaQuery.from(Owner.class);

			Expression<Date> birthDateField = owner.get("birthDate");

			criteriaQuery.select(owner);
			criteriaQuery.where(
					criteriaBuilder.greaterThanOrEqualTo(
							birthDateField,
							Date.from(
									LocalDate.parse("1980-01-01", DateTimeFormatter.ISO_LOCAL_DATE)
											.atStartOfDay(ZoneId.systemDefault())
											.toInstant())));

			TypedQuery<Owner> query = em.createQuery(criteriaQuery);
			System.out.printf("query: %s%n", query);
//			TypedQuery<Owner> query = em.createQuery("select o from Owner o where o.birthDate > '1700-1-1'", Owner.class);

			List<Owner> owners = query.getResultList();
			System.out.printf("owners count: %d%n", owners.size());
			
			return newOwner;
		});

		tryToAddGarageToOwner(createdOwner.getId());
	}

	private void tryToAddGarageToOwner(Integer ownerId) {
		jpa.withEmVoid(em -> {
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
		});
	}
}
