package net.officefloor.hq.app.claim;

import java.util.List;

/** The kinds a proof may be: receipt (when none is chosen) or photo. */
public final class ProofKind {

    public static final String RECEIPT = "receipt";

    public static final String PHOTO = "photo";

    public static final List<String> KINDS = List.of(RECEIPT, PHOTO);

    private ProofKind() {
    }
}
