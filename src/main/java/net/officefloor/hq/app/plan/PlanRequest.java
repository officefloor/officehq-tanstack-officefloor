package net.officefloor.hq.app.plan;

import jakarta.validation.constraints.Size;

/** A plan as the row's form submits it: an amount, a start date (blank: the sheet's today), every N days and an end date. */
public record PlanRequest(@Size(max = 30) String amount, @Size(max = 30) String start, @Size(max = 30) String every,
        @Size(max = 30) String end) {
}
