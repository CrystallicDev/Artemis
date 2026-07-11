package fr.natsu.artemis.mixins;

import java.io.IOException;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.S3EPacketTeams;

/**
 * Retire le plafond de 16 caractères sur les champs du paquet Team côté client.
 *
 * <p>En vanilla, {@link S3EPacketTeams#readPacketData} lit {@code prefix} et {@code suffix} via
 * {@code readStringFromBuffer(16)}, qui <b>lève un {@code DecoderException}</b> (et déconnecte le
 * client) si la chaîne dépasse 16 caractères. Un préfixe hex complet ({@code §x§r§r§g§g§b§b} = 14
 * chars) ne laisse alors que 2 caractères utiles.</p>
 *
 * <p>Comme un client Lunar, Artemis relève ce plafond : le serveur (qui sait quels joueurs sont des
 * clients Artemis via l'enregistrement de canal) peut envoyer un paquet Team <b>normal</b> avec un
 * préfixe hex complet uniquement à ces clients, sans changer l'architecture. Le {@code §x} qui en
 * résulte est rendu nativement par {@code MixinFontRenderer} + HexTextRenderer, partout où le nom
 * est affiché (tab, nametag, sidebar...). Les non-Artemis reçoivent le préfixe 16-couleurs habituel.</p>
 *
 * <p>Le redirect couvre toutes les lectures de {@code readPacketData} (name/displayName/prefix/
 * suffix/visibility/joueurs) : relever leur plafond est sans effet de bord.</p>
 */
@Mixin(S3EPacketTeams.class)
public class MixinS3EPacketTeams {

    @Redirect(
        method = "readPacketData",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/network/PacketBuffer;readStringFromBuffer(I)Ljava/lang/String;"),
        require = 0)
    private String artemis$readUncappedString(PacketBuffer buf, int maxLength) throws IOException {
        // On conserve le plafond d'origine pour les champs déjà larges, on l'élargit seulement pour
        // les petits (prefix/suffix = 16) afin d'accueillir le hex sans risque de rejet.
        return buf.readStringFromBuffer(Math.max(maxLength, Short.MAX_VALUE));
    }
}
