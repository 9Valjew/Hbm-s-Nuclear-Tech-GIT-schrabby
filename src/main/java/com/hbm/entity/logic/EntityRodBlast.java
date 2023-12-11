package com.hbm.entity.logic;

import com.hbm.explosion.ExplosionLarge;
import com.hbm.packet.AuxParticlePacketNT;
import com.hbm.packet.PacketDispatcher;

import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public class EntityRodBlast extends Entity {
	
	public static final int maxAge = 60;

	public EntityRodBlast(World p_i1582_1_) {
		super(p_i1582_1_);
		this.ignoreFrustumCheck = true;
	}

	@Override
	protected void entityInit() { }

	@Override
	protected void readEntityFromNBT(NBTTagCompound nbt) { }

	@Override
	protected void writeEntityToNBT(NBTTagCompound nbt) { }
	
	@Override
	public void onUpdate() {
		
		if(this.ticksExisted >= maxAge && !worldObj.isRemote) {
			this.setDead();
			
			worldObj.spawnEntityInWorld(EntityNukeExplosionMK5.statFacNoRad(worldObj, 40, posX, posY, posZ).mute());
			
			for(int i = 0; i < 20; i++) this.worldObj.createExplosion(this, this.posX, this.posY - i, this.posZ, 7.5F, true);
			ExplosionLarge.spawnParticles(worldObj, this.posX, this.posY, this.posZ, 8);
			ExplosionLarge.spawnShrapnels(worldObj, this.posX, this.posY, this.posZ, 8);
			ExplosionLarge.spawnRubble(worldObj, this.posX, this.posY, this.posZ, 8);
			
			NBTTagCompound data = new NBTTagCompound();
			data.setString("type", "muke");
			PacketDispatcher.wrapper.sendToAllAround(new AuxParticlePacketNT(data, posX, posY + 0.5, posZ), new TargetPoint(worldObj.provider.dimensionId, posX, posY, posZ, 250));
			worldObj.playSoundEffect(posX, posY, posZ, "hbm:weapon.mukeExplosion", 25.0F, 0.9F);
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public int getBrightnessForRender(float p_70070_1_) {
		return 15728880;
	}

	@Override
	public float getBrightness(float p_70013_1_) {
		return 1.0F;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean isInRangeToRenderDist(double distance) {
		return distance < 25000;
	}
}
