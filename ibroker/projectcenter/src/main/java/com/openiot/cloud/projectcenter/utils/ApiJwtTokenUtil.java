/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.projectcenter.utils;

import com.openiot.cloud.base.help.BaseUtil;
import com.openiot.cloud.base.help.ConstDef;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

// refer to
// https://github.com/szerhusenBC/jwt-spring-security-demo/blob/master/src/main/java/org/zerhusen/security/JwtTokenUtil.java

@Component
public class ApiJwtTokenUtil implements Serializable {
  Logger logger = LoggerFactory.getLogger(ApiJwtTokenUtil.class);
  static final String CLAIM_KEY_USERNAME = "sub";
  static final String CLAIM_KEY_CREATED = "iat";
  private static final long serialVersionUID = -3301605591108950415L;
  // @SuppressFBWarnings(value = "SE_BAD_FIELD", justification = "It's okay here")

  @Value("${jwt.secret}")
  private String secret;

  @Value("${jwt.expiration}")
  private Long expiration;

  public String getUsernameFromToken(String token) {
    return getClaimFromToken(token, Claims::getSubject);
  }

  public Date getIssuedAtDateFromToken(String token) {
    return getClaimFromToken(token, Claims::getIssuedAt);
  }

  public Date getExpirationDateFromToken(String token) {
    return getClaimFromToken(token, Claims::getExpiration);
  }

  public String getPidFromToken(String token) {
    final Claims claims = getAllClaimsFromToken(token);
    logger.info("claims: " + claims);
    Object pid = claims.get(ConstDef.KEY_PID);
    return pid == null ? null : (String) pid;
  }

  public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = getAllClaimsFromToken(token);
    return claimsResolver.apply(claims);
  }

  private Claims getAllClaimsFromToken(String token) {
    return Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
  }

  private Boolean isTokenExpired(String token) {
    final Date expiration = getExpirationDateFromToken(token);
    boolean result = expiration.getTime() < (BaseUtil.getNow().getTime() / 1000 * 1000);
    if (result) {
      logger.warn("{} is expired, expiration of token is {} and now is {}",
                  token,
                  expiration,
                  BaseUtil.getNow());
    }
    return result;
  }

  private Boolean isCreatedAfterLastPasswordReset(Date created, Date lastPasswordReset) {
    boolean result = lastPasswordReset == null
        || created.getTime() >= (lastPasswordReset.getTime() / 1000 * 1000);
    if (!result) {
      logger.warn("the token creation time {}({}) is not after password latest modification time {}({})",
                  created,
                  created.toInstant(),
                  lastPasswordReset,
                  lastPasswordReset.toInstant());
    }
    return result;
  }

  private Boolean ignoreTokenExpiration(String token) {
    // here you specify tokens, for that the expiration is ignored
    return false;
  }

  public String generateToken(String userName, String projectID) {
    Objects.requireNonNull(userName);

    Map<String, Object> claims = new HashMap<>();
    claims.put(ConstDef.KEY_PID, projectID);
    return doGenerateToken(claims, userName);
  }

  private String doGenerateToken(Map<String, Object> claims, String subject) {
    final Date createdDate = BaseUtil.getNow();
    final Date expirationDate = calculateExpirationDate(createdDate);
    return Jwts.builder()
               .setClaims(claims)
               .setSubject(subject)
               .setIssuedAt(createdDate)
               .setExpiration(expirationDate)
               .signWith(SignatureAlgorithm.HS512, secret)
               .compact();
  }

  // TODO: should be checked
  public Boolean canTokenBeRefreshed(String token, Date lastPasswordReset) {
    final Date created = getIssuedAtDateFromToken(token);
    return isCreatedAfterLastPasswordReset(created, lastPasswordReset)
        && (!isTokenExpired(token) || ignoreTokenExpiration(token));
  }

  public String refreshToken(String token) {
    final Date createdDate = BaseUtil.getNow();
    final Date expirationDate = calculateExpirationDate(createdDate);

    final Claims claims = getAllClaimsFromToken(token);
    claims.setIssuedAt(createdDate);
    claims.setExpiration(expirationDate);

    return Jwts.builder().setClaims(claims).signWith(SignatureAlgorithm.HS512, secret).compact();
  }

  public boolean validateToken(String token) {
    Objects.requireNonNull(token);

    return !isTokenExpired(token);
  }

  private Date calculateExpirationDate(Date createdDate) {
    return new Date(createdDate.getTime() + expiration * 1000);
  }
}
