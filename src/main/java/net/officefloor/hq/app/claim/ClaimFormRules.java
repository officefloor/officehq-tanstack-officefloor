package net.officefloor.hq.app.claim;

/** The claim form's rules (see ValidateClaim) for steps outside this package that add a claim, or keep values a claim
 *  will be added with, by them. Not a step itself. */
public final class ClaimFormRules {
    private ClaimFormRules() {
    }

    /** The claim form's add, checked as ValidateClaim checks it. */
    public static ClaimInput check(ClaimRequest request) throws InvalidClaimException {
        return ValidateClaim.check(request);
    }
}
