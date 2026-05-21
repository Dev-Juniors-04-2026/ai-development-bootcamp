package com.example.todoapp.adapter.in.http;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration for the REST API.
 *
 * <p>By registering a {@link SecurityFilterChain} bean, this class replaces Spring Boot's
 * default "lock everything down with HTTP Basic" auto-configuration with a JWT-bearer setup.
 *
 * <p>Net effect at runtime (once {@link JwtFilter} is implemented):
 * <ul>
 *   <li>{@code POST /api/auth/login} with no token → reaches {@link AuthController}.</li>
 *   <li>{@code GET /api/todos} with no token → 401 (filter sets no auth; authorize rule rejects).</li>
 *   <li>{@code GET /api/todos} with valid {@code Bearer xyz} → filter puts an {@code Authentication}
 *       whose principal is the user's {@code UUID} on the {@code SecurityContext}; the controller
 *       reads it via {@code SecurityContextHolder} and scopes queries by user.</li>
 * </ul>
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService) throws Exception {
        return http
                // CSRF protection guards against forged requests that ride on browser-attached
                // session cookies. We authenticate via the Authorization header (bearer JWT),
                // which the browser does not auto-attach cross-origin, so CSRF doesn't apply.
                // Without this disable, every POST/PATCH/DELETE would 403.
                .csrf(csrf -> csrf.disable())

                // Stateless: no HttpSession, no JSESSIONID cookie. Every request must carry
                // its own credentials (the JWT). Matches "JWT in localStorage, sent on every request".
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // URL-to-rule map:
                //   /api/auth/** is open — you can't get a token before logging in.
                //   everything else requires an authenticated principal in the SecurityContext;
                //   absence of one yields 401.
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/todos/reset").permitAll()
                        .requestMatchers("/", "/index.html", "/css/**", "/src/**").permitAll()
                        .anyRequest().authenticated())

                // Without this, the default entry point is Http403ForbiddenEntryPoint, which
                // turns missing/invalid authentication into 403. We want 401 so the frontend
                // can detect expired tokens and bounce the user back to login.
                .exceptionHandling(eh -> eh.authenticationEntryPoint(
                        (req, res, ex) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED)))

                // Insert JwtFilter before the slot where form login would normally run.
                // The filter reads "Authorization: Bearer ...", asks JwtService to validate
                // and extract the UUID, then puts an Authentication (principal = UUID) on
                // SecurityContextHolder so downstream authorize checks pass and controllers
                // can read the current user.
                .addFilterBefore(new JwtFilter(jwtService), UsernamePasswordAuthenticationFilter.class)

                .build();
    }
}
