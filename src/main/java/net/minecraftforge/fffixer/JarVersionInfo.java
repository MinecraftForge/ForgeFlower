/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.fffixer;

record JarVersionInfo(
    String specificationTitle,
    String specificationVendor,
    String specificationVersion,
    String implementationTitle,
    String implementationVendor,
    String implementationVersion
) {
    JarVersionInfo(String title) {
        this("", "", "Undefined", title, "", "Undefined");
    }

    JarVersionInfo(
        String fallbackTitle,
        String specificationTitle,
        String specificationVendor,
        String specificationVersion,
        String implementationTitle,
        String implementationVendor,
        String implementationVersion
    ) {
        this(
            notNullOrEmpty(specificationTitle, ""),
            notNullOrEmpty(specificationVendor, ""),
            notNullOrEmpty(specificationVersion, "Undefined"),
            notNullOrEmpty(implementationTitle, fallbackTitle),
            notNullOrEmpty(implementationVendor, ""),
            notNullOrEmpty(implementationVersion, "Undefined")
        );
    }

    // NOTE: This should only be used once. If we need to use it more than once, consider making it a record parameter.
    String implementation() {
        var size = this.implementationTitle.length() + this.implementationVersion.length() + this.implementationVendor.length() + 5;
        var ret = new StringBuilder(size).append(this.implementationTitle);
        if (notEmptyOrUndefined(this.implementationVersion))
            ret.append(' ').append(this.implementationVersion);
        if (notEmptyOrUndefined(this.implementationVendor))
            ret.append(" by ").append(this.implementationVendor);
        return ret.toString();
    }

    @SuppressWarnings("deprecation")
    static JarVersionInfo of(String fallbackTitle, String packageName) {
        return of(fallbackTitle, Package.getPackage(packageName));
    }

    static JarVersionInfo of(String fallbackTitle, Object object) {
        return of(fallbackTitle, object.getClass());
    }

    static JarVersionInfo of(String fallbackTitle, Class<?> clazz) {
        return of(fallbackTitle, clazz.getPackage());
    }

    static JarVersionInfo of(String fallbackTitle, Package pkg) {
        if (pkg == null)
            return new JarVersionInfo(fallbackTitle);

        return new JarVersionInfo(
            fallbackTitle,
            pkg.getSpecificationTitle(),
            pkg.getSpecificationVendor(),
            pkg.getSpecificationVersion(),
            pkg.getImplementationTitle(),
            pkg.getImplementationVendor(),
            pkg.getImplementationVersion()
        );
    }

    private static String notNullOrEmpty(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static boolean notEmptyOrUndefined(String value) {
        return !value.isEmpty() && !"Undefined".equals(value);
    }
}
