package sonnicon.sonnicore;

import io.anuke.mindustry.mod.Mod;
import sonnicon.sonnicore.types.ModCategory;
import sonnicon.sonnicore.ui.ModPlacementFragment;

public class Sonnicore extends Mod{
    @Override
    public void init(){
        ModCategory.init();
        ModPlacementFragment.init();
    }
}
