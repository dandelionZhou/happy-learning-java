package com.happy.learning.zh.auth.security;

import com.happy.learning.zh.auth.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

public class CustomUserDetails extends User implements UserDetails {
    private final String userId;
    private final String username;
    private final String password;
    private final boolean enabled;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(String userId, String username, String password, boolean enabled,
                             Collection<? extends GrantedAuthority> authorities) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.enabled = enabled;
        this.authorities = authorities;
    }

    // 实现 UserDetails 接口方法
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // 根据业务需求实现
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // 根据业务需求实现
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // 根据业务需求实现
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public String getUserId() {
        return this.userId;
    }
}
