package org.anta.service;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.anta.dto.request.AddressRequest;
import org.anta.dto.response.AddressResponse;
import org.anta.entity.Address;
import org.anta.mapper.AddressMapper;
import org.anta.repository.AddressRepository;
import org.anta.repository.UserRepository;

import java.util.List;

@ApplicationScoped
public class AddressService {

    @Inject
    AddressRepository addressRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    AddressMapper addressMapper;

    public List<AddressResponse> getAddressById(Long userId) {
        var checked = addressRepository.findByUserId(userId);

        if (checked.isEmpty()) {
            throw new RuntimeException("not found user id :" + userId);
        }

        return addressRepository.findByUserId(userId)
                .stream()
                .map(addressMapper::toResponse)
                .toList();
    }

    @Transactional
    public AddressResponse add(Long userId, AddressRequest addressRequest) {
        var user = userRepository.findByIdOptional(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        var address = addressMapper.toEntity(addressRequest);
        address.setUser(user);

        if (Boolean.TRUE.equals(addressRequest.getIsDefault())) {
            addressRepository.findByUserId(userId).forEach(addr -> {
                addr.setIsDefault(false);
                addressRepository.save(addr);
            });
        }

        var savedAddress = addressRepository.save(address);

        return addressMapper.toResponse(savedAddress);
    }

    @Transactional
    public AddressResponse update(Long addressId, Long userId, AddressRequest addressRequest) {
        var address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new RuntimeException("Address not found or not yours"));

        if (Boolean.TRUE.equals(addressRequest.getIsDefault())) {
            addressRepository.findByUserId(userId).forEach(addr -> {
                if (!addr.getId().equals(addressId)) {
                    addr.setIsDefault(false);
                    addressRepository.save(addr);
                }
            });
        }

        addressMapper.updateFromRequest(addressRequest, address);

        var updatedAddress = addressRepository.save(address);

        return addressMapper.toResponse(updatedAddress);
    }

    @Transactional
    public void delete(Long addressId, Long userId) {
        var address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new RuntimeException("Address not found or not yours"));

        addressRepository.delete(address);
    }

    @Transactional
    public AddressResponse setDefaultAddress(Long addressId, Long userId) {
        addressRepository.findByUserId(userId)
                .forEach(addr -> {
                    addr.setIsDefault(addr.getId().equals(addressId));
                    addressRepository.save(addr);
                });

        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new RuntimeException("Address not found"));

        return addressMapper.toResponse(address);
    }
}
