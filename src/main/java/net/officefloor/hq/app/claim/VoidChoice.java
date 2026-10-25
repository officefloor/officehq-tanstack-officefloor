package net.officefloor.hq.app.claim;

/** The void reason chosen for a void (null: none given). */
public record VoidChoice(String reason) {

    /** No reason chosen: an open or settle. */
    public static final VoidChoice NONE = new VoidChoice(null);
}
