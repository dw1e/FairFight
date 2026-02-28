package me.dw1e.ff.data.processor;

import me.dw1e.ff.data.PlayerData;
import me.dw1e.ff.misc.math.OptiFineMath;
import me.dw1e.ff.misc.math.VanillaMath;
import me.dw1e.ff.misc.util.BlockUtil;
import me.dw1e.ff.packet.wrapper.WrappedPacket;
import me.dw1e.ff.packet.wrapper.client.CPacketFlying;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

// 模拟来自AntiHaxerman, 半成品, 模拟不全, 目前只使用在Velocity检测
public final class EmulationProcessor {

    private final PlayerData data;

    private double minDistance;
    private boolean sprint, jump, using, hitSlowdown, fastMath;

    private float lastSlipperiness = 0.6F; // 只有这个模拟需要上次的方块摩擦, 不写PlayerData里了

    public EmulationProcessor(PlayerData data) {
        this.data = data;
    }

    public void process(WrappedPacket packet) {
        if (packet instanceof CPacketFlying) {
            Vector realMotion = new Vector(data.getDeltaX(), 0.0, data.getDeltaZ());

            minDistance = Double.MAX_VALUE;

            Location lastLastLoc = data.getLastLastLocation();

            Block blockBelow = BlockUtil.getBlockAsync(new Location(data.getPlayer().getWorld(),
                    lastLastLoc.getX(), lastLastLoc.getY() - 1.0, lastLastLoc.getZ()
            ));

            if (blockBelow != null) lastSlipperiness = BlockUtil.getSlipperiness(blockBelow.getType());

            iteration:
            {
                for (int f = -1; f < 2; f++) {
                    for (int s = -1; s < 2; s++) {
                        for (int sp = 0; sp < 2; sp++) {
                            for (int jp = 0; jp < 2; jp++) {
                                for (int ui = 0; ui < 2; ui++) {
                                    for (int hs = 0; hs < 2; hs++) {
                                        for (int fm = 0; fm < 2; fm++) {
                                            boolean sprint = sp == 0 && data.getTickSinceSprinting() < 3;
                                            boolean jump = jp == 0 && data.getTickSinceJumped() < 3;
                                            boolean using = ui == 0 && data.getTickSinceUsingItem() < 3;
                                            boolean hitSlowdown = hs == 0 && data.getTickSinceAttack() < 3;
                                            boolean fastMath = fm == 1;
                                            boolean ground = data.isLastClientGround();
                                            boolean sneaking = data.isSneaking();

                                            if (f <= 0.0F && sprint && ground) continue;

                                            float forward = f;
                                            float strafe = s;

                                            if (using) {
                                                forward *= 0.2F;
                                                strafe *= 0.2F;
                                            }

                                            if (sneaking) {
                                                forward *= 0.3F;
                                                strafe *= 0.3F;
                                            }

                                            forward *= 0.98F;
                                            strafe *= 0.98F;

                                            Vector motion = new Vector(data.getLastDeltaX(), 0.0, data.getLastDeltaZ());

                                            if (data.isLastLastClientGround()) {
                                                motion.multiply(new Vector(lastSlipperiness * 0.91F, 0.0, lastSlipperiness * 0.91F));
                                            } else {
                                                motion.multiply(new Vector(0.91F, 0.0, 0.91F));
                                            }

                                            if (data.getVelocityXZ() != 0.0)
                                                motion = new Vector(data.getVelocityX(), 0.0, data.getVelocityZ());

                                            if (hitSlowdown) motion.multiply(new Vector(0.6F, 0.0, 0.6F));

                                            if (Math.abs(motion.getX()) < 0.005) motion.setX(0.0);
                                            if (Math.abs(motion.getZ()) < 0.005) motion.setZ(0.0);

                                            if (jump && sprint) {
                                                float radians = data.getLocation().getYaw() * 0.017453292F;

                                                motion.add(new Vector(-sin(fastMath, radians) * 0.2F, 0.0, cos(fastMath, radians) * 0.2F));
                                            }

                                            float moveFlyingFriction;

                                            if (ground) {
                                                float moveSpeedMultiplier = 0.16277136F / (float) Math.pow(data.getFriction(), 3.0F);

                                                moveFlyingFriction = data.getAttributeSpeed() * moveSpeedMultiplier;
                                            } else {
                                                moveFlyingFriction = sprint ? 0.026F : 0.02F;
                                            }

                                            float[] moveFlying = this.moveFlying(forward, strafe, moveFlyingFriction, fastMath);

                                            motion.add(new Vector(moveFlying[0], 0.0, moveFlying[1]));

                                            double distance = realMotion.distanceSquared(motion);

                                            if (distance < minDistance) {
                                                minDistance = distance;

                                                this.sprint = sprint;
                                                this.jump = jump;
                                                this.using = using;
                                                this.hitSlowdown = hitSlowdown;
                                                this.fastMath = fastMath;

                                                if (distance < 1E-14) break iteration;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } // ====================

            /*double limit = data.isOffsetMotion() ? 0.03 : 1E-14;

            if (minDistance > limit) data.getPlayer().sendMessage(String.format("min=%.14f", minDistance));*/
        }
    }

    private float[] moveFlying(float moveForward, float moveStrafe, float friction, boolean fastMath) {
        float diagonal = moveStrafe * moveStrafe + moveForward * moveForward;

        float moveFlyingFactorX = 0.0F;
        float moveFlyingFactorZ = 0.0F;

        if (diagonal >= 1.0E-4F) {
            diagonal = VanillaMath.sqrt(diagonal);

            if (diagonal < 1.0F) diagonal = 1.0F;

            diagonal = friction / diagonal;

            float strafe = moveStrafe * diagonal;
            float forward = moveForward * diagonal;

            float rotationYaw = data.getLocation().getYaw();

            float f1 = sin(fastMath, rotationYaw * (float) Math.PI / 180.0F);
            float f2 = cos(fastMath, rotationYaw * (float) Math.PI / 180.0F);

            float factorX = strafe * f2 - forward * f1;
            float factorZ = forward * f2 + strafe * f1;

            moveFlyingFactorX = factorX;
            moveFlyingFactorZ = factorZ;
        }

        return new float[]{moveFlyingFactorX, moveFlyingFactorZ};
    }

    private float sin(boolean fastMath, float yaw) {
        return fastMath ? OptiFineMath.sin(yaw) : VanillaMath.sin(yaw);
    }

    private float cos(boolean fastMath, float yaw) {
        return fastMath ? OptiFineMath.cos(yaw) : VanillaMath.cos(yaw);
    }

    public boolean isSprint() {
        return sprint;
    }

    public boolean isJump() {
        return jump;
    }

    public boolean isUsing() {
        return using;
    }

    public boolean isHitSlowdown() {
        return hitSlowdown;
    }

    public boolean isFastMath() {
        return fastMath;
    }

    public double getMinDistance() {
        return minDistance;
    }
}
