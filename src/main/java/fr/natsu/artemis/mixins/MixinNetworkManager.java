package fr.natsu.artemis.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fr.natsu.artemis.module.chat.ArtemisChat;
import fr.natsu.artemis.module.lightning.ArtemisLightning;
import fr.natsu.artemis.net.ApolloNetwork;
import fr.natsu.artemis.net.ApolloProtocol;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.S3FPacketCustomPayload;

/**
 * Intercepte les packets entrants au niveau Netty pour capter le trafic Apollo sur le canal
 * {@code lunar:apollo} avant que le client vanilla ne le traite.
 */
@Mixin(NetworkManager.class)
public class MixinNetworkManager {

    @Inject(method = "channelRead0", at = @At("HEAD"), cancellable = true)
    private void artemis$onReceive(ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
        if (!(packet instanceof S3FPacketCustomPayload)) {
            return;
        }

        S3FPacketCustomPayload payload = (S3FPacketCustomPayload) packet;
        String channel = payload.getChannelName();
        boolean apollo = ApolloProtocol.CHANNEL.equals(channel);
        boolean chat = ArtemisChat.CHANNEL.equals(channel);
        boolean lightning = ArtemisLightning.CHANNEL.equals(channel);
        if (!apollo && !chat && !lightning) {
            return;
        }

        // Copie absolue : ne consomme pas le buffer (le packet est de toute façon annulé ensuite).
        PacketBuffer data = payload.getBufferData();
        byte[] bytes = new byte[data.readableBytes()];
        data.getBytes(data.readerIndex(), bytes);

        if (apollo) {
            ApolloNetwork.getInstance().handleIncoming(bytes);
        } else if (chat) {
            ArtemisChat.handleIncoming(bytes);
        } else {
            ArtemisLightning.handleIncoming(bytes);
        }

        // Canal géré par Artemis : on évite que le packet remonte au NetHandlerPlayClient vanilla.
        ci.cancel();
    }
}
