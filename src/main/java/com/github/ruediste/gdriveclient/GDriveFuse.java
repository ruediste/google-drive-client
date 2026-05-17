package com.github.ruediste.gdriveclient;

import jnr.ffi.Pointer;
import ru.serce.jnrfuse.ErrorCodes;
import ru.serce.jnrfuse.FuseFillDir;
import ru.serce.jnrfuse.FuseStubFS;
import ru.serce.jnrfuse.struct.FileStat;
import ru.serce.jnrfuse.struct.FuseFileInfo;

public class GDriveFuse extends FuseStubFS {

    @Override
    public int readdir(String path, Pointer buf, FuseFillDir filter, long offset, FuseFileInfo fi) {
        System.out.println("readdir " + path);
        if ("/".equals(path)) {
            filter.apply(buf, "test", null, 0);
            return 0;
        }

        return -ErrorCodes.ENOENT();
    }

    @Override
    public int getattr(String path, FileStat stat) {
        if ("/".equals(path)) {
            stat.st_mode.set(FileStat.S_IFDIR | 0755);
            return 0;
        }
        return -ErrorCodes.ENOENT();
    }
}
