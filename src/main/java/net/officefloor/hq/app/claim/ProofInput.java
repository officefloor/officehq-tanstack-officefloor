package net.officefloor.hq.app.claim;

/** A validated proof added to a claim: who is adding it, and the kind chosen (receipt or photo; null: none chosen, it
 *  is a receipt). */
public record ProofInput(String user, String kind) {

    /** The proof's kind: the one chosen, else a receipt. */
    public String kindOrReceipt() {
        return kind != null ? kind : "receipt";
    }
}
