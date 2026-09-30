package net.officefloor.hq.app;

/**
 * The request body for writing a note: the target it is on ({@code targetType} + {@code targetId})
 * and the text (the id and the written-at instant are set server-side). Bound from the POST JSON
 * body via {@code @RequestBody}.
 */
public class NewNote {

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
