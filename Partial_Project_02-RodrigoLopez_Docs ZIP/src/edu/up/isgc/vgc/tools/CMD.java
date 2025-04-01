package edu.up.isgc.vgc.tools;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

public class CMD {
    /**
     * Normalizes a file path by replacing backslashes with forward slashes
     * and ensuring it is enclosed in quotes.
     */
    public static String normalizePath(String path) {
        return "\"" + path.replace('\\', '/').replace("\"", "") + "\"";
    }

    /**
     * Concatenates two string arrays into a single array.
     */
    public static String[] concat(String[] a, String[] b) {
        return Stream.concat(Arrays.stream(a), Arrays.stream(b)).toArray(String[]::new);
    }

    /**
     * Joins elements of a string array into a single string, separated by a specified delimiter.
     */
    public static String join(String[] array, String delimiter) {
        return String.join(delimiter, array);
    }

    /**
     * Executes a command in the system's command line and prints its output.
     * Returns true if the command executes successfully (exit code 0), otherwise false.
     */
    public static boolean run(String[] command) {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);

        try {
            System.out.println("Executing: " + String.join(" ", command));
            Process process = builder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("FFmpeg: " + line);
                }
            }

            int exitCode = process.waitFor();
            System.out.println("Process exited with code: " + exitCode);

            if (process.isAlive()) {
                process.destroyForcibly();
            }

            return exitCode == 0;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Executes a command and returns its output as a string.
     */
    public static String expect(String[] command) {
        final ProcessBuilder builder = new ProcessBuilder();
        try {
            System.out.println("Executing command: " + String.join(" ", command));
            final Process process = builder.command(command).start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            int exitCode = process.waitFor();
            System.out.println("Process exited with code: " + exitCode);
            process.destroy();

            return output.toString().trim();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Removes all text before and including the first occurrence of a specified string.
     */
    public static String trimFrom(String output, String trim) {
        return output.substring(output.indexOf(trim) + 1).trim();
    }

    /**
     * Removes all text after and including the first occurrence of a specified string.
     */
    public static String trimUntil(String output, String trim) {
        if (output == null || trim == null) {
            throw new IllegalArgumentException("Input strings cannot be null.");
        }

        int index = output.indexOf(trim);
        if (index == -1) {
            return output.trim();
        }

        return output.substring(0, index).trim();
    }

    /**
     * Executes a command and trims its output from a specified substring.
     */
    public static String normalize(String[] command, String trim) {
        return trimFrom(expect(command), trim);
    }

    /**
     * Returns the input array unchanged (acts as an identity function).
     */
    public static String[] echo(String[] input) {
        return input;
    }

    /**
     * Writes an array of strings to a text file with a specified filename.
     */
    public static void writeTextFile(String filename, String[] text) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename + ".txt"))) {
            for (String line : text) {
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
