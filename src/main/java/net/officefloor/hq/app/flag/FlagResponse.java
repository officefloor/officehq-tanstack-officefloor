package net.officefloor.hq.app.flag;

/**
 * A person's flag as the screen sees it: its reason and until-date (yyyy-mm-dd), and {@code shown}, the reason while
 * the sheet's today is on or before the until-date, else blank.
 */
public record FlagResponse(long personId, String reason, String until, String shown) {

    public static FlagResponse from(Flag flag, java.time.LocalDate today) {
        return new FlagResponse(flag.personId(), flag.reason(), flag.until().toString(),
                flag.inForceOn(today) ? flag.reason() : "");
    }
}
