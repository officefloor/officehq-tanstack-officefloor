package net.officefloor.hq.app.claim;

import java.util.List;

/** The kind chosen for a proof being added: receipt or photo (null: none chosen, so a receipt). */
public record ProofKind(String kind) {
    public static final String RECEIPT = "receipt";
    public static final String PHOTO = "photo";

    /** The kinds a proof may have, in the order they are shown. */
    public static final List<String> KINDS = List.of(RECEIPT, PHOTO);
}
