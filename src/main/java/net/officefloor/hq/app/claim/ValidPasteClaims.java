package net.officefloor.hq.app.claim;

import java.util.List;

/** A paste whose every line passed the claim form's own (body) rules: the named person and the claim, in order. */
public record ValidPasteClaims(List<Line> lines, String user) {

    public record Line(String name, ValidClaim claim) {
    }
}
