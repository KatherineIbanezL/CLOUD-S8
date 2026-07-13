package cl.duoc.guia_service.producer.config;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.disable())
            .authorizeRequests(auth -> auth
                    // Endpoint público de salud
                    .antMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                    
                    // Rol para SOLO descargar
                    .antMatchers(HttpMethod.GET, "/api/documentos/descargar/**").hasAuthority("ROLE_DESCARGAR")
                    
                    // Rol para el resto de endpoints
                    .antMatchers("/api/documentos/**").hasAuthority("ROLE_OPERADOR")
                    
                    .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            )
            .build();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter defaultScopesConverter = new JwtGrantedAuthoritiesConverter();

        Converter<Jwt, Collection<GrantedAuthority>> authoritiesConverter = jwt -> {
            Set<GrantedAuthority> authorities = new HashSet<>(defaultScopesConverter.convert(jwt));
            
            addClaimAuthorities(jwt.getClaim("extension_role"), authorities);
            addClaimAuthorities(jwt.getClaim("role"), authorities);
            addClaimAuthorities(jwt.getClaim("roles"), authorities);
            addClaimAuthorities(jwt.getClaim("extension_consultaRole"), authorities);
            
            return authorities;
        };

        JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
        authenticationConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return authenticationConverter;
    }

    private void addClaimAuthorities(Object claimValue, Set<GrantedAuthority> authorities) {
        if (claimValue instanceof String role) {
            addSingleRole(role, authorities);
            return;
        }

        if (claimValue instanceof Collection<?> roles) {
            for (Object role : roles) {
                if (role instanceof String roleValue) {
                    addSingleRole(roleValue, authorities);
                }
            }
        }
    }

    private void addSingleRole(String role, Set<GrantedAuthority> authorities) {
        String normalizedRole = role == null ? "" : role.trim();

        if (normalizedRole.isEmpty()) {
            return;
        }

        // Normaliza el rol para que siempre empiece con "ROLE_" en mayúsculas
        String authority = normalizedRole.startsWith("ROLE_")
                ? normalizedRole.toUpperCase()
                : "ROLE_" + normalizedRole.toUpperCase();

        authorities.add(new SimpleGrantedAuthority(authority));
    }
}