package com.ncop.auth.setup;

/**
 * Deprecated runner kept for compatibility. Prefer running RoleInitializer.main(...) directly.
 */
@Deprecated
public class RoleSetupRunner {
    public static void main(String[] args) {
        System.out.println("RoleSetupRunner is deprecated. Running RoleInitializer.main(...) instead.");
        RoleInitializer.main(args);
    }
}
