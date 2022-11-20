package application;

import java.io.Closeable;
import java.io.IOException;
import java.util.Objects;

//关闭所有资源
public class closeAll {
    public static void close(Closeable... closeables) {
        if (Objects.nonNull(closeables)) {
            for (Closeable closeable : closeables) {
                if (closeable!=null) {
                    try {
                        closeable.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
