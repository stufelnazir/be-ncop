//package com.ncop;
//
//import com.ncop.auth.*;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.stereotype.Component;
//
//import java.util.Set;
//
//@Component
//public class DataSeeder implements CommandLineRunner {
//
//    private final UserRepository userRepository;
//    private final RoleRepository roleRepository;
//    private final PasswordEncoder passwordEncoder;
//
//    public DataSeeder(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
//        this.userRepository = userRepository;
//        this.roleRepository = roleRepository;
//        this.passwordEncoder = passwordEncoder;
//    }
//
//    @Override
//    public void run(String... args) {
//        for (RoleName rn : RoleName.values()) {
//            roleRepository.findByName(rn).orElseGet(() -> roleRepository.save(new Role(rn)));
//        }
//
//        seedUser("admin@ncop.com", "Admin User", "Admin@123", RoleName.ADMIN);
//        seedUser("sales@ncop.com", "Sales User", "Sales@123", RoleName.SALES);
//        seedUser("qa@ncop.com", "QA User", "Qa@12345", RoleName.QA);
//        seedUser("regulatory@ncop.com", "Regulatory User", "Reg@1234", RoleName.REGULATORY);
//        System.out.println("DEBUG: Total users in DB = " + userRepository.count());
//    }
//
//    private void seedUser(String email, String fullName, String rawPassword, RoleName roleName) {
//        if (userRepository.findByEmail(email).isPresent()) return;
//
//        Role role = roleRepository.findByName(roleName).orElseThrow();
//
//        User user = new User();
//        user.setEmail(email);
//        user.setFullName(fullName);
//        user.setPasswordHash(passwordEncoder.encode(rawPassword));
//        user.setRoles(Set.of(role));
//
//        userRepository.save(user);
//    }
//}