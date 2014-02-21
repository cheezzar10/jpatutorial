package com.parallels.pa.rnd.jpa;

import java.util.*;
import javax.persistence.*;
import javax.persistence.criteria.*;
import org.junit.*;
import org.hibernate.exception.*;
import java.sql.*;

import static org.junit.Assert.*;

public class AppTest
{
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
   public void testCreateCars() {
    EntityManager em = emf.createEntityManager();
		EntityTransaction tx = em.getTransaction();
		tx.begin();
		
    Car car = new Car("BMW", "320d");
    Map<String, String> options = new HashMap<>();
    options.put("interiour trim", "aluminium");
    options.put("rims style", "double spoke");
    car.setOptions(options);

    Engine engine = new Engine("BMW", "N47");
    car.setEngine(engine);

		em.persist(car);
    System.out.printf("Car id: %d%n", car.getId());

    String[] opts = { "interiour trim" };
    // TypedQuery<String> query = em.createQuery("select o from Car c join c.options o where c.maker = :maker and c.model = :model and index(o) in ('interiour trim')", String.class);

    // using map join MapJoin<Car, String, String> options = root.joinMap("options") criteriaQuery.multiselect(options.key(), options.value()) 
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Object[]> cq = cb.createQuery(Object[].class);
    Root<Car> car1 = cq.from(Car.class);
    MapJoin<Car, String, String> options1 = car1.joinMap("options");
    cq.multiselect(options1.value(), options1.value());
    cq.where(cb.equal(car1.get("maker"), cb.parameter(String.class, "maker")), cb.equal(car1.get("model"), cb.parameter(String.class, "model")), options1.key().in(opts));

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
      Engine engine = new Engine("BMW", "N57");
      car.setEngine(engine);
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

      TypedQuery<ProductionStatistics> prodStatsQuery = em.createQuery("from ProductionStatistics ps where ps.unitsMade = :unitsMade",
          ProductionStatistics.class);
      prodStatsQuery.setParameter("unitsMade", 1000);

      for (ProductionStatistics loadedProdStats : prodStatsQuery.getResultList()) {
          System.out.printf("Units made: %d car: %d%n", loadedProdStats.getUnitsMade(), loadedProdStats.getId());
      }

      tx.commit();
      em.close();
   }

   public void testRemoveEngine() {
      EntityManager em = emf.createEntityManager();
      EntityTransaction tx = em.getTransaction();

      tx.begin();

      TypedQuery<Engine> query = em.createQuery("from Engine e where e.maker = :maker and e.model = :model", Engine.class);
      query.setParameter("maker", "BMW");
      query.setParameter("model", "N47");
      Engine engine = query.getSingleResult();

      try {
        // check constraints
        em.remove(engine);
        em.flush();
        tx.commit();
      } catch (PersistenceException pEx) {
        System.out.println("Catched");
        if (pEx.getCause() instanceof ConstraintViolationException) {
          ConstraintViolationException cvEx = (ConstraintViolationException)pEx.getCause();
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

   public void testCarArchival() {
     EntityManager em = emf.createEntityManager();
      EntityTransaction tx = em.getTransaction();
      tx.begin();

      Query query = em.createNativeQuery("insert into removed_car select id, maker from car where id = ?");
      query.setParameter(1, 1);
      query.executeUpdate();

      tx.commit();
      em.close();
   }

   @Test
   public void testSetEngineProperties() {
      EntityManager em = emf.createEntityManager();
      EntityTransaction tx = em.getTransaction();
      tx.begin();

      Engine engine = new Engine("BMW", "N54");

      List<EngineProperty> properties = new LinkedList<>();
      properties.add(new EngineProperty("type", "four stroke bi-turbo"));
      properties.add(new EngineProperty("engine block", "aluminium with cast iron liners"));
      engine.setProperties(properties);

      em.persist(engine);

      tx.commit();
      em.close();

      testGetEnginePropertyUsingQuery(engine.getId());
      testGetEnginePropertyUsingCriteriaAPI();
   }

   public void testGetEnginePropertyUsingQuery(Integer engineId) {
      EntityManager em = emf.createEntityManager();
      EntityTransaction tx = em.getTransaction();
      tx.begin();

      CriteriaBuilder cb = em.getCriteriaBuilder();
      CriteriaQuery<Object[]> cq = cb.createQuery(Object[].class);
      Root<Engine> engine = cq.from(Engine.class);
      Join<Engine, EngineProperty> properties = engine.join("properties");
      cq.multiselect(engine.get("model"), properties.get("name"), properties.get("value"));
      Predicate p = cb.conjunction();
      p = cb.and(p, cb.equal(engine.get("id"), cb.parameter(Integer.class, "engineId")));
      p = cb.and(p, properties.get("name").in(new String[] { "engine block", "type" }));
      cq.where(p);

      // TypedQuery<Engine> query = em.createQuery("select e from Engine e join fetch e.properties ep where e.id = :engineId and ep.name in ('engine block')", Engine.class);
      TypedQuery<Object[]> query = em.createQuery(cq);
      query.setParameter("engineId", engineId);
      List<Object[]> result = query.getResultList();

      assertEquals("Incorrect number of rows", 2, result.size());
      assertEquals("Incorrect model", "N54", (String)result.get(1)[0]);

      tx.commit();
      em.close();
   }

   public void testGetEnginePropertyUsingCriteriaAPI() {
      EntityManager em = emf.createEntityManager();
      EntityTransaction tx = em.getTransaction();
      tx.begin();

      // select ep.name, ep.value from Engine e join e.properties ep where e.maker = :maker and e.model = :model and ep.name in (...)

      CriteriaBuilder cb = em.getCriteriaBuilder();
      CriteriaQuery<EngineProperty> cq = cb.createQuery(EngineProperty.class);
      Root<Engine> engine = cq.from(Engine.class);
      Join<Engine, EngineProperty> properties = engine.join("properties");
      cq.multiselect(properties.get("name"), properties.get("value"));
      Predicate p = cb.conjunction();
      p = cb.and(p, cb.equal(engine.get("maker"), cb.parameter(String.class, "maker")));
      p = cb.and(p, cb.equal(engine.get("model"), cb.parameter(String.class, "model")));
      p = cb.and(p, properties.get("name").in(new String[] { "engine block" }));
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
}
