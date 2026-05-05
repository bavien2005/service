package org.anta.mapper;


import org.anta.dto.request.AddressRequest;
import org.anta.dto.response.AddressResponse;
import org.anta.entity.Address;
import org.mapstruct.*;

@Mapper(componentModel = "cdi")
public interface AddressMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    Address toEntity(AddressRequest request);

    AddressResponse toResponse(Address address);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    void updateFromRequest(AddressRequest request, @MappingTarget Address entity);
}
