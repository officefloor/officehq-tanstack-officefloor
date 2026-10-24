package net.officefloor.hq.app.role;

/** A delegation as the screen shows it. */
public record DelegationResponse(String user, String by, String from, String to) {

    public static DelegationResponse from(Delegation d) {
        return new DelegationResponse(d.getUserName(), d.getByName(), d.getFromDate().toString(), d.getToDate().toString());
    }
}
