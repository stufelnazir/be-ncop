package com.ncop.auth.setup;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.ncop.auth.model.Role;
import com.ncop.auth.repository.RoleRepository;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Standalone role initializer. It seeds the roles collection and assigns module-right names
 * according to the requested role mapping. It does not auto-run with the main application.
 */
public class RoleInitializer {

    private static final Map<String, List<String>> ROLE_MODULE_RIGHTS = Map.of(
            "ADMIN", Arrays.asList("DASHBOARD", "USER_MANAGEMENT", "ROLE_MANAGEMENT", "MODULE_RIGHT_MANAGEMENT", "AUTHENTICATION", "SALES", "QA", "QC"),
            "SALES", Arrays.asList("DASHBOARD", "SALES"),
            "QA", Arrays.asList("DASHBOARD", "QA"),
            "QC", Arrays.asList("DASHBOARD", "QC")
    );

    public static void seedRoles(RoleRepository roleRepository) {
        List<String> requiredRoles = List.of("ADMIN", "SALES", "QA", "QC");

        for (String roleName : requiredRoles) {
            List<String> moduleRights = new ArrayList<>(ROLE_MODULE_RIGHTS.getOrDefault(roleName, List.of()));
            Role existing = roleRepository.findByName(roleName).orElse(null);
            if (existing == null) {
                Role role = new Role(roleName, moduleRights);
                roleRepository.save(role);
            } else {
                existing.setModuleRights(moduleRights);
                roleRepository.save(existing);
            }
        }
    }

    public static void main(String[] args) {
        Properties props = new Properties();
        String[] filesToTry = {"application-dev.properties", "application-prod.properties", "application-int.properties"};
        for (String file : filesToTry) {
            try (InputStream in = RoleInitializer.class.getClassLoader().getResourceAsStream(file)) {
                if (in != null) {
                    props.load(in);
                }
            } catch (IOException ignored) {
            }
        }

        String uri = props.getProperty("spring.mongodb.uri");
        if (uri == null || uri.isBlank()) {
            uri = System.getenv("MONGODB_URI");
            if (uri == null || uri.isBlank()) {
                System.err.println("ERROR: MongoDB URI not found. Set spring.mongodb.uri or MONGODB_URI env var.");
                System.exit(2);
            }
        }

        String dbName = props.getProperty("spring.mongodb.database");
        if (dbName == null || dbName.isBlank()) dbName = System.getenv("MONGODB_DATABASE");

        com.mongodb.ConnectionString cs = new com.mongodb.ConnectionString(uri);
        if ((dbName == null || dbName.isBlank()) && cs.getDatabase() != null) {
            dbName = cs.getDatabase();
        }

        if (dbName == null || dbName.isBlank()) {
            System.err.println("ERROR: MongoDB database name not found. Set spring.mongodb.database, MONGODB_DATABASE, or include it in the URI.");
            System.exit(2);
        }

        System.out.println("Using MongoDB URI: " + (uri.length() > 40 ? uri.substring(0, 40) + "..." : uri));
        System.out.println("Using MongoDB database: " + dbName);

        try (MongoClient client = MongoClients.create(uri)) {
            MongoDatabase db = client.getDatabase(dbName);
            MongoCollection<Document> roles = db.getCollection("roles");

            roles.drop();

            for (String roleName : ROLE_MODULE_RIGHTS.keySet()) {
                List<String> moduleRights = new ArrayList<>(ROLE_MODULE_RIGHTS.get(roleName));
                Document found = roles.find(Filters.eq("name", roleName)).first();
                if (found == null) {
                    Document doc = new Document("name", roleName).append("moduleRights", moduleRights);
                    roles.insertOne(doc);
                    ObjectId id = doc.getObjectId("_id");
                    System.out.println("Inserted role '" + roleName + "' with id " + id.toHexString() + " and moduleRights=" + moduleRights);
                } else {
                    roles.updateOne(Filters.eq("_id", found.get("_id")), Updates.set("moduleRights", moduleRights));
                    ObjectId id = found.getObjectId("_id");
                    System.out.println("Updated role '" + roleName + "' with id " + id.toHexString() + " and moduleRights=" + moduleRights);
                }
            }

            System.out.println("Role seeding completed.");
        } catch (Exception e) {
            System.err.println("Role seeding failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(2);
        }
    }
}
