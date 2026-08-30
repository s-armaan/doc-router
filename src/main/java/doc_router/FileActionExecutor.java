package doc_router;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

public class FileActionExecutor {
    public void execute(Path file, AppConfig.Then actions) {
        Path source = file.toAbsolutePath().normalize();
        LocalDate today = LocalDate.now();
        String originalName = originalName(source);
        Path destinationDirectory = source.getParent();

        String moveTo = actions.moveTo();
        if (moveTo != null && !moveTo.isBlank()) {
            destinationDirectory = destinationDirectory.resolve(expandTokens(moveTo, today, originalName));
        }

        String renameAs = actions.renameAs();
        String destinationFilename = renameAs != null && !renameAs.isBlank()
                ? expandTokens(renameAs, today, originalName)
                : source.getFileName().toString();
        Path destination = destinationDirectory.resolve(destinationFilename);

        if (source.equals(destination)) {
            return;
        }

        try {
            Files.createDirectories(destinationDirectory);
            Files.move(source, destination);
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
}
