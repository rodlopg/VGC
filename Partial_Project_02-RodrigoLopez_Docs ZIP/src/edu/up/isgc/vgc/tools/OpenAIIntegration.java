package edu.up.isgc.vgc.tools;

import edu.up.isgc.vgc.Component;
import edu.up.isgc.vgc.tools.CMD;

/**
 * Handles integration with the OpenAI API for generating postcards and text-to-speech narration.
 */
public class OpenAIIntegration extends Component {
    private static final String API_KEY = "your_openai_api_key";
    private static final String IMAGE_GENERATION_URL = "https://api.openai.com/v1/images/generations";
    private static final String TEXT_TO_SPEECH_URL = "https://api.openai.com/v1/audio/speech";

    public OpenAIIntegration(int width, int height, String date, Double duration, String type, String path) {
        super(width, height, date, duration, type, path);
    }

    @Override
    public void printAttributes() {
        System.out.println("|Path: " + getPath());
        System.out.println("||Resolution: " + getWidth() + ":" + getHeight());
        System.out.println("|||Date: " + getDate());
        System.out.println("||||Duration: " + getDuration());
        System.out.println("|||||Type: " + getType());
    }

    /**
     * Generates a postcard using the OpenAI API.
     */
    public String generatePostcard(String prompt) {
        String[] command = new String[]{
                "curl", "-X", "POST", IMAGE_GENERATION_URL,
                "-H", "Authorization: Bearer " + API_KEY,
                "-H", "Content-Type: application/json",
                "-d", "{\"prompt\": \"" + prompt + "\", \"n\": 1, \"size\": \"1024x1024\"}"
        };
        return CMD.expect(command);
    }

    /**
     * Generates text-to-speech narration using the OpenAI API.
     */
    public String generateNarration(String text) {
        String[] command = new String[]{
                "curl", "-X", "POST", TEXT_TO_SPEECH_URL,
                "-H", "Authorization: Bearer " + API_KEY,
                "-H", "Content-Type: application/json",
                "-d", "{\"model\": \"tts-1\", \"input\": \"" + text + "\", \"voice\": \"alloy\"}"
        };
        return CMD.expect(command);
    }
}