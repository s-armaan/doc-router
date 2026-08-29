package doc_router.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class Config {
    private static final String DEFAULT_CONFIG = """
            # Application-wide settings
            #
            # Add settings that apply to the whole application here. For example:
            # TODO: ADD EXAMPLE WHEN WE HAVE AT LEAST ONE PROGRAM-WIDE SETTING
            settings: {}

            # File organization rules
            #
            # Add one rule per file category. Each rule begins with `- name:`.
            # Use `priority` to control rule order; give more important rules a higher value.
            rules: []
            #
            # - name: descriptive-rule-name
            #   priority: 100
            #   when:
            #     # Match one extension or a list of extensions.
            #     extension: pdf
            #
            #     # Optional: match words or phrases in the filename.
            #     filename_contains: ["word-or-phrase"]
            #
            #     # Optional: match filenames beginning with this text.
            #     filename_starts_with: "starting-text"
            #
            #   then:
            #     # Use {year} and {month} for date-based folders.
            #     move_to: "Category/Subcategory/{year}/{month}"
            #
            #     # Optional: rename the file.
            #     rename_as: "Descriptive_Name_{year}-{month}.pdf"
            #
            #     # Optional: add tags.
            #     tags: ["tag-one", "tag-two"]
            #
            # Copy the template, indent it beneath `rules:`, remove the leading `#`
            # characters, and replace the example values to create a rule.
            """;
    private static final Path DATA_DIRECTORY = createDataDirectory();
    private static final Path CONFIG_FILE = createConfigFile();

    private Config() {
    }

    public static void validate() {

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
