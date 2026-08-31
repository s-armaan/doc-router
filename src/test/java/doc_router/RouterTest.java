package doc_router;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RouterTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void higherPriorityMatchingRuleWinsRegardlessOfInputOrder() throws IOException {
        Path source = Files.writeString(temporaryDirectory.resolve("invoice.pdf"), "content");
        AppConfig.Rule general = rule("general", 100, "General");
        AppConfig.Rule specific = rule("specific", 200, "Specific");

        new Router(new ArrayList<>(List.of(general, specific))).route(source);

        assertTrue(Files.exists(temporaryDirectory.resolve("Specific/invoice.pdf")));
        assertTrue(Files.notExists(temporaryDirectory.resolve("General/invoice.pdf")));
    }

    private static AppConfig.Rule rule(String name, int priority, String destination) {
        return new AppConfig.Rule(
                name,
                priority,
                new AppConfig.When(List.of("pdf"), null, null),
                new AppConfig.Then(destination, null, null));
    }
}
