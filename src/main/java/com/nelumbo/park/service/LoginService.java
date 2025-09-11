package com.nelumbo.park.service;

import com.nelumbo.park.dto.request.LoginRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

public interface LoginService {
    ResponseEntity<?> authenticate(LoginRequest request, HttpServletRequest httpServletRequest);
}
