package com.wexresprise.store.style;

import java.io.File;

/** A style installed from a .ws archive. */
public final class InstalledStyle {

    public final String id;
    public final String name;
    public final String version;
    public final String author;
    public final String description;
    public final String className;
    public final File dir;
    public final File icon;

    public InstalledStyle(String id, String name, String version, String author,
                          String description, String className, File dir, File icon) {
        this.id = id;
        this.name = name == null || name.trim().isEmpty() ? id : name.trim();
        this.version = version == null ? "" : version.trim();
        this.author = author == null ? "" : author.trim();
        this.description = description == null ? "" : description.trim();
        this.className = className == null ? "" : className.trim();
        this.dir = dir;
        this.icon = icon;
    }
}
