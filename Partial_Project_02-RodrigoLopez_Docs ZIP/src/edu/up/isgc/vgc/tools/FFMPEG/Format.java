package edu.up.isgc.vgc.tools.FFMPEG;

public class Format {
    private final static String[] iFormat = new String[]{"v", "a", "s"};
    private final static String[] pFormat = new String[]{"yuv420p", "yuv422p", "yuv444p", "yuvj420p", "rgb24", "rgba", "gray"};
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

    private final static String[] crf = {
            "0",    // 0 → Lossless (highest quality, very large file)
            "20",   // 18-22 → Visually lossless (very high quality)
            "25",   // 23-28 → Good balance of quality and file size (default range)
            "32",   // 29-35 → Noticeable compression, lower quality
            "51"    // 51 → Worst quality (smallest file)
    };



    public static String getFile(int format) { return Format.iFormat[format % Format.iFormat.length]; }
    public static String getPixel(int format) { return Format.pFormat[format % Format.pFormat.length]; }
    public static String getCodec(int iFormat, int format) {
        return Format.cFormat[iFormat % Format.iFormat.length][format % Format.cFormat.length];
    }
    public static String getPreset(int preset) { return Format.presets[preset % Format.presets.length]; }
    public static String getCRF(int crf) { return Format.crf[crf % Format.crf.length]; }
}
