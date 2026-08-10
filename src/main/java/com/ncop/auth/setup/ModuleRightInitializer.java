package com.ncop.auth.setup;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Standalone initializer for module rights. It seeds the module_rights collection in MongoDB
 * without running automatically with the main Spring Boot application.
 */
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

        List<ModuleSeed> seeds = new ArrayList<>();
        seeds.add(new ModuleSeed("DASHBOARD", "Dashboard access"));
        seeds.add(new ModuleSeed("USER_MANAGEMENT", "Manage users"));
        seeds.add(new ModuleSeed("ROLE_MANAGEMENT", "Manage roles"));
        seeds.add(new ModuleSeed("MODULE_RIGHT_MANAGEMENT", "Manage module rights"));
        seeds.add(new ModuleSeed("AUTHENTICATION", "Authentication and session operations"));
        seeds.add(new ModuleSeed("SALES", "Sales module access"));
        seeds.add(new ModuleSeed("QA", "QA module access"));
        seeds.add(new ModuleSeed("QC", "QC module access"));
        seeds.add(new ModuleSeed("PRODUCT_MASTER", "Manage product master data"));
        seeds.add(new ModuleSeed("CLIENT_MASTER", "Manage client master data"));
        seeds.add(new ModuleSeed("SALES", "Manage sales data"));
        seeds.add(new ModuleSeed("QA", "Show QA Data"));
        seeds.add(new ModuleSeed("QC", "Show QC Data"));
        seeds.add(new ModuleSeed("DASHBOARD", "Show Dashboard Data"));

        try (MongoClient client = MongoClients.create(uri)) {
            MongoDatabase db = client.getDatabase(dbName);
            MongoCollection<Document> moduleRights = db.getCollection("module_rights");

            for (ModuleSeed seed : seeds) {
                Document existing = moduleRights.find(Filters.eq("name", seed.name())).first();
                if (existing != null) {
                    ObjectId id = existing.getObjectId("_id");
                    System.out.println("Module right '" + seed.name() + "' already exists with id " + id.toHexString());
                    continue;
                }

                Document doc = new Document("name", seed.name())
                        .append("description", seed.description());
                moduleRights.insertOne(doc);
                ObjectId insertedId = doc.getObjectId("_id");
                System.out.println("Inserted module right '" + seed.name() + "' with id " + insertedId.toHexString());
            }

            System.out.println("Module right seeding completed.");
        } catch (Exception e) {
            System.err.println("Module right seeding failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(2);
        }
    }

    private record ModuleSeed(String name, String description) {}
}
