package edu.up.isgc.vgc.graphic;
import edu.up.isgc.vgc.Component;

public class Image extends Component implements Edit {
    public Image(int width, int height, String date, Double duration, String type, String path){
        super(width, height, date, duration, type, path);
    }

    @Override
    public void printAttributes(){
        System.out.println("|Path: " + getPath());
        System.out.println("||Resolution: " + getWidth() + ":" + getHeight());
        System.out.println("|||Date: " + getDate());
        System.out.println("||||Duration: " + getDuration());
        System.out.println("|||||Type: " + getType());
    }

    @Override
    public String returnIFormat(){ return "Image"; }

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
