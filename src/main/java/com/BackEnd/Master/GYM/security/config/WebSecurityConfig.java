package com.BackEnd.Master.GYM.security.config;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig {

    @Value("${jwt.secret}")
    private String SecretKey;

    @Value("${app.upload.dir}")
    private String uploadDir;
    // **** pour tester l'app avant d'ajouter la securiter avec (oauth2,spring
    // securtiy)
    // @Bean
    // public SecurityFilterChain securityFilterChain(HttpSecurity http) throws
    // Exception {
    // http
    // .csrf(csrf -> csrf.disable()) // Désactive CSRF pour simplifier les tests
    // .authorizeHttpRequests(auth -> auth
    // .anyRequest().permitAll() // Permet toutes les requêtes sans authentification
    // )
    // .oauth2ResourceServer(oauth2 -> oauth2.disable()); // Désactive OAuth2
    // Resource Server

    // return http.build();
    // }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Désactiver CSRF pour simplifier les tests via Postman
                .csrf(csrf -> csrf.disable())

                .cors(Customizer.withDefaults())

                // Configuration des autorisations
                .authorizeHttpRequests(auth -> auth
                        // Accès libre pour le login
                        .requestMatchers("/auth/login/**").permitAll()
                        .requestMatchers("/training-sessions/**").permitAll()
                        .requestMatchers("/contact-messages/**").permitAll()
                        .requestMatchers(HttpMethod.GET,"/user/by-role").permitAll()
                        
                        // Accès libre pour les images
                        .requestMatchers("/user/images/**").permitAll()
                        .requestMatchers("/customer/images/**").permitAll()
                        .requestMatchers("/photos/images/**").permitAll()
                        .requestMatchers("/user/filtre/**").permitAll()

                        // Accès pour les album
                        .requestMatchers(HttpMethod.GET, "/albums/**").hasAnyRole("Admin", "Coach")
                        .requestMatchers(HttpMethod.POST, "/albums").hasAnyRole("Admin", "Coach")
                        .requestMatchers(HttpMethod.PUT, "/albums/**").hasAnyRole("Admin", "Coach")
                        .requestMatchers(HttpMethod.DELETE, "/albums/**").hasAnyRole("Admin", "Coach")

                        // Accès pour les photos
                        .requestMatchers(HttpMethod.GET, "/photos/**").hasAnyRole("Admin", "Coach")
                        .requestMatchers(HttpMethod.POST, "/photos").hasAnyRole("Admin", "Coach")
                        .requestMatchers(HttpMethod.PUT, "/photos/**").hasAnyRole("Admin", "Coach")
                        .requestMatchers(HttpMethod.DELETE, "/photos/**").hasAnyRole("Admin", "Coach")

                        // Accès pour les rôles (uniquement Admin)
                        .requestMatchers("/role/**").hasRole("Admin")

                        // Toutes les autres requêtes nécessitent une authentification
                        .anyRequest().authenticated())

                // Gestion des sessions (Stateless pour JWT)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Configuration OAuth2 Resource Server pour JWT
                .oauth2ResourceServer(oa -> oa.jwt(Customizer.withDefaults()));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(SecretKey.getBytes()));
    }

    @Bean
    JwtDecoder jwtDecoder() {
        SecretKeySpec secretKeySpec = new SecretKeySpec(SecretKey.getBytes(), "RSA");
        return NimbusJwtDecoder.withSecretKey(secretKeySpec).macAlgorithm(MacAlgorithm.HS512).build();
    }

    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService) {
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider();
        daoAuthenticationProvider.setPasswordEncoder(passwordEncoder());
        daoAuthenticationProvider.setUserDetailsService(userDetailsService);
        return new ProviderManager(daoAuthenticationProvider);
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthorityPrefix("");
        grantedAuthoritiesConverter.setAuthoritiesClaimName("scope");

        JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
        authenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return authenticationConverter;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.addAllowedOrigin("*");
        corsConfiguration.addAllowedMethod("*");
        corsConfiguration.addAllowedHeader("*");
        // corsConfiguration.setExposedHeaders(List.of("x-auth-token"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);
        return source;
    }
    
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Expose files under /user/images/** from the filesystem directory
        registry
          .addResourceHandler("/user/images/**")
          .addResourceLocations("file:" + uploadDir + "/");
    }

}
