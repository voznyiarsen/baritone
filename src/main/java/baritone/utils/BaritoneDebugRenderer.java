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

package baritone.utils;

import baritone.api.BaritoneAPI;
import baritone.behavior.PathingBehavior;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.util.debug.DebugValueAccess;

public class BaritoneDebugRenderer implements DebugRenderer.SimpleDebugRenderer {

    public static final BaritoneDebugRenderer INSTANCE = new BaritoneDebugRenderer();

    private BaritoneDebugRenderer() {}

    @Override
    public void emitGizmos(double cameraX, double cameraY, double cameraZ, DebugValueAccess debugValueAccess, Frustum frustum, float partialTicks) {
        for (baritone.api.IBaritone ibaritone : BaritoneAPI.getProvider().getAllBaritones()) {
            PathingBehavior behavior = (PathingBehavior) ibaritone.getPathingBehavior();
            if (behavior != null && behavior.ctx != null && behavior.ctx.world() != null) {
                PathRenderer.render(cameraX, cameraY, cameraZ, behavior, partialTicks);
            }
        }
    }
}
