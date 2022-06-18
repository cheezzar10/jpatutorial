package com.parallels.pa.rnd.jpa;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.function.Consumer;

public class AuditLogTest {
    private static EntityManagerFactory emf;

    @BeforeClass
    public static void createEntityManagerFactory() {
        emf = Persistence.createEntityManagerFactory("car");
    }

    @AfterClass
    public static void closeEntityManagerFactory() {
        emf.close();
    }

    private void inTx(Consumer<EntityManager> txAction) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        tx.begin();

        txAction.accept(em);

        tx.commit();
        em.close();
    }

    @Test
    public void testAuditLogOperations() {
        System.out.println("audit log test started");

        inTx(em -> {
            User user = new User(1001, "Billy Bones");
            em.persist(user);

            System.out.printf("user created: %s%n", user);

            AuditLogRecord record = new AuditLogRecord("user created", user);
            em.persist(record);

            AuditLogRecord loginRecord = new AuditLogRecord("user logged in", user);
            em.persist(loginRecord);

            AuditLogRecord systemRecord = new AuditLogRecord("catalog synchronized", null);
            em.persist(systemRecord);
        });

        inTx(em -> {
            TypedQuery<Object[]> query = em.createQuery(
                    "select r, ru from AuditLogRecord r left join r.user ru where r.id < 10 and ((ru.id = 1 and r.user is not null) or r.user is null)",
                    Object[].class);

            List<Object[]> rows = query.getResultList();
            for (Object[] row : rows) {
                System.out.printf("fetched record: %s%n", row[0]);
                System.out.printf("fetched user: %s%n", row[1]);
            }
        });

//        inTx(em -> {
//            AuditLogRecord record = em.find(AuditLogRecord.class, 2);
//            System.out.printf("record found: %s%n", record);
//        });
    }
}
