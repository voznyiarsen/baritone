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
import baritone.api.Settings;
import baritone.utils.accessor.IEntityRenderManager;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.awt.*;

public interface IRenderer {

    IEntityRenderManager renderManager = (IEntityRenderManager) Minecraft.getInstance().getEntityRenderDispatcher();
    TextureManager textureManager = Minecraft.getInstance().getTextureManager();
    Settings settings = BaritoneAPI.getSettings();

    float[] color = new float[]{1.0F, 1.0F, 1.0F, 255.0F};

    static void glColor(Color color, float alpha) {
        float[] colorComponents = color.getColorComponents(null);
        IRenderer.color[0] = colorComponents[0];
        IRenderer.color[1] = colorComponents[1];
        IRenderer.color[2] = colorComponents[2];
        IRenderer.color[3] = alpha;
    }

    static GizmoStyle lineStyle(Color color, float alpha, float lineWidth, boolean ignoreDepth) {
        int argb = color.getAlpha() << 24 | color.getBlue() << 16 | color.getGreen() << 8 | color.getRed();
        // Pack alpha into the color
        int strokeColor = ((int)(alpha * 255) << 24) | (color.getRGB() & 0x00FFFFFF);
        return GizmoStyle.stroke(strokeColor, lineWidth);
    }

    static void startLines(Color color, float alpha, float lineWidth, boolean ignoreDepth) {
        // In the new rendering API, lines are drawn per-segment using Gizmos
        // This method is kept for API compatibility but doesn't do anything
    }

    static void startLines(Color color, float lineWidth, boolean ignoreDepth) {
        startLines(color, .4f, lineWidth, ignoreDepth);
    }

    static void endLines(boolean ignoredDepth) {
        // In the new rendering API, lines are drawn per-segment using Gizmos
        // This method is kept for API compatibility but doesn't do anything
    }

    static void emitLine(PoseStack stack,
                         float x1, float y1, float z1,
                         float x2, float y2, float z2,
                         float nx, float ny, float nz) {
        // Legacy compatibility - actual rendering is done via emitAABB
    }

    static void emitLine(PoseStack stack,
                         double x1, double y1, double z1,
                         double x2, double y2, double z2) {
        emitLine(stack, (float) x1, (float) y1, (float) z1, (float) x2, (float) y2, (float) z2, 0f, 1f, 0f);
    }

    static void emitLine(PoseStack stack,
                         double x1, double y1, double z1,
                         double x2, double y2, double z2,
                         double nx, double ny, double nz) {
        emitLine(stack, (float) x1, (float) y1, (float) z1, (float) x2, (float) y2, (float) z2, (float) nx, (float) ny, (float) nz);
    }

    static void emitAABB(PoseStack stack, AABB aabb) {
        emitAABB(stack, aabb, 0.0);
    }

    static void emitAABB(PoseStack stack, AABB aabb, double expand) {
        AABB toDraw = aabb.inflate(expand, expand, expand);
        GizmoStyle style = GizmoStyle.stroke(0xFFFFFFFF, 1.0f);
        Gizmos.cuboid(toDraw, style);
    }

    static void emitLine(PoseStack stack, Vec3 start, Vec3 end) {
        Gizmos.line(start, end, 0xFFFFFFFF);
    }

}
