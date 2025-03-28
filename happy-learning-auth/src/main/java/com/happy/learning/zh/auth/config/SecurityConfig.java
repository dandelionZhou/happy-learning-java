package com.happy.learning.zh.auth.config;

import com.happy.learning.zh.auth.security.*;
import com.happy.learning.zh.auth.service.CustomUserDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientCredentialsAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.*;
import org.springframework.security.oauth2.server.authorization.web.authentication.OAuth2AuthorizationCodeAuthenticationConverter;
import org.springframework.security.oauth2.server.authorization.web.authentication.OAuth2ClientCredentialsAuthenticationConverter;
import org.springframework.security.oauth2.server.authorization.web.authentication.OAuth2RefreshTokenAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.DelegatingAuthenticationConverter;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

import java.util.Arrays;

// SecurityConfig.java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final RegisteredClientRepository registeredClientRepository;
    private final AuthorizationServerSettings authorizationServerSettings;
    private final OAuth2TokenGenerator<?> jwtTokenGenerator;
    private final OAuth2AuthorizationService authorizationService;
    private final SmsCodeAuthenticationConverter smsCodeAuthenticationConverter;
    private final CustomAuthorizationSuccessHandler customAuthorizationSuccessHandler;
    private final CustomAuthorizationFailureHandler customAuthorizationFailureHandler;
    private final CustomAuthorizationCodeGenerator customAuthorizationCodeGenerator; // 注入自定义生成器


    //oath2端点配置
    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerFilterChain(
            HttpSecurity http,
            SmsCodeAuthenticationProvider smsCodeAuthenticationProvider,
            RegisteredClientRepository registeredClientRepository,
            OAuth2AuthorizationService authorizationService,
            AuthorizationServerSettings authorizationServerSettings) throws Exception {

        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
                new OAuth2AuthorizationServerConfigurer();

        http.securityMatcher("/oauth2/**")
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login"))
                )
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/oauth2/jwks", "/oauth2/client/**", "/sms/code").permitAll()
                        .anyRequest().authenticated()
                )
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/oauth2/token", "/oauth2/authorize")
                )
                .sessionManagement(session -> session
                        .sessionFixation().migrateSession()
                )
                .userDetailsService(userDetailsService())
                .with(authorizationServerConfigurer, configurer -> {
                    configurer
                            .authorizationService(authorizationService)
                            .tokenGenerator(tokenGenerator())
                            .registeredClientRepository(registeredClientRepository)
                            .authorizationServerSettings(authorizationServerSettings);

                    // 配置 Token 端点
                    configurer.tokenEndpoint(token -> {
                        token.accessTokenRequestConverter(
                                new DelegatingAuthenticationConverter(
                                        Arrays.asList(
                                                new OAuth2AuthorizationCodeAuthenticationConverter(),
                                                new OAuth2RefreshTokenAuthenticationConverter(),
                                                new OAuth2ClientCredentialsAuthenticationConverter(),
                                                new SmsCodeAuthenticationConverter() // 自定义转换器
                                        )
                                )
                        );
                        token.authenticationProvider(clientCredentialsProvider());
                        token.authenticationProvider(smsCodeAuthenticationProvider);
                    });

                    // 配置授权端点（可选）
                    configurer.authorizationEndpoint(authorization ->{
                                authorization.authorizationResponseHandler(customAuthorizationSuccessHandler);
                                authorization.errorResponseHandler(customAuthorizationFailureHandler);
                                authorization.consentPage("/oauth2/consent");
                            }

                    );

                });
        return http.build();
    }

    // 其它端点默认配置：
    // 新增短信验证码端点
    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder) throws Exception {
        http.securityMatcher("/login", "/sms/**")
                .authenticationProvider(daoAuthenticationProvider())
                // 启用 OAuth2 资源服务器配置
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder) ))// 配置 JWT 解码器
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/sms/code", "/login").permitAll()
                        .anyRequest().authenticated()
                )
                .csrf(csrf -> csrf.ignoringRequestMatchers("/sms/code", "/sms/test"))
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", false)
                        .permitAll());
                //.httpBasic(Customizer.withDefaults());
        return http.build();
    }

    // 配置 OAuth2 Token 生成器组合（授权码 + JWT）
    @Bean
    public OAuth2TokenGenerator<?> tokenGenerator() {
        OAuth2AccessTokenGenerator accessTokenGenerator = new OAuth2AccessTokenGenerator();
        OAuth2RefreshTokenGenerator refreshTokenGenerator = new OAuth2RefreshTokenGenerator();
        // 组合生成器：授权码使用自定义生成器，访问令牌使用 JWT 生成器
        return new DelegatingOAuth2TokenGenerator(
                customAuthorizationCodeGenerator,
                jwtTokenGenerator,
                accessTokenGenerator,
                refreshTokenGenerator
        );
    }

    // 认证提供器
    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService());
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        // 必须使用 DelegatingPasswordEncoder
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider clientCredentialsProvider() {
        OAuth2ClientCredentialsAuthenticationProvider provider =
                new OAuth2ClientCredentialsAuthenticationProvider(
                        authorizationService,
                        tokenGenerator()
                );
        return provider;
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return new CustomUserDetailService();
    }

}
