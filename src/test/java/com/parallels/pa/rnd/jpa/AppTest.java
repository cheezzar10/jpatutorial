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
   public void testFindCarsWithEngines() {
    EntityManager em = emf.createEntityManager();
    EntityTransaction tx = em.getTransaction();
    tx.begin();

    Integer[] carIds = { createCarWithEngine(em, "BMW", "530d", "BMW", "N57D30O1"), createCarWithEngine(em, "BMW", "535d", "BMW", "N57D30T1") };
    System.out.printf("car ids: %s%n", Arrays.toString(carIds));

    tx.commit();
    em.close();

    testLoadCars(carIds);
    testFindCarsWithEngine();

    System.out.println("test cars with engines test completed");
   }

   private void testFindCarsWithEngine() {
      EntityManager em = emf.createEntityManager();
      EntityTransaction tx = em.getTransaction();
      tx.begin();

      TypedQuery<Car> query = em.createQuery("select c from Car c where c.engine.id = (select e.id from Engine e where e.maker = :maker and e.model = :model)", Car.class);
      query.setParameter("maker", "BM");
      query.setParameter("model", "N57D30O1");
      query.getResultList();

      tx.commit();
      em.close();
   }

   private void testLoadCars(Integer[] carIds) {
      EntityManager em = emf.createEntityManager();
      EntityTransaction tx = em.getTransaction();
      tx.begin();

      System.out.println("test cars with engines test completed");

      // select c.engine.id from Car c where c.id = :carId
      CriteriaBuilder cb = em.getCriteriaBuilder();
      CriteriaQuery<Object[]> cq = cb.createQuery(Object[].class);
      Root<Car> car = cq.from(Car.class);
      Join<Car,Engine> engine = car.join("engine");
      // cq.select(car.<Engine>get("engine").<Integer>get("id"));
      cq.multiselect(car, engine.get("id"));
      cq.where(car.get("id").in(carIds));
      TypedQuery<Object[]> query = em.createQuery(cq);

      List<Object[]> carsAndEngineIds = query.getResultList();

      assertEquals("incorrect amount of used engines", carIds.length, carsAndEngineIds.size());
      for (Object[] carAndEngineId : carsAndEngineIds) {
        System.out.printf("engine id: %d%n", carAndEngineId[1]);
        // ((Car)carAndEngineId[0]).getModel();
      }
    
      tx.commit();
      em.close();      
   }

   private Integer createCarWithEngine(EntityManager em, String carMaker, String carModel, String engineMaker, String engineModel) {
    Car car = new Car(carMaker, carModel);
    Engine engine = new Engine(engineMaker, engineModel);
    car.setEngine(engine);
    em.persist(car);
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
      Engine engine = new Engine("BMW", "N55");
      engine.setDiesel(true);
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

   private void testRemoveEngine() {
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

   private void testCarArchival() {
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
      engine.addProperty("type", "four stroke bi-turbo");
      engine.addProperty("engine block", "aluminium with cast iron liners");

      em.persist(engine);

      for (Iterator<EngineProperty> enginePropsIter = engine.getProperties().iterator();enginePropsIter.hasNext();) {
          EngineProperty engineProp = enginePropsIter.next();
          enginePropsIter.remove();
      }

      tx.commit();
      em.close();

      testGetEnginePropertyUsingQuery(engine.getId());
      testGetEnginePropertyUsingCriteriaAPI();
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
      p = cb.and(p, engineProp.get("engine").get("id").in(new Integer[] { engineId }));
      p = cb.and(p, engineProp.get("name").in(new String[] { "engine block", "type" }));
      cq.where(p);

      TypedQuery<EngineProperty> query = em.createQuery(cq);
      List<EngineProperty> result = query.getResultList();

      assertEquals("Incorrect number of rows", 2, result.size());
      assertEquals("Incorrect model", "N54", ((EngineProperty)result.get(0)).getEngine().getModel());

      tx.commit();
      em.close();
   }

   private void testGetEnginePropertyUsingCriteriaAPI() {
      EntityManager em = emf.createEntityManager();
      EntityTransaction tx = em.getTransaction();
      tx.begin();

      // select ep.name, ep.value from Engine e join e.properties ep where e.maker = :maker and e.model = :model and ep.name in (...)

      CriteriaBuilder cb = em.getCriteriaBuilder();
      CriteriaQuery<EngineProperty> cq = cb.createQuery(EngineProperty.class);
      Root<Engine> engine = cq.from(Engine.class);
      Join<Engine, EngineProperty> properties = engine.join("properties");
      cq.select(properties);
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

   @Test
   public void testCreateOwnerWithCars() {
      EntityManager em = emf.createEntityManager();
      EntityTransaction tx = em.getTransaction();
      tx.begin();

      Owner owner = new Owner("Billy", "Bones");
      Integer[] carIds = { createCarWithEngine(em, "BMW", "M3", "BMW", "S55B30"), createCarWithEngine(em, "BMW", "M5", "BMW", "S63B44T0") };

      for (Integer carId : carIds) {
        Car car = em.find(Car.class, carId);
        car.setOwner(owner);
      }

      em.persist(owner);

      Integer carId = createCarWithEngine(em, "BMW", "1M Coupe", "BMW", "N54B30TO");

      em.flush();

      Car car = em.find(Car.class, carIds[1]);

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

      TypedQuery<Owner> query = em.createQuery("from Owner o where o.id = :id", Owner.class);
      query.setParameter("id", ownerId);
      // query.setHint("org.hibernate.readOnly", Boolean.TRUE.toString());
      Owner owner = query.getSingleResult();

      Car car = em.find(Car.class, carId);

      car.setOwner(owner);
      owner.getCars().add(car);

      tx.commit();
      em.close();
   }

   private void tryToLoadCarAndAccessEngine(Integer carId) {
     EntityManager em = emf.createEntityManager();

     System.out.println("loading car engine id");

     TypedQuery<Object[]> query = em.createQuery("select c.id, c.engine.id from Car c where c.id = :carId", Object[].class);
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

      Query query = em.createQuery("delete Car c where c.owner = :owner");
      query.setParameter("owner", owner);
      query.executeUpdate();

      Car car = em.find(Car.class, carId);
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

      owner.setGarages(new HashSet<>(Arrays.asList( garage )));
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
