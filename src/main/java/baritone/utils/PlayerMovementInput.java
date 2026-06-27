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

import baritone.api.utils.input.Input;

public class PlayerMovementInput extends net.minecraft.client.player.ClientInput {

    private final InputOverrideHandler handler;

    PlayerMovementInput(InputOverrideHandler handler) {
        this.handler = handler;
    }

    @Override
    public void tick() {
        this.keyPresses = new net.minecraft.world.entity.player.Input(
            handler.isInputForcedDown(Input.JUMP),
            handler.isInputForcedDown(Input.MOVE_FORWARD),
            handler.isInputForcedDown(Input.MOVE_BACK),
            handler.isInputForcedDown(Input.MOVE_LEFT),
            handler.isInputForcedDown(Input.MOVE_RIGHT),
            handler.isInputForcedDown(Input.SNEAK),
            false
        );
        float forwardImpulse = 0.0F;
        float leftImpulse = 0.0F;

        if (this.keyPresses.forward()) {
            forwardImpulse++;
        }
        if (this.keyPresses.backward()) {
            forwardImpulse--;
        }
        if (this.keyPresses.left()) {
            leftImpulse++;
        }
        if (this.keyPresses.right()) {
            leftImpulse--;
        }
        if (this.keyPresses.shift()) {
            leftImpulse *= 0.3D;
            forwardImpulse *= 0.3D;
        }
        this.moveVector = new net.minecraft.world.phys.Vec2(leftImpulse, forwardImpulse);
    }
}
