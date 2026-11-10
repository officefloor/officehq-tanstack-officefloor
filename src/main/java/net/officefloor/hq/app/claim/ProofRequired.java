package net.officefloor.hq.app.claim;

/** Why a claim above the proof threshold is refused a settle or an approval while it has no proofs. */
final class ProofRequired {
    private ProofRequired() {}

    static final String MESSAGE = "A claim whose due is above the proof threshold needs a proof first";
}
