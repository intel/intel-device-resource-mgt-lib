/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.httpproxy.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class ApiSecurityConfig extends WebSecurityConfigurerAdapter {
  private static final Logger logger = LoggerFactory.getLogger(ApiSecurityConfig.class);

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http.cors().and().csrf().disable();
    http.headers().cacheControl();
    http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    http.httpBasic().disable().formLogin().disable();

    // for JWT token authorization
    http.addFilterBefore(jwtAuthenticationTokenFilter(), UsernamePasswordAuthenticationFilter.class)
        .exceptionHandling()
        .authenticationEntryPoint(((request, response, authException) -> {
          logger.info("in private authenticationEntryPoint with {}", authException.getClass());
          response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        }))
        .accessDeniedHandler(((request, response, accessDeniedException) -> {
          logger.info("in private accessDeniedHandler with {}", accessDeniedException.getClass());
          response.sendError(HttpServletResponse.SC_FORBIDDEN);
        }));

    http.authorizeRequests()
        .antMatchers(HttpMethod.POST, "/fc/meta/**")
        .hasRole("SYS_ADMIN")
        .antMatchers(HttpMethod.PUT, "/fc/meta/**")
        .hasRole("SYS_ADMIN")
        .antMatchers("/fc/**")
        .authenticated()
        .anyRequest()
        .authenticated();
  }

  @Bean
  public ApiJwtAuthenticationTokenFilter jwtAuthenticationTokenFilter() throws Exception {
    return new ApiJwtAuthenticationTokenFilter();
  }

  @Override
  public void configure(WebSecurity web) throws Exception {
    web.ignoring().antMatchers("/error/**", "/login/**", "/resources/**");
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    CorsConfiguration cfg = new CorsConfiguration().applyPermitDefaultValues();
    cfg.setAllowedMethods(Arrays.asList(HttpMethod.DELETE.name(),
                                        HttpMethod.GET.name(),
                                        HttpMethod.HEAD.name(),
                                        HttpMethod.OPTIONS.name(),
                                        HttpMethod.POST.name(),
                                        HttpMethod.PUT.name()));
    source.registerCorsConfiguration("/**", cfg);
    return source;
  }

  @Bean
  public HttpFirewall allowUrlEncodedSlashHttpFirewall() {
    StrictHttpFirewall firewall = new StrictHttpFirewall();
    firewall.setAllowUrlEncodedSlash(true); // TODO there maybe double slash in URL
    return firewall;
  }
}
