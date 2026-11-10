package net.officefloor.hq.app.voidreason;

/** A void reason, as the screen reads it. */
public record VoidReasonResponse(String reason) {
    public static VoidReasonResponse from(VoidReason r) {
        return new VoidReasonResponse(r.reason());
    }
}
