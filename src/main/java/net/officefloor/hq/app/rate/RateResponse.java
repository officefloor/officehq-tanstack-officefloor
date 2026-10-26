package net.officefloor.hq.app.rate;

import java.util.List;

/** A rate as the screen shows it; level 0 is the sheet's own, 1 its parent's, and so on up; version is how many
 * versions it has had. A forward-only rate carries {@code after}: it applies only to claims whose {@code added} is
 * above it (null: it applies to every claim); {@code earlier} are its older versions, newest first, for the claims it
 * does not apply to. */
public record RateResponse(String grade, String rate, String from, String to, int level, int version, Long after,
        List<RateResponse> earlier) {
    public static RateResponse from(Rate rate) {
        return new RateResponse(rate.grade(), rate.rate().toPlainString(),
                rate.from() == null ? null : rate.from().toString(),
                rate.to() == null ? null : rate.to().toString(), rate.level(), rate.version(), rate.after(),
                rate.earlier().stream().map(RateResponse::from).toList());
    }
}
