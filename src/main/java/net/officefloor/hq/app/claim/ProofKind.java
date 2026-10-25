package net.officefloor.hq.app.claim;

import java.util.List;

/** The kinds a proof is of: receipt (when none is chosen) or photo. */
public final class ProofKind {
    public static final String RECEIPT = "receipt", PHOTO = "photo";

    public static final List<String> ALL = List.of(RECEIPT, PHOTO);

    private ProofKind() {
    }
}
