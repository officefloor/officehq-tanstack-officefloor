package net.officefloor.hq.app;

/**
 * The request body for writing a note: which target it is about (type + id) and the text typed.
 */
public class NoteForm {

    private String targetType;
    private Long targetId;
    private String text;

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
