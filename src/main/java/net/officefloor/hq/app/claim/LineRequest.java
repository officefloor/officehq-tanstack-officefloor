package net.officefloor.hq.app.claim;

import jakarta.validation.constraints.Size;

/** What a claim line submits: a label, a quantity (a whole number, at least 1) and a unit amount (two decimals). */
public record LineRequest(@Size(max = 200) String label, @Size(max = 30) String qty, @Size(max = 30) String unit) {
}
