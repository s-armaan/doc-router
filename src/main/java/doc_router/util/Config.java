package doc_router.util;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

final class InvalidConfigurationException extends IllegalArgumentException {
    public InvalidConfigurationException(String message) {
        super(message);
    }

    public InvalidConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}

record AppConfig(Settings settings, List<Rule> rules) {
    public record Settings() {
        // this'll be app settings later
    }

    public record Rule(
            String name,
            Integer priority,
            When when,
            Then then) {
    }

    public record When(
            List<String> extensions,
            List<String> filenameContains,
            String filenameStartsWith) {
    }

    public record Then(
            String moveTo,
            String renameAs,
            List<String> tags) {
    }
}

public final class Config {
    private static final String DEFAULT_CONFIG = """
            # Application-wide settings
            #
            # Add settings that apply to the whole application here. For example:
            # TODO: ADD EXAMPLE WHEN WE HAVE AT LEAST ONE PROGRAM-WIDE SETTING
            settings: {}

            # File organization rules
            #
            # To add your first rule:
            # 1. Replace the line `rules: []` below with `rules:` (remove the square brackets).
            # 2. Copy the example rule below, including its spaces at the beginning of each line.
            # 3. Paste it directly below `rules:` and remove the `#` from each copied line.
            #
            # The spaces are important. Do not move the example rule all the way to the left.
            # Add one rule per file category. Each rule begins with `- name:`.
            # Use `priority` to control rule order; give more important rules a higher value.
            rules: []
            #
            # - name: descriptive-rule-name
            #   priority: 100
            #   when:
            #     # Match one extension or a list of extensions.
            #     extensions: ["pdf"]
            #
            #     # Optional: match words or phrases in the filename.
            #     filenameContains: ["word-or-phrase"]
            #
            #     # Optional: match filenames beginning with this text.
            #     filenameStartsWith: "starting-text"
            #
            #   then:
            #     # Use {year} and {month} for date-based folders.
            #     moveTo: "Category/Subcategory/{year}/{month}"
            #
            #     # Optional: rename the file.
            #     renameAs: "Descriptive_Name_{year}-{month}.pdf"
            #
            #     # Optional: add tags.
            #     tags: ["tag-one", "tag-two"]
            #
            # Add additional rules by copying the whole example again. Put each new rule
            # directly below the previous one, starting with the same `  - name:` spacing.
            """;
    private static Path DATA_DIRECTORY;
    private static Path CONFIG_FILE;

    private Config() {
    }

    public static AppConfig load() {
        DATA_DIRECTORY = createDataDirectory();
        CONFIG_FILE = createConfigFile();

        try (Reader reader = Files.newBufferedReader(CONFIG_FILE, StandardCharsets.UTF_8)) {
            YAMLMapper mapper = YAMLMapper.builder()
                    .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .build();

            AppConfig config = mapper.readValue(reader, AppConfig.class);

            validate(config);

            return config;
        } catch (JsonProcessingException e) {
            throw new InvalidConfigurationException(
                    "Could not parse config file: " + e.getOriginalMessage(), e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

    }

    private static void validate(AppConfig config) {
        if (config == null) {
            throw new InvalidConfigurationException("Config file is empty");
        }

        if (config.rules() == null) {
            throw new InvalidConfigurationException("Config file is missing a rules list");
        }

        Set<String> seenNames = new HashSet<String>();
        Set<Integer> seenPriorities = new HashSet<Integer>();

        for (AppConfig.Rule item : config.rules()) {
            if (item == null) {
                throw new InvalidConfigurationException("Rule cannot be null");
            }

            if (!hasText(item.name())) {
                throw new InvalidConfigurationException("Rule missing name");
            }

            if (!seenNames.add(item.name().trim())) {
                throw new InvalidConfigurationException(
                        String.format("Multiple rules have the same name: %s", item.name()));
            }

            if (item.priority() == null) {
                throw new InvalidConfigurationException(String.format("Rule `%s` is missing a priority", item.name()));
            }

            if (!seenPriorities.add(item.priority())) {
                throw new InvalidConfigurationException(
                        String.format("Multiple rules have the same priority: %d", item.priority()));
            }

            validateMatchConditions(item);
            validateActions(item);
        }
    }

    private static void validateMatchConditions(AppConfig.Rule rule) {
        AppConfig.When when = rule.when();

        if (when == null
                || (!hasText(when.extensions())
                        && !hasText(when.filenameContains())
                        && !hasText(when.filenameStartsWith()))) {
            throw new InvalidConfigurationException(
                    String.format("Rule `%s` must have at least one match condition", rule.name()));
        }
    }

    private static void validateActions(AppConfig.Rule rule) {
        AppConfig.Then then = rule.then();

        if (then == null
                || (!hasText(then.moveTo())
                        && !hasText(then.renameAs())
                        && !hasText(then.tags()))) {
            throw new InvalidConfigurationException(
                    String.format("Rule `%s` must have at least one action", rule.name()));
        }

        if (hasText(then.moveTo())) {
            validateDestinationPath(rule.name(), then.moveTo());
        }
    }

    private static void validateDestinationPath(String ruleName, String moveTo) {
        try {
            Path destination = Path.of(moveTo);

            if (destination.isAbsolute() || destination.getRoot() != null) {
                throw new InvalidConfigurationException(
                        String.format("Rule `%s` must use a relative destination path", ruleName));
            }

            if (destination.normalize().startsWith("..")) {
                throw new InvalidConfigurationException(
                        String.format("Rule `%s` destination path cannot leave the target folder", ruleName));
            }
        } catch (InvalidPathException e) {
            throw new InvalidConfigurationException(
                    String.format("Rule `%s` has an invalid destination path: %s", ruleName, moveTo), e);
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static boolean hasText(List<String> values) {
        return values != null && values.stream().anyMatch(Config::hasText);
    }

    private static Path createDataDirectory() {
        String appData = System.getenv("APPDATA");

        if (appData == null || appData.isBlank()) {
            throw new IllegalStateException("APPDATA is not set");
        }

        Path directory = Path.of(appData, "doc-router");

        try {
            return Files.createDirectories(directory);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create application directory: " + directory, e);
        }
    }

    private static Path createConfigFile() {
        Path configFile = DATA_DIRECTORY.resolve("config.yaml");

        try {
            Files.writeString(
                    configFile,
                    DEFAULT_CONFIG,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW);
        } catch (FileAlreadyExistsException e) {
            if (!Files.isRegularFile(configFile)) {
                throw new IllegalStateException(
                        "Config path exists but is not a regular file: " + configFile);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Could not create config file: " + configFile, e);
        }

        return configFile;
    }
}
