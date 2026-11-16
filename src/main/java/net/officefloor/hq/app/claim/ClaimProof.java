package net.officefloor.hq.app.claim;

/** A proof added once its body is read: who adds it, and the kind chosen (null when none was: a receipt). */
public record ClaimProof(String by, String kind) {

    /** The kind the proof is stored with: the one chosen, else receipt. */
    public String kindOrDefault() {
        return kind == null ? net.officefloor.hq.app.setting.SettingRepository.DEFAULT_KIND : kind;
    }
}
