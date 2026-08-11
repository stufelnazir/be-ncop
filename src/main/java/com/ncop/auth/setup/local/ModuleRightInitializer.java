package com.ncop.auth.setup.local;

import com.ncop.auth.model.ModuleRight;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ModuleRightInitializer {

    public static void main(String[] args) {
        Properties props = new Properties();
        String[] filesToTry = {"application-dev.properties"};
        for (String file : filesToTry) {
            try (InputStream in = ModuleRightInitializer.class.getClassLoader().getResourceAsStream(file)) {
                if (in != null) {
                    props.load(in);
                }
            } catch (IOException ignored) {
            }
        }

        String baseUrl = resolveBaseUrl(props, args);
        System.out.println("Using module-right controller base URL: " + baseUrl);

        RestTemplate restTemplate = new RestTemplate();
        List<ModuleSeed> seeds = new ArrayList<>();
        seeds.add(new ModuleSeed("DASHBOARD", "Dashboard", "Dashboard access"));
        seeds.add(new ModuleSeed("USER_MANAGEMENT", "User Management", "Manage users"));
        seeds.add(new ModuleSeed("ROLE_MANAGEMENT", "Role Management", "Manage roles"));
        seeds.add(new ModuleSeed("MODULE_RIGHT_MANAGEMENT", "Module Right Management", "Manage module rights"));
        seeds.add(new ModuleSeed("AUTHENTICATION", "Authentication", "Authentication and session operations"));
        seeds.add(new ModuleSeed("SALES", "Sales", "Sales module access"));
        seeds.add(new ModuleSeed("QA", "Quality Assurance", "QA module access"));
        seeds.add(new ModuleSeed("QC", "Quality Control", "QC module access"));
        seeds.add(new ModuleSeed("PRODUCT_MASTER", "Product Master", "Manage product master data"));
        seeds.add(new ModuleSeed("CLIENT_MASTER", "Client Master", "Manage client master data"));

        try {
            ModuleRight[] existingModules = restTemplate.getForObject(baseUrl + "/auth/module-rights", ModuleRight[].class);
            if (existingModules != null) {
                for (ModuleRight existing : existingModules) {
                    restTemplate.delete(baseUrl + "/auth/module-rights/{id}", existing.getId());
                    System.out.println("Deleted module right '" + existing.getName() + "' via controller");
                }
            }

            for (ModuleSeed seed : seeds) {
                ModuleRight moduleRight = new ModuleRight();
                moduleRight.setName(seed.name());
                moduleRight.setLabel(seed.label());
                moduleRight.setDescription(seed.description());

                ModuleRight created = restTemplate.postForObject(baseUrl + "/auth/module-rights", moduleRight, ModuleRight.class);
                System.out.println("Created module right '" + created.getName() + "' via controller");
            }

            System.out.println("Module right seeding completed via controller.");
        } catch (Exception e) {
            System.err.println("Module right seeding failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(2);
        }
    }

    private static String resolveBaseUrl(Properties props, String[] args) {
        if (args != null && args.length > 0 && args[0] != null && !args[0].isBlank()) {
            return args[0].trim();
        }

        String port = props.getProperty("server.port", "8080");
        String host = props.getProperty("server.address", "localhost");
        String contextPath = props.getProperty("server.servlet.context-path", "");

        if (contextPath == null || contextPath.isBlank()) {
            return "http://" + host + ":" + port;
        }
        return "http://" + host + ":" + port + contextPath;
    }

    private record ModuleSeed(String name, String label, String description) {}
}
