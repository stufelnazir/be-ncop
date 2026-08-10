package com.ncop;

//import com.ncop.auth.*;
//import com.ncop.auth.enums.UserStatus;
//import com.ncop.auth.enums.UserType;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.stereotype.Component;
//
//import java.util.List;
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
//        // Seed default roles
//        seedRole("ROLE_ADMIN", List.of("CLIENT_READ", "CLIENT_WRITE", "RFQ_READ", "RFQ_WRITE", "USER_MANAGE"));
//        seedRole("ROLE_SALES", List.of("CLIENT_READ", "CLIENT_WRITE", "RFQ_READ", "RFQ_WRITE"));
//        seedRole("ROLE_QA", List.of("CLIENT_READ", "RFQ_READ"));
//        seedRole("ROLE_REGULATORY", List.of("CLIENT_READ", "RFQ_READ"));
//
//        // Seed default users
//        Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElseThrow();
//        seedUser("admin@ncop.com", "Admin", "User", "Admin@123", adminRole, UserType.ADMIN);
//
//        Role salesRole = roleRepository.findByName("ROLE_SALES").orElseThrow();
//        seedUser("sales@ncop.com", "Sales", "User", "Sales@123", salesRole, UserType.EMPLOYEE);
//
//        System.out.println("DEBUG: Total users in DB = " + userRepository.count());
//    }
//
//    private void seedRole(String name, List<String> moduleRights) {
//        if (roleRepository.findByName(name).isEmpty()) {
//            Role role = new Role(name, moduleRights);
//            roleRepository.save(role);
//        }
//    }
//
//    private void seedUser(String email, String firstName, String lastName,
//                          String rawPassword, Role role, UserType userType) {
//        if (userRepository.findByEmail(email).isPresent()) return;
//
//        User user = new User();
//        user.setEmail(email);
//        user.setUsername(email);  // username = email
//        user.setPassword(passwordEncoder.encode(rawPassword));
//        user.setFirstName(firstName);
//        user.setLastName(lastName);
//        user.setRoleIds(List.of(role.getRoleId()));
//        user.setUserStatus(UserStatus.ACTIVE);
//        user.setUserType(userType);
//
//        userRepository.save(user);
//    }
//}