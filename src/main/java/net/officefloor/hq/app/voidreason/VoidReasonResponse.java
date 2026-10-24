package net.officefloor.hq.app.voidreason;

/** A void reason as the screen shows it: its text. */
public record VoidReasonResponse(long id, String reason) {

    public static VoidReasonResponse from(VoidReason reason) {
        return new VoidReasonResponse(reason.getId(), reason.getReason());
    }
}
