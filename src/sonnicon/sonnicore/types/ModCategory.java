package sonnicon.sonnicore.types;

import java.util.ArrayList;
import java.util.HashMap;

public class ModCategory{
    public final String name;
    public String icon;

    private static HashMap<String, ModCategory> categories = new HashMap<>();
    public static ArrayList<ModCategory> categoriesOrdered = new ArrayList<>();

    public ModCategory(String name){
        this(name, "icon-" + name + "-smaller");
    }

    public ModCategory(String name, String icon){
        this.name = name;
        this.icon = icon;
        categories.put(name, this);
        categoriesOrdered.add(this);
    }

    public static void init(){
        for(io.anuke.mindustry.type.Category cat : io.anuke.mindustry.type.Category.values()) new ModCategory(cat.name());
    }

    public static ModCategory get(int index){
        return all().get(index);
    }

    public static ModCategory get(String name){
        return categories.get(name);
    }

    public static int size(){
        return categories.size();
    }

    public static ArrayList<ModCategory> all(){
        return categoriesOrdered;
    }

    public static int indexOf(ModCategory cat){
        return categoriesOrdered.indexOf(cat);
    }

    public ModCategory next(){
        return get((indexOf(this) + 1) % size());
    }

    public ModCategory prev(){
        return get((indexOf(this) - 1 + size()) % size());
    }
}
