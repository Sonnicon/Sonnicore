package sonnicon.sonnicore;

import io.anuke.arc.Core;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.mod.Mod;
import io.anuke.mindustry.mod.Mods;
import sonnicon.sonnicore.core.ModsMod;
import sonnicon.sonnicore.types.ModCategory;
import sonnicon.sonnicore.ui.ModPlacementFragment;

public class Sonnicore extends Mod{
    public Sonnicore(){
        //called even if disabled (??)
        if(!Core.settings.getBool("mod-[#8855ff]sonnicore-enabled", true)) return;

        if(firstInstance()) Vars.mods = new ModsMod(Vars.mods);
    }

    Boolean firstInstance(){
        for(Mods.LoadedMod lm : Vars.mods.all()){
            if(lm.name.equals("[#8855ff]sonnicore")) return false;
        }
        return true;
    }

    @Override
    public void init(){
        ModCategory.init();
        ModPlacementFragment.init();
    }
}
