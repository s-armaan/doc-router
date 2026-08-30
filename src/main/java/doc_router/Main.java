package doc_router;

import java.nio.file.Path;
import javax.swing.JOptionPane;

public final class Main {
    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            error.printStackTrace();

            JOptionPane.showMessageDialog(
                    null,
                    "Unexpected error:\n" + error.getMessage(),
                    "Doc Router - Error",
                    JOptionPane.ERROR_MESSAGE);

            System.exit(1);
        });

        // TODO: add inbox (the folder to watch) to settings or per rule
        Path inbox = Path.of(System.getProperty("user.home"), "Downloads");

        AppConfig config = Config.load();
        Router router = new Router(config.rules());
        DirectoryWatcher watcher = new DirectoryWatcher(inbox, router);

        if (config.rules().size() == 0) {
            Path configDirectory = Path.of(System.getenv("APPDATA"), "doc-router");

            JOptionPane.showMessageDialog(
                    null,
                    "Doc Router found no rules in config.yaml. Configure it before using the program:\n"
                            + configDirectory,
                    "Doc Router - Info",
                    JOptionPane.INFORMATION_MESSAGE);
        }

        watcher.start();
    }
}
