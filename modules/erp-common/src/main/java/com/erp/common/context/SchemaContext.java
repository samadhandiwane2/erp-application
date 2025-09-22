package com.erp.common.context;

public class SchemaContext {

    private static final ThreadLocal<String> currentSchema = new ThreadLocal<>();

    public static void setSchema(String schema) {
        currentSchema.set(schema);
    }

    public static String getCurrentSchema() {
        return currentSchema.get();
    }

    public static void clear() {
        currentSchema.remove();
    }

    public static boolean hasForcedSchema() {
        return currentSchema.get() != null;
    }

    // Force use master schema for user management operations
    public static void useMasterSchema() {
        currentSchema.set("master");
    }

    // Force use tenant schema for business operations
    public static void useTenantSchema() {
        currentSchema.set("tenant");
    }

}
