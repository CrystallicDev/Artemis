package fr.natsu.artemis.module.events;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import io.netty.buffer.Unpooled;

public final class ArtemisEvents {

    public static final String CHANNEL = "artemis:events";

    private static final int EVENT_CHAT_OPEN = 0;
    private static final int EVENT_CHAT_CLOSE = 1;

    private boolean chatOpen;

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        boolean nowChat = event.gui instanceof GuiChat;
        if (nowChat && !this.chatOpen) {
            this.chatOpen = true;
            send(EVENT_CHAT_OPEN);
        } else if (!nowChat && this.chatOpen) {
            this.chatOpen = false;
            send(EVENT_CHAT_CLOSE);
        }
    }

    private static void send(int opcode) {
        NetHandlerPlayClient handler = Minecraft.getMinecraft().getNetHandler();
        if (handler == null) {
            return;
        }
        PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
        buffer.writeByte(opcode);
        buffer.writeLong(System.currentTimeMillis());
        handler.addToSendQueue(new C17PacketCustomPayload(CHANNEL, buffer));
    }
}
