package net.officefloor.hq.app.asof;

/** The schema the current request reads the sheet from while it is shown as of a date (none: the
 *  store as it is now). Set by {@link AsOfFilter} around the request. */
public final class AsOfView {

    private static final ThreadLocal<String> SCHEMA = new ThreadLocal<>();

    private AsOfView() {
    }

    public static String schema() {
        return SCHEMA.get();
    }

    static void enter(String schema) {
        SCHEMA.set(schema);
    }

    static void leave() {
        SCHEMA.remove();
    }
}
