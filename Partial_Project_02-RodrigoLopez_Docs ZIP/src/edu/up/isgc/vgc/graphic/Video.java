package edu.up.isgc.vgc.graphic;
import edu.up.isgc.vgc.Component;

public class Video extends Component implements Edit {
    private String codec;

    public Video(int width, int height, String date, Double duration, String type, String path, String codec){
        super(width, height, date, duration, type, path);
        this.setCodec(codec);
    }

    @Override
    public void printAttributes(){
        System.out.println("|Path: " + getPath());
        System.out.println("||Resolution: " + getWidth() + ":" + getHeight());
        System.out.println("|||Date: " + getDate());
        System.out.println("||||Duration: " + getDuration());
        System.out.println("|||||Type: " + getType());
        System.out.println("||||||Codec: " + getCodec());
    }

    public String getCodec() {
        return codec;
    }

    public void setCodec(String codec) {
        this.codec = codec;
    }

    @Override
    public Component scale(){
        return this;
    }

    @Override
    public Component cut(){
        return this;
    }

    @Override
    public Component copy(){
        return this;
    }

}
