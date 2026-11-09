package pl.riftplots;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ShadedSqliteTest {
    @Test
    void packagedDriverLoadsItsNativeLibraryInAnIsolatedJvm() throws Exception {
        String javaExecutable = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        String classes = Path.of(Probe.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        String classpath = System.getProperty("riftplots.shadedJar") + java.io.File.pathSeparator + classes;
        Process process = new ProcessBuilder(javaExecutable, "--enable-native-access=ALL-UNNAMED", "-cp", classpath, Probe.class.getName())
                .redirectErrorStream(true).start();
        assertTrue(process.waitFor(30, TimeUnit.SECONDS), "Driver probe timed out");
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(0, process.exitValue(), output);
        assertTrue(output.contains("SQLITE_OK"), output);
    }

    public static class Probe {
        public static void main(String[] args) throws Exception {
            Class.forName("org.sqlite.JDBC");
            try (var connection = DriverManager.getConnection("jdbc:sqlite::memory:");
                 var statement = connection.createStatement();
                 var result = statement.executeQuery("SELECT 42")) {
                if (!result.next() || result.getInt(1) != 42) throw new AssertionError("SQLite query failed");
                System.out.println("SQLITE_OK");
            }
        }
    }
}
