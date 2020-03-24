package sonnicon.sonnicore.core;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.mod.Mods;
import sonnicon.sonnicore.Sonnicore;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;

public class ModsMod extends Mods{
    private Mods original;
    public ModsMod(Mods mods){
        this.original = mods;
    }

    @Override
    public void loadContent(){
        Vars.mods = original;
        try{
            ArrayList<URL> urls = new ArrayList<>();
            for(LoadedMod lm : original.all()) urls.add(lm.file.file().toURI().toURL());
            URLClassLoader loader = new URLClassLoader(urls.toArray(new URL[original.all().size]));
            for(LoadedMod lm : original.all()){
                Class<?> main = loader.loadClass(lm.meta.main);
                Field f = lm.getClass().getDeclaredField("mod");
                Field modifiersField = Field.class.getDeclaredField("modifiers");
                modifiersField.setAccessible(true);
                modifiersField.setInt(f, f.getModifiers() & ~Modifier.FINAL);
                f.set(lm, main.getDeclaredConstructor().newInstance());
            }
        }catch(Exception ex){
            Vars.mods.handleError(ex, Vars.mods.getMod(Sonnicore.class));
        }
        original = null;
        Vars.mods.loadContent();
    }
}
