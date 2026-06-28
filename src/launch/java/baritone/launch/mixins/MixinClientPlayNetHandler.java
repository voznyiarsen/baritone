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

import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;

/**
 * @author Brady
 * @since 8/3/2018
 *
 * Disabled for MC 26.1.2 - @Shadow cannot find inherited fields from parent classes.
 * The minecraft field is in ClientCommonPacketListenerImpl, not ClientPacketListener.
 * Using accessor interface pattern instead.
 */
@Mixin(ClientPacketListener.class)
public class MixinClientPlayNetHandler {
    // All hooks disabled for MC 26.1.2 compatibility
    // TODO: reimplement using @Accessor interface for minecraft field access
}
