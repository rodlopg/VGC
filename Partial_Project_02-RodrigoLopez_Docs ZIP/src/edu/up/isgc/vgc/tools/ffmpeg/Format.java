package edu.up.isgc.vgc.tools.ffmpeg;

public class Format {
    private final static String[] iFormat = new String[]{"v", "a", "s"};
    private final static String[] pFormat = new String[]{"yuv420p", "yuv422p", "yuv444p", "yuvj420p", "rgb24", "rgba", "gray"};
    private final static String[] bFormat = new String[]{"k", "m", "g"};
    private final static String[][] cFormat = new String[][]{
            // Video Codecs
            {
                    "libx264",    // Most widely used, efficient, supported everywhere
                    "libx265",    // More efficient than H.264 but requires more processing power
                    "vp9",        // Open-source, good quality at low bitrates
                    "av1",        // Newer, better compression, but slow encoding
                    "mpeg4",      // Older, still in use for compatibility
                    "libvpx",     // VP8, mainly for WebRTC
                    "prores",     // Apple ProRes, high-quality intermediate format
                    "dnxhd",      // Avid DNxHD, similar to ProRes, used in editing
                    "rawvideo"    // Uncompressed, massive file sizes
            },
            // Audio Codecs
            {
                    "aac",        // Most common, supported in almost every device
                    "mp3",        // Universal, slightly outdated
                    "opus",       // Excellent quality at low bitrates, used in WebRTC
                    "vorbis",     // Open-source, used in Ogg files
                    "flac",       // Lossless compression
                    "pcm_s16le",  // Uncompressed PCM (used in WAV)
                    "ac3",        // Dolby Digital, used in DVDs
                    "eac3",       // Enhanced AC3, used in streaming services
                    "dts"         // DTS surround sound, used in Blu-rays
            },
            // Subtitle Codecs
            {
                    "mov_text",   // Most common embedded subtitle format (MP4)
                    "srt",        // Simple text-based subtitles, widely used
                    "ass",        // Advanced SubStation Alpha, used for styled subs
                    "ssa",        // SubStation Alpha, older version of ASS
                    "vtt",        // WebVTT, used in HTML5 videos
                    "pgs",        // Blu-ray subtitles (bitmap-based)
                    "dvdsub",     // DVD subtitles (bitmap-based)
                    "hdmv_pgs_subtitle" // Used in Blu-rays, similar to PGS
            }
    };

    private final static String[] presets = {
            "ultrafast",  // Fastest encoding speed, but largest file size and lowest compression
            "superfast",  // Very fast encoding, slightly better compression than ultrafast
            "veryfast",   // Fast encoding, good for quick processing with larger files
            "faster",     // Still fast, but with slightly better compression efficiency
            "fast",       // Good balance between speed and compression
            "medium",     // Default preset, balancing speed and quality
            "slow",       // Slower encoding, but improved compression and smaller file size
            "slower",     // Very slow encoding, high compression efficiency, smaller file
            "veryslow"    // Slowest encoding, best compression, highest quality, smallest file
    };

    public static String bitRate(int bitRate, int unit){ return bitRate + Format.getBitUnit(unit); }

    public static String getFile(int iFormat) { return Format.iFormat[iFormat % Format.iFormat.length]; }
    public static String getPixel(int pFormat) { return Format.pFormat[pFormat % Format.pFormat.length]; }
    public static String getBitUnit(int bFormat) { return Format.bFormat[bFormat % Format.bFormat.length]; }
    public static String getCodec(int iFormat, int cFormat) {
        return Format.cFormat[iFormat % Format.iFormat.length][cFormat % Format.cFormat.length];
    }
    public static String getPreset(int preset) { return Format.presets[preset % Format.presets.length]; }
    public static String getCRF(int crf) { return Integer.toString(crf % 51); }
}
