package net.officefloor.hq.app.claim;

/** The kind of proof chosen when it is added, receipt or photo; {@code value} null: none chosen, a receipt. */
public record ProofKind(String value) {

    public static final String RECEIPT = "receipt";

    public static final String PHOTO = "photo";

    /** Whether the name is a proof kind. */
    public static boolean isKind(String name) {
        return RECEIPT.equals(name) || PHOTO.equals(name);
    }

    /** The kind the proof is stored as: the one chosen, else a receipt. */
    public String stored() {
        return value == null ? RECEIPT : value;
    }
}
