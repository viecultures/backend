package com.viecultures.backend.modules.auth.service;

import com.viecultures.backend.modules.auth.dto.AuthRequest;
import com.viecultures.backend.modules.auth.dto.AuthResponse;
import com.viecultures.backend.modules.auth.dto.GoogleLoginRequest;
import com.viecultures.backend.modules.auth.dto.RegisterRequest;

public interface AuthService {

    AuthResponse login(AuthRequest request);

    AuthResponse register(RegisterRequest request);

    AuthResponse loginWithGoogle(GoogleLoginRequest request);

    AuthResponse.UserInfo getCurrentUser(String email);
}
