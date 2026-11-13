package net.officefloor.hq.app.holiday;

import jakarta.validation.constraints.Size;

/** A holiday as the form submits it: its date (yyyy-mm-dd). */
public record HolidayRequest(@Size(max = 30) String date) {
}
