package com.parallels.pa.rnd.jpa;

import org.hibernate.jpa.QueryHints;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.function.Consumer;

@Disabled("too slow")
public class AuditLogTest {
    private static EntityManagerFactory emf;

    @BeforeAll
    public static void createEntityManagerFactory() {
        emf = Persistence.createEntityManagerFactory("car");
    }

    @AfterAll
    public static void closeEntityManagerFactory() {
        emf.close();
    }

    private void withJpa(boolean inTx, Consumer<EntityManager> action) {
        EntityManager em = emf.createEntityManager();
        try {
            if (inTx) {
                EntityTransaction tx = em.getTransaction();
                tx.begin();

                try {
                    action.accept(em);
                    tx.commit();
                } catch (Exception ex) {
                    tx.rollback();
                }
            } else {
                action.accept(em);
            }
        } finally {
            em.close();
        }
    }

    private void withJpa(Consumer<EntityManager> txAction) {
        withJpa(true, txAction);
    }

    @Test
    public void testAuditLogOperations() {
        System.out.println("audit log test started");

        withJpa(em -> {
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

        withJpa(em -> {
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

    @Test
    public void testFetchAllAuditLogRecords() {
        System.out.println("all records fetching started");

        withJpa(em -> {
            User user = new User(1002, "Blind Pew");
            em.persist(user);

            for (int recordNum=0;recordNum<10000;recordNum++) {
                AuditLogRecord record = new AuditLogRecord("record #" + recordNum, user);
                em.persist(record);

                if  (recordNum != 0 && recordNum % 100 == 0) {
                    em.flush();
                    em.clear();
                }
            }
        });

        withJpa(false, em -> {
            TypedQuery<AuditLogRecord> query = em.createQuery(
                    "select r from AuditLogRecord r left join fetch r.user",
                    AuditLogRecord.class);

            query.setHint(QueryHints.HINT_FETCH_SIZE, "100");

            query.getResultStream().forEach(record -> {
                System.out.printf("record: %s%n", record);

                // invalid record detection
                // update AuditLogRecord set status = 'invalid' where id >= <current record> and node = <current node> and status = 'valid'
            });
        });

        System.out.println("all records fetching completed");
    }
}
