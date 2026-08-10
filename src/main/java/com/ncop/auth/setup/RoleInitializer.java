package com.ncop.auth.setup;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.ncop.auth.Role;
import com.ncop.auth.RoleRepository;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.io.InputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Helper class to seed roles collection. This is intentionally NOT a Spring component so it won't run
 * automatically with the main BeNcopApplication. It now provides a public main(...) so executing this class
 * (clicking Run in the IDE) will seed the roles immediately.
 */
public class RoleInitializer {

    public static void seedRoles(RoleRepository roleRepository) {
        List<String> requiredRoles = List.of("ADMIN", "SALES", "QA", "QC");

        for (String roleName : requiredRoles) {
            if (!roleRepository.existsByName(roleName)) {
                Role role = new Role(roleName, new ArrayList<>());
                roleRepository.save(role);
            }
        }
    }

    /**
     * Standalone entrypoint. Running this class directly will seed the roles using the MongoDB driver.
     * It will NOT participate in the main Spring Boot lifecycle.
     *
     * Env vars:
     *  - MONGODB_URI (default: mongodb://localhost:27017)
     *  - MONGODB_DATABASE (default: be-ncop)
     */
    public static void main(String[] args) {
        // Read Mongo configuration from application.properties on the classpath if available,
        // otherwise fall back to environment variables and finally sensible defaults.
        Properties props = new Properties();
        String uri = System.getenv().getOrDefault("MONGODB_URI", "mongodb+srv://stufelnazir_db_user:9JADXQIgxTqoppFm@ncop-dev.71bsnb7.mongodb.net/?appName=ncop-dev");
        String dbName = System.getenv().getOrDefault("MONGODB_DATABASE", "dev");

        try (InputStream in = RoleInitializer.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (in != null) {
                props.load(in);
                String propUri = props.getProperty("spring.data.mongodb.uri");
                String propDb = props.getProperty("spring.data.mongodb.database");
                if (propUri != null && !propUri.isBlank()) uri = propUri;
                if (propDb != null && !propDb.isBlank()) dbName = propDb;
            }
        } catch (IOException e) {
            System.err.println("Failed to read application.properties from classpath: " + e.getMessage());
        }

        try (MongoClient client = MongoClients.create(uri)) {
            MongoDatabase db = client.getDatabase(dbName);
            MongoCollection<Document> roles = db.getCollection("roles");

            List<String> requiredRoles = Arrays.asList("ADMIN", "SALES", "QA", "QC");

            for (String roleName : requiredRoles) {
                Document found = roles.find(Filters.eq("name", roleName)).first();
                if (found == null) {
                    Document doc = new Document("name", roleName)
                            .append("moduleRights", Arrays.asList());
                    roles.insertOne(doc);
                    ObjectId id = doc.getObjectId("_id");
                    System.out.println("Inserted role '" + roleName + "' with id " + id.toHexString());
                } else {
                    ObjectId id = found.getObjectId("_id");
                    System.out.println("Role '" + roleName + "' already exists with id " + id.toHexString());
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
