package org.anta.controller;


import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.anta.client.MailClient;
import org.anta.config.JwtUtil;
import org.anta.dto.request.AdminCreateUserRequest;
import org.anta.dto.request.LoginRequest;
import org.anta.dto.request.RegisterRequest;
import org.anta.entity.User;
import org.anta.service.AuthService;

import java.util.List;
import java.util.Map;

@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthController {

    @Inject
    AuthService authService;

    @Inject
    JwtUtil jwtUtil;

    @Inject
    MailClient mailClient;

    @POST
    @Path("/register")
    public Response register(RegisterRequest request) {
        User savedUser = authService.register(request);
        return Response.status(Response.Status.CREATED)
                .entity(Map.of("message", "Đăng ký thành công"))
                .build();
    }

    @POST
    @Path("/login")
    public Response login(LoginRequest request) {
        String input = request.getName() != null ? request.getName() : request.getEmail();

        User user = authService.login(input, request.getPassword());

        String accessToken = jwtUtil.generateAccessToken(
                user.getId(),
                user.getName(),
                user.getRole().toString(),
                user.getEmail(),
                user.getPhoneNumber()
        );

        String refreshToken = jwtUtil.generateRefreshToken(user.getName());

        return Response.ok(Map.of(
                "name", user.getName(),
                "role", user.getRole().toString(),
                "accessToken", accessToken,
                "refreshToken", refreshToken
        )).build();
    }

    @POST
    @Path("/forgot-password")
    public Response forgotPassword(Map<String, String> body) {
        String email = body.get("email");

        if (email == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Missing email")
                    .build();
        }

        String code = authService.createResetCode(email);

        try {
            mailClient.sendResetCodeEmail(email, code);
        } catch (Exception ignored) {
        }

        return Response.ok(Map.of("message", "Reset code sent")).build();
    }

    @POST
    @Path("/verify-reset-code")
    public Response verifyResetCode(Map<String, String> body) {
        String email = body.get("email");
        String code = body.get("code");

        if (email == null || code == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Missing fields")
                    .build();
        }

        boolean verify = authService.verifyResetCode(email, code);

        if (!verify) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Invalid or expired code")
                    .build();
        }

        return Response.ok(Map.of("message", "Code verified")).build();
    }

    @POST
    @Path("/reset-password")
    public Response resetPassword(Map<String, String> body) {
        String email = body.get("email");
        String newPassword = body.get("newPassword");

        if (email == null || newPassword == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Missing fields")
                    .build();
        }

        authService.resetPassword(email, newPassword);

        return Response.ok(Map.of("message", "Password reset successfully")).build();
    }

    @GET
    @Path("/validate-token")
    public Response validateToken(@HeaderParam("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("valid", false, "error", "Missing token"))
                    .build();
        }

        String token = authHeader.substring(7);

        try {
            String username = jwtUtil.extractUsername(token);
            List<String> roles = jwtUtil.extractRoles(token);
            boolean expired = jwtUtil.isTokenExpired(token);

            if (expired) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(Map.of("valid", false, "error", "Token expired"))
                        .build();
            }

            return Response.ok(Map.of(
                    "username", username,
                    "role", roles,
                    "valid", true
            )).build();

        } catch (Exception ex) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("valid", false, "error", "Invalid token"))
                    .build();
        }
    }

    @POST
    @Path("/refresh-token")
    public Response refresh(Map<String, String> request) {
        String refreshToken = request.get("refreshToken");

        if (refreshToken == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Missing refreshToken")
                    .build();
        }

        try {
            if (!jwtUtil.isRefreshToken(refreshToken)) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Not a refresh token")
                        .build();
            }

            String username = jwtUtil.extractUsername(refreshToken);
            User user = authService.findByUsername(username);

            String newAccessToken = jwtUtil.generateAccessToken(
                    user.getId(),
                    user.getName(),
                    user.getRole().toString(),
                    user.getEmail(),
                    user.getPhoneNumber()
            );

            return Response.ok(Map.of("accessToken", newAccessToken)).build();

        } catch (Exception e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Invalid or expired refresh token")
                    .build();
        }
    }

    @POST
    @Path("/admin/create-user")
    @RolesAllowed("ADMIN")
    public Response createUserByAdmin(AdminCreateUserRequest request) {
        try {
            User savedUser = authService.createUserByAdmin(request);

            return Response.status(Response.Status.CREATED)
                    .entity(Map.of(
                            "message", "Create user successfully",
                            "username", savedUser.getName(),
                            "role", savedUser.getRole().toString()
                    ))
                    .build();

        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }
}