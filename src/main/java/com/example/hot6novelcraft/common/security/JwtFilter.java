package com.example.hot6novelcraft.common.security;

import com.example.hot6novelcraft.common.exception.domain.UserExceptionEnum;
import com.example.hot6novelcraft.domain.user.entity.UserDetailsImpl;
import com.example.hot6novelcraft.domain.user.entity.userEnum.UserRole;
import com.example.hot6novelcraft.domain.user.service.UserCacheService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.internal.constraintvalidators.bv.time.futureorpresent.FutureOrPresentValidatorForDate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j(topic = "JwtFilter")
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final RedisUtil redisUtil;
    private final UserDetailsService userDetailsService;
    private final UserCacheService userCacheService;


    // 임시 JWT로만 접근 가능한 URL - 회원가입 시에만 사용
    private static final List<String> TEMP_TOKEN_ALLOWED_USERS
            = List.of("/api/auth/signup/reader", "/api/auth/signup/author");

    // 토큰 없이 통과 가능한 URL
    private static final List<String> PUBLIC_URLS
            = List.of(
                    "/api/auth/signup"
                    ,"/api/auth/login"
                    , "/api/auth/email/check"
                    , "/api/auth/nickname/check");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestURL = request.getRequestURI();

        // 인증 불필요 경로 바로 통과
        if (PUBLIC_URLS.contains(requestURL)) {
            filterChain.doFilter(request, response);
            return;
        }

        // JWT 토큰 유무 검사
        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader == null || !authorizationHeader.startsWith(JwtUtil.BEARER_PREFIX)) {

            log.warn("JWT 토큰이 없거나 형식이 잘못되었습니다. URL : {}", requestURL);
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "JWT 토큰이 없거나 형식이 잘못되었습니다.");
            return;
        }

        // AccessToken 전달 및 유효성 검사
        String accessToken = jwtUtil.substringToken(authorizationHeader);

        if (jwtUtil.validateToken(accessToken)) {

            // Redis 블랙리스트 검사
            if (redisUtil.isBlackList(accessToken)) {

                log.warn("블랙리스트에 등록된 토큰입니다.");
                response.setCharacterEncoding("UTF-8");
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "토큰이 유효하지 않습니다.");
                return;
            }

            // 인증 실패 시 return
            if (!setAuthentication(response, accessToken, requestURL)) {
                return;
            }

            // AccessToken 인증인가 필터 종료
            filterChain.doFilter(request, response);
            return;
        }

        log.info("[Silent Refresh] AccessToken 만료 감지. URL: {}", requestURL);

        String refreshTokenHeader = request.getHeader("Refresh-Token");

        if(refreshTokenHeader == null || !refreshTokenHeader.startsWith(JwtUtil.BEARER_PREFIX)) {

            log.warn("[Silent Refresh] Refresh-Token 헤더가 없습니다.");
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "AccessToken이 만료되었습니다. Refresh-Token을 보내주세요.");
            return;
        }
        String refreshToken = jwtUtil.substringToken(refreshTokenHeader);

        if(!jwtUtil.validateToken(refreshToken)) {

            log.warn("[Silent Refresh] RefreshToken이 만료되었습니다");
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "다시 로그인해주세요.");
            return;
        }

        String email = jwtUtil.extractEmail(refreshToken);
        String savedRefreshToken = userCacheService.getRefreshToken(email);

        if(savedRefreshToken == null || !savedRefreshToken.equals(refreshTokenHeader)) {

            log.warn("[Silent Refresh] Redis에 RefreshToken과 불일치. email: {}", email);
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "RefreshToken이 유효하지 않습니다. 다시 로그인해주세요.");
            return;
        }

        UserDetailsImpl userDetailsImpl = (UserDetailsImpl) userDetailsService.loadUserByUsername(email);
        String newAccessToken = jwtUtil.createAccessToken(
                userDetailsImpl.getUser().getEmail(),
                userDetailsImpl.getUser().getRole()
        );

        response.setHeader("Authorization", newAccessToken);
        response.setHeader("Access-Control-Expose-Headers", "Authorization");
        log.info("[Silent Refresh] 새로운 AccessToken 발급 완료, email: {}", email);

        String pureNewAccessToken = jwtUtil.substringToken(newAccessToken);
        if(!setAuthentication(response, pureNewAccessToken, requestURL)) {
            return;
        }

        // 만료 및 재발행 Silent Refresh 필터 종료
        filterChain.doFilter(request, response);
    }

    // 임시 토큰 인가 확인
    private boolean setAuthentication(HttpServletResponse response, String token, String requestURL) {

        try {
            String email = jwtUtil.extractEmail(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            boolean isTempoToken = userDetails.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals(UserRole.TEMP.name()));

            if(isTempoToken && !TEMP_TOKEN_ALLOWED_USERS.contains(requestURL)) {

                log.warn("임시 토큰으로 허용되지 않은 URL 접근, URL: {}", requestURL);
                sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, "추가 정보 회원가입이 필요합니다.");
                return false;
            }
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            return true;
        } catch (Exception e) {

            log.error("인증 처리 중 오류 발생: {}", e.getMessage());
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,"인증 처리 중 서버 오류가 발생했습니다.");
            return false;
        }
    }

    // 오류 메시지 공통 메서드
    private void sendErrorResponse(HttpServletResponse response, int status, String message) {
        try {
            response.setStatus(status);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(String.format("{\"message\": \"%s\"}", message));
        } catch (IOException e) {
            log.error("에러 응답 전송 실패: {}", e.getMessage());
        }
    }
}