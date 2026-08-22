package com.ncop.auth.setup;

import com.mongodb.ConnectionString;
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
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Standalone product catalogue seeder for Nourish Pharmaceutical Pvt. Ltd.
 *
 * Follows the same pattern as UserInitializer / RoleInitializer / ModuleRightInitializer:
 *  - Reads MongoDB URI + DB name from application-dev.properties (classpath),
 *    evaluates placeholders, or falls back to MONGODB_URI / MONGODB_URI_DEV env vars
 *    or mongodb://localhost:27017.
 *  - Does NOT drop the collection — skips any product whose brandName already exists.
 *  - Seeds: Diabetic Care Tablets, Capsules (GI + Cardiac), and Oral Liquids
 *    exactly as listed in the Nourish Pharmaceutical product catalogue.
 *
 * Run via IDE: right-click → Run 'ProductInitializer.main()'
 */
public class ProductInitializer {

    public static void main(String[] args) {
        // ── 1. Load properties ──────────────────────────────────────────────
        Properties props = new Properties();
        String[] filesToTry = {
                "application-dev.properties",
                "application-prod.properties",
                "application-int.properties"
        };
        for (String f : filesToTry) {
            try (InputStream in = ProductInitializer.class.getClassLoader().getResourceAsStream(f)) {
                if (in != null) {
                    props.load(in);
                    System.out.println("Loaded config from: " + f);
                    break;
                }
            } catch (IOException ignored) {
            }
        }

        // ── 2. Resolve URI & DB ─────────────────────────────────────────────
        String uri = resolveProperty(props.getProperty("spring.mongodb.uri"));
        if (uri == null || uri.isBlank()) {
            uri = System.getenv("MONGODB_URI");
            if (uri == null || uri.isBlank()) {
                uri = System.getenv("MONGODB_URI_DEV");
            }
            if (uri == null || uri.isBlank()) {
                uri = "mongodb://localhost:27017";
            }
        }

        String dbName = resolveProperty(props.getProperty("spring.mongodb.database"));
        if (dbName == null || dbName.isBlank()) {
            dbName = System.getenv("MONGODB_DATABASE");
            if (dbName == null || dbName.isBlank()) {
                dbName = System.getenv("MONGODB_DATABASE_DEV");
            }
        }

        try {
            ConnectionString cs = new ConnectionString(uri);
            if ((dbName == null || dbName.isBlank()) && cs.getDatabase() != null) {
                dbName = cs.getDatabase();
            }
        } catch (Exception ignored) {
        }

        if (dbName == null || dbName.isBlank()) {
            dbName = "dev";
        }

        System.out.println("Connecting to: " + (uri.length() > 40 ? uri.substring(0, 40) + "..." : uri));
        System.out.println("Database     : " + dbName);

        // ── 3. Seed ─────────────────────────────────────────────────────────
        try (MongoClient client = MongoClients.create(uri)) {
            MongoDatabase db = client.getDatabase(dbName);
            MongoCollection<Document> products = db.getCollection("products");

            products.drop();

            List<ProductSpec> catalogue = buildCatalogue();
            AtomicInteger inserted = new AtomicInteger(0);
            AtomicInteger skipped  = new AtomicInteger(0);
            int counter = (int) products.countDocuments() + 1; // start code after existing count

            for (ProductSpec spec : catalogue) {
                Document existing = products.find(Filters.eq("brandName", spec.brandName)).first();
                if (existing != null) {
                    System.out.println("SKIP  '" + spec.brandName + "' — already exists.");
                    skipped.incrementAndGet();
                    continue;
                }

                String code = String.format("PROD-%06d", counter++);

                List<Document> ingredientDocs = new ArrayList<>();
                for (Ingredient ing : spec.ingredients) {
                    ingredientDocs.add(new Document()
                            .append("api",         ing.api)
                            .append("strength",    ing.strength)
                            .append("unit",        ing.unit)
                            .append("pharmacopeia", ing.pharmacopeia));
                }

                Document doc = new Document()
                        .append("productCode",      code)
                        .append("brandName",        spec.brandName)
                        .append("category",         spec.category)
                        .append("therapeuticClass", spec.therapeuticClass)
                        .append("dosageForm",       spec.dosageForm)
                        .append("dosageVariant",    spec.dosageVariant)
                        .append("ingredients",      ingredientDocs)
                        .append("composition",      spec.composition)
                        .append("packaging",        spec.packaging)
                        .append("moq",              5000L)
                        .append("currency",         "USD")
                        .append("shelfLife",        "24 Months")
                        .append("storageCondition", "Store below 25°C in a dry place. Protect from light.")
                        .append("status",           "ACTIVE")
                        .append("documents",        new ArrayList<>())
                        .append("createdOn",        new Date())
                        .append("lastUpdatedOn",    new Date());

                products.insertOne(doc);
                ObjectId id = doc.getObjectId("_id");
                System.out.println("INSERT '" + spec.brandName + "' → " + code + " (id=" + id.toHexString() + ")");
                inserted.incrementAndGet();
            }

            System.out.println("\n✓ Product seeding completed.");
            System.out.println("  Inserted : " + inserted.get());
            System.out.println("  Skipped  : " + skipped.get());
            System.out.println("  Total    : " + products.countDocuments());

        } catch (Exception e) {
            System.err.println("Product seeding failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(2);
        }
    }

    private static String resolveProperty(String raw) {
        if (raw == null || raw.isBlank()) return null;
        raw = raw.trim();
        if (raw.startsWith("${") && raw.endsWith("}")) {
            String inner = raw.substring(2, raw.length() - 1);
            String varName = inner;
            String defaultValue = null;
            if (inner.contains(":")) {
                int idx = inner.indexOf(":");
                varName = inner.substring(0, idx);
                defaultValue = inner.substring(idx + 1);
            }
            String envVal = System.getenv(varName);
            if (envVal != null && !envVal.isBlank()) {
                return envVal;
            }
            return defaultValue;
        }
        return raw;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CATALOGUE DATA — Nourish Pharmaceutical Pvt. Ltd.
    // ══════════════════════════════════════════════════════════════════════════

    private static List<ProductSpec> buildCatalogue() {
        List<ProductSpec> list = new ArrayList<>();

        // ── TABLETS → Diabetic Care ──────────────────────────────────────────
        String diabeticCare = "Endocrine & Metabolic";
        String tablet       = "Tablet";

        list.add(new ProductSpec(
                "Dapagliflozin Tablets 5mg",
                diabeticCare, "SGLT-2 Inhibitor", tablet, "Film Coated Tablet",
                "1x10 Alu Alu / Blister",
                ing("Dapagliflozin", "5", "mg", "IP"),
                "Dapagliflozin 5mg Film Coated Tablet"));

        list.add(new ProductSpec(
                "Dapagliflozin Tablets 10mg",
                diabeticCare, "SGLT-2 Inhibitor", tablet, "Film Coated Tablet",
                "1x10 Alu Alu / Blister",
                ing("Dapagliflozin", "10", "mg", "IP"),
                "Dapagliflozin 10mg Film Coated Tablet"));

        list.add(new ProductSpec(
                "Dapagliflozin & Metformin (SR) Tablets 5mg+500mg",
                diabeticCare, "SGLT-2 Inhibitor + Biguanide", tablet, "Sustained Release (SR) Tablet",
                "1x10 Alu Alu / Blister",
                Arrays.asList(ing("Dapagliflozin", "5", "mg", "IP"), ing("Metformin Hydrochloride", "500", "mg", "IP")),
                "Dapagliflozin 5mg + Metformin HCl 500mg SR Tablet"));

        list.add(new ProductSpec(
                "Dapagliflozin & Metformin (SR) Tablets 5mg+1000mg",
                diabeticCare, "SGLT-2 Inhibitor + Biguanide", tablet, "Sustained Release (SR) Tablet",
                "1x10 Alu Alu / Blister",
                Arrays.asList(ing("Dapagliflozin", "5", "mg", "IP"), ing("Metformin Hydrochloride", "1000", "mg", "IP")),
                "Dapagliflozin 5mg + Metformin HCl 1000mg SR Tablet"));

        list.add(new ProductSpec(
                "Dapagliflozin & Metformin (SR) Tablets 10mg+500mg",
                diabeticCare, "SGLT-2 Inhibitor + Biguanide", tablet, "Sustained Release (SR) Tablet",
                "1x10 Alu Alu / Blister",
                Arrays.asList(ing("Dapagliflozin", "10", "mg", "IP"), ing("Metformin Hydrochloride", "500", "mg", "IP")),
                "Dapagliflozin 10mg + Metformin HCl 500mg SR Tablet"));

        list.add(new ProductSpec(
                "Dapagliflozin & Metformin (SR) Tablets 10mg+1000mg",
                diabeticCare, "SGLT-2 Inhibitor + Biguanide", tablet, "Sustained Release (SR) Tablet",
                "1x10 Alu Alu / Blister",
                Arrays.asList(ing("Dapagliflozin", "10", "mg", "IP"), ing("Metformin Hydrochloride", "1000", "mg", "IP")),
                "Dapagliflozin 10mg + Metformin HCl 1000mg SR Tablet"));

        list.add(new ProductSpec(
                "Dapagliflozin & Vildagliptin (SR) Tablets 5mg+100mg",
                diabeticCare, "SGLT-2 + DPP-4 Inhibitor", tablet, "Sustained Release (SR) Tablet",
                "1x10 Alu Alu / Blister",
                Arrays.asList(ing("Dapagliflozin", "5", "mg", "IP"), ing("Vildagliptin", "100", "mg", "IP")),
                "Dapagliflozin 5mg + Vildagliptin 100mg SR Tablet"));

        list.add(new ProductSpec(
                "Dapagliflozin & Vildagliptin (SR) Tablets 10mg+100mg",
                diabeticCare, "SGLT-2 + DPP-4 Inhibitor", tablet, "Sustained Release (SR) Tablet",
                "1x10 Alu Alu / Blister",
                Arrays.asList(ing("Dapagliflozin", "10", "mg", "IP"), ing("Vildagliptin", "100", "mg", "IP")),
                "Dapagliflozin 10mg + Vildagliptin 100mg SR Tablet"));

        list.add(new ProductSpec(
                "Dapagliflozin, Vildagliptin (SR) & Metformin (SR) Tablets 10mg+100mg+500mg",
                diabeticCare, "SGLT-2 + DPP-4 + Biguanide", tablet, "Sustained Release (SR) Tablet",
                "1x10 Alu Alu / Blister",
                Arrays.asList(ing("Dapagliflozin","10","mg","IP"), ing("Vildagliptin","100","mg","IP"), ing("Metformin Hydrochloride","500","mg","IP")),
                "Dapagliflozin 10mg + Vildagliptin 100mg + Metformin HCl 500mg SR Tablet"));

        list.add(new ProductSpec(
                "Dapagliflozin, Vildagliptin (SR) & Metformin (SR) Tablets 10mg+100mg+1000mg",
                diabeticCare, "SGLT-2 + DPP-4 + Biguanide", tablet, "Sustained Release (SR) Tablet",
                "1x10 Alu Alu / Blister",
                Arrays.asList(ing("Dapagliflozin","10","mg","IP"), ing("Vildagliptin","100","mg","IP"), ing("Metformin Hydrochloride","1000","mg","IP")),
                "Dapagliflozin 10mg + Vildagliptin 100mg + Metformin HCl 1000mg SR Tablet"));

        list.add(new ProductSpec(
                "Glimepiride Tablets 1mg",
                diabeticCare, "Sulfonylurea", tablet, "Plain Tablet",
                "1x10 Blister",
                ing("Glimepiride", "1", "mg", "IP"),
                "Glimepiride 1mg Tablet"));

        list.add(new ProductSpec(
                "Glimepiride Tablets 2mg",
                diabeticCare, "Sulfonylurea", tablet, "Plain Tablet",
                "1x10 Blister",
                ing("Glimepiride", "2", "mg", "IP"),
                "Glimepiride 2mg Tablet"));

        list.add(new ProductSpec(
                "Glimepiride Tablets 3mg",
                diabeticCare, "Sulfonylurea", tablet, "Plain Tablet",
                "1x10 Blister",
                ing("Glimepiride", "3", "mg", "IP"),
                "Glimepiride 3mg Tablet"));

        list.add(new ProductSpec(
                "Glimepiride Tablets 4mg",
                diabeticCare, "Sulfonylurea", tablet, "Plain Tablet",
                "1x10 Blister",
                ing("Glimepiride", "4", "mg", "IP"),
                "Glimepiride 4mg Tablet"));

        list.add(new ProductSpec(
                "Glimepiride & Metformin HCl (SR) Tablets 1mg+500mg",
                diabeticCare, "Sulfonylurea + Biguanide", tablet, "Sustained Release (SR) Tablet",
                "1x10 Blister",
                Arrays.asList(ing("Glimepiride","1","mg","IP"), ing("Metformin Hydrochloride","500","mg","IP")),
                "Glimepiride 1mg + Metformin HCl 500mg SR Tablet"));

        list.add(new ProductSpec(
                "Glimepiride & Metformin HCl (SR) Tablets 1mg+1000mg",
                diabeticCare, "Sulfonylurea + Biguanide", tablet, "Sustained Release (SR) Tablet",
                "1x10 Blister",
                Arrays.asList(ing("Glimepiride","1","mg","IP"), ing("Metformin Hydrochloride","1000","mg","IP")),
                "Glimepiride 1mg + Metformin HCl 1000mg SR Tablet"));

        list.add(new ProductSpec(
                "Glimepiride & Metformin HCl (SR) Tablets 2mg+500mg",
                diabeticCare, "Sulfonylurea + Biguanide", tablet, "Sustained Release (SR) Tablet",
                "1x10 Blister",
                Arrays.asList(ing("Glimepiride","2","mg","IP"), ing("Metformin Hydrochloride","500","mg","IP")),
                "Glimepiride 2mg + Metformin HCl 500mg SR Tablet"));

        list.add(new ProductSpec(
                "Glimepiride & Metformin HCl (SR) Tablets 2mg+1000mg",
                diabeticCare, "Sulfonylurea + Biguanide", tablet, "Sustained Release (SR) Tablet",
                "1x10 Blister",
                Arrays.asList(ing("Glimepiride","2","mg","IP"), ing("Metformin Hydrochloride","1000","mg","IP")),
                "Glimepiride 2mg + Metformin HCl 1000mg SR Tablet"));

        list.add(new ProductSpec(
                "Glimepiride & Metformin HCl (SR) Tablets 3mg+500mg",
                diabeticCare, "Sulfonylurea + Biguanide", tablet, "Sustained Release (SR) Tablet",
                "1x10 Blister",
                Arrays.asList(ing("Glimepiride","3","mg","IP"), ing("Metformin Hydrochloride","500","mg","IP")),
                "Glimepiride 3mg + Metformin HCl 500mg SR Tablet"));

        list.add(new ProductSpec(
                "Glimepiride & Metformin HCl (SR) Tablets 3mg+1000mg",
                diabeticCare, "Sulfonylurea + Biguanide", tablet, "Sustained Release (SR) Tablet",
                "1x10 Blister",
                Arrays.asList(ing("Glimepiride","3","mg","IP"), ing("Metformin Hydrochloride","1000","mg","IP")),
                "Glimepiride 3mg + Metformin HCl 1000mg SR Tablet"));

        list.add(new ProductSpec(
                "Glimepiride & Metformin HCl (SR) Tablets 4mg+500mg",
                diabeticCare, "Sulfonylurea + Biguanide", tablet, "Sustained Release (SR) Tablet",
                "1x10 Blister",
                Arrays.asList(ing("Glimepiride","4","mg","IP"), ing("Metformin Hydrochloride","500","mg","IP")),
                "Glimepiride 4mg + Metformin HCl 500mg SR Tablet"));

        list.add(new ProductSpec(
                "Glimepiride & Metformin HCl (SR) Tablets 4mg+1000mg",
                diabeticCare, "Sulfonylurea + Biguanide", tablet, "Sustained Release (SR) Tablet",
                "1x10 Blister",
                Arrays.asList(ing("Glimepiride","4","mg","IP"), ing("Metformin Hydrochloride","1000","mg","IP")),
                "Glimepiride 4mg + Metformin HCl 1000mg SR Tablet"));

        list.add(new ProductSpec(
                "Glimepiride, Pioglitazone & Metformin HCL (SR) Tablets 1mg+15mg+500mg",
                diabeticCare, "Sulfonylurea + Thiazolidinedione + Biguanide", tablet, "Sustained Release (SR) Tablet",
                "1x10 Blister",
                Arrays.asList(ing("Glimepiride","1","mg","IP"), ing("Pioglitazone","15","mg","IP"), ing("Metformin Hydrochloride","500","mg","IP")),
                "Glimepiride 1mg + Pioglitazone 15mg + Metformin HCl 500mg SR Tablet"));

        list.add(new ProductSpec(
                "Glimepiride, Pioglitazone & Metformin HCL (SR) Tablets 2mg+15mg+500mg",
                diabeticCare, "Sulfonylurea + Thiazolidinedione + Biguanide", tablet, "Sustained Release (SR) Tablet",
                "1x10 Blister",
                Arrays.asList(ing("Glimepiride","2","mg","IP"), ing("Pioglitazone","15","mg","IP"), ing("Metformin Hydrochloride","500","mg","IP")),
                "Glimepiride 2mg + Pioglitazone 15mg + Metformin HCl 500mg SR Tablet"));

        list.add(new ProductSpec(
                "Glimepiride, Voglibose & Metformin HCL (SR) Tablets 1mg+0.2mg+500mg",
                diabeticCare, "Sulfonylurea + Alpha-Glucosidase Inhibitor + Biguanide", tablet, "Sustained Release (SR) Tablet",
                "1x10 Blister",
                Arrays.asList(ing("Glimepiride","1","mg","IP"), ing("Voglibose","0.2","mg","IP"), ing("Metformin Hydrochloride","500","mg","IP")),
                "Glimepiride 1mg + Voglibose 0.2mg + Metformin HCl 500mg SR Tablet"));

        list.add(new ProductSpec(
                "Glimepiride, Voglibose & Metformin HCL (SR) Tablets 2mg+0.2mg+500mg",
                diabeticCare, "Sulfonylurea + Alpha-Glucosidase Inhibitor + Biguanide", tablet, "Sustained Release (SR) Tablet",
                "1x10 Blister",
                Arrays.asList(ing("Glimepiride","2","mg","IP"), ing("Voglibose","0.2","mg","IP"), ing("Metformin Hydrochloride","500","mg","IP")),
                "Glimepiride 2mg + Voglibose 0.2mg + Metformin HCl 500mg SR Tablet"));

        list.add(new ProductSpec(
                "Metformin Hydrochloride Tablets 500mg",
                diabeticCare, "Biguanide", tablet, "Plain Tablet",
                "1x10 Blister",
                ing("Metformin Hydrochloride", "500", "mg", "IP"),
                "Metformin Hydrochloride 500mg Tablet"));

        list.add(new ProductSpec(
                "Metformin Hydrochloride (SR) Tablets 500mg",
                diabeticCare, "Biguanide", tablet, "Sustained Release (SR) Tablet",
                "1x10 Blister",
                ing("Metformin Hydrochloride", "500", "mg", "IP"),
                "Metformin Hydrochloride 500mg SR Tablet"));

        list.add(new ProductSpec(
                "Metformin Hydrochloride (SR) Tablets 1000mg",
                diabeticCare, "Biguanide", tablet, "Sustained Release (SR) Tablet",
                "1x10 Blister",
                ing("Metformin Hydrochloride", "1000", "mg", "IP"),
                "Metformin Hydrochloride 1000mg SR Tablet"));

        list.add(new ProductSpec(
                "Sitagliptin Phosphate Tablets 50mg",
                diabeticCare, "DPP-4 Inhibitor", tablet, "Film Coated Tablet",
                "1x10 Alu Alu / Blister",
                ing("Sitagliptin Phosphate", "50", "mg", "USP"),
                "Sitagliptin Phosphate 50mg Film Coated Tablet"));

        list.add(new ProductSpec(
                "Sitagliptin Phosphate Tablets 100mg",
                diabeticCare, "DPP-4 Inhibitor", tablet, "Film Coated Tablet",
                "1x10 Alu Alu / Blister",
                ing("Sitagliptin Phosphate", "100", "mg", "USP"),
                "Sitagliptin Phosphate 100mg Film Coated Tablet"));

        list.add(new ProductSpec(
                "Sitagliptin & Metformin HCl (SR) Tablets 50mg+500mg",
                diabeticCare, "DPP-4 Inhibitor + Biguanide", tablet, "Sustained Release (SR) Tablet",
                "1x10 Alu Alu / Blister",
                Arrays.asList(ing("Sitagliptin Phosphate","50","mg","USP"), ing("Metformin Hydrochloride","500","mg","IP")),
                "Sitagliptin 50mg + Metformin HCl 500mg SR Tablet"));

        list.add(new ProductSpec(
                "Sitagliptin & Dapagliflozin Tablets 100mg+10mg",
                diabeticCare, "DPP-4 + SGLT-2 Inhibitor", tablet, "Film Coated Tablet",
                "1x10 Alu Alu / Blister",
                Arrays.asList(ing("Sitagliptin Phosphate","100","mg","USP"), ing("Dapagliflozin","10","mg","IP")),
                "Sitagliptin 100mg + Dapagliflozin 10mg Film Coated Tablet"));

        list.add(new ProductSpec(
                "Sitagliptin, Dapagliflozin & Metformin HCl (SR) Tablets 100mg+10mg+500mg",
                diabeticCare, "DPP-4 + SGLT-2 + Biguanide", tablet, "Sustained Release (SR) Tablet",
                "1x10 Alu Alu / Blister",
                Arrays.asList(ing("Sitagliptin Phosphate","100","mg","USP"), ing("Dapagliflozin","10","mg","IP")),
                "Sitagliptin 100mg + Dapagliflozin 10mg + Metformin HCl 500mg SR Tablet"));

        list.add(new ProductSpec(
                "Sitagliptin, Dapagliflozin & Metformin HCl (SR) Tablets 100mg+10mg+1000mg",
                diabeticCare, "DPP-4 + SGLT-2 + Biguanide", tablet, "Sustained Release (SR) Tablet",
                "1x10 Alu Alu / Blister",
                Arrays.asList(ing("Sitagliptin Phosphate","100","mg","USP"), ing("Dapagliflozin","10","mg","IP"), ing("Metformin Hydrochloride","1000","mg","IP")),
                "Sitagliptin 100mg + Dapagliflozin 10mg + Metformin HCl 1000mg SR Tablet"));

        list.add(new ProductSpec(
                "Teneligliptin Tablets 20mg",
                diabeticCare, "DPP-4 Inhibitor", tablet, "Film Coated Tablet",
                "1x10 Alu Alu / Blister",
                ing("Teneligliptin Hydrobromide", "20", "mg", "IP"),
                "Teneligliptin 20mg Film Coated Tablet"));

        list.add(new ProductSpec(
                "Teneligliptin + Metformin HCl (SR) Tablets 20mg+500mg",
                diabeticCare, "DPP-4 Inhibitor + Biguanide", tablet, "Sustained Release (SR) Tablet",
                "1x10 Alu Alu / Blister",
                Arrays.asList(ing("Teneligliptin Hydrobromide","20","mg","IP"), ing("Metformin Hydrochloride","500","mg","IP")),
                "Teneligliptin 20mg + Metformin HCl 500mg SR Tablet"));

        list.add(new ProductSpec(
                "Teneligliptin + Metformin HCl (SR) Tablets 20mg+1000mg",
                diabeticCare, "DPP-4 Inhibitor + Biguanide", tablet, "Sustained Release (SR) Tablet",
                "1x10 Alu Alu / Blister",
                Arrays.asList(ing("Teneligliptin Hydrobromide","20","mg","IP"), ing("Metformin Hydrochloride","1000","mg","IP")),
                "Teneligliptin 20mg + Metformin HCl 1000mg SR Tablet"));

        list.add(new ProductSpec(
                "Voglibose Tablets 0.1mg",
                diabeticCare, "Alpha-Glucosidase Inhibitor", tablet, "Plain Tablet",
                "1x10 Blister",
                ing("Voglibose", "0.1", "mg", "IP"),
                "Voglibose 0.1mg Tablet"));

        list.add(new ProductSpec(
                "Voglibose Tablets 0.2mg",
                diabeticCare, "Alpha-Glucosidase Inhibitor", tablet, "Plain Tablet",
                "1x10 Blister",
                ing("Voglibose", "0.2", "mg", "IP"),
                "Voglibose 0.2mg Tablet"));

        list.add(new ProductSpec(
                "Voglibose & Metformin HCl (SR) Tablets 0.2mg+500mg",
                diabeticCare, "Alpha-Glucosidase Inhibitor + Biguanide", tablet, "Sustained Release (SR) Tablet",
                "1x10 Blister",
                Arrays.asList(ing("Voglibose","0.2","mg","IP"), ing("Metformin Hydrochloride","500","mg","IP")),
                "Voglibose 0.2mg + Metformin HCl 500mg SR Tablet"));

        list.add(new ProductSpec(
                "Voglibose & Metformin HCl (SR) Tablets 0.2mg+1000mg",
                diabeticCare, "Alpha-Glucosidase Inhibitor + Biguanide", tablet, "Sustained Release (SR) Tablet",
                "1x10 Blister",
                Arrays.asList(ing("Voglibose","0.2","mg","IP"), ing("Metformin Hydrochloride","1000","mg","IP")),
                "Voglibose 0.2mg + Metformin HCl 1000mg SR Tablet"));

        list.add(new ProductSpec(
                "Vildagliptin Tablets 50mg",
                diabeticCare, "DPP-4 Inhibitor", tablet, "Film Coated Tablet",
                "1x10 Alu Alu / Blister",
                ing("Vildagliptin", "50", "mg", "IP"),
                "Vildagliptin 50mg Film Coated Tablet"));

        list.add(new ProductSpec(
                "Vildagliptin Tablets 100mg (SR)",
                diabeticCare, "DPP-4 Inhibitor", tablet, "Sustained Release (SR) Tablet",
                "1x10 Alu Alu / Blister",
                ing("Vildagliptin", "100", "mg", "IP"),
                "Vildagliptin 100mg SR Tablet"));

        list.add(new ProductSpec(
                "Vildagliptin & Metformin HCl Tablets 50mg+500mg",
                diabeticCare, "DPP-4 Inhibitor + Biguanide", tablet, "Film Coated Tablet",
                "1x10 Alu Alu / Blister",
                Arrays.asList(ing("Vildagliptin","50","mg","IP"), ing("Metformin Hydrochloride","500","mg","IP")),
                "Vildagliptin 50mg + Metformin HCl 500mg Film Coated Tablet"));

        list.add(new ProductSpec(
                "Vildagliptin & Metformin HCl Tablets 50mg+1000mg",
                diabeticCare, "DPP-4 Inhibitor + Biguanide", tablet, "Film Coated Tablet",
                "1x10 Alu Alu / Blister",
                Arrays.asList(ing("Vildagliptin","50","mg","IP"), ing("Metformin Hydrochloride","1000","mg","IP")),
                "Vildagliptin 50mg + Metformin HCl 1000mg Film Coated Tablet"));

        // ── CAPSULES → Gastrointestinal Tract Care ───────────────────────────
        String gi       = "Gastrointestinal";
        String capsule  = "Capsule";
        String pelletCapsule = "Pellet Filled Capsule";

        list.add(new ProductSpec(
                "Omeprazole (ER Pallets) Capsule IP 20mg",
                gi, "Proton Pump Inhibitor", capsule, pelletCapsule,
                "1x10 / 1x15 Alu Alu / Strip",
                ing("Omeprazole", "20", "mg", "IP"),
                "Omeprazole 20mg ER Pellets Capsule IP"));

        list.add(new ProductSpec(
                "Omeprazole (ER Pallets) & Domperidone (SR Pallets) Capsule 20mg+30mg",
                gi, "PPI + Prokinetic", capsule, pelletCapsule,
                "1x10 / 1x15 Alu Alu / Strip",
                Arrays.asList(ing("Omeprazole","20","mg","IP"), ing("Domperidone","30","mg","IP")),
                "Omeprazole 20mg ER + Domperidone 30mg SR Pellets Capsule"));

        list.add(new ProductSpec(
                "Pantoprazole (Gastro-resistant Pallets) & Domperidone (SR Pallets) Capsules 40mg+30mg",
                gi, "PPI + Prokinetic", capsule, pelletCapsule,
                "1x10 / 1x15 Alu Alu / Strip",
                Arrays.asList(ing("Pantoprazole Sodium","40","mg","IP"), ing("Domperidone","30","mg","IP")),
                "Pantoprazole 40mg GR + Domperidone 30mg SR Pellets Capsule"));

        list.add(new ProductSpec(
                "Pantoprazole (ER Pallets) & Levosulpride (SR Pallets) Capsules 40mg+75mg",
                gi, "PPI + Prokinetic", capsule, pelletCapsule,
                "1x10 / 1x15 Alu Alu / Strip",
                Arrays.asList(ing("Pantoprazole Sodium","40","mg","IP"), ing("Levosulpride","75","mg","IP")),
                "Pantoprazole 40mg ER + Levosulpride 75mg SR Pellets Capsule"));

        list.add(new ProductSpec(
                "Pantoprazole (ER Pallets) & Itopride HCl (SR Pallets) Capsules 40mg+150mg",
                gi, "PPI + Prokinetic", capsule, pelletCapsule,
                "1x10 / 1x15 Alu Alu / Strip",
                Arrays.asList(ing("Pantoprazole Sodium","40","mg","IP"), ing("Itopride Hydrochloride","150","mg","IP")),
                "Pantoprazole 40mg ER + Itopride HCl 150mg SR Pellets Capsule"));

        list.add(new ProductSpec(
                "Rabeprazole (Gastro-resistant Pallets) & Domperidone (SR Pallets) Capsules 20mg+30mg",
                gi, "PPI + Prokinetic", capsule, pelletCapsule,
                "1x10 / 1x15 Alu Alu / Strip",
                Arrays.asList(ing("Rabeprazole Sodium","20","mg","IP"), ing("Domperidone","30","mg","IP")),
                "Rabeprazole 20mg GR + Domperidone 30mg SR Pellets Capsule"));

        list.add(new ProductSpec(
                "Rabeprazole (Gastro-resistant Pallets) & Domperidone (SR Pallets) Capsules 40mg+30mg",
                gi, "PPI + Prokinetic", capsule, pelletCapsule,
                "1x10 / 1x15 Alu Alu / Strip",
                Arrays.asList(ing("Rabeprazole Sodium","40","mg","IP"), ing("Domperidone","30","mg","IP")),
                "Rabeprazole 40mg GR + Domperidone 30mg SR Pellets Capsule"));

        list.add(new ProductSpec(
                "Rabeprazole (ER Pallets) & Levosulpride (SR Pallets) Capsules 20mg+75mg",
                gi, "PPI + Prokinetic", capsule, pelletCapsule,
                "1x10 / 1x15 Alu Alu / Strip",
                Arrays.asList(ing("Rabeprazole Sodium","20","mg","IP"), ing("Levosulpride","75","mg","IP")),
                "Rabeprazole 20mg ER + Levosulpride 75mg SR Pellets Capsule"));

        list.add(new ProductSpec(
                "Rabeprazole (ER Pallets) & Itopride HCl (SR Pallets) Capsules 20mg+150mg",
                gi, "PPI + Prokinetic", capsule, pelletCapsule,
                "1x10 / 1x15 Alu Alu / Strip",
                Arrays.asList(ing("Rabeprazole Sodium","20","mg","IP"), ing("Itopride Hydrochloride","150","mg","IP")),
                "Rabeprazole 20mg ER + Itopride HCl 150mg SR Pellets Capsule"));

        list.add(new ProductSpec(
                "Racecadotril Capsule IP 100mg",
                gi, "Enkephalinase Inhibitor", capsule, "Hard Gelatin Capsule",
                "1x10 Blister / Strip",
                ing("Racecadotril", "100", "mg", "IP"),
                "Racecadotril 100mg Capsule IP"));

        // ── CAPSULES → Cardiac Care ──────────────────────────────────────────
        String cardiac = "Cardiovascular";

        list.add(new ProductSpec(
                "Rosuvastatin & Aspirin (ER Pallets) Capsules 10mg+75mg",
                cardiac, "Statin + Antiplatelet", capsule, pelletCapsule,
                "1x10 / 1x15 Alu Alu / Strip",
                Arrays.asList(ing("Rosuvastatin","10","mg","IP"), ing("Aspirin","75","mg","IP")),
                "Rosuvastatin 10mg + Aspirin 75mg ER Pellets Capsule"));

        list.add(new ProductSpec(
                "Rosuvastatin & Clopidogrel Capsules 10mg+75mg",
                cardiac, "Statin + Antiplatelet", capsule, pelletCapsule,
                "1x10 / 1x15 Alu Alu / Strip",
                Arrays.asList(ing("Rosuvastatin","10","mg","IP"), ing("Clopidogrel","75","mg","IP")),
                "Rosuvastatin 10mg + Clopidogrel 75mg Capsule"));

        list.add(new ProductSpec(
                "Rosuvastatin & Clopidogrel Capsules 20mg+75mg",
                cardiac, "Statin + Antiplatelet", capsule, pelletCapsule,
                "1x10 / 1x15 Alu Alu / Strip",
                Arrays.asList(ing("Rosuvastatin","20","mg","IP"), ing("Clopidogrel","75","mg","IP")),
                "Rosuvastatin 20mg + Clopidogrel 75mg Capsule"));

        list.add(new ProductSpec(
                "Rosuvastatin, Clopidogrel & Aspirin (ER Pallets) Capsules 10mg+75mg+75mg",
                cardiac, "Statin + Antiplatelet", capsule, pelletCapsule,
                "1x10 / 1x15 Alu Alu / Strip",
                Arrays.asList(ing("Rosuvastatin","10","mg","IP"), ing("Clopidogrel","75","mg","IP"), ing("Aspirin","75","mg","IP")),
                "Rosuvastatin 10mg + Clopidogrel 75mg + Aspirin 75mg ER Pellets Capsule"));

        list.add(new ProductSpec(
                "Rosuvastatin, Clopidogrel & Aspirin (ER Pallets) Capsules 20mg+75mg+75mg",
                cardiac, "Statin + Antiplatelet", capsule, pelletCapsule,
                "1x10 / 1x15 Alu Alu / Strip",
                Arrays.asList(ing("Rosuvastatin","20","mg","IP"), ing("Clopidogrel","75","mg","IP"), ing("Aspirin","75","mg","IP")),
                "Rosuvastatin 20mg + Clopidogrel 75mg + Aspirin 75mg ER Pellets Capsule"));

        // ── ORAL LIQUIDS → Analgesic, Antipyretic, Anti-inflammatory ─────────
        String analgesic   = "Analgesics & Antipyretics";
        String suspension  = "Suspension";
        String syrup       = "Syrup";
        String sugarFree   = "Sugar Free Syrup";
        String sugarBased  = "Sugar Based Syrup";
        String petBottle60  = "60 ml Pet Bottle";
        String petBottle100 = "100 ml Pet Bottle";
        String petBottle200 = "200 ml Pet Bottle";

        list.add(new ProductSpec(
                "Paracetamol Oral Suspension 125mg/5ml",
                analgesic, "Analgesic / Antipyretic", suspension, "Ready to Use Suspension",
                petBottle60,
                ing("Paracetamol", "125", "mg", "IP"),
                "Paracetamol 125mg / 5ml Oral Suspension"));

        list.add(new ProductSpec(
                "Paracetamol Oral Suspension 250mg/5ml",
                analgesic, "Analgesic / Antipyretic", suspension, "Ready to Use Suspension",
                petBottle60,
                ing("Paracetamol", "250", "mg", "IP"),
                "Paracetamol 250mg / 5ml Oral Suspension"));

        list.add(new ProductSpec(
                "Aceclofenac & Paracetamol Oral Suspension 50mg+125mg/5ml",
                analgesic, "NSAID + Analgesic / Antipyretic", suspension, "Ready to Use Suspension",
                petBottle60,
                Arrays.asList(ing("Aceclofenac","50","mg","IP"), ing("Paracetamol","125","mg","IP")),
                "Aceclofenac 50mg + Paracetamol 125mg / 5ml Oral Suspension"));

        list.add(new ProductSpec(
                "Mefenamic Acid & Paracetamol Oral Suspension 50mg+125mg/5ml",
                analgesic, "NSAID + Analgesic / Antipyretic", suspension, "Ready to Use Suspension",
                petBottle60,
                Arrays.asList(ing("Mefenamic Acid","50","mg","IP"), ing("Paracetamol","125","mg","IP")),
                "Mefenamic Acid 50mg + Paracetamol 125mg / 5ml Oral Suspension"));

        list.add(new ProductSpec(
                "Mefenamic Acid & Paracetamol Oral Suspension 100mg+250mg/5ml",
                analgesic, "NSAID + Analgesic / Antipyretic", suspension, "Ready to Use Suspension",
                petBottle60,
                Arrays.asList(ing("Mefenamic Acid","100","mg","IP"), ing("Paracetamol","250","mg","IP")),
                "Mefenamic Acid 100mg + Paracetamol 250mg / 5ml Oral Suspension"));

        list.add(new ProductSpec(
                "Deflazacort Oral Suspension 6mg/5ml",
                analgesic, "Corticosteroid", suspension, "Ready to Use Suspension",
                petBottle60,
                ing("Deflazacort", "6", "mg", "IP"),
                "Deflazacort 6mg / 5ml Oral Suspension"));

        // ── ORAL LIQUIDS → Antihistaminic, Anti-Allergics, Cold & Cough ─────
        String respiratory = "Respiratory";

        list.add(new ProductSpec(
                "Fexofenadine HCl Oral Suspension 30mg/5ml",
                respiratory, "Antihistamine", suspension, "Ready to Use Suspension",
                petBottle60,
                ing("Fexofenadine Hydrochloride", "30", "mg", "IP"),
                "Fexofenadine HCl 30mg / 5ml Oral Suspension"));

        list.add(new ProductSpec(
                "Levocetirizine DiHCl & Montelukast Syrup 2.5mg+4mg/5ml",
                respiratory, "Antihistamine + Leukotriene Antagonist", syrup, sugarFree,
                petBottle60,
                Arrays.asList(ing("Levocetirizine Dihydrochloride","2.5","mg","IP"), ing("Montelukast Sodium","4","mg","IP")),
                "Levocetirizine 2.5mg + Montelukast 4mg / 5ml Sugar Free Syrup"));

        list.add(new ProductSpec(
                "Paracetamol, Phenylephrine HCl, Chlorpheniramine Maleate Syrup 125mg+5mg+2mg/5ml",
                respiratory, "Analgesic + Decongestant + Antihistamine", syrup, sugarBased,
                petBottle60,
                Arrays.asList(ing("Paracetamol","125","mg","IP"), ing("Phenylephrine Hydrochloride","5","mg","IP"), ing("Chlorpheniramine Maleate","2","mg","IP")),
                "Paracetamol 125mg + Phenylephrine HCl 5mg + Chlorpheniramine Maleate 2mg / 5ml Syrup"));

        list.add(new ProductSpec(
                "Paracetamol, Phenylephrine HCl, Chlorpheniramine Maleate Syrup 125mg+2.5mg+1mg/ml",
                respiratory, "Analgesic + Decongestant + Antihistamine", syrup, sugarFree,
                petBottle60,
                Arrays.asList(ing("Paracetamol","125","mg","IP"), ing("Phenylephrine Hydrochloride","2.5","mg","IP"), ing("Chlorpheniramine Maleate","1","mg","IP")),
                "Paracetamol 125mg + Phenylephrine HCl 2.5mg + Chlorpheniramine Maleate 1mg / ml Sugar Free Syrup"));

        list.add(new ProductSpec(
                "Dextromethorphan HBr, Phenylephrine HCl, Chlorpheniramine Maleate Syrup 10mg+5mg+2mg/5ml",
                respiratory, "Antitussive + Decongestant + Antihistamine", syrup, sugarBased,
                petBottle100,
                Arrays.asList(ing("Dextromethorphan Hydrobromide","10","mg","IP"), ing("Phenylephrine Hydrochloride","5","mg","IP"), ing("Chlorpheniramine Maleate","2","mg","IP")),
                "Dextromethorphan HBr 10mg + Phenylephrine HCl 5mg + Chlorpheniramine Maleate 2mg / 5ml Syrup"));

        list.add(new ProductSpec(
                "Dextromethorphan HBr, Phenylephrine HCl, Chlorpheniramine Maleate Syrup 15mg+5mg+2mg/5ml",
                respiratory, "Antitussive + Decongestant + Antihistamine", syrup, sugarBased,
                petBottle60,
                Arrays.asList(ing("Dextromethorphan Hydrobromide","15","mg","IP"), ing("Phenylephrine Hydrochloride","5","mg","IP"), ing("Chlorpheniramine Maleate","2","mg","IP")),
                "Dextromethorphan HBr 15mg + Phenylephrine HCl 5mg + Chlorpheniramine Maleate 2mg / 5ml Syrup"));

        list.add(new ProductSpec(
                "Terbutaline Sulphate, Bromhexine HCl, Guaiphenesin & Menthol Syrup",
                respiratory, "Bronchodilator + Mucolytic + Expectorant", syrup, sugarBased,
                petBottle100,
                Arrays.asList(ing("Terbutaline Sulphate","1.25","mg","IP"), ing("Bromhexine Hydrochloride","2","mg","IP"), ing("Guaiphenesin","50","mg","IP"), ing("Menthol","0.5","mg","IP")),
                "Terbutaline Sulphate 1.25mg + Bromhexine HCl 2mg + Guaiphenesin 50mg + Menthol 0.5mg / 5ml Syrup"));

        list.add(new ProductSpec(
                "Levosalbutamol Sulphate, Ambroxol HCl & Guaiphenesin Syrup",
                respiratory, "Bronchodilator + Mucolytic + Expectorant", syrup, sugarBased,
                petBottle100,
                Arrays.asList(ing("Levosalbutamol Sulphate","1","mg","IP"), ing("Ambroxol Hydrochloride","30","mg","IP"), ing("Guaiphenesin","50","mg","IP")),
                "Levosalbutamol Sulphate 1mg + Ambroxol HCl 30mg + Guaiphenesin 50mg / 5ml Syrup"));

        list.add(new ProductSpec(
                "Ambroxol HCl, Terbutaline Sulphate, Guaiphenesin & Menthol Syrup",
                respiratory, "Mucolytic + Bronchodilator + Expectorant", syrup, sugarBased,
                petBottle100,
                Arrays.asList(ing("Ambroxol Hydrochloride","15","mg","IP"), ing("Terbutaline Sulphate","1.25","mg","IP"), ing("Guaiphenesin","50","mg","IP"), ing("Menthol","2.5","mg","IP")),
                "Ambroxol HCl 15mg + Terbutaline Sulphate 1.25mg + Guaiphenesin 50mg + Menthol 2.5mg / 5ml Syrup"));

        list.add(new ProductSpec(
                "Dextromethorphan HBr, Chlorpheniramine Maleate, Guaiphenesin & Ammonium Chloride Syrup",
                respiratory, "Antitussive + Antihistamine + Expectorant", syrup, sugarBased,
                petBottle100,
                Arrays.asList(ing("Dextromethorphan Hydrobromide","5","mg","IP"), ing("Chlorpheniramine Maleate","2.5","mg","IP"), ing("Guaiphenesin","50","mg","IP"), ing("Ammonium Chloride","60","mg","IP")),
                "Dextromethorphan HBr 5mg + Chlorpheniramine Maleate 2.5mg + Guaiphenesin 50mg + Ammonium Chloride 60mg / 5ml Syrup"));

        list.add(new ProductSpec(
                "Phenylephrine HCl & Chlorpheniramine Maleate Syrup 5mg+2mg/5ml",
                respiratory, "Decongestant + Antihistamine", syrup, sugarFree,
                petBottle60,
                Arrays.asList(ing("Phenylephrine Hydrochloride","5","mg","IP"), ing("Chlorpheniramine Maleate","2","mg","IP")),
                "Phenylephrine HCl 5mg + Chlorpheniramine Maleate 2mg / 5ml Sugar Free Syrup"));

        // ── ORAL LIQUIDS → Urology ───────────────────────────────────────────
        String urology = "General";

        list.add(new ProductSpec(
                "Potassium Citrate Monohydrate & Citric Acid Monohydrate Oral Solution",
                urology, "Urinary Alkaliniser", "Solution", "Oral Solution",
                petBottle200,
                Arrays.asList(ing("Potassium Citrate Monohydrate","1100","mg","IP"), ing("Citric Acid Monohydrate","334","mg","IP")),
                "Potassium Citrate Monohydrate 1100mg + Citric Acid Monohydrate 334mg / 5ml Oral Solution"));

        list.add(new ProductSpec(
                "Disodium Hydrogen Citrate Syrup 1.35g/5ml",
                urology, "Urinary Alkaliniser", syrup, sugarFree,
                petBottle100,
                ing("Disodium Hydrogen Citrate", "1.35", "g", "IP"),
                "Disodium Hydrogen Citrate 1.35g / 5ml Sugar Free Syrup"));

        return list;
    }

    // ── Helper: single-ingredient shorthand ──────────────────────────────────
    private static Ingredient ing(String api, String strength, String unit, String pharmacopeia) {
        return new Ingredient(api, strength, unit, pharmacopeia);
    }

    // ── Data records ─────────────────────────────────────────────────────────
    private record Ingredient(String api, String strength, String unit, String pharmacopeia) {}

    private static class ProductSpec {
        final String brandName;
        final String category;
        final String therapeuticClass;
        final String dosageForm;
        final String dosageVariant;
        final String packaging;
        final List<Ingredient> ingredients;
        final String composition;

        ProductSpec(String brandName, String category, String therapeuticClass,
                    String dosageForm, String dosageVariant, String packaging,
                    Ingredient singleIng, String composition) {
            this(brandName, category, therapeuticClass, dosageForm, dosageVariant,
                    packaging, List.of(singleIng), composition);
        }

        ProductSpec(String brandName, String category, String therapeuticClass,
                    String dosageForm, String dosageVariant, String packaging,
                    List<Ingredient> ingredients, String composition) {
            this.brandName        = brandName;
            this.category         = category;
            this.therapeuticClass = therapeuticClass;
            this.dosageForm       = dosageForm;
            this.dosageVariant    = dosageVariant;
            this.packaging        = packaging;
            this.ingredients      = ingredients;
            this.composition      = composition;
        }
    }
}
