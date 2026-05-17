package com.github.ruediste.gdriveclient.model;

import java.util.HashMap;

public class Data {

    public long nextId;

    public HashMap<Long, RootDirectory> roots = new HashMap<>();

    public static class RootDirectory {
        public String name;
        public long directoryId;
    }
}
