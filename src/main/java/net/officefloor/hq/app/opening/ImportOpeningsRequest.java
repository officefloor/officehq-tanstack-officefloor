package net.officefloor.hq.app.opening;

import jakarta.validation.constraints.Size;

/** The opening import box's text: one opening balance per line, as name,period,amount. */
public record ImportOpeningsRequest(@Size(max = 200000) String text) {
}
