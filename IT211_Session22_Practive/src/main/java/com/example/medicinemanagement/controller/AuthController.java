package com.example.medicinemanagement.controller;

import com.example.medicinemanagement.dto.request.LoginRequest;
import com.example.medicinemanagement.dto.request.RegisterRequest;
import com.example.medicinemanagement.dto.request.TokenRefreshRequest;
import com.example.medicinemanagement.dto.response.JwtRes;
import com.example.medicinemanagement.dto.response.TokenRefreshResponse;
import com.example.medicinemanagement.entity.RefreshToken;
import com.example.medicinemanagement.entity.User;
import com.example.medicinemanagement.repository.UserRepository;
import com.example.medicinemanagement.security.JwtTokenProvider;
import com.example.medicinemanagement.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;

    // Nếu project của bạn đã có API register thì giữ code cũ.
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        // TODO: Gọi AuthService.register(request) của project bạn nếu đã có.
        return ResponseEntity.ok("Register API - giữ logic cũ của project tại đây");
    }

    @PostMapping("/login")
    public ResponseEntity<JwtRes> login(@RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String accessToken = jwtTokenProvider.generateToken(authentication);

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user: " + request.getUsername()));

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

        JwtRes response = JwtRes.builder()
                .fullName(user.getFullName())
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .role(user.getRole().name())
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<TokenRefreshResponse> refreshToken(@RequestBody TokenRefreshRequest request) {
        RefreshToken refreshToken = refreshTokenService.findByToken(request.getRefreshToken());
        RefreshToken validToken = refreshTokenService.verifyExpiration(refreshToken);

        User user = validToken.getUser();
        String newAccessToken = jwtTokenProvider.generateTokenFromUsername(user.getUsername());

        TokenRefreshResponse response = TokenRefreshResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(validToken.getToken())
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).body("Người dùng chưa đăng nhập.");
        }

        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user hiện tại."));

        // Cách 1: Xóa toàn bộ refresh token của user.
        refreshTokenService.deleteByUser(user);

        // Cách 2: Nếu muốn giữ lịch sử token thì dùng revoke thay vì delete:
        // refreshTokenService.revokeAllByUser(user);

        SecurityContextHolder.clearContext();

        return ResponseEntity.ok("Đăng xuất thành công. Refresh Token đã bị thu hồi.");
    }
}
