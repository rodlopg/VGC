package edu.up.isgc.vgc.tools;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import static edu.up.isgc.vgc.tools.CMD.concat;

public class Pipeline {
    public static String[] biLambda(List<Function<String[], String[]>> fStream, BiFunction<String[], String[], String[]> pFunction){
        return fStream.stream()
                .reduce((first, second) -> (input) -> pFunction.apply((String[]) first.apply(input), (String[]) second.apply(input)))
                .map(f -> (String[]) f.apply(null)) // Applying the final function and casting the result
                .orElse(new String[]{});
    }

}
