package org.example;

public class ReadEntry {

    String id;

    String user;

    String username;

    String experiment;

    String date = "";

    String eventType = "";

    String event = "";

    String spritename = "";

    String metadata = "";

    String blockId = "";

    String group = "";

    String scriptId = "";

    String scriptIdAfterMove = "";

    String oldParentId = "";

    String newParentId = "";

    String valueType = "";

    String oldValue = "";

    String newValue = "";

    String category = "";

    String oldInputName = "";

    String newInputName = "";

    boolean loopInvolved;

    boolean conditionalInvolved;

    String blockCount = "";

    String eventId = "";

    public ReadEntry(String id, String user, String username, String experiment, String date, String eventType, String event, String spritename, String metadata, String blockId, String group, String scriptId, String scriptIdAfterMove, String oldParentId, String newParentId, String valueType, String oldValue, String newValue, String category, String oldInputName, String newInputName, boolean loopInvolved, boolean conditionalInvolved, String blockCount, String eventId) {
        this.id = id;
        this.user = user;
        this.username = username;
        this.experiment = experiment;
        this.date = date;
        this.eventType = eventType;
        this.event = event;
        this.spritename = spritename;
        this.metadata = metadata;
        this.blockId = blockId;
        this.group = group;
        this.scriptId = scriptId;
        this.scriptIdAfterMove = scriptIdAfterMove;
        this.oldParentId = oldParentId;
        this.newParentId = newParentId;
        this.valueType = valueType;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.oldInputName = oldInputName;
        this.newInputName = newInputName;
        this.loopInvolved = loopInvolved;
        this.conditionalInvolved = conditionalInvolved;
        this.blockCount = blockCount;
        this.eventId = eventId;
    }

    public ReadEntry() {

    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getExperiment() {
        return experiment;
    }

    public void setExperiment(String experiment) {
        this.experiment = experiment;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getSpritename() {
        return spritename;
    }

    public void setSpritename(String spritename) {
        this.spritename = spritename;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public String getBlockId() {
        return blockId;
    }

    public void setBlockId(String blockId) {
        this.blockId = blockId;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getScriptId() {
        return scriptId;
    }

    public void setScriptId(String scriptId) {
        this.scriptId = scriptId;
    }

    public String getScriptIdAfterMove() {
        return scriptIdAfterMove;
    }

    public void setScriptIdAfterMove(String scriptIdAfterMove) {
        this.scriptIdAfterMove = scriptIdAfterMove;
    }

    public String getOldParentId() {
        return oldParentId;
    }

    public void setOldParentId(String oldParentId) {
        this.oldParentId = oldParentId;
    }

    public String getNewParentId() {
        return newParentId;
    }

    public void setNewParentId(String newParentId) {
        this.newParentId = newParentId;
    }

    public String getValueType() {
        return valueType;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getOldInputName() {
        return oldInputName;
    }

    public void setOldInputName(String oldInputName) {
        this.oldInputName = oldInputName;
    }

    public String getNewInputName() {
        return newInputName;
    }

    public void setNewInputName(String newInputName) {
        this.newInputName = newInputName;
    }

    public boolean getLoopInvolved() {
        return loopInvolved;
    }

    public void setLoopInvolved(boolean loopInvolved) {
        this.loopInvolved = loopInvolved;
    }

    public boolean getConditionalInvolved() {
        return conditionalInvolved;
    }

    public void setConditionalInvolved(boolean conditionalInvolved) {
        this.conditionalInvolved = conditionalInvolved;
    }

    public String getBlockCount() {
        return blockCount;
    }

    public void setBlockCount(String blockCount) {
        this.blockCount = blockCount;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    @Override
    public String toString() {
        return "ReadEntry{" +
                "id=" + id +
                ", user=" + user +
                ", username='" + username + '\'' +
                ", experiment=" + experiment +
                ", date='" + date + '\'' +
                ", eventType='" + eventType + '\'' +
                ", event='" + event + '\'' +
                ", spritename='" + spritename + '\'' +
                ", metadata='" + metadata + '\'' +
                ", blockId='" + blockId + '\'' +
                ", group='" + group + '\'' +
                ", scriptId='" + scriptId + '\'' +
                ", scriptIdAfterMove='" + scriptIdAfterMove + '\'' +
                ", oldParentId='" + oldParentId + '\'' +
                ", newParentId='" + newParentId + '\'' +
                ", valueType='" + valueType + '\'' +
                ", oldValue='" + oldValue + '\'' +
                ", newValue='" + newValue + '\'' +
                ", category=" + category + '\'' +
                ", oldInputName=" + oldInputName + '\'' +
                ", newInputName=" + newInputName + '\'' +
                ", loopInvolved=" + loopInvolved + '\'' +
                ", conditionalInvolved=" + conditionalInvolved + '\'' +
                ", blockCount=" + blockCount + '\'' +
                ", eventId=" + eventId + '\'' +
                '}';
    }
}
