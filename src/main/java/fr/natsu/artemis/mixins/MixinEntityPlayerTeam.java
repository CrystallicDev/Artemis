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
 * During the outline pass, hands a placeholder team to players that don't have one, so that
 * {@code setScoreTeamColor} (and therefore our color injection) runs. Without a team the vanilla
 * outline render skips coloring. Strictly gated on the outline pass so nothing changes during normal
 * play.
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
