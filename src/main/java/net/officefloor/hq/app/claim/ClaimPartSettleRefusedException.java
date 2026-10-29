package net.officefloor.hq.app.claim;

/** A part-settle whose split or whose settle of the split-off claim would be refused: nothing is stored. */
public class ClaimPartSettleRefusedException extends Exception {

    public ClaimPartSettleRefusedException(String message) {
        super(message);
    }
}
