package net.officefloor.hq.app.asof;

import java.time.LocalDate;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

/**
 * Reads the sheet as it stood at the end of a date ({@link AsOfView}), with the date standing in for its today, from
 * within a request that changes the store now: the read runs on a thread of its own, outside the request's
 * transaction, so every connection it takes is pointed at that date's schema, exactly as a read of the as-of view is.
 */
@Component
public class AsOfReader {

    private final SheetDayRepository days;
    private final ExecutorService threads = Executors.newCachedThreadPool((r) -> {
        Thread t = new Thread(r, "as-of-read");
        t.setDaemon(true);
        return t;
    });

    public AsOfReader(SheetDayRepository days) {
        this.days = days;
    }

    /** The read's result, made against the sheet as it stood at the end of the date. */
    public <T> T read(String sheetId, LocalDate date, Supplier<T> read) {
        String schema = days.schemaAsOf(sheetId, date);
        try {
            return threads.submit(() -> {
                AsOfView.set(new AsOfView(schema, date));
                try {
                    return read.get();
                } finally {
                    AsOfView.set(null);
                }
            }).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted reading sheet " + sheetId + " as of " + date, e);
        } catch (ExecutionException e) {
            throw e.getCause() instanceof RuntimeException r ? r
                    : new IllegalStateException("Could not read sheet " + sheetId + " as of " + date, e.getCause());
        }
    }
}
