package com.github.ruediste.gdriveclient.model;

import java.util.HashSet;
import java.util.Optional;

public class Directory {
    public boolean locallyDeleted;
    public boolean shallow;

    public HashSet<Long> files = new HashSet<>();
    public HashSet<Long> subDirectories = new HashSet<>();

    public DirectoryState ors;
    public Optional<DirectoryState> ls = Optional.empty();
}
