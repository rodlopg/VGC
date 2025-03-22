package edu.up.isgc.vgc.tools;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

public class CMD{
    public static String[] concat(String[] a, String[] b) {
        return Stream.concat(Arrays.stream(a), Arrays.stream(b)).toArray(String[]::new);
    }

    public static String join(String[] array, String delimiter){
        StringBuilder result = new StringBuilder();
        for(int i = 0; i < array.length; i++){
            result.append(array[i]);

            if(i < array.length - 1){ result.append(delimiter); }
        }
        return result.toString();
    }

    public static void run(String[] command){
        final ProcessBuilder builder = new ProcessBuilder();
        try{
            System.out.println("Executing command: " + CMD.join(command, " "));
            final Process process = builder.command(command).start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            System.out.println("Waiting... " + process.waitFor());
            process.destroy();

        }catch(IOException | InterruptedException e){
            throw new RuntimeException(e);
        }
    }

    public static String expect(String[] command){
        final ProcessBuilder builder = new ProcessBuilder();
        try{
            System.out.println("Executing command: " + CMD.join(command, " "));
            final Process process = builder.command(command).start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            System.out.println("Waiting... " + process.waitFor());
            process.destroy();

            return output.toString().trim();

        }catch(IOException | InterruptedException e){
            throw new RuntimeException(e);
        }
    }

    public static String trimFrom(String output, String trim){
        return output.substring(output.indexOf(trim) + 1).trim();
    }

    public static String trimUntil(String output, String trim) {
        // Handle null inputs
        if (output == null || trim == null) {
            throw new IllegalArgumentException("Input strings cannot be null.");
        }

        // Find the index of the trim string
        int index = output.indexOf(trim);

        // If trim is not found, return the entire output string (trimmed)
        if (index == -1) { return output.trim(); }

        // Return the substring from the start up to the trim string (trimmed)
        return output.substring(0, index).trim();
    }

    public static String normalize(String[] command, String trim){
        return trimFrom(expect(command), trim);
    }

    public static String[] echo(String[] input){
        return input;
    }

    public static void writeTextFile(String filename, String[] text){
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