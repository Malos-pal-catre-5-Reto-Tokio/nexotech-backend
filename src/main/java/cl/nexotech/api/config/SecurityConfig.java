package cl.nexotech.api.config;

import cl.nexotech.api.seguridad.FiltroJwt;
import cl.nexotech.api.seguridad.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * API sin sesión de servidor (stateless): cada petición se autentica con su token JWT.
 * Qué rol puede hacer qué se define con @PreAuthorize en los controladores; aquí solo se abren las rutas públicas
 * y todo lo demás exige haber iniciado sesión.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain cadenaDeSeguridad(HttpSecurity http, JwtService jwt) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // no hay sesión por cookie que falsificar: el acceso va en la cabecera Authorization
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(rutas -> rutas
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/login", "/api/auth/registro",
                                "/api/auth/refresh", "/api/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/productos/**", "/api/categorias/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/contacto").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) ->
                                escribirError(res, 401, "Debes iniciar sesión."))
                        .accessDeniedHandler((req, res, ex) ->
                                escribirError(res, 403, "No tienes permiso para realizar esta acción.")))
                .addFilterBefore(new FiltroJwt(jwt), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(@Value("${cors.origenes}") String origenes) {
        CorsConfiguration config = new CorsConfiguration();
        List<String> lista = Arrays.stream(origenes.split(",")).map(String::trim).filter(o -> !o.isEmpty()).toList();
        config.setAllowedOrigins(lista);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true); // necesario para que el navegador envíe la cookie del refresh token
        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/**", config);
        return fuente;
    }

    private static void escribirError(jakarta.servlet.http.HttpServletResponse res, int codigo, String mensaje)
            throws java.io.IOException {
        res.setStatus(codigo);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write("{\"error\":\"" + mensaje + "\"}");
    }
}
