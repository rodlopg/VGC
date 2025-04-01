package edu.up.isgc.vgc.tools;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import static edu.up.isgc.vgc.tools.CMD.concat;

public class Pipeline {

    // Method to process a list of functions using a BiFunction to combine the results
    public static String[] biLambda(List<Function<String[], String[]>> fStream, BiFunction<String[], String[], String[]> pFunction) {

        // Stream through the list of functions (fStream) and apply the BiFunction to combine them
        return fStream.stream()
                // Reduce the stream of functions to a single function that applies the BiFunction
                .reduce((first, second) -> (input) -> pFunction.apply((String[]) first.apply(input), (String[]) second.apply(input)))
                // Apply the final function and cast the result to a String array
                .map(f -> (String[]) f.apply(null))
                // If the stream is empty, return an empty String array
                .orElse(new String[]{});
    }
}
