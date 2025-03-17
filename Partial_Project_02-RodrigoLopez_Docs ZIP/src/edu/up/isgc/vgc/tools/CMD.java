package edu.up.isgc.vgc.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

public class CMD{
    public static String[] concat(String[] a, String[] b) {
        return Stream.concat(Arrays.stream(a), Arrays.stream(b)).toArray(String[]::new);
    }

    public static void run(String[] command){
        final ProcessBuilder builder = new ProcessBuilder();
        try{
            final Process process = builder.command(command).start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            System.out.println("Waiting... " + process.waitFor());
            process.destroy();

        }catch(IOException | InterruptedException e){
            throw new RuntimeException(e);
        }
    }

    public static String echo(String[] command){
        final ProcessBuilder builder = new ProcessBuilder();
        try{
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

    public static void cwTextFile(String[] text){

    }

}