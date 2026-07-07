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
import baritone.api.event.events.RenderEvent;
import baritone.api.pathing.goals.*;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.IPlayerContext;
import baritone.api.utils.interfaces.IGoalRenderPos;
import baritone.behavior.PathingBehavior;
import baritone.pathing.path.PathExecutor;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.awt.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Brady
 * @since 8/9/2018
 */
public final class PathRenderer implements IRenderer {



    private PathRenderer() {}

    public static double posX() {
        return renderManager.renderPosX();
    }

    public static double posY() {
        return renderManager.renderPosY();
    }

    public static double posZ() {
        return renderManager.renderPosZ();
    }

    public static void render(RenderEvent event, PathingBehavior behavior) {
        final IPlayerContext ctx = behavior.ctx;
        if (ctx.world() == null) {
            return;
        }
        if (ctx.minecraft().gui.screen() instanceof GuiClick) {
            ((GuiClick) ctx.minecraft().gui.screen()).onRender(event.getModelViewStack(), event.getProjectionMatrix());
        }

        final float partialTicks = event.getPartialTicks();
        double cameraX = posX();
        double cameraY = posY();
        double cameraZ = posZ();

        render(cameraX, cameraY, cameraZ, behavior, partialTicks);
    }

    /**
     * Render path visualization using the Gizmos API. This method is called from
     * BaritoneDebugRenderer.emitGizmos() when the Gizmos.withCollector() scope is active.
     */
    public static void render(double cameraX, double cameraY, double cameraZ, PathingBehavior behavior, float partialTicks) {
        final IPlayerContext ctx = behavior.ctx;
        if (ctx.world() == null) {
            return;
        }

        final Goal goal = behavior.getGoal();

        final DimensionType thisPlayerDimension = ctx.world().dimensionType();
        final DimensionType currentRenderViewDimension = BaritoneAPI.getProvider().getPrimaryBaritone().getPlayerContext().world().dimensionType();

        if (thisPlayerDimension != currentRenderViewDimension) {
            // this is a path for a bot in a different dimension, don't render it
            return;
        }

        if (goal != null && settings.renderGoal.value) {
            drawGoal(cameraX, cameraY, cameraZ, ctx, goal, partialTicks, settings.colorGoalBox.value);
        }

        if (!settings.renderPath.value) {
            return;
        }

        PathExecutor current = behavior.getCurrent(); // this should prevent most race conditions?
        PathExecutor next = behavior.getNext(); // like, now it's not possible for current!=null to be true, then suddenly false because of another thread
        if (current != null && settings.renderSelectionBoxes.value) {
            drawManySelectionBoxes(cameraX, cameraY, cameraZ, ctx.player(), current.toBreak(), settings.colorBlocksToBreak.value);
            drawManySelectionBoxes(cameraX, cameraY, cameraZ, ctx.player(), current.toPlace(), settings.colorBlocksToPlace.value);
            drawManySelectionBoxes(cameraX, cameraY, cameraZ, ctx.player(), current.toWalkInto(), settings.colorBlocksToWalkInto.value);
        }

        // Render the current path, if there is one
        if (current != null && current.getPath() != null) {
            int renderBegin = Math.max(current.getPosition() - 3, 0);
            drawPath(cameraX, cameraY, cameraZ, current.getPath().positions(), renderBegin, settings.colorCurrentPath.value, settings.fadePath.value, 10, 20);
        }

        if (next != null && next.getPath() != null) {
            drawPath(cameraX, cameraY, cameraZ, next.getPath().positions(), 0, settings.colorNextPath.value, settings.fadePath.value, 10, 20);
        }

        // If there is a path calculation currently running, render the path calculation process
        behavior.getInProgress().ifPresent(currentlyRunning -> {
            currentlyRunning.bestPathSoFar().ifPresent(p -> {
                drawPath(cameraX, cameraY, cameraZ, p.positions(), 0, settings.colorBestPathSoFar.value, settings.fadePath.value, 10, 20);
            });

            currentlyRunning.pathToMostRecentNodeConsidered().ifPresent(mr -> {
                drawPath(cameraX, cameraY, cameraZ, mr.positions(), 0, settings.colorMostRecentConsidered.value, settings.fadePath.value, 10, 20);
                drawManySelectionBoxes(cameraX, cameraY, cameraZ, ctx.player(), Collections.singletonList(mr.getDest()), settings.colorMostRecentConsidered.value);
            });
        });
    }

    public static void drawPath(PoseStack stack, List<BetterBlockPos> positions, int startIndex, Color color, boolean fadeOut, int fadeStart0, int fadeEnd0) {
        drawPath(stack, positions, startIndex, color, fadeOut, fadeStart0, fadeEnd0, 0.5D);
    }

    public static void drawPath(PoseStack stack, List<BetterBlockPos> positions, int startIndex, Color color, boolean fadeOut, int fadeStart0, int fadeEnd0, double offset) {
        IRenderer.startLines(color, settings.pathRenderLineWidthPixels.value, settings.renderPathIgnoreDepth.value);

        int fadeStart = fadeStart0 + startIndex;
        int fadeEnd = fadeEnd0 + startIndex;

        for (int i = startIndex, next; i < positions.size() - 1; i = next) {
            BetterBlockPos start = positions.get(i);
            BetterBlockPos end = positions.get(next = i + 1);

            int dirX = end.x - start.x;
            int dirY = end.y - start.y;
            int dirZ = end.z - start.z;

            while (next + 1 < positions.size() && (!fadeOut || next + 1 < fadeStart) &&
                    (dirX == positions.get(next + 1).x - end.x &&
                            dirY == positions.get(next + 1).y - end.y &&
                            dirZ == positions.get(next + 1).z - end.z)) {
                end = positions.get(++next);
            }

            if (fadeOut) {
                float alpha;

                if (i <= fadeStart) {
                    alpha = 0.4F;
                } else {
                    if (i > fadeEnd) {
                        break;
                    }
                    alpha = 0.4F * (1.0F - (float) (i - fadeStart) / (float) (fadeEnd - fadeStart));
                }
                IRenderer.glColor(color, alpha);
            }

            emitPathLine(stack, start.x, start.y, start.z, end.x, end.y, end.z, offset);
        }

        IRenderer.endLines(settings.renderPathIgnoreDepth.value);
    }

    /**
     * Draw path using world coordinates for the Gizmos API.
     */
    public static void drawPath(double cameraX, double cameraY, double cameraZ, List<BetterBlockPos> positions, int startIndex, Color color, boolean fadeOut, int fadeStart0, int fadeEnd0) {
        drawPath(cameraX, cameraY, cameraZ, positions, startIndex, color, fadeOut, fadeStart0, fadeEnd0, 0.5D);
    }

    public static void drawPath(double cameraX, double cameraY, double cameraZ, List<BetterBlockPos> positions, int startIndex, Color color, boolean fadeOut, int fadeStart0, int fadeEnd0, double offset) {
        IRenderer.startLines(color, settings.pathRenderLineWidthPixels.value, settings.renderPathIgnoreDepth.value);

        int fadeStart = fadeStart0 + startIndex;
        int fadeEnd = fadeEnd0 + startIndex;

        for (int i = startIndex, next; i < positions.size() - 1; i = next) {
            BetterBlockPos start = positions.get(i);
            BetterBlockPos end = positions.get(next = i + 1);

            int dirX = end.x - start.x;
            int dirY = end.y - start.y;
            int dirZ = end.z - start.z;

            while (next + 1 < positions.size() && (!fadeOut || next + 1 < fadeStart) &&
                    (dirX == positions.get(next + 1).x - end.x &&
                            dirY == positions.get(next + 1).y - end.y &&
                            dirZ == positions.get(next + 1).z - end.z)) {
                end = positions.get(++next);
            }

            if (fadeOut) {
                float alpha;

                if (i <= fadeStart) {
                    alpha = 0.4F;
                } else {
                    if (i > fadeEnd) {
                        break;
                    }
                    alpha = 0.4F * (1.0F - (float) (i - fadeStart) / (float) (fadeEnd - fadeStart));
                }
                IRenderer.glColor(color, alpha);
            }

            emitPathLine(cameraX, cameraY, cameraZ, start.x, start.y, start.z, end.x, end.y, end.z, offset);
        }

        IRenderer.endLines(settings.renderPathIgnoreDepth.value);
    }

    private static void emitPathLine(PoseStack stack, double x1, double y1, double z1, double x2, double y2, double z2, double offset) {
        final double extraOffset = offset + 0.03D;

        double vpX = posX();
        double vpY = posY();
        double vpZ = posZ();
        boolean renderPathAsFrickinThingy = !settings.renderPathAsLine.value;

        IRenderer.emitLine(stack,
                x1 + offset - vpX, y1 + offset - vpY, z1 + offset - vpZ,
                x2 + offset - vpX, y2 + offset - vpY, z2 + offset - vpZ
        );
        if (renderPathAsFrickinThingy) {
            IRenderer.emitLine(stack,
                    x2 + offset - vpX, y2 + offset - vpY, z2 + offset - vpZ,
                    x2 + offset - vpX, y2 + extraOffset - vpY, z2 + offset - vpZ
            );
            IRenderer.emitLine(stack,
                    x2 + offset - vpX, y2 + extraOffset - vpY, z2 + offset - vpZ,
                    x1 + offset - vpX, y1 + extraOffset - vpY, z1 + offset - vpZ
            );
            IRenderer.emitLine(stack,
                    x1 + offset - vpX, y1 + extraOffset - vpY, z1 + offset - vpZ,
                    x1 + offset - vpX, y1 + offset - vpY, z1 + offset - vpZ
            );
        }
    }

    /**
     * Emit a path line using world coordinates for the Gizmos API.
     */
    private static void emitPathLine(double cameraX, double cameraY, double cameraZ, double x1, double y1, double z1, double x2, double y2, double z2, double offset) {
        final double extraOffset = offset + 0.03D;

        // Gizmos uses world coordinates, no need to subtract camera position
        boolean renderPathAsFrickinThingy = !settings.renderPathAsLine.value;

        IRenderer.emitLine(null,
                x1 + offset, y1 + offset, z1 + offset,
                x2 + offset, y2 + offset, z2 + offset
        );
        if (renderPathAsFrickinThingy) {
            IRenderer.emitLine(null,
                    x2 + offset, y2 + offset, z2 + offset,
                    x2 + offset, y2 + extraOffset, z2 + offset
            );
            IRenderer.emitLine(null,
                    x2 + offset, y2 + extraOffset, z2 + offset,
                    x1 + offset, y1 + extraOffset, z1 + offset
            );
            IRenderer.emitLine(null,
                    x1 + offset, y1 + extraOffset, z1 + offset,
                    x1 + offset, y1 + offset, z1 + offset
            );
        }
    }

    public static void drawManySelectionBoxes(PoseStack stack, Entity player, Collection<BlockPos> positions, Color color) {
        IRenderer.startLines(color, settings.pathRenderLineWidthPixels.value, settings.renderSelectionBoxesIgnoreDepth.value);

        BlockStateInterface bsi = new BlockStateInterface(BaritoneAPI.getProvider().getPrimaryBaritone().getPlayerContext());

        positions.forEach(pos -> {
            BlockState state = bsi.get0(pos);
            VoxelShape shape = state.getShape(player.level(), pos);
            AABB toDraw = shape.isEmpty() ? Shapes.block().bounds() : shape.bounds();
            toDraw = toDraw.move(pos.getX(), pos.getY(), pos.getZ());
            IRenderer.emitAABB(stack, toDraw, 0.02D);
        });

        IRenderer.endLines(settings.renderSelectionBoxesIgnoreDepth.value);
    }

    /**
     * Draw selection boxes using world coordinates for the Gizmos API.
     */
    public static void drawManySelectionBoxes(double cameraX, double cameraY, double cameraZ, Entity player, Collection<BlockPos> positions, Color color) {
        IRenderer.startLines(color, settings.pathRenderLineWidthPixels.value, settings.renderSelectionBoxesIgnoreDepth.value);

        BlockStateInterface bsi = new BlockStateInterface(BaritoneAPI.getProvider().getPrimaryBaritone().getPlayerContext());

        positions.forEach(pos -> {
            BlockState state = bsi.get0(pos);
            VoxelShape shape = state.getShape(player.level(), pos);
            AABB toDraw = shape.isEmpty() ? Shapes.block().bounds() : shape.bounds();
            toDraw = toDraw.move(pos.getX(), pos.getY(), pos.getZ());
            // Inflate slightly to avoid z-fighting
            toDraw = toDraw.inflate(0.02);
            // Draw the 12 edges of the cuboid using lines
            double minX = toDraw.minX, minY = toDraw.minY, minZ = toDraw.minZ;
            double maxX = toDraw.maxX, maxY = toDraw.maxY, maxZ = toDraw.maxZ;
            // Bottom face edges
            IRenderer.emitLine(null, minX, minY, minZ, maxX, minY, minZ);
            IRenderer.emitLine(null, maxX, minY, minZ, maxX, minY, maxZ);
            IRenderer.emitLine(null, maxX, minY, maxZ, minX, minY, maxZ);
            IRenderer.emitLine(null, minX, minY, maxZ, minX, minY, minZ);
            // Top face edges
            IRenderer.emitLine(null, minX, maxY, minZ, maxX, maxY, minZ);
            IRenderer.emitLine(null, maxX, maxY, minZ, maxX, maxY, maxZ);
            IRenderer.emitLine(null, maxX, maxY, maxZ, minX, maxY, maxZ);
            IRenderer.emitLine(null, minX, maxY, maxZ, minX, maxY, minZ);
            // Vertical edges
            IRenderer.emitLine(null, minX, minY, minZ, minX, maxY, minZ);
            IRenderer.emitLine(null, maxX, minY, minZ, maxX, maxY, minZ);
            IRenderer.emitLine(null, maxX, minY, maxZ, maxX, maxY, maxZ);
            IRenderer.emitLine(null, minX, minY, maxZ, minX, maxY, maxZ);
        });

        IRenderer.endLines(settings.renderSelectionBoxesIgnoreDepth.value);
    }

    public static void drawGoal(PoseStack stack, IPlayerContext ctx, Goal goal, float partialTicks, Color color) {
        drawGoal(stack, ctx, goal, partialTicks, color, true);
    }

    public static void drawGoal(double cameraX, double cameraY, double cameraZ, IPlayerContext ctx, Goal goal, float partialTicks, Color color) {
        drawGoal(cameraX, cameraY, cameraZ, ctx, goal, partialTicks, color, true);
    }

    private static void drawGoal(PoseStack stack, IPlayerContext ctx, Goal goal, float partialTicks, Color color, boolean setupRender) {
        double minX, maxX;
        double minZ, maxZ;
        double minY, maxY;
        double y, y1, y2;
        if (!settings.renderGoalAnimated.value) {
            y = 0.999F;
        } else {
            y = Mth.cos((float) (((float) ((System.nanoTime() / 100000L) % 20000L)) / 20000F * Math.PI * 2));
        }
        if (goal instanceof IGoalRenderPos) {
            BlockPos goalPos = ((IGoalRenderPos) goal).getGoalPos();
            minX = goalPos.getX() - 0.02;
            maxX = goalPos.getX() + 1 + 0.02;
            minZ = goalPos.getZ() - 0.02;
            maxZ = goalPos.getZ() + 1 + 0.02;
            if (goal instanceof GoalGetToBlock || goal instanceof GoalTwoBlocks) {
                y /= 2;
            }
            y1 = 1 + y + goalPos.getY();
            y2 = 1 - y + goalPos.getY();
            minY = goalPos.getY();
            maxY = minY + 2;
            if (goal instanceof GoalGetToBlock || goal instanceof GoalTwoBlocks) {
                y1 -= 0.5;
                y2 -= 0.5;
                maxY--;
            }
            drawDankLitGoalBox(stack, color, minX, maxX, minZ, maxZ, minY, maxY, y1, y2, setupRender);
        } else if (goal instanceof GoalXZ) {
            GoalXZ goalPos = (GoalXZ) goal;
            minY = ctx.world().getMinY();
            maxY = ctx.world().getMaxY();

            minX = goalPos.getX() - 0.02;
            maxX = goalPos.getX() + 1 + 0.02;
            minZ = goalPos.getZ() - 0.02;
            maxZ = goalPos.getZ() + 1 + 0.02;

            y1 = 0;
            y2 = 0;
            drawDankLitGoalBox(stack, color, minX, maxX, minZ, maxZ, minY, maxY, y1, y2, setupRender);
        } else if (goal instanceof GoalComposite) {
            boolean batch = Arrays.stream(((GoalComposite) goal).goals()).allMatch(IGoalRenderPos.class::isInstance);

            if (batch) {
                IRenderer.startLines(color, settings.goalRenderLineWidthPixels.value, settings.renderGoalIgnoreDepth.value);
            }
            for (Goal g : ((GoalComposite) goal).goals()) {
                drawGoal(stack, ctx, g, partialTicks, color, !batch);
            }
            if (batch) {
                IRenderer.endLines(settings.renderGoalIgnoreDepth.value);
            }
        } else if (goal instanceof GoalInverted) {
            drawGoal(stack, ctx, ((GoalInverted) goal).origin, partialTicks, settings.colorInvertedGoalBox.value);
        } else if (goal instanceof GoalYLevel) {
            GoalYLevel goalpos = (GoalYLevel) goal;
            minX = ctx.player().position().x - settings.yLevelBoxSize.value;
            minZ = ctx.player().position().z - settings.yLevelBoxSize.value;
            maxX = ctx.player().position().x + settings.yLevelBoxSize.value;
            maxZ = ctx.player().position().z + settings.yLevelBoxSize.value;
            minY = ((GoalYLevel) goal).level;
            maxY = minY + 2;
            y1 = 1 + y + goalpos.level;
            y2 = 1 - y + goalpos.level;
            drawDankLitGoalBox(stack, color, minX, maxX, minZ, maxZ, minY, maxY, y1, y2, setupRender);
        }
    }

    /**
     * Draw goal using world coordinates for the Gizmos API.
     */
    private static void drawGoal(double cameraX, double cameraY, double cameraZ, IPlayerContext ctx, Goal goal, float partialTicks, Color color, boolean setupRender) {
        double minX, maxX;
        double minZ, maxZ;
        double minY, maxY;
        double y, y1, y2;
        if (!settings.renderGoalAnimated.value) {
            y = 0.999F;
        } else {
            y = Mth.cos((float) (((float) ((System.nanoTime() / 100000L) % 20000L)) / 20000F * Math.PI * 2));
        }
        if (goal instanceof IGoalRenderPos) {
            BlockPos goalPos = ((IGoalRenderPos) goal).getGoalPos();
            minX = goalPos.getX() - 0.02;
            maxX = goalPos.getX() + 1 + 0.02;
            minZ = goalPos.getZ() - 0.02;
            maxZ = goalPos.getZ() + 1 + 0.02;
            if (goal instanceof GoalGetToBlock || goal instanceof GoalTwoBlocks) {
                y /= 2;
            }
            y1 = 1 + y + goalPos.getY();
            y2 = 1 - y + goalPos.getY();
            minY = goalPos.getY();
            maxY = minY + 2;
            if (goal instanceof GoalGetToBlock || goal instanceof GoalTwoBlocks) {
                y1 -= 0.5;
                y2 -= 0.5;
                maxY--;
            }
            drawDankLitGoalBox(null, color, minX, maxX, minZ, maxZ, minY, maxY, y1, y2, setupRender);
        } else if (goal instanceof GoalXZ) {
            GoalXZ goalPos = (GoalXZ) goal;
            minY = ctx.world().getMinY();
            maxY = ctx.world().getMaxY();

            minX = goalPos.getX() - 0.02;
            maxX = goalPos.getX() + 1 + 0.02;
            minZ = goalPos.getZ() - 0.02;
            maxZ = goalPos.getZ() + 1 + 0.02;

            y1 = 0;
            y2 = 0;
            drawDankLitGoalBox(null, color, minX, maxX, minZ, maxZ, minY, maxY, y1, y2, setupRender);
        } else if (goal instanceof GoalComposite) {
            boolean batch = Arrays.stream(((GoalComposite) goal).goals()).allMatch(IGoalRenderPos.class::isInstance);

            if (batch) {
                IRenderer.startLines(color, settings.goalRenderLineWidthPixels.value, settings.renderGoalIgnoreDepth.value);
            }
            for (Goal g : ((GoalComposite) goal).goals()) {
                drawGoal(cameraX, cameraY, cameraZ, ctx, g, partialTicks, color, !batch);
            }
            if (batch) {
                IRenderer.endLines(settings.renderGoalIgnoreDepth.value);
            }
        } else if (goal instanceof GoalInverted) {
            drawGoal(cameraX, cameraY, cameraZ, ctx, ((GoalInverted) goal).origin, partialTicks, settings.colorInvertedGoalBox.value);
        } else if (goal instanceof GoalYLevel) {
            GoalYLevel goalpos = (GoalYLevel) goal;
            minX = ctx.player().position().x - settings.yLevelBoxSize.value;
            minZ = ctx.player().position().z - settings.yLevelBoxSize.value;
            maxX = ctx.player().position().x + settings.yLevelBoxSize.value;
            maxZ = ctx.player().position().z + settings.yLevelBoxSize.value;
            minY = ((GoalYLevel) goal).level;
            maxY = minY + 2;
            y1 = 1 + y + goalpos.level;
            y2 = 1 - y + goalpos.level;
            drawDankLitGoalBox(null, color, minX, maxX, minZ, maxZ, minY, maxY, y1, y2, setupRender);
        }
    }

    private static void drawDankLitGoalBox(PoseStack stack, Color colorIn, double minX, double maxX, double minZ, double maxZ, double minY, double maxY, double y1, double y2, boolean setupRender) {
        if (setupRender) {
            IRenderer.startLines(colorIn, settings.goalRenderLineWidthPixels.value, settings.renderGoalIgnoreDepth.value);
        }

        renderHorizontalQuad(stack, minX, maxX, minZ, maxZ, y1);
        renderHorizontalQuad(stack, minX, maxX, minZ, maxZ, y2);

        for (double y = minY; y < maxY; y += 16) {
            double max = Math.min(maxY, y + 16);
            IRenderer.emitLine(stack, minX, y, minZ, minX, max, minZ, 0.0, 1.0, 0.0);
            IRenderer.emitLine(stack, maxX, y, minZ, maxX, max, minZ, 0.0, 1.0, 0.0);
            IRenderer.emitLine(stack, maxX, y, maxZ, maxX, max, maxZ, 0.0, 1.0, 0.0);
            IRenderer.emitLine(stack, minX, y, maxZ, minX, max, maxZ, 0.0, 1.0, 0.0);
        }

        if (setupRender) {
            IRenderer.endLines(settings.renderGoalIgnoreDepth.value);
        }
    }

    private static void renderHorizontalQuad(PoseStack stack, double minX, double maxX, double minZ, double maxZ, double y) {
        if (y != 0) {
            IRenderer.emitLine(stack, minX, y, minZ, maxX, y, minZ, 1.0, 0.0, 0.0);
            IRenderer.emitLine(stack, maxX, y, minZ, maxX, y, maxZ, 0.0, 0.0, 1.0);
            IRenderer.emitLine(stack, maxX, y, maxZ, minX, y, maxZ, -1.0, 0.0, 0.0);
            IRenderer.emitLine(stack, minX, y, maxZ, minX, y, minZ, 0.0, 0.0, -1.0);
        }
    }
}
