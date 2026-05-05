package org.anta.controller;


import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.anta.client.MailClient;
import org.anta.dto.request.VerifyConfirmRequest;
import org.anta.dto.request.VerifyRequest;
import org.anta.service.OtpRedisService;

import java.util.Map;
import java.util.UUID;

@Path("/api/auth/verify")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VerifyController {

    @Inject
    OtpRedisService otpService;

    @Inject
    MailClient mailClient;

    @POST
    @Path("/request")
    public Response request(@Valid VerifyRequest req) {
        try {
            String otp = otpService.generateAndSave(req.getEmail());
            mailClient.sendResetCodeEmail(req.getEmail(), otp);

            return Response.ok(Map.of(
                    "message", "OTP has been sent to your email.",
                    "requestId", UUID.randomUUID().toString()
            )).build();

        } catch (IllegalStateException cooldown) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", cooldown.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/confirm")
    public Response confirm(@Valid VerifyConfirmRequest req) {
        boolean ok = otpService.verify(req.getEmail(), req.getOtp());

        if (!ok) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of(
                            "verified", false,
                            "error", "OTP code is incorrect/expired or exceeds the number of attempts"
                    ))
                    .build();
        }

        return Response.ok(Map.of("verified", true)).build();
    }
}