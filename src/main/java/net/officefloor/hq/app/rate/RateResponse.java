package net.officefloor.hq.app.rate;

import java.util.List;

/** A rate as the screen sees it: `pct` with two decimals, `from` and `to` as yyyy-mm-dd or null when it has none,
 *  `version` its version count, `forwardAfter` the claim sequence a forward-only rate applies after (null when
 *  it applies to every claim), and `earlier` its older versions, newest first (see {@link Rate}). */
public record RateResponse(String grade, String pct, String from, String to, int version, Long forwardAfter,
        List<VersionResponse> earlier) {
    public static RateResponse from(Rate rate) {
        return new RateResponse(rate.grade(), rate.pct().toPlainString(),
                rate.from() != null ? rate.from().toString() : null,
                rate.to() != null ? rate.to().toString() : null, rate.version(), rate.forwardAfter(),
                rate.earlier().stream().map(VersionResponse::from).toList());
    }

    /** An older version of a rate: `pct` with two decimals, `to` as yyyy-mm-dd or null, and its `forwardAfter`. */
    public record VersionResponse(String pct, String to, Long forwardAfter) {
        static VersionResponse from(Rate.Version version) {
            return new VersionResponse(version.pct().toPlainString(),
                    version.to() != null ? version.to().toString() : null, version.forwardAfter());
        }
    }
}
