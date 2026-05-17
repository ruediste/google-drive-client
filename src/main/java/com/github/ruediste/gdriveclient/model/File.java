package com.github.ruediste.gdriveclient.model;

import java.util.Optional;

public class File {
    public boolean locallyDeleted;
    public FileState ors;
    public Optional<FileState> ls = Optional.empty();
}
