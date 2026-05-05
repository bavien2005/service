package org.anta.repository;


import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.anta.entity.User;
import org.anta.enums.Role;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class UserRepository implements PanacheRepository<User> {

    @Inject
    EntityManager entityManager;

    public Optional<User> findByName(String name) {
        return find("name", name).firstResultOptional();
    }

    public Optional<User> findByEmail(String email) {
        return find("email", email).firstResultOptional();
    }

    public boolean existsByName(String name) {
        return count("name", name) > 0;
    }

    public boolean existsByEmail(String email) {
        return count("email", email) > 0;
    }

    public User save(User user) {
        if (user.getId() == null) {
            persist(user);
            return user;
        }
        return entityManager.merge(user);
    }

    public List<Object[]> countUsersByMonthFull(int year) {
        String sql = """
                WITH RECURSIVE months AS (
                    SELECT 1 AS month
                    UNION ALL
                    SELECT month + 1 FROM months WHERE month < 12
                )
                SELECT 
                    ?1 AS year,
                    m.month,
                    COALESCE(COUNT(u.id), 0) AS count
                FROM months m
                LEFT JOIN users u 
                       ON MONTH(u.created_at) = m.month 
                      AND YEAR(u.created_at) = ?1
                GROUP BY m.month
                ORDER BY m.month
                """;

        @SuppressWarnings("unchecked")
        List<Object[]> result = entityManager
                .createNativeQuery(sql)
                .setParameter(1, year)
                .getResultList();

        return result;
    }

    public List<User> findAllByRole(Role role) {
        return list("role", role);
    }
}
