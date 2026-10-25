package fr.natsu.artemis.net;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.protobuf.Any;

import fr.natsu.artemis.Artemis;
import fr.natsu.artemis.LunarHost;
import fr.natsu.artemis.module.glow.GlowModule;
import fr.natsu.artemis.module.chat.ArtemisChat;
import fr.natsu.artemis.module.events.ArtemisEvents;
import fr.natsu.artemis.module.lightning.ArtemisLightning;
import io.netty.buffer.Unpooled;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public final class ApolloNetwork {

    private static final ApolloNetwork INSTANCE = new ApolloNetwork();

    private static final String REGISTER_SEPARATOR = String.valueOf((char) 0);

    private final Map<String, ApolloPacketHandler> handlers = new ConcurrentHashMap<>();
    private final java.util.Set<String> passedToLunar = java.util.Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    private boolean logAllIncoming = true;

    private ApolloNetwork() {
    }

    public static ApolloNetwork getInstance() {
        return INSTANCE;
    }

    public void register(String messageType, ApolloPacketHandler handler) {
        this.handlers.put(messageType, handler);
        Artemis.LOGGER.info("[Apollo] handler registered for {}", messageType);
    }

    public void setLogAllIncoming(boolean logAllIncoming) {
        this.logAllIncoming = logAllIncoming;
    }

    @SubscribeEvent
    public void onConnect(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        this.sendChannelRegistration(event.manager);
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        Artemis.LOGGER.info("[Apollo] disconnected from the server");
    }

    private void sendChannelRegistration(NetworkManager manager) {
        if (manager == null) {
            return;
        }
        String channels = LunarHost.isLunar()
            ? String.join(REGISTER_SEPARATOR, ArtemisChat.CHANNEL, ArtemisLightning.CHANNEL)
            : String.join(REGISTER_SEPARATOR,
                ApolloProtocol.CHANNEL, ArtemisChat.CHANNEL, ArtemisLightning.CHANNEL, ArtemisEvents.CHANNEL);
        PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
        buffer.writeBytes(channels.getBytes(StandardCharsets.UTF_8));
        manager.sendPacket(new C17PacketCustomPayload(ApolloProtocol.REGISTER_CHANNEL, buffer));
        Artemis.LOGGER.info("[Apollo] announced channels to the server: {}",
            channels.replace(REGISTER_SEPARATOR, ", "));
    }

    public boolean handleIncomingIfOwned(byte[] payload) {
        if (!LunarHost.isLunar()) {
            this.handleIncoming(payload);
            return true;
        }
        Any any;
        try {
            any = Any.parseFrom(payload);
        } catch (Exception e) {
            return false;
        }
        String type = ApolloProtocol.messageType(any.getTypeUrl());
        if (!type.startsWith(GlowModule.MESSAGE_PREFIX)) {
            if (this.passedToLunar.add(type)) {
                Artemis.LOGGER.info("[Apollo] left to Lunar Client: {}", type);
            }
            return false;
        }
        this.handleIncoming(payload);
        return true;
    }

    public void handleIncoming(byte[] payload) {
        Any any;
        try {
            any = Any.parseFrom(payload);
        } catch (Exception e) {
            Artemis.LOGGER.warn("[Apollo] unreadable payload ({} bytes): {}", payload.length, e.toString());
            return;
        }

        String type = ApolloProtocol.messageType(any.getTypeUrl());

        if (this.logAllIncoming) {
            Artemis.LOGGER.info("[Apollo] received {} ({} bytes)", type, payload.length);
        }

        ApolloPacketHandler handler = this.handlers.get(type);
        if (handler == null) {
            return;
        }

        try {
            handler.handle(any);
        } catch (Exception e) {
            Artemis.LOGGER.error("[Apollo] error in the handler for {}", type, e);
        }
    }
}
