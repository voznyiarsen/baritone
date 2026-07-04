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

    /**
     * Current line color components: {r, g, b, alpha} where r/g/b are 0.0-1.0 and alpha is 0.0-1.0
     */
    float[] color = new float[]{1.0F, 1.0F, 1.0F, 0.4F};

    /**
     * Packs the current color from {@link #color} into an ARGB int for the Gizmos API.
     */
    static int packColor() {
        int a = (int) (color[3] * 255) & 0xFF;
        int r = (int) (color[0] * 255) & 0xFF;
        int g = (int) (color[1] * 255) & 0xFF;
        int b = (int) (color[2] * 255) & 0xFF;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    static void glColor(Color color, float alpha) {
        float[] colorComponents = color.getColorComponents(null);
        IRenderer.color[0] = colorComponents[0];
        IRenderer.color[1] = colorComponents[1];
        IRenderer.color[2] = colorComponents[2];
        IRenderer.color[3] = alpha;
    }

    static void startLines(Color color, float alpha, float lineWidth, boolean ignoreDepth) {
        glColor(color, alpha);
    }

    static void startLines(Color color, float lineWidth, boolean ignoreDepth) {
        startLines(color, 0.4f, lineWidth, ignoreDepth);
    }

    static void endLines(boolean ignoredDepth) {
        // No-op in the new rendering API
    }

    static void emitLine(PoseStack stack,
                         float x1, float y1, float z1,
                         float x2, float y2, float z2,
                         float nx, float ny, float nz) {
        System.out.println("[Baritone] emitLine(" + x1 + "," + y1 + "," + z1 + " -> " + x2 + "," + y2 + "," + z2 + ")");
        Gizmos.line(new Vec3(x1, y1, z1), new Vec3(x2, y2, z2), packColor());
    }

    static void emitLine(PoseStack stack,
                         double x1, double y1, double z1,
                         double x2, double y2, double z2) {
        System.out.println("[Baritone] emitLine(" + x1 + "," + y1 + "," + z1 + " -> " + x2 + "," + y2 + "," + z2 + ")");
        Gizmos.line(new Vec3(x1, y1, z1), new Vec3(x2, y2, z2), packColor());
    }

    static void emitLine(PoseStack stack,
                         double x1, double y1, double z1,
                         double x2, double y2, double z2,
                         double nx, double ny, double nz) {
        System.out.println("[Baritone] emitLine(" + x1 + "," + y1 + "," + z1 + " -> " + x2 + "," + y2 + "," + z2 + ")");
        Gizmos.line(new Vec3(x1, y1, z1), new Vec3(x2, y2, z2), packColor());
    }

    static void emitAABB(PoseStack stack, AABB aabb) {
        emitAABB(stack, aabb, 0.0);
    }

    static void emitAABB(PoseStack stack, AABB aabb, double expand) {
        AABB toDraw = aabb.inflate(expand, expand, expand);
        System.out.println("[Baritone] emitAABB(" + toDraw.minX + "," + toDraw.minY + "," + toDraw.minZ + " -> " + toDraw.maxX + "," + toDraw.maxY + "," + toDraw.maxZ + ")");
        int strokeColor = packColor();
        GizmoStyle style = GizmoStyle.stroke(strokeColor, 1.0f);
        Gizmos.cuboid(toDraw, style);
    }

    static void emitLine(PoseStack stack, Vec3 start, Vec3 end) {
        System.out.println("[Baritone] emitLine(Vec3 " + start + " -> " + end + ")");
        Gizmos.line(start, end, packColor());
    }

}
