package net.officefloor.hq.app.claim;

import java.util.List;

/** Every line of a paste, checked, in the order typed. */
public record PastedLines(List<PastedLine> lines) {
}
