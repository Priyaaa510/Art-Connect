package com.example.ArtConnect.security;

import java.util.Optional;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.example.ArtConnect.exception.ApiException;
import com.example.ArtConnect.model.User;
import com.example.ArtConnect.service.AuthService;

@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {
    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;

    public CurrentUserArgumentResolver(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && User.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        User user = bearerToken(webRequest.getHeader(HttpHeaders.AUTHORIZATION))
                .flatMap(authService::authenticate)
                .orElse(null);
        CurrentUser annotation = parameter.getParameterAnnotation(CurrentUser.class);
        if (user == null && annotation != null && annotation.required()) {
            throw ApiException.unauthorized("Please log in to continue");
        }
        return user;
    }

    public static Optional<String> bearerToken(String header) {
        if (header == null || !header.startsWith(BEARER_PREFIX)) return Optional.empty();
        String token = header.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? Optional.empty() : Optional.of(token);
    }
}
