package net.officefloor.hq.app.period;

import java.util.List;

/** A sheet's periods, by from-date. */
public record PeriodsResponse(List<PeriodResponse> periods) {

    public static PeriodsResponse from(List<Period> periods) {
        return new PeriodsResponse(periods.stream().map(PeriodResponse::from).toList());
    }
}
