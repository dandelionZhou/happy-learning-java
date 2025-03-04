package com.happy.learning.zh.auth.config;

import com.happy.learning.zh.auth.security.SmsCodeAuthenticationConverter;
import com.happy.learning.zh.auth.security.SmsCodeAuthenticationProvider;
import com.happy.learning.zh.auth.service.CustomUserDetailService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientCredentialsAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.web.authentication.OAuth2AuthorizationCodeAuthenticationConverter;
import org.springframework.security.oauth2.server.authorization.web.authentication.OAuth2ClientCredentialsAuthenticationConverter;
import org.springframework.security.oauth2.server.authorization.web.authentication.OAuth2RefreshTokenAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.DelegatingAuthenticationConverter;

import java.util.Arrays;

// SecurityConfig.java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final RegisteredClientRepository registeredClientRepository;
    private final AuthorizationServerSettings authorizationServerSettings;
    private final OAuth2TokenGenerator<?> tokenGenerator;
    private final OAuth2AuthorizationService authorizationService;
    private final SmsCodeAuthenticationConverter smsCodeAuthenticationConverter;

    // 新增依赖注入
    public SecurityConfig(RegisteredClientRepository registeredClientRepository,
                          AuthorizationServerSettings authorizationServerSettings,
                          OAuth2TokenGenerator<?> tokenGenerator,
                          OAuth2AuthorizationService authorizationService,
                          SmsCodeAuthenticationConverter smsCodeAuthenticationConverter) {
        this.registeredClientRepository = registeredClientRepository;
        this.authorizationServerSettings = authorizationServerSettings;
        this.tokenGenerator = tokenGenerator;
        this.authorizationService = authorizationService;
        this.smsCodeAuthenticationConverter = smsCodeAuthenticationConverter;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerFilterChain(HttpSecurity http,
                                                              SmsCodeAuthenticationProvider smsCodeAuthenticationProvider) throws Exception {
        OAuth2AuthorizationServerConfigurer configurer = new OAuth2AuthorizationServerConfigurer();

        /*http.addFilterBefore(
                AuthorizationFilter.class
        );*/

        http
                .securityMatcher("/oauth2/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/oauth2/jwks", "/oauth2/client/**", "/sms/code").permitAll()
                        .anyRequest().authenticated()
                )
                .csrf(csrf -> csrf.ignoringRequestMatchers("/oauth2/token", "/sms/code"))
                .apply(configurer)
                .authorizationService(authorizationService)
                .tokenGenerator(tokenGenerator)
                .registeredClientRepository(registeredClientRepository)
                .authorizationServerSettings(authorizationServerSettings);

        // 配置客户端认证提供器
        configurer.tokenEndpoint(token ->
                token.accessTokenRequestConverter(
                                new DelegatingAuthenticationConverter(
                                        Arrays.asList(
                                                new OAuth2AuthorizationCodeAuthenticationConverter(),
                                                new OAuth2RefreshTokenAuthenticationConverter(),
                                                new OAuth2ClientCredentialsAuthenticationConverter(),
                                                smsCodeAuthenticationConverter // 添加短信验证码转换器
                                        )
                                ))
                        .authenticationProvider(clientCredentialsProvider())
        );

        // 注册认证提供器
        http.authenticationProvider(smsCodeAuthenticationProvider);
        http.authenticationProvider(clientCredentialsProvider());

        return http.build();
    }

    // 新增短信验证码端点
    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/sms/code").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(Customizer.withDefaults());
        return http.build();
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
                        tokenGenerator
                );
        return provider;
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return new CustomUserDetailService();
    }

    /*@Bean
    public ApplicationRunner debugRunner(RegisteredClientRepository repo) {
        return args -> {
            System.out.println("已注册客户端：");
            repo.findByClientId("web-client"); // 断点调试此处
        };
    }*/

    public static void main(String[] args) {
        System.out.println(new BCryptPasswordEncoder().encode("web-secret"));
    }
}
