package net.officefloor.hq.app.setting;

/** The sheet's terms: the whole number of days after its date a claim is due by. It is 14 until it is
 *  set. The screen works out each claim's due-by and whether it is overdue from it. */
public final class Terms {

    public static final String KEY = "terms";

    public static final String DEFAULT = "14";

    private Terms() {
    }
}
