package net.officefloor.hq.app.person;

/** Own terms a person was given (null: none) from a date, as YYYY-MM-DD. */
public record TermsResponse(Integer terms, String from) {
    public static TermsResponse from(TermsFrom terms) {
        return new TermsResponse(terms.terms(), terms.from().toString());
    }
}
