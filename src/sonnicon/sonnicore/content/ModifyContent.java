package sonnicon.sonnicore.content;

import io.anuke.arc.collection.Array;
import io.anuke.arc.collection.ObjectMap;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.ContentLoader;
import io.anuke.mindustry.ctype.Content;
import io.anuke.mindustry.ctype.MappableContent;
import io.anuke.mindustry.type.ContentType;
import io.anuke.mindustry.world.Block;

import java.lang.reflect.Field;
import java.util.function.Supplier;

public class ModifyContent{
    private static Field f1, f2;

    public static boolean replaceBlock(String target, Supplier<Block> sup){
        try{
            if(f1 == null){
                f1 = ContentLoader.class.getDeclaredField("contentMap");
                f1.setAccessible(true);
            }
            Array<Content> cpointer = ((Array<Content>[]) f1.get(Vars.content))[ContentType.block.ordinal()];
            short id = (short) cpointer.indexOf(Vars.content.getByName(ContentType.block, target));
            if(!removeBlockNameMap(target)) return false;

            Block b = sup.get();

            Field f3 = Content.class.getDeclaredField("id");
            f3.setAccessible(true);
            f3.set(b, id);

            cpointer.insert(id, cpointer.get(cpointer.size - 1));
            cpointer.remove(id + 1);
            cpointer.remove(cpointer.size - 1);
            f3.setAccessible(false);
            return true;
        }catch(NoSuchFieldException | IllegalAccessException ex){
            ex.printStackTrace();
            return false;
        }
    }

    public static boolean removeBlockNameMap(String name){
        try{
            if(f2 == null){
                f2 = ContentLoader.class.getDeclaredField("contentNameMap");
                f2.setAccessible(true);
            }
            ((ObjectMap<String, MappableContent>[]) f2.get(Vars.content))[ContentType.block.ordinal()].remove(name);
            return true;
        }catch(NoSuchFieldException | IllegalAccessException ex){
            ex.printStackTrace();
            return false;
        }
    }
}
