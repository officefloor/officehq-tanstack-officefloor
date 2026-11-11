package net.officefloor.hq.app.setting;

import java.util.List;

/**
 * The sheet's policy versions, oldest first: each its number (from 1), its from-date (yyyy-MM-dd; '' for
 * none, counting always), the policy setting it changed and the value it set ('' for none), and the count of
 * the sheet's claims whose policy version it is (the last version in force on the claim's date).
 */
public record PolicyResponse(List<Line> versions) {

    /** One policy version. */
    public record Line(int version, String from, String key, String value, int claims) {
    }
}
