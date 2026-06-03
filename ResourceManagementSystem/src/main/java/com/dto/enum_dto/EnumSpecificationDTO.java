package com.dto.enum_dto;

public class EnumSpecificationDTO {
    private String enumName;
    private String category;
    private String description;
    private String[] values;
    private String usageContext;

    public EnumSpecificationDTO() {}

    public EnumSpecificationDTO(String enumName, String category, String description, String[] values, String usageContext) {
        this.enumName = enumName;
        this.category = category;
        this.description = description;
        this.values = values;
        this.usageContext = usageContext;
    }

    public String getEnumName() {
        return enumName;
    }

    public void setEnumName(String enumName) {
        this.enumName = enumName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String[] getValues() {
        return values;
    }

    public void setValues(String[] values) {
        this.values = values;
    }

    public String getUsageContext() {
        return usageContext;
    }

    public void setUsageContext(String usageContext) {
        this.usageContext = usageContext;
    }
}
