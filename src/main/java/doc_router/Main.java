package doc_router;

import java.nio.file.Path;

import javax.swing.JOptionPane;

import doc_router.util.Config;

public final class Main {
    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            error.printStackTrace();

            JOptionPane.showMessageDialog(
                    null,
                    "Unexpected error:\n" + error.getMessage(),
                    "Doc Router error",
                    JOptionPane.ERROR_MESSAGE);

            System.exit(1);
        });
    }
}
