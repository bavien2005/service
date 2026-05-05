package org.anta.config;


import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.security.Principal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Provider
@ApplicationScoped
@Priority(Priorities.AUTHENTICATION)
public class JwtAuthenticationFilter implements ContainerRequestFilter {

    private static final Logger LOG = Logger.getLogger(JwtAuthenticationFilter.class);

    @Inject
    JwtUtil jwtUtil;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String path = requestContext.getUriInfo().getPath();
        String normalizedPath = path.startsWith("/") ? path : "/" + path;

        LOG.debugf("JwtFilter called for path=%s method=%s", normalizedPath, requestContext.getMethod());

        if (shouldNotFilter(normalizedPath)) {
            return;
        }

        String authHeader = requestContext.getHeaderString("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            requestContext.abortWith(jakarta.ws.rs.core.Response.status(401).build());
            return;
        }

        String token = authHeader.substring(7);

        try {
            String name = jwtUtil.extractUsername(token);

            if (name == null || !jwtUtil.isTokenValid(token)) {
                requestContext.abortWith(jakarta.ws.rs.core.Response.status(401).build());
                return;
            }

            List<String> rolesFromToken = jwtUtil.extractRoles(token);
            Set<String> roles = new HashSet<>(rolesFromToken);

            SecurityContext currentSecurityContext = requestContext.getSecurityContext();

            requestContext.setSecurityContext(new SecurityContext() {
                @Override
                public Principal getUserPrincipal() {
                    return () -> name;
                }

                @Override
                public boolean isUserInRole(String role) {
                    return roles.contains(role) || roles.contains("ROLE_" + role);
                }

                @Override
                public boolean isSecure() {
                    return currentSecurityContext != null && currentSecurityContext.isSecure();
                }

                @Override
                public String getAuthenticationScheme() {
                    return "Bearer";
                }
            });

            LOG.debugf("Set authentication for user %s with roles %s", name, roles);

        } catch (Exception e) {
            LOG.warnf("JWT invalid: %s", e.getMessage());
            requestContext.abortWith(jakarta.ws.rs.core.Response.status(401).build());
        }
    }

    private boolean shouldNotFilter(String path) {
        return path.startsWith("/api/auth/")
                || path.startsWith("/api/auth")
                || path.startsWith("/api/user/")
                || path.startsWith("/api/user")
                || path.startsWith("/api/public/")
                || path.startsWith("/api/public")
                || path.startsWith("/actuator/")
                || path.startsWith("/actuator")
                || path.startsWith("/api/address/")
                || path.startsWith("/api/address");
    }
}