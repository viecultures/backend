package com.viecultures.backend.service;

import com.viecultures.backend.dto.AuthRequest;
import com.viecultures.backend.dto.AuthResponse;
import com.viecultures.backend.dto.GoogleLoginRequest;
import com.viecultures.backend.dto.RegisterRequest;

public interface AuthService {

    AuthResponse login(AuthRequest request);

    AuthResponse register(RegisterRequest request);

    AuthResponse loginWithGoogle(GoogleLoginRequest request);

    AuthResponse.UserInfo getCurrentUser(String email);
}
