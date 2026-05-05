package org.anta.repository;


import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.anta.entity.Address;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class AddressRepository implements PanacheRepository<Address> {

    @Inject
    EntityManager entityManager;

    public List<Address> findByUserId(Long userId) {
        return list("user.id", userId);
    }

    public Optional<Address> findByIdAndUserId(Long id, Long userId) {
        return find("id = ?1 and user.id = ?2", id, userId).firstResultOptional();
    }

    public Address save(Address address) {
        if (address.getId() == null) {
            persist(address);
            return address;
        }
        return entityManager.merge(address);
    }
}