package org.anta.controller;


import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.anta.dto.request.UserRequest;
import org.anta.dto.response.UserResponse;
import org.anta.service.UserService;

import java.util.List;

@Path("/api/user")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserController {

    @Inject
    UserService userService;

    @GET
    @Path("/all")
    public Response getAllUser() {
        List<UserResponse> users = userService.getAllUsers();
        return Response.ok(users).build();
    }

    @GET
    @Path("/{id}")
    public Response getUserById(@PathParam("id") Long id) {
        UserResponse user = userService.getUserById(id);
        return Response.ok(user).build();
    }

    @POST
    @Path("/add")
    public Response add(UserRequest userRequest) {
        UserResponse created = userService.addUser(userRequest);
        return Response.ok(created).build();
    }

    @PUT
    @Path("/update/{id}")
    public Response update(@PathParam("id") Long id, UserRequest userUpdate) {
        UserResponse updated = userService.updateUser(id, userUpdate);
        return Response.ok(updated).build();
    }

    @DELETE
    @Path("/delete/{id}")
    public Response delete(@PathParam("id") Long id) {
        userService.deleteUser(id);
        return Response.ok("Delete successfully: " + id).build();
    }

    @GET
    @Path("/stats/monthly/full")
    public Response getStatsFull(@QueryParam("year") int year) {
        return Response.ok(userService.getUserMonthlyStatsFull(year)).build();
    }
}