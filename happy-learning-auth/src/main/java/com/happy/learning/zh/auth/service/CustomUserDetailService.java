package com.happy.learning.zh.auth.service;

import com.happy.learning.zh.auth.entity.User;
import com.happy.learning.zh.auth.mapper.UserMapper;
import com.happy.learning.zh.auth.security.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class CustomUserDetailService implements UserDetailsService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        List<User> users = userMapper.findByUsernameWithRoles(username);
        if (users.isEmpty()) {
            throw new UsernameNotFoundException("用户不存在");
        }
        //System.out.println(passwordEncoder.encode("123456"));
        // MyBatis 会自动合并多条记录为一个 User 对象
        return convertToUserDetails(users.get(0));
    }

    private UserDetails convertToUserDetails(User user) {
        // 合并角色和权限
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();

        // 处理角色（添加 ROLE_前缀）
        user.getRoles().forEach(role -> {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
            // 处理角色下的权限
            role.getPermissions().forEach(perm ->
                    authorities.add(new SimpleGrantedAuthority(perm.getValue()))
            );
        });

        return new CustomUserDetails(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                user.isEnabled(),
                authorities
        );
    }
}
