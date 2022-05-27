package sonnicon.sonnicore.net;

import io.anuke.arc.collection.Array;
import io.anuke.arc.collection.ObjectMap;
import io.anuke.arc.func.Cons;
import io.anuke.arc.func.Cons2;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.NetConnection;
import io.anuke.mindustry.net.Packets.InvokePacket;

import java.lang.reflect.Field;

public class Packets{
    static ObjectMap<Class<?>, Object> listeners;
    static ObjectMap<Class<?>, Array<Object>> modListeners = new ObjectMap<>();
    static Array<Class> modifiedListeners = new Array<Class>();

    private static boolean inited = false;

    public static void init(){
        if(inited) return;
        inited = true;
        try{
            if(!Vars.headless){
                Field field = Net.class.getDeclaredField("clientListeners");
                field.setAccessible(true);
                listeners = (ObjectMap<Class<?>, Object>) field.get(Vars.net);
            }else{
                Field field = Net.class.getDeclaredField("serverListeners");
                field.setAccessible(true);
                listeners = (ObjectMap<Class<?>, Object>) field.get(Vars.net);
            }
        }catch(NoSuchFieldException | IllegalAccessException ex){
            ex.printStackTrace();
            System.exit(0);
        }
    }

    private static boolean modifyListener(Class<?> c){
        if(modifiedListeners.contains(c)) return true;
        if(listeners.containsKey(c)){
            if(!Vars.headless){
                final Cons oldListener = (Cons) listeners.get(c);
                listeners.put(c, (Cons) packet -> {
                    if(vanillaReadPacket(packet)) oldListener.get(packet);
                    for(Object listener : modListeners.get(c)){
                        ((Cons) listener).get(packet);
                    }
                });
            }else{
                final Cons2 oldListener = (Cons2) listeners.get(c);
                listeners.put(c, (Cons2)(netcon, packet) -> {
                    if(vanillaReadPacket(packet)) oldListener.get(netcon, packet);
                    if(modListeners.containsKey(c)){
                        for(Object listener : modListeners.get(c)){
                            ((Cons2) listener).get(netcon, packet);
                        }
                    }
                });
            }
            modListeners.put(c, new Array<>());
        }else return false;
        return true;
    }

    static boolean vanillaReadPacket(Object packet){
        return !(packet instanceof InvokePacket) || ((InvokePacket)packet).type < 64;
    }

    //For parameter types
    public static void addClientListener(Class<?> c, Cons cons){
        init();
        if(Vars.headless) throw new IllegalStateException("A mod attempted to add a NetClient listener on a headless instance.");
        if(!modifyListener(c)){
            modListeners.put(c, new Array<>());
        }
        modListeners.get(c).add(cons);
    }

    //For parameter types
    public static void addServerListener(Class<?> c, Cons2<NetConnection, Object> cons){
        init();
        if(!Vars.headless) throw new IllegalStateException("A mod attempted to add a NetServer listener on a non-headless instance.");
        if(!modifyListener(c)){
            modListeners.put(c, new Array<>());
        }
        modListeners.get(c).add(cons);
    }
}
