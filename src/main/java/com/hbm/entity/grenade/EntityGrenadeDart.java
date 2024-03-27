package com.hbm.entity.grenade;

import net.minecraft.util.AxisAlignedBB;
import com.hbm.explosion.ExplosionLarge;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;
import net.minecraft.util.DamageSource;

import java.util.List;

public class EntityGrenadeDart extends EntityGrenadeBase {

    private EntityLivingBase target;
    private EntityLivingBase thrower;

    public EntityGrenadeDart(World p_i1773_1_) {
        super(p_i1773_1_);
    }

    public EntityGrenadeDart(World p_i1774_1_, EntityLivingBase p_i1774_2_) {
        super(p_i1774_1_, p_i1774_2_);
        this.thrower = p_i1774_2_;
    }

    public EntityGrenadeDart(World p_i1775_1_, double p_i1775_2_, double p_i1775_4_, double p_i1775_6_) {
        super(p_i1775_1_, p_i1775_2_, p_i1775_4_, p_i1775_6_);
    }

    @Override
    public void explode() {

        this.motionX = 0;
        this.motionY = 0;
        this.motionZ = 0;
        /*
        try
        {
            Thread.sleep(5000);
        }
        catch(InterruptedException ex)
        {
            Thread.currentThread().interrupt();
        }*/
        if (!this.worldObj.isRemote) {
            if (this.target == null) {
                this.motionX = 0;
                this.motionY = 0;
                this.motionZ = 0;

                List<EntityLivingBase> entities = this.worldObj.getEntitiesWithinAABB(EntityLivingBase.class, AxisAlignedBB.getBoundingBox(
                        this.posX - 50, this.posY - 50, this.posZ - 50,
                        this.posX + 50, this.posY + 50, this.posZ + 50
                ));
                entities.removeIf(entity -> entity.equals(this) || entity.equals(this.thrower));

                if (!entities.isEmpty()) {
                    this.target = entities.get(0);
                    this.setThrowableHeading(this.target.posX - this.posX, this.target.posY - this.posY, this.target.posZ - this.posZ, 8.0F, 0.0F);
                }

            } else {
                // Adjust the arguments to match the correct method in ExplosionLarge class
                ExplosionLarge.explode(worldObj, posX, posY, posZ, 2.5F, false, false, false);
                this.setDead();
            }
        }
    }
}