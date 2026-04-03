package com.example.aidevelop.model.dto.loanform;

public class FieldFillRequest {
    private String tabId;
    private String fieldName;
    private String fieldLabel;
    private String value;

    public FieldFillRequest() {
    }

    public FieldFillRequest(String tabId, String fieldName, String fieldLabel, String value) {
        this.tabId = tabId;
        this.fieldName = fieldName;
        this.fieldLabel = fieldLabel;
        this.value = value;
    }

    public String getTabId() {
        return tabId;
    }

    public void setTabId(String tabId) {
        this.tabId = tabId;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldLabel() {
        return fieldLabel;
    }

    public void setFieldLabel(String fieldLabel) {
        this.fieldLabel = fieldLabel;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
