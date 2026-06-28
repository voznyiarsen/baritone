/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.launch.mixins;

import baritone.utils.PlayerMovementInput;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * In MC 26.1.2, ClientInput.tick() is NOT called automatically.
 * This mixin ensures that PlayerMovementInput.tick() is called BEFORE applyInput()
 * so that the keyPresses and moveVector are set correctly.
 * It also updates lastSentInput to prevent server anti-cheat from teleporting the player.
 */
@Mixin(LocalPlayer.class)
public abstract class MixinLocalPlayer {

    @Shadow
    private net.minecraft.world.entity.player.Input lastSentInput;

    @Inject(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;tick()V", shift = At.Shift.AFTER)
    )
    private void onTickAfterAbstract(CallbackInfo ci) {
        LocalPlayer self = (LocalPlayer) (Object) this;
        if (self.input instanceof PlayerMovementInput) {
            // Update lastSentInput to match the current input so the server doesn't reject movement
            // This must be done AFTER AbstractClientPlayer.tick() which calls applyInput()
            this.lastSentInput = self.input.keyPresses;
        }
    }

    @Inject(
            method = "tick",
            at = @At("HEAD")
    )
    private void onTickHead(CallbackInfo ci) {
        LocalPlayer self = (LocalPlayer) (Object) this;
        if (self.input instanceof PlayerMovementInput) {
            ((PlayerMovementInput) self.input).tick();
        }
    }
}
