package doc_router;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

public class DirectoryWatcher {
    private final Path inbox;
    private final Router router;
    private final WatchService watchService;

    public DirectoryWatcher(Path inbox, Router router) {
        this.inbox = inbox;
        this.router = router;
        try {
            watchService = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void start() {
        try {
            inbox.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);

            while (true) {
                WatchKey key;
                try {
                    key = watchService.take();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }

                    Path addedFile = inbox.resolve((Path) event.context());

                    router.route(addedFile);
                }

                if (!key.reset()) {
                    return;
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
