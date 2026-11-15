package net.officefloor.hq.app.claim;

import java.util.Set;

/** The kinds a proof may be: a receipt (when none is chosen) or a photo. */
public final class ProofKind {
    public static final String RECEIPT = "receipt";
    public static final String PHOTO = "photo";
    public static final Set<String> ALL = Set.of(RECEIPT, PHOTO);

    private ProofKind() {
    }
}
