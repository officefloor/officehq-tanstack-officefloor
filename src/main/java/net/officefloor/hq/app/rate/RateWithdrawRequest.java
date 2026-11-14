package net.officefloor.hq.app.rate;

import jakarta.validation.constraints.Size;

/** The rate to withdraw the newest version of: its grade and from-date (yyyy-MM-dd; blank: from the start). */
public record RateWithdrawRequest(@Size(max = 10) String grade, @Size(max = 30) String from) {
}
