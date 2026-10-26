package net.officefloor.hq.app.claim;

/** The kind of proof being added: the one chosen (null when none was), and the kind it counts as. */
public record ChosenProofKind(String chosen) {

    /** The kind the proof counts as: the chosen one, else a receipt. */
    public String kind() {
        return chosen == null ? ProofKind.RECEIPT : chosen;
    }
}
