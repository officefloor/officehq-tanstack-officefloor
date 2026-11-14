package net.officefloor.hq.app.asof;

import java.time.LocalDate;

/**
 * The sheet as a request reads it as of a date: the schema ({@link AsOfSchemas}) holding it as it stood at the end of
 * that date, and the date, which stands in for the sheet's today. Held for the request's thread by {@link AsOfFilter}
 * and applied to every connection it takes by {@link AsOfConnections}. A preview reads the sheet's current records
 * ({@link AsOfSchemas#LIVE}) with the date standing in for its today.
 */
public record AsOfView(String schema, LocalDate date, boolean preview) {

    public AsOfView(String schema, LocalDate date) {
        this(schema, date, false);
    }

    private static final ThreadLocal<AsOfView> CURRENT = new ThreadLocal<>();

    /** The view the current request reads, or null: the store as it stands now. */
    public static AsOfView current() {
        return CURRENT.get();
    }

    static void set(AsOfView view) {
        if (view == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(view);
        }
    }
}
