package pl.riftplots.storage;

public enum StorageType {
    YAML,
    SQLITE,
    MYSQL;

    public static StorageType parse(String input) {
        if (input == null || input.isBlank()) {
            return SQLITE;
        }
        try {
            return StorageType.valueOf(input.toUpperCase());
        } catch (IllegalArgumentException e) {
            if ("MARIADB".equalsIgnoreCase(input)) {
                return MYSQL;
            }
            return SQLITE;
        }
    }
}
