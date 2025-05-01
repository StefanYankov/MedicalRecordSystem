package nbu.cscb869.config;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import org.hibernate.Session;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JpaConfig {
    @PersistenceUnit
    private EntityManagerFactory entityManagerFactory;

    @PostConstruct
    public void enableSoftDeleteFilter() {
        try (EntityManager entityManager = entityManagerFactory.createEntityManager()) {
            Session session = entityManager.unwrap(Session.class);
            session.enableFilter("softDeleteFilter");
        }
    }
}