package net.officefloor.hq.app.claim;

/** The kinds a proof may be: a receipt (when none is chosen) or a photo. */
public final class ProofKind {

    public static final String RECEIPT = "receipt";

    public static final String PHOTO = "photo";

    /** Every kind, in the order the screen lists them. */
    public static final java.util.List<String> ALL = java.util.List.of(RECEIPT, PHOTO);

    private ProofKind() {
    }
}
