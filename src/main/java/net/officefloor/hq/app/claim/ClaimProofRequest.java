package net.officefloor.hq.app.claim;

import jakarta.validation.constraints.Size;

/** A proof to add: its kind, receipt or photo (null or blank: none chosen, a receipt). */
public record ClaimProofRequest(@Size(max = 30) String kind) {
}
