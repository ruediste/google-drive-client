package com.github.ruediste.gdriveclient;

import java.nio.ByteBuffer;
import java.nio.file.Files;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.type.BasicDataType;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.github.ruediste.gdriveclient.model.Data;
import com.github.ruediste.gdriveclient.model.Directory;
import com.github.ruediste.gdriveclient.model.DirectoryState;
import com.github.ruediste.gdriveclient.model.File;
import com.github.ruediste.gdriveclient.model.FileState;

public class DataStore {
    private MVStore s;
    private Kryo kryo;
    public MVMap<Long, Directory> directories;
    public MVMap<Long, File> files;
    private MVMap<Long, Data> datas;

    private long nextId;
    public Data data;

    public void open() throws Exception {

        // temporary
        Files.deleteIfExists(App.dataPath().resolve("client.db"));

        s = new MVStore.Builder().fileName(App.dataPath().resolve("client.db").toAbsolutePath().toString()).compress()
                .autoCommitDisabled().open();

        kryo = new Kryo();
        kryo.register(java.util.HashMap.class);

        datas = register(Data.class);
        kryo.register(Data.RootDirectory.class);

        directories = register(Directory.class);
        kryo.register(DirectoryState.class);

        files = register(File.class);
        kryo.register(FileState.class);

        data = datas.get(0L);
        if (data == null) {
            data = new Data();
            datas.put(0L, data);
            s.commit();
        }

        nextId = data.nextId;

    }

    public void saveData() {
        datas.put(0L, data);
    }

    public void commit() {
        s.commit();
    }

    public long nextId() {
        if (nextId >= data.nextId) {
            data.nextId += 50; // increase in chunks of 50
            datas.put(0L, data);

        }
        return nextId++;
    }

    private <T> MVMap<Long, T> register(Class<T> cls) {
        kryo.register(cls);
        return s.openMap(cls.getSimpleName(),
                new MVMap.Builder<Long, T>().valueType(new KryoDataType(cls)));
    }

    private class KryoDataType extends BasicDataType<Object> {
        private int averageSize = 10_000;

        private Class<?> cls;

        public KryoDataType(Class<?> cls) {
            this.cls = cls;
        }

        @Override
        public Object[] createStorage(int size) {
            return new Object[size];
        }

        @Override
        public int getMemory(Object obj) {
            return averageSize;
        }

        @Override
        public void write(WriteBuffer buff, Object obj) {
            var output = new Output(4096, -1);
            kryo.writeObject(output, obj);
            buff.putInt(output.position());
            updateAverageSize(output.position());
            buff.put(output.getBuffer(), 0, output.position());
        }

        @Override
        public Object read(ByteBuffer buff) {
            var size = buff.getInt();
            updateAverageSize(size);

            var buf = new byte[size];
            buff.get(buf);
            var input = new Input(buf);
            return kryo.readObject(input, cls);
        }

        private void updateAverageSize(int size) {
            averageSize = (int) ((size + 15L * averageSize) / 16);
        }

    }
}
