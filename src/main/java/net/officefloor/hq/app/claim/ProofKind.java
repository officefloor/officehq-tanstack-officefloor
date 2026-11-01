package net.officefloor.hq.app.claim;

import java.util.List;

/** The kinds a proof can be: a receipt (the default when none is chosen) or a photo. */
public final class ProofKind {
    public static final String RECEIPT = "receipt", PHOTO = "photo";

    /** Every kind, in the order the screen offers them. */
    public static final List<String> ALL = List.of(RECEIPT, PHOTO);

    private ProofKind() {
    }
}
