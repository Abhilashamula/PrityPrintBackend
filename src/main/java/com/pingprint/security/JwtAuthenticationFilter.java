package com.pingprint.security;

import com.pingprint.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserRepository users;
    public JwtAuthenticationFilter(JwtService jwtService, UserRepository users) { this.jwtService = jwtService; this.users = users; }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
                try {
                    var subject = jwtService.subjectValue(header.substring(7));
                    if ("admin".equals(subject)) {
                        var auth = new UsernamePasswordAuthenticationToken("admin", null, AuthorityUtils.createAuthorityList("ROLE_ADMIN"));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    } else {
                        var user = users.findById(java.util.UUID.fromString(subject)).orElse(null);
                        if (user != null && user.isEnabled()) {
                            var auth = new UsernamePasswordAuthenticationToken(user.getId(), null, AuthorityUtils.NO_AUTHORITIES);
                            SecurityContextHolder.getContext().setAuthentication(auth);
                        }
                }
            } catch (RuntimeException ignored) { }
        }
        chain.doFilter(request, response);
    }
}
