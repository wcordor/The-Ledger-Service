package com.github.wcordor.ledger;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
            .csrf(csrf -> csrf.disable())
			.authorizeHttpRequests((authorize) -> authorize
				.requestMatchers(HttpMethod.POST, "/users").permitAll()
				.requestMatchers("/", "/index.html").permitAll()
				.requestMatchers("/admin").hasRole("ADMIN")
				.requestMatchers("/admin/*").hasRole("ADMIN")
				.requestMatchers("/users/{username}")
					.access(new WebExpressionAuthorizationManager("#username == authentication.name or hasRole('ADMIN')"))
				.requestMatchers("/users/{username}/*")
					.access(new WebExpressionAuthorizationManager("#username == authentication.name or hasRole('ADMIN')"))
        		.anyRequest().authenticated()
			)
			.httpBasic(Customizer.withDefaults())
			.formLogin(Customizer.withDefaults());

		return http.build();
	}

	@Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
