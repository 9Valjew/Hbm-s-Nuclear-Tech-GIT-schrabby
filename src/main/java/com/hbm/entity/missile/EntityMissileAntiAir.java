package com.hbm.entity.missile;

import java.util.ArrayList;
import java.util.List;

import com.hbm.entity.logic.IChunkLoader;
import com.hbm.entity.projectile.EntityThrowableInterp;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.main.MainRegistry;
import com.hbm.tileentity.machine.TileEntityMachineRadarNT;

import api.hbm.entity.IRadarDetectable;
import api.hbm.entity.IRadarDetectableNT;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.ForgeChunkManager.Ticket;
import net.minecraftforge.common.ForgeChunkManager.Type;

import java.util.List;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.util.AxisAlignedBB;

public class EntityMissileAntiAir extends EntityThrowableInterp implements IChunkLoader, IRadarDetectable, IRadarDetectableNT {

    private Ticket loaderTicket;
    public Entity tracking;
    public double velocity;
    protected int activationTimer;

    public static double baseSpeed = 1.5D;

    public EntityMissileAntiAir(World world) {
        super(world);
        this.setSize(1.5F, 1.5F);
        this.motionY = baseSpeed;
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        init(ForgeChunkManager.requestTicket(MainRegistry.instance, worldObj, Type.ENTITY));
    }

    @Override
    protected double motionMult() {
        return velocity;
    }

    @Override
    public boolean doesImpactEntities() {
        return true;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();

        if(!worldObj.isRemote) {

            if(velocity < 6) velocity += 0.1;

            if(activationTimer < 20) {
                activationTimer++;
                motionY = baseSpeed;
            } else {
                Entity prevTracking = this.tracking;

                if(this.tracking == null || this.tracking.isDead) this.targetMissile();

                if(prevTracking == null && this.tracking != null) {
                    ExplosionLarge.spawnShock(worldObj, posX, posY, posZ, 24, 3F);
                }

                if(this.tracking != null) {
                    this.aimAtTarget();
                } else {
                    if(this.ticksExisted > 600) this.setDead();
                }
            }

            loadNeighboringChunks((int) Math.floor(posX / 16), (int) Math.floor(posZ / 16));

            if(this.posY > 2000 && (this.tracking == null || this.tracking.isDead)) this.setDead();

        } else {

            Vec3 vec = Vec3.createVectorHelper(motionX, motionY, motionZ).normalize();
            MainRegistry.proxy.particleControl(posX - vec.xCoord, posY - vec.yCoord, posZ - vec.zCoord, 2);
        }

        float f2 = MathHelper.sqrt_double(this.motionX * this.motionX + this.motionZ * this.motionZ);
        this.rotationYaw = (float) (Math.atan2(this.motionX, this.motionZ) * 180.0D / Math.PI);
        for(this.rotationPitch = (float) (Math.atan2(this.motionY, f2) * 180.0D / Math.PI) - 90; this.rotationPitch - this.prevRotationPitch < -180.0F; this.prevRotationPitch -= 360.0F);
        while(this.rotationPitch - this.prevRotationPitch >= 180.0F) this.prevRotationPitch += 360.0F;
        while(this.rotationYaw - this.prevRotationYaw < -180.0F) this.prevRotationYaw -= 360.0F;
        while(this.rotationYaw - this.prevRotationYaw >= 180.0F) this.prevRotationYaw += 360.0F;

    }

    /** Detects and caches nearby Entities */
    protected void targetMissile() {
        World thisWorld = this.worldObj;

        Entity closest = null;
        double dist = 1_000;
        List<Entity> matchingEntitiesLocal = new ArrayList<Entity>();
        for(Object entity : thisWorld.loadedEntityList) {
            for(Class clazz : TileEntityMachineRadarNT.classes)
            {
                if(clazz.isAssignableFrom(entity.getClass()))
                {
                    matchingEntitiesLocal.add((Entity) entity);
                    break;
                }
            }
        }
        for(Entity e :matchingEntitiesLocal) {
            if(e.dimension != this.dimension) {continue; }
            if(e instanceof EntityMissileBaseNT){continue; } //can NOT lock onto missiles
            if(e instanceof EntityMissileAntiBallistic || e instanceof EntityMissileAntiAir) {continue; } //can NOT lock onto SAMs and ABMs
            if(e.posY < 120) {continue; } //can only lock onto entities above 120 so it wont fucking lock onto players on the ground
            if(e instanceof EntityMissileStealth) continue; //cannot lack onto missiles with stealth coating

            else {
                Vec3 vec = Vec3.createVectorHelper(e.posX - posX, e.posY - posY, e.posZ - posZ);
                if(vec.lengthVector() < dist) {
                    closest = e;
                }
            }}
        if (closest != null)
        {
            this.tracking = closest;
        }
    }

    /** Predictive targeting system */
    protected void aimAtTarget() {

        Vec3 delta = Vec3.createVectorHelper(tracking.posX - posX, tracking.posY - posY, tracking.posZ - posZ);
        double intercept = delta.lengthVector() / (this.baseSpeed * this.velocity);
        Vec3 predicted = Vec3.createVectorHelper(tracking.posX + (tracking.posX - tracking.lastTickPosX) * intercept, tracking.posY + (tracking.posY - tracking.lastTickPosY) * intercept, tracking.posZ + (tracking.posZ - tracking.lastTickPosZ) * intercept);
        Vec3 motion = Vec3.createVectorHelper(predicted.xCoord - posX, predicted.yCoord - posY, predicted.zCoord - posZ).normalize();

        if((delta.lengthVector() < 10 && activationTimer >= 40) || isProximity())
        {
            this.setDead();
            //ExplosionLarge.explode(worldObj, posX, posY, posZ, 7F, false, false, false);
            ExplosionVNT explosion = new ExplosionVNT(worldObj,posX, posY, posZ, 20F).makeStandard();
            explosion.setBlockAllocator(null);
            explosion.explode();

        }

        this.motionX = motion.xCoord * baseSpeed;
        this.motionY = motion.yCoord * baseSpeed;
        this.motionZ = motion.zCoord * baseSpeed;
    }

    @Override
    protected void onImpact(MovingObjectPosition mop) {
        if(this.activationTimer >= 40) {
            this.setDead();
            ExplosionVNT explosion = new ExplosionVNT(worldObj,posX, posY, posZ, 20F).makeStandard();
            explosion.setBlockAllocator(null);
            explosion.explode();
        }
    }

    @Override
    public double getGravityVelocity() {
        return 0.0D;
    }

    @Override
    protected float getAirDrag() {
        return 1F;
    }

    @Override
    protected float getWaterDrag() {
        return 1F;
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound nbt) {
        super.readEntityFromNBT(nbt);
        this.velocity = nbt.getDouble("veloc");
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound nbt) {
        super.writeEntityToNBT(nbt);
        nbt.setDouble("veloc", this.velocity);
    }

    @Override
    public void init(Ticket ticket) {
        if(!worldObj.isRemote) {

            if(ticket != null) {

                if(loaderTicket == null) {

                    loaderTicket = ticket;
                    loaderTicket.bindEntity(this);
                    loaderTicket.getModData();
                }

                ForgeChunkManager.forceChunk(loaderTicket, new ChunkCoordIntPair(chunkCoordX, chunkCoordZ));
            }
        }
    }

    List<ChunkCoordIntPair> loadedChunks = new ArrayList<ChunkCoordIntPair>();

    public void loadNeighboringChunks(int newChunkX, int newChunkZ) {
        if(!worldObj.isRemote && loaderTicket != null) {

            clearChunkLoader();

            loadedChunks.clear();
            for(int i = -1; i <= 1; i++) for(int j = -1; j <= 1; j++) loadedChunks.add(new ChunkCoordIntPair(newChunkX + i, newChunkZ + j));

            for(ChunkCoordIntPair chunk : loadedChunks) {
                ForgeChunkManager.forceChunk(loaderTicket, chunk);
            }
        }
    }

    @Override
    public void setDead() {
        super.setDead();
        this.clearChunkLoader();
    }

    public void clearChunkLoader() {
        if(!worldObj.isRemote && loaderTicket != null) {
            for(ChunkCoordIntPair chunk : loadedChunks) {
                ForgeChunkManager.unforceChunk(loaderTicket, chunk);
            }
        }
    }

    public boolean isProximity()
    {
        if(this.tracking != null)
        {
            if(this.getDistanceToEntity(this.tracking) < 7)
            {
                return true;
            }
        }
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean isInRangeToRenderDist(double distance) {
        return true;
    }

    @Override
    public RadarTargetType getTargetType() {
        return RadarTargetType.MISSILE_SAM;
    }

    @Override
    public String getUnlocalizedName() {
        return "radar.target.sam";
    }
    @Override
    public int getBlipLevel() {
        return IRadarDetectableNT.TIER_SAM;
    }

    @Override
    public boolean canBeSeenBy(Object radar) {
        return true;
    }

    @Override
    public boolean paramsApplicable(RadarScanParams params) {
        return params.scanMissiles;
    }

    @Override
    public boolean suppliesRedstone(RadarScanParams params) {
        return false;
    }
}
