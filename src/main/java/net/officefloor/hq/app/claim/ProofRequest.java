package net.officefloor.hq.app.claim;

/** The body of a proof being added: its kind, receipt or photo (null or blank: none chosen, so a receipt). */
public record ProofRequest(String kind) {

    /** The proof kinds a claim may be given. */
    public static final java.util.List<String> KINDS = java.util.List.of("receipt", "photo");

    /** The kind chosen, trimmed; "" when none was chosen. */
    public String chosen() {
        return kind == null ? "" : kind.trim();
    }
}
