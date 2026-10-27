package net.officefloor.hq.app.claim;

/** The claim form's rules on a typed body, for a feature outside claims that must take only what the form takes. */
public final class ClaimFormRules {

    private ClaimFormRules() {
    }

    public static ClaimInput parse(ClaimRequest body) throws InvalidClaimException {
        return ValidateClaim.parse(body);
    }
}
