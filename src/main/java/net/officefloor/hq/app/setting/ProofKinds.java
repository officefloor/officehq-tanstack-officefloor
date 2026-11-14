package net.officefloor.hq.app.setting;

import java.util.List;

/** Validated proof kinds, in the order typed, each once; {@code value} empty: blank, none. {@code given} false: left as it is. */
public record ProofKinds(boolean given, List<String> value) {
}
