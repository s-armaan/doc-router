package doc_router;

enum ConflictPolicy {
    AUTO_SUFFIX("autoSuffix"),
    SKIP("skip"),
    OVERWRITE("overwrite");

    private final String configValue;

    ConflictPolicy(String configValue) {
        this.configValue = configValue;
    }

    static ConflictPolicy fromConfigValue(String value) {
        if (value == null) {
            return AUTO_SUFFIX;
        }

        for (ConflictPolicy policy : values()) {
            if (policy.configValue.equals(value)) {
                return policy;
            }
        }

        return null;
    }
}
