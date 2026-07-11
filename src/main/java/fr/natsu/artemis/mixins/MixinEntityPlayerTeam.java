package fr.natsu.artemis.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import fr.natsu.artemis.render.GlowRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;

/**
 * Pendant la passe d'outline, fournit une équipe « placeholder » aux joueurs qui n'en ont pas, afin
 * que {@code setScoreTeamColor} (et donc l'injection de couleur) s'exécute. Sans équipe, le rendu
 * d'outline vanilla saute la coloration. Gate strictement sur la passe d'outline pour ne rien
 * changer au jeu en temps normal.
 */
@Mixin(EntityPlayer.class)
public abstract class MixinEntityPlayerTeam {

    @Inject(method = "getTeam", at = @At("HEAD"), cancellable = true)
    private void artemis$placeholderTeam(CallbackInfoReturnable<ScorePlayerTeam> cir) {
        if (!GlowRenderer.isRenderingOutline) {
            return;
        }

        EntityLivingBase self = (EntityLivingBase) (Object) this;
        if (self.worldObj == null || self.worldObj.getScoreboard() == null || self.getName() == null) {
            return;
        }

        Scoreboard scoreboard = self.worldObj.getScoreboard();
        if (scoreboard.getPlayersTeam(self.getName()) != null) {
            return;
        }

        cir.setReturnValue(new ScorePlayerTeam(scoreboard, "ARTEMIS_GLOW_PLACEHOLDER"));
    }
}
