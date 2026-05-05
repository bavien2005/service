package org.anta.controller;


import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.anta.dto.request.AddressRequest;
import org.anta.dto.response.AddressResponse;
import org.anta.service.AddressService;

import java.util.List;

@Path("/api/address")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AddressController {

    @Inject
    AddressService addressService;

    @GET
    @Path("/allUserAddress/{userId}")
    public Response getAllAddressByUserId(@PathParam("userId") Long userId) {
        List<AddressResponse> result = addressService.getAddressById(userId);
        return Response.ok(result).build();
    }

    @POST
    @Path("/add/{userId}")
    public Response addAddress(
            @PathParam("userId") Long userId,
            AddressRequest addressRequest
    ) {
        AddressResponse result = addressService.add(userId, addressRequest);
        return Response.ok(result).build();
    }

    @PUT
    @Path("/setDefault/{addressId}/user/{userId}")
    public Response setDefaultAddress(
            @PathParam("addressId") Long addressId,
            @PathParam("userId") Long userId
    ) {
        AddressResponse result = addressService.setDefaultAddress(addressId, userId);
        return Response.ok(result).build();
    }

    @PUT
    @Path("/update/addressId/{addressId}/userId/{userId}")
    public Response updateAddress(
            @PathParam("addressId") Long addressId,
            @PathParam("userId") Long userId,
            AddressRequest addressRequest
    ) {
        AddressResponse result = addressService.update(addressId, userId, addressRequest);
        return Response.ok(result).build();
    }

    @DELETE
    @Path("/delete/addressId/{addressId}/userId/{userId}")
    public Response deleteAddress(
            @PathParam("addressId") Long addressId,
            @PathParam("userId") Long userId
    ) {
        addressService.delete(addressId, userId);
        return Response.ok("Delete address successfully" + addressId + " for user " + userId).build();
    }
}