package sonnicon.sonnicore.world;

import io.anuke.mindustry.type.Category;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.meta.BuildVisibility;
import sonnicon.sonnicore.types.ModCategory;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;

public class BlockUtils{
    private static HashMap<Block, ModCategory> blockCategories = new HashMap<>();

    public static Block requirements(Block block, ModCategory category, ItemStack[] stack, boolean unlocked){
        try{
            Method method = getMethod(block.getClass(),"requirements", Category.class, ItemStack[].class, boolean.class);
            method.setAccessible(true);
            method.invoke(block, null, stack, unlocked);
            blockCategories.put(block, category);
        }catch(Exception ex){
            ex.printStackTrace();
            System.exit(0);
        }
        return block;
    }

    public static Block requirements(Block block, ModCategory category, ItemStack[] stack){
        try{
            Method method = getMethod(block.getClass(),"requirements", Category.class, ItemStack[].class);
            method.setAccessible(true);
            method.invoke(block, null, stack);
            blockCategories.put(block, category);
        }catch(Exception ex){
            ex.printStackTrace();
            System.exit(0);
        }
        return block;

    }

    public static Block requirements(Block block, ModCategory category, BuildVisibility visible, ItemStack[] stacks){
        try{
            Method method = getMethod(block.getClass(),"requirements", Category.class, BuildVisibility.class, ItemStack[].class);
            method.setAccessible(true);
            method.invoke(block, null, visible, stacks);
            blockCategories.put(block, category);
        }catch(Exception ex){
            ex.printStackTrace();
            System.exit(0);
        }
        return block;
    }

    public static void setCategory(Block block, ModCategory category){
        block.category = null;
        blockCategories.put(block, category);
    }

    public static ModCategory getByBlock(Block block){
        return blockCategories.get(block);
    }

    private static Method getMethod(Class<?> clazz, String name, Object... args) throws NoSuchMethodException{
        Method[] methods;
        while (clazz != null) {
            methods = clazz.getDeclaredMethods();
            for(Method method : methods) if(method.getName().equals(name) && Arrays.equals(method.getParameterTypes(), args)) return method;
            clazz = clazz.getSuperclass();
        }
        throw new NoSuchMethodException();
    }
}
