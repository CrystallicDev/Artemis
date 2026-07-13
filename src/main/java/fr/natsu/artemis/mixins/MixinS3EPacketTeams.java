package fr.natsu.artemis.mixins;

import java.io.IOException;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.S3EPacketTeams;

/**
 * Removes the 16-character cap on the Team packet fields, client-side.
 *
 * <p>In vanilla, {@link S3EPacketTeams#readPacketData} reads {@code prefix} and {@code suffix} with
 * {@code readStringFromBuffer(16)}, which <b>throws a {@code DecoderException}</b> (and disconnects the
 * client) if the string is longer than 16 characters. A full hex prefix ({@code §x§r§r§g§g§b§b} = 14
 * chars) would leave only 2 usable characters.</p>
 *
 * <p>Like a Lunar client, Artemis lifts that cap: the server (which knows which players are Artemis
 * clients from the channel registration) can send a <b>normal</b> Team packet with a full hex prefix
 * to those clients only, without changing anything else. The resulting {@code §x} is rendered natively
 * by {@code MixinFontRenderer} + HexTextRenderer everywhere the name shows up (tab, nametag,
 * sidebar...). Non-Artemis players still get the usual 16-color prefix.</p>
 *
 * <p>The redirect covers every read in {@code readPacketData} (name/displayName/prefix/suffix/
 * visibility/players); raising their cap has no side effect.</p>
 */
@Mixin(S3EPacketTeams.class)
public class MixinS3EPacketTeams {

    @Redirect(
        method = "readPacketData",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/network/PacketBuffer;readStringFromBuffer(I)Ljava/lang/String;"),
        require = 0)
    private String artemis$readUncappedString(PacketBuffer buf, int maxLength) throws IOException {
        // Keep the original cap for the already-large fields, only widen the small ones
        // (prefix/suffix = 16) so they can hold the hex without being rejected.
        return buf.readStringFromBuffer(Math.max(maxLength, Short.MAX_VALUE));
    }
}
