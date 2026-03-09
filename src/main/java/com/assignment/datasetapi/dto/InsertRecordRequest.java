package com.assignment.datasetapi.dto;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;
@Data
public class InsertRecordRequest {

    /**
     * So { "id": 1, "name": "John" } becomes:
     * fields = {"id": 1, "name": "John"}
     */
    private Map<String, Object> fields = new LinkedHashMap<>();

    @JsonAnySetter
    public void addField(String key, Object value) {
        this.fields.put(key, value);
    }
    @JsonAnyGetter
    public Map<String, Object> getFields() {
        return fields;
    }
    @NotNull
    public Map<String, Object> getFieldsForValidation() {
        return fields;
    }

    public boolean isEmpty() {
        return fields == null || fields.isEmpty();
    }
}
