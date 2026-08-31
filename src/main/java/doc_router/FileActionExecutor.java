package doc_router;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;

public class FileActionExecutor {
    public void execute(Path file, AppConfig.Then actions) {
        Path source = file.toAbsolutePath().normalize();
        Path sourceDirectory = source.getParent();
        LocalDate today = LocalDate.now();
        String originalName = originalName(source);
        Path destinationDirectory = sourceDirectory;

        String moveTo = actions.moveTo();
        if (moveTo != null && !moveTo.isBlank()) {
            destinationDirectory = destinationDirectory.resolve(expandTokens(moveTo, today, originalName));
        }

        String renameAs = actions.renameAs();
        String destinationFilename = renameAs != null && !renameAs.isBlank()
                ? expandTokens(renameAs, today, originalName)
                : source.getFileName().toString();
        Path destination = destinationDirectory.resolve(destinationFilename).toAbsolutePath().normalize();

        try {
            if (!destination.startsWith(sourceDirectory)) {
                throw new IllegalArgumentException(
                        "Destination must remain inside the source directory: " + destination);
            }

            if (source.equals(destination)) {
                return;
            }

            Files.createDirectories(destination.getParent());
            ConflictPolicy conflictPolicy = ConflictPolicy.fromConfigValue(actions.onConflict());

            if (conflictPolicy == ConflictPolicy.OVERWRITE) {
                Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
            } else if (conflictPolicy == ConflictPolicy.SKIP) {
                moveOrSkip(source, destination);
            } else {
                moveWithAutoSuffix(source, destination);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Could not move " + source + " to " + destination, e);
        }
    }

    private static String expandTokens(String value, LocalDate date, String originalName) {
        return value
                .replace("{year}", Integer.toString(date.getYear()))
                .replace("{month}", "%02d".formatted(date.getMonthValue()))
                .replace("{originalName}", originalName);
    }

    private static String originalName(Path file) {
        String filename = file.getFileName().toString();
        int extensionStart = filename.lastIndexOf('.');
        return extensionStart > 0 ? filename.substring(0, extensionStart) : filename;
    }

    private static void moveOrSkip(Path source, Path destination) throws IOException {
        try {
            Files.move(source, destination);
        } catch (FileAlreadyExistsException e) {
            return;
        }
    }

    private static void moveWithAutoSuffix(Path source, Path destination) throws IOException {
        String filename = destination.getFileName().toString();
        int extensionStart = filename.lastIndexOf('.');
        String baseName = extensionStart > 0 ? filename.substring(0, extensionStart) : filename;
        String extension = extensionStart > 0 ? filename.substring(extensionStart) : "";

        for (int suffix = 0;; suffix++) {
            Path candidate = suffix == 0
                    ? destination
                    : destination.resolveSibling(baseName + " (" + suffix + ")" + extension);
            try {
                Files.move(source, candidate);
                return;
            } catch (FileAlreadyExistsException e) {
            }
        }
    }
}
