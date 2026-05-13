package com.smartoptimizer.config;

public class ConfigEntry {

    private final String modId;
    private final String fileName;
    private final String key;
    private final String value;

    public ConfigEntry(String modId, String fileName, String key, String value) {
        this.modId    = modId;
        this.fileName = fileName;
        this.key      = key;
        this.value    = value;
    }

    public String getModId()    { return modId; }
    public String getFileName() { return fileName; }
    public String getKey()      { return key; }
    public String getValue()    { return value; }
}
