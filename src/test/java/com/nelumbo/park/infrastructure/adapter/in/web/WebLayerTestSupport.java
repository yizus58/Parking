package com.nelumbo.park.infrastructure.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nelumbo.park.application.port.out.UserRepositoryPort;
import com.nelumbo.park.infrastructure.config.JacksonConfig;
import com.nelumbo.park.infrastructure.config.security.CustomAuthenticationEntryPoint;
import com.nelumbo.park.infrastructure.config.security.JwtAuthenticationFilter;
import com.nelumbo.park.infrastructure.config.security.JwtService;
import com.nelumbo.park.infrastructure.config.security.SecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Web slice tests run with the real security configuration (JWT filter,
 * entry point, method security) and the real Jackson configuration, so status
 * codes and JSON match production. Use cases are mocked per test class.
 *
 * CSRF is disabled (stateless JWT API), so requests carry only the Bearer token, as real clients do.
 */
@Import({SecurityConfig.class, CustomAuthenticationEntryPoint.class, JwtAuthenticationFilter.class, JacksonConfig.class})
abstract class WebLayerTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @MockitoBean
    protected JwtService jwtService;

    @MockitoBean
    protected UserRepositoryPort userRepositoryPort;

    protected static MockHttpServletRequestBuilder post(String url) {
        return MockMvcRequestBuilders.post(url);
    }

    protected static MockHttpServletRequestBuilder put(String url) {
        return MockMvcRequestBuilders.put(url);
    }

    protected static MockHttpServletRequestBuilder delete(String url) {
        return MockMvcRequestBuilders.delete(url);
    }
}
