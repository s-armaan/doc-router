package doc_router;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.StringReader;

import org.junit.jupiter.api.Test;

class ConfigTest {
  @Test
  void rejectsUnknownConflictPolicy() {
    InvalidConfigurationException error = assertThrows(
        InvalidConfigurationException.class,
        () -> Config.parse(new StringReader(configWithConflictPolicy("rename"))));

    assertEquals(
        "Rule `invoices` has invalid onConflict value `rename`; expected autoSuffix, skip, or overwrite",
        error.getMessage());
  }

  @Test
  void rejectsRenameAsOutsideTheDestinationDirectory() {
    InvalidConfigurationException error = assertThrows(
        InvalidConfigurationException.class,
        () -> Config.parse(new StringReader(configWithRenameAs("../outside.pdf"))));

    assertEquals(
        "Rule `invoices` renameAs must be a single filename without path segments",
        error.getMessage());
  }

  private static String configWithConflictPolicy(String onConflict) {
    return """
        settings: {}
        rules:
          - name: invoices
            priority: 100
            when:
              extensions: [pdf]
            then:
              moveTo: Invoices
              onConflict: %s
        """.formatted(onConflict);
  }

  private static String configWithRenameAs(String renameAs) {
    return """
        settings: {}
        rules:
          - name: invoices
            priority: 100
            when:
              extensions: [pdf]
            then:
              renameAs: '%s'
        """.formatted(renameAs);
  }
}
