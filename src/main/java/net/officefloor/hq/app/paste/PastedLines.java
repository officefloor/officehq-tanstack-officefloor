package net.officefloor.hq.app.paste;

import java.util.List;

/** Every line of the paste box, in order. */
public record PastedLines(List<PastedLine> lines) {
}
