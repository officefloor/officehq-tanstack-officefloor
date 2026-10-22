package net.officefloor.hq.app.voidreason;

/** A void reason as the screen shows it, with how many VOID claims were voided with it. */
public record VoidReasonResponse(long id, String reason, String by, int voided) {
    public static VoidReasonResponse from(VoidReason r, int voided) {
        return new VoidReasonResponse(r.id(), r.reason(), r.by(), voided);
    }
}
