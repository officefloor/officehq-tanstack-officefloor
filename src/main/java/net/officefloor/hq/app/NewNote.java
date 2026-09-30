package net.officefloor.hq.app;

/**
 * Request body for {@code POST /api/notes}: the fields the user supplies when writing a note — the
 * target it is kept against ({@code targetType}/{@code targetId}) plus the note text. Bound from the
 * JSON payload by the Spring MVC {@code @RequestBody} argument resolver.
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
