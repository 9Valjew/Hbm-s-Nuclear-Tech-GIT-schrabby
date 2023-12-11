package com.hbm.saveddata.satellites;

import com.hbm.entity.logic.EntityRodBlast;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public class SatelliteRod extends Satellite {
	
	public long lastOp;
	
	public SatelliteRod() {
		this.ifaceAcs.add(InterfaceActions.HAS_MAP);
		this.ifaceAcs.add(InterfaceActions.SHOW_COORDS);
		this.ifaceAcs.add(InterfaceActions.CAN_CLICK);
		this.satIface = Interfaces.SAT_PANEL;
	}
	
	public void writeToNBT(NBTTagCompound nbt) {
		nbt.setLong("lastOp", lastOp);
	}
	
	public void readFromNBT(NBTTagCompound nbt) {
		lastOp = nbt.getLong("lastOp");
	}
	
	public void onClick(World world, int x, int z) {
		
		if(lastOp + 10000 < System.currentTimeMillis()) {
    		lastOp = System.currentTimeMillis();
    		
    		int y = world.getHeightValue(x, z);
    		
    		EntityRodBlast blast = new EntityRodBlast(world);
    		blast.posX = x;
    		blast.posY = y;
    		blast.posZ = z;
    		
    		world.spawnEntityInWorld(blast);
    	}
	}
}
