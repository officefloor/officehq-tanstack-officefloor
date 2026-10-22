package net.officefloor.hq.app.delegation;

/** A delegation as the screen shows it. */
public record DelegationResponse(long id, String user, String from, String to, String by) {
    public static DelegationResponse from(Delegation d) {
        return new DelegationResponse(d.id(), d.user(), d.from().toString(), d.to().toString(), d.by());
    }
}
