package Find.read.Read.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .authorizeHttpRequests(auth -> auth
                        // Publicly accessible:
                                .requestMatchers(
                                        "/auth/**",
                                        "/static/**",
                                        "/images/**",
                                        "/error",
                                        "/novels",
                                        "/novels/search-page",
                                        "/novels/detail/**",
                                        "/novels/image/**",
                                        "/novels/ranking"
                                ).permitAll()
                                  // Protected AI generation endpoint:
                                .requestMatchers("/api/ai/generate").authenticated()



                        // Any other AI endpoints (if you really want them open)
                        // can be listed here explicitly, e.g.:
                        // .requestMatchers("/api/ai/help", "/api/ai/status").permitAll()

                        // All other requests require authentication:
                        .anyRequest().authenticated()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/novels")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID", "jwtToken")
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
