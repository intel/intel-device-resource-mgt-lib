/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.httpproxy.security;

import com.openiot.cloud.base.common.model.TokenContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ApiJwtAuthenticationTokenFilter extends GenericFilter {
  private static final long serialVersionUID = -9162264954595683033L;

  private static final Logger logger =
      LoggerFactory.getLogger(ApiJwtAuthenticationTokenFilter.class);

  @Value("${jwt.header}")
  private String authenticationKeyword;

  @Value("${jwt.tokenHead}")
  private String tokenPrefix;

  @Autowired
  TokenClient tokenClient;

  private static final List<AntPathRequestMatcher> passJWTAuthenticationList =
      Stream.of(new AntPathRequestMatcher("/api/user", HttpMethod.GET.name()),
                new AntPathRequestMatcher("/api/user", HttpMethod.POST.name()),
                new AntPathRequestMatcher("/api/user/login"),
                new AntPathRequestMatcher("/api/user/refresh"),
                new AntPathRequestMatcher("/api/user/validation"))
            .collect(Collectors.toList());

  @Override
  public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
      throws IOException, ServletException {
    // avoid run the token validation twice
    if (SecurityContextHolder.getContext().getAuthentication() != null) {
      logger.info("previous filters have generated authentication {}",
                  SecurityContextHolder.getContext().getAuthentication());
      chain.doFilter(req, res);
      return;
    }

    HttpServletRequest request = (HttpServletRequest) req;
    HttpServletResponse response = (HttpServletResponse) res;

    logger.info("match the request path {} if able to skip", request.getRequestURI());
    if (passJWTAuthenticationList.stream().anyMatch(matcher -> matcher.matches(request))) {
      chain.doFilter(req, res);
      return;
    }

    // we are the only authentication filter in the chain and we have to assign a
    // result no matter what

    String authHeader = request.getHeader(this.authenticationKeyword);
    if (authHeader != null && authHeader.startsWith(tokenPrefix)) {
      // The part after "Bearer "
      final String token = authHeader.substring(tokenPrefix.length());
      if (!token.isEmpty()) {
        TokenContent tokenContent = tokenClient.getTokenContent(token);
        logger.info("the token validation result {}", tokenContent);

        if (tokenContent != null && tokenContent.getUser() != null
            && !tokenContent.getUser().isEmpty()) {
          List<SimpleGrantedAuthority> grantedAuthorityList = new ArrayList<>();
          if (tokenContent.getRole() != null) {
            grantedAuthorityList.add(new SimpleGrantedAuthority(tokenContent.getRole().getValue()));
          }

          UsernamePasswordAuthenticationToken authentication =
              new UsernamePasswordAuthenticationToken(tokenContent.getUser(),
                                                      null,
                                                      grantedAuthorityList);
          authentication.setDetails(tokenContent);

          logger.info("Set SecurityContext with {}", authentication);
          SecurityContextHolder.getContext().setAuthentication(authentication);

          chain.doFilter(req, res);
        } else {
          SecurityContextHolder.clearContext();
          response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "invalid token");
        }
      } else {
        logger.info("an empty token in {}", request);
        SecurityContextHolder.clearContext();
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "an empty token");
      }
    } else {
      logger.info("there is no item named {} in the header of {}", authenticationKeyword, request);
      SecurityContextHolder.clearContext();
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                         "need an item for authentication in header");
    }
  }
}
