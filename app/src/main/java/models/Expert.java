package models;

/**
 * Created by bipinbhandari on 7/22/16.
 */

public class Expert {
    public String id;
    public String name;

    public Expert(String id, String name){
        this.id = id;
        this.name = name;
    }

    @Override
    public String toString() {
        return "Expert ("+ this.id+")";
    }
}
