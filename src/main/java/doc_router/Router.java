package doc_router;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

public class Router {
    private final List<AppConfig.Rule> rules;
    private final FileActionExecutor fileActionExecutor;

    public Router(List<AppConfig.Rule> rules) {
        this.rules = rules.stream()
                .sorted(Comparator.comparing(AppConfig.Rule::priority).reversed())
                .toList();
        this.fileActionExecutor = new FileActionExecutor();
    }

    public void route(Path fileToRoute) {
        for (AppConfig.Rule rule : rules) {
            List<String> extensions = rule.when().extensions();
            List<String> filenameContains = rule.when().filenameContains();
            String filenameStartsWith = rule.when().filenameStartsWith();

            if (extensions != null && !matchesExtension(fileToRoute, extensions)) {
                continue;
            }

            if (filenameContains != null && !matchesFilenameContains(fileToRoute, filenameContains)) {
                continue;
            }

            if (filenameStartsWith != null && !matchesFilenameStartsWith(fileToRoute, filenameStartsWith)) {
                continue;
            }

            // past this point: matches rule

            fileActionExecutor.execute(fileToRoute, rule.then());

            break;
        }
    }

    private static boolean matchesExtension(Path file, List<String> extensions) {
        String filename = file.getFileName().toString();
        int dotIndex = filename.lastIndexOf('.');

        // no extension
        if (dotIndex <= 0 || dotIndex == filename.length() - 1) {
            return false;
        }

        String fileExtension = filename.substring(dotIndex + 1);

        return extensions.stream()
                .filter(extension -> extension != null && !extension.isBlank())
                .map(String::strip)
                .map(extension -> extension.startsWith(".")
                        ? extension.substring(1)
                        : extension)
                .anyMatch(extension -> extension.equalsIgnoreCase(fileExtension));
    }

    private static boolean matchesFilenameContains(Path file, List<String> filenameContains) {
        String filenameWithExtension = file.getFileName().toString();
        String filenameWithoutExtension;
        int dotIndex = filenameWithExtension.lastIndexOf('.');

        if (dotIndex > 0 && dotIndex != filenameWithExtension.length() - 1) {
            filenameWithoutExtension = filenameWithExtension.substring(0, dotIndex);
        } else {
            filenameWithoutExtension = filenameWithExtension; // file has no extension
        }

        return filenameContains.stream()
                .filter(text -> text != null && !text.isBlank())
                .map(String::strip)
                .anyMatch(text -> filenameWithoutExtension.toLowerCase()
                        .contains(text.toLowerCase()));
    }

    private static boolean matchesFilenameStartsWith(Path file, String filenameStartsWith) {
        String filenameWithExtension = file.getFileName().toString();
        String filenameWithoutExtension;
        int dotIndex = filenameWithExtension.lastIndexOf('.');

        if (dotIndex > 0 && dotIndex != filenameWithExtension.length() - 1) {
            filenameWithoutExtension = filenameWithExtension.substring(0, dotIndex);
        } else {
            filenameWithoutExtension = filenameWithExtension; // file has no extension
        }

        return filenameWithoutExtension.startsWith(filenameStartsWith);
    }
}
