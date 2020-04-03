package sonnicon.sonnicore.net;

import io.anuke.arc.collection.IntMap;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.net.Packets.InvokePacket;

import java.nio.ByteBuffer;

public class ModInvokePacketHandler{
    static byte ids = 0;
    public static IntMap<ModInvokePacketHandler> handlers;

    public final byte id;

    public ModInvokePacketHandler(){
        id = (byte) (Byte.MAX_VALUE-(ids++));
        if(handlers == null){
            handlers = new IntMap<>();
            if(Vars.headless){
                Packets.addServerListener(InvokePacket.class, (netConnection, packet) -> {
                    if(!handlers.containsKey(((InvokePacket)packet).type)) return;
                    handlers.get(((InvokePacket)packet).type).readServer(((InvokePacket)packet).writeBuffer, ((InvokePacket)packet).type, netConnection.player);
                });
            }else{
                Packets.addClientListener(InvokePacket.class, packet -> {
                    if(!handlers.containsKey(((InvokePacket)packet).type)) return;
                    handlers.get(((InvokePacket)packet).type).readClient(((InvokePacket) packet).writeBuffer, ((InvokePacket) packet).type);
                });
            }
        }
        handlers.put(id, this);
    }

    public void readClient(ByteBuffer buffer, int id){
    }

    public void readServer(ByteBuffer buffer, int id, Player player){
    }
}
