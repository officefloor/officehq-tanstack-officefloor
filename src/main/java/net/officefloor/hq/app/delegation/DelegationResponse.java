package net.officefloor.hq.app.delegation;

/** A delegation, as the screen reads it. */
public record DelegationResponse(long id, String user, String by, String from, String to) {
    public static DelegationResponse from(Delegation d) {
        return new DelegationResponse(d.id(), d.user(), d.by(), d.from().toString(), d.to().toString());
    }
}
