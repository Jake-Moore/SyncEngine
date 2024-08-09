package com.kamikazejam.syncengine.util;

import com.kamikazejam.syncengine.EngineSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * Utility class for logging stack traces to file
 * For developer warnings, developers need the trace, but don't necessarily need to spam the console
 * We can print helpful stack traces to a log file, and send a reduced warning to the console
 */
public class SyncFileLogger {
    /**
     * Logs a warning message to the console, and saves the current stack trace to a log file
     * @return The file written, if successful
     */
    @Nullable
    public static File warn(@NotNull String msg) {
        // Print the message + a stack trace to a file
        File file = new File(EngineSource.get().getDataFolder() + File.separator + "logs",
                System.currentTimeMillis() + ".log");

        IOException saveError = appendToFile(createStackTrace(msg), file);
        if (saveError == null) {
            EngineSource.get().getColorLogger().warn(msg + " (Logged to " + "/logs/" + file.getName() + ")");
        }else {
            EngineSource.get().getLogger().warning("Failed to write stack trace to file (" + file.getAbsoluteFile() + "): " + saveError.getMessage());
            return null;
        }
        return file;
    }

    /**
     * Logs a warning message to the console, and saves the current stack trace to a log file
     * Also appends the given trace to the file
     * @return The file written, if successful
     */
    @Nullable
    public static File warn(@NotNull String msg, @NotNull Throwable trace) {
        // Log the trace from what we can find
        File file = SyncFileLogger.warn(msg);
        if (file == null) { return null; }

        // Add some empty lines for separation
        IOException err1 = appendToFile(List.of("", "", "Extra Trace (if necessary)", ""), file);
        if (err1 != null) {
            EngineSource.get().getLogger().warning("Failed to write stack trace to file (" + file.getAbsoluteFile() + "): " + err1.getMessage());
            return null;
        }

        // Save the original trace after
        IOException err2 = appendToFile(trace, file);
        if (err2 != null) {
            EngineSource.get().getLogger().warning("Failed to write stack trace to file (" + file.getAbsoluteFile() + "): " + err2.getMessage());
            return null;
        }
        return file;
    }

    public static Throwable createStackTrace(@NotNull String msg) {
        try {
            throw new Exception(msg);
        }catch (Throwable t) {
            return t;
        }
    }

    @Nullable
    public static IOException appendToFile(@NotNull Throwable throwable, @NotNull File file) {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            boolean ignored = parent.mkdirs();
        }

        try (FileWriter fileWriter = new FileWriter(file, true); PrintWriter printWriter = new PrintWriter(fileWriter)) {
            // Write the stack trace to the file
            throwable.printStackTrace(printWriter);
            return null;
        } catch (IOException e) {
            return e;
        }
    }

    @Nullable
    public static IOException appendToFile(@NotNull List<String> lines, @NotNull File file) {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            boolean ignored = parent.mkdirs();
        }

        try (FileWriter fileWriter = new FileWriter(file, true); PrintWriter printWriter = new PrintWriter(fileWriter)) {
            // Write the stack trace to the file
            for (String line : lines) {
                printWriter.println(line);
            }
            return null;
        } catch (IOException e) {
            return e;
        }
    }
}
