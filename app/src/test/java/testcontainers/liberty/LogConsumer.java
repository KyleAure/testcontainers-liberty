package testcontainers.liberty;

import java.util.Objects;
import java.util.function.Consumer;

import org.testcontainers.containers.output.OutputFrame;

public class LogConsumer implements Consumer<OutputFrame> {
    private final Class<?> clazz;
    private final String containerName;

    /**
     * @param clazz         The class to log container output as. Usually the test class itself.
     * @param containerName The prefix ID to use for log statements. Usually the container name.
     */
    public LogConsumer(Class<?> clazz, String containerName) {
        Objects.requireNonNull(clazz);
        Objects.requireNonNull(containerName);
        this.clazz = clazz;
        this.containerName = containerName;
    }

    @Override
    public void accept(OutputFrame frame) {
        String msg = frame.getUtf8String();
        if (msg.endsWith("\n"))
            msg = msg.substring(0, msg.length() - 1);
        System.out.println(clazz + "\t[" + containerName + "]\t" + msg);
    }
}
