package com.hbm.entity.grenade;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import com.hbm.explosion.ExplosionLarge;

import java.util.Comparator;
import java.util.List;

public class EntityGrenadeDart extends EntityGrenadeBase {

    private EntityLivingBase target;
    private EntityLivingBase thrower;
    private int delayTimer = 10; // 10 ticks
    private int pauseTimer = 20; // 1 seconds with 20 ticks per second
    private double posXBeforePause;
    private double posYBeforePause;
    private double posZBeforePause;
    private double motionXBeforePause;
    private double motionYBeforePause;
    private double motionZBeforePause;

    public EntityGrenadeDart(World world) {
        super(world);
    }

    public EntityGrenadeDart(World world, EntityLivingBase thrower) {
        super(world, thrower);
        this.thrower = thrower;
    }

    public EntityGrenadeDart(World world, double x, double y, double z) {
        super(world, x, y, z);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();

        if (delayTimer > 0) {
            delayTimer--;
            return; // Delaying the tracking and exploding logic
        }

        if (pauseTimer > 0) {
            pauseTimer--;
            posXBeforePause = posX;
            posYBeforePause = posY;
            posZBeforePause = posZ;
            motionXBeforePause = motionX;
            motionYBeforePause = motionY;
            motionZBeforePause = motionZ;
            motionX = 0;
            motionY = 0.1; // Counteract gravity during the pause
            motionZ = 0;
            if (pauseTimer == 0) {
                setMotionTowardsTarget();
            }
            return; // Pausing mid-air after finding the target
        }

        if (!worldObj.isRemote) {
            // If there is no target or the target is dead, find a new target
            if (target == null || target.isDead) {
                findNewTarget();
            }

            if (target != null) {
                double deltaX = target.posX - posX;
                double deltaY = target.posY - posY;
                double deltaZ = target.posZ - posZ;
                double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);

                if (distance <= 30) { // Maximum range is 30
                    double speed = 1.0; // Adjust the speed as needed
                    motionX = deltaX / distance * speed;
                    motionY = deltaY / distance * speed;
                    motionZ = deltaZ / distance * speed;
                } else {
                    explode();
                }
            } else {
                // If no valid target found, explode
                explode();
            }
        }
    }

    private void findNewTarget() {
        List<EntityLivingBase> entities = worldObj.getEntitiesWithinAABB(EntityLivingBase.class,
                AxisAlignedBB.getBoundingBox(posX - 30, posY - 30, posZ - 30, posX + 30, posY + 30, posZ + 30)); // Maximum range is 30

        // Sort entities by distance to grenade
        entities.sort(Comparator.comparingDouble(entity -> entity.getDistanceSqToEntity(this)));

        // Find the closest living entity that is not the thrower
        for (EntityLivingBase entity : entities) {
            if (!entity.equals(thrower)) {
                target = entity;
                pauseTimer = 40; // Reset the pause timer
                return;
            }
        }

        // If no valid target found, set target to null
        target = null;
    }

    private void setMotionTowardsTarget() {
        if (target != null) {
            posX = posXBeforePause;
            posY = posYBeforePause;
            posZ = posZBeforePause;
            motionX = motionXBeforePause;
            motionY = motionYBeforePause;
            motionZ = motionZBeforePause;
        }
    }

    @Override
    public void explode() {
        if (!worldObj.isRemote) {
            ExplosionLarge.explode(worldObj, posX, posY, posZ, 1F, false, false, true);
            setDead();
        }
    }
}
