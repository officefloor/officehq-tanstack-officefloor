package net.officefloor.hq.app.claim;

import java.util.List;

/** The kinds a proof may be: a receipt (when none is chosen) or a photo. */
public final class ProofKind {

    public static final String RECEIPT = "receipt";

    public static final String PHOTO = "photo";

    public static final List<String> ALL = List.of(RECEIPT, PHOTO);

    private ProofKind() {
    }
}
