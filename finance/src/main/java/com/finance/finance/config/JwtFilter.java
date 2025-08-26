package com.finance.finance.config;

import com.finance.finance.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {
// onceperrequest spring tarafından her bir http isteği için yalnızca1 kes çalışması garanti demek
    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        System.out.println("Processing request: " + request.getRequestURI());
// gelen isteğin authorilization isimli http başlığını okıycak
        final String authorizationHeader = request.getHeader("Authorization");
        System.out.println("Authorization header: " + authorizationHeader);

        String username = null;
        String jwt = null;
// başlık var mı yok mu ve bearer ile başlıyo mu kontrolü eğer doğruysa bearer kısmını çıkarıp
        //JWT elde edilir
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            System.out.println("JWT token: " + jwt);

            try {
                // tokenın içeriğini anlamak için kullanıcı adını çıkarıyor
                // tokenın süresi dolarsa diye try catch içinde
                username = jwtUtil.extractUsername(jwt);
                System.out.println("Extracted username: " + username);
            } catch (Exception e) {
                System.out.println("Error extracting username: " + e.getMessage());
            }
        }
// tokendan ge.erli bi kullanıcı adı geldi im geelmedi mi ve gereksiz yere tekrar kontrol edilmesin diye
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            if (jwtUtil.validateToken(jwt)) {
                //token ın imzası sunucudaki secret key ile eşleşiyor mu diye bakçak
                System.out.println("Token is valid");
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(username, null, null);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                System.out.println("Token is invalid");
            }
        } else {
            System.out.println("No username or already authenticated");
        }
// bi sonraki aşamaya geçmesi için bu controller da olabilir başka güvenlik filtresi de olabilir
        filterChain.doFilter(request, response);
    }
}