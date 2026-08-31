package doc_router;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileActionExecutorTest {
    @TempDir
    Path temporaryDirectory;

    private final FileActionExecutor executor = new FileActionExecutor();

    @Test
    void defaultsToAutoSuffixAndSkipsOccupiedSuffixes() throws IOException {
        Path source = write("invoice.pdf", "new");
        Path destination = write("Invoices/invoice.pdf", "existing");
        write("Invoices/invoice (1).pdf", "existing suffix");

        executor.execute(source, new AppConfig.Then("Invoices", null, null));

        assertFalse(Files.exists(source));
        assertEquals("existing", Files.readString(destination));
        assertEquals("new", Files.readString(temporaryDirectory.resolve("Invoices/invoice (2).pdf")));
    }

    @Test
    void skipLeavesSourceAndDestinationUnchanged() throws IOException {
        Path source = write("invoice.pdf", "new");
        Path destination = write("Invoices/invoice.pdf", "existing");

        executor.execute(source, new AppConfig.Then("Invoices", null, "skip"));

        assertTrue(Files.exists(source));
        assertEquals("new", Files.readString(source));
        assertEquals("existing", Files.readString(destination));
    }

    @Test
    void overwriteReplacesExistingDestination() throws IOException {
        Path source = write("invoice.pdf", "new");
        Path destination = write("Invoices/invoice.pdf", "existing");

        executor.execute(source, new AppConfig.Then("Invoices", null, "overwrite"));

        assertFalse(Files.exists(source));
        assertEquals("new", Files.readString(destination));
    }

    private Path write(String relativePath, String content) throws IOException {
        Path path = temporaryDirectory.resolve(relativePath);
        Files.createDirectories(path.getParent());
        return Files.writeString(path, content);
    }
}
