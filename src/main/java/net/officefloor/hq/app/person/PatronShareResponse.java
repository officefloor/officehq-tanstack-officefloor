package net.officefloor.hq.app.person;

/** One of a person's patrons: their name and their share of 100.00, with two decimals. */
public record PatronShareResponse(String name, String share) {
    public static PatronShareResponse from(PatronShare share) {
        return new PatronShareResponse(share.name(), share.share().toPlainString());
    }
}
