package carpet.network;

import carpet.CarpetServer;
import carpet.CarpetSettings;
import carpet.script.utils.ShapesRenderer;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;

public class CarpetClient
{
    public record CarpetPayload(CompoundTag data) implements CustomPacketPayload
    {
        public static final StreamCodec<FriendlyByteBuf, CarpetPayload> STREAM_CODEC = CustomPacketPayload.codec(CarpetPayload::write, CarpetPayload::new);

        public static final Type<CarpetPayload> TYPE = new CustomPacketPayload.Type<>(CARPET_CHANNEL);

        public CarpetPayload(FriendlyByteBuf input)
        {
            this(input.readNbt());
        }

        public void write(FriendlyByteBuf output)
        {
            output.writeNbt(data);
        }

        @Override public Type<CarpetPayload> type()
        {
            return TYPE;
        }
    }

    public static final String HI = "69";
    public static final String HELLO = "420";

    public static ShapesRenderer shapes = null;

    private static LocalPlayer clientPlayer = null;
    private static boolean isServerCarpet = false;
    public static String serverCarpetVersion;
    public static final ResourceLocation CARPET_CHANNEL = ResourceLocation.fromNamespaceAndPath("carpet", "hello");

    public static void gameJoined(LocalPlayer player)
    {
        clientPlayer = player;
    }

    public static void disconnect()
    {
        if (isServerCarpet) // multiplayer connection
        {
            isServerCarpet = false;
            clientPlayer = null;
            CarpetServer.onServerClosed(null);
            CarpetServer.onServerDoneClosing(null);
        }
        else // singleplayer disconnect
        {
            CarpetServer.clientPreClosing();
        }
    }

    public static void setCarpet()
    {
        isServerCarpet = true;
    }

    public static LocalPlayer getPlayer()
    {
        return clientPlayer;
    }

    public static boolean isCarpet()
    {
        return isServerCarpet;
    }

    public static boolean sendClientCommand(String command)
    {
        if (!isServerCarpet && CarpetServer.minecraft_server == null)
        {
            return false;
        }
        ClientNetworkHandler.clientCommand(command);
        return true;
    }

    public static void onClientCommand(Tag t)
    {
        CarpetSettings.LOG.info("Server Response:");
        CompoundTag tag = (CompoundTag) t;
        CarpetSettings.LOG.info(" - id: " + tag.getString("id"));
        if (tag.contains("error"))
        {
            CarpetSettings.LOG.warn(" - error: " + tag.getString("error"));
        }
        if (tag.contains("output"))
        {
            ListTag outputTag = (ListTag) tag.get("output");
            Gson gson = new Gson();
            RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, clientPlayer.registryAccess());
            for (int i = 0; i < outputTag.size(); i++)
            {
                JsonElement element = gson.fromJson(outputTag.getString(i).orElseThrow(), JsonElement.class);
                Component component = ComponentSerialization.CODEC.parse(ops, element).getOrThrow();
                CarpetSettings.LOG.info(" - response: " + component.getString());
            }
        }
    }
}
