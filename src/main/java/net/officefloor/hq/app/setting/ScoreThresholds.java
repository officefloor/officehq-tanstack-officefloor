package net.officefloor.hq.app.setting;

/** The validated score thresholds, mid and high. */
public record ScoreThresholds(Threshold mid, Threshold high) {

    /** A validated score threshold, a whole number; {@code value} null: blank. {@code given} false: left as it is. */
    public record Threshold(boolean given, Integer value) {
    }
}
