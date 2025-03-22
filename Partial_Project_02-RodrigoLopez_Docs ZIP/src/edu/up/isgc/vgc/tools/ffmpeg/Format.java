package edu.up.isgc.vgc.tools.ffmpeg;

/**
 * Provides constants and utility methods for FFMPEG formats.
 */
public class Format {
    private final static String[] ifformat = new String[]{"v", "a", "s"}; // Input formats
    private final static String[] pFormat = new String[]{"yuv420p", "yuv422p", "yuv444p", "yuv4420p", "rgb24", "rgba", "gray"}; // Pixel formats
    private final static String[] bFormat = new String[]{"k", "m", "g"}; // Bitrate units
    private final static String[][] cFormat = new String[][]{
            {"libx264", "libx265", "vp9", "avl", "mpeg4", "libvpx", "prores", "dnxhd", "rawvideo"}, // Video codecs
            {"aac", "mp3", "opus", "vorbis", "flac", "pcm_s16le", "ac3", "eac3", "dts"}, // Audio codecs
            {"mov_text", "srt", "ass", "ssa", "vtt", "pgs", "dvdsub", "hdmv_pgs_subtitle"} // Subtitle codecs
    };

    private final static String[] presets = {
            "ultrafast", "superfast", "veryfast", "faster", "fast", "medium", "slow", "slower", "veryslow" // Encoding presets
    };

    /**
     * Generates a bitrate string.
     */
    public static String bitRate(int bitRate, int unit) {
        return bitRate + Format.getBitUnit(unit);
    }

    /**
     * Returns the file format for a given index.
     */
    public static String getFile(int iFormat) {
        return Format.ifformat[iFormat % Format.ifformat.length];
    }

    /**
     * Returns the pixel format for a given index.
     */
    public static String getPixel(int pFormat) {
        return Format.pFormat[pFormat % Format.pFormat.length];
    }

    /**
     * Returns the bitrate unit for a given index.
     */
    public static String getBitUnit(int bFormat) {
        return Format.bFormat[bFormat % Format.bFormat.length];
    }

    /**
     * Returns the codec for a given format and index.
     */
    public static String getCodec(int iFormat, int cFormat) {
        return Format.cFormat[iFormat % Format.ifformat.length][cFormat % Format.cFormat.length];
    }

    /**
     * Returns the encoding preset for a given index.
     */
    public static String getPreset(int preset) {
        return Format.presets[preset % Format.presets.length];
    }

    /**
     * Returns the CRF value for a given index.
     */
    public static String getCRF(int crf) {
        return Integer.toString(crf % 51);
    }
}