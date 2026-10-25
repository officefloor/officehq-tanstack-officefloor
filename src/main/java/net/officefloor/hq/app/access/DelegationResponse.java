package net.officefloor.hq.app.access;

public record DelegationResponse(String user, String from, String to, String by) {
    public static DelegationResponse from(Delegation delegation) {
        return new DelegationResponse(delegation.user(), delegation.from().toString(), delegation.to().toString(),
                delegation.by());
    }
}
