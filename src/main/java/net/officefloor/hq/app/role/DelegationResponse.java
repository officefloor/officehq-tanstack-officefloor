package net.officefloor.hq.app.role;

/** A delegation as the front end reads it. */
public record DelegationResponse(String user, String from, String to, String by) {
    static DelegationResponse of(Delegation d) {
        return new DelegationResponse(d.user(), d.from().toString(), d.to().toString(), d.by());
    }
}
