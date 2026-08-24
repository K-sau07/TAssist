package com.tassist.infrastructure.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Binds tassist.storage.dir — root directory for local-disk raw file storage (§11.1 step 3). */
@ConfigurationProperties(prefix = "tassist.storage")
public class StorageProperties {
    private String dir = "./storage";
    public String getDir() { return dir; }
    public void setDir(String dir) { this.dir = dir; }
}
