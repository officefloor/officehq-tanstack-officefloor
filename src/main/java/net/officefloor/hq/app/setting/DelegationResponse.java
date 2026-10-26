package net.officefloor.hq.app.setting;

/** A delegation as the screen sees it; dates are yyyy-mm-dd. */
public record DelegationResponse(String user, String from, String to, String by) {

    public static DelegationResponse from(Delegation d) {
        return new DelegationResponse(d.user(), d.from().toString(), d.to().toString(), d.by());
    }
}
