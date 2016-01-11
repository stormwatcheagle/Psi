/**
 * This class was created by <Vazkii>. It's distributed as
 * part of the Psi Mod. Get the Source Code in github:
 * https://github.com/Vazkii/Psi
 * 
 * Psi is Open Source and distributed under the
 * CC-BY-NC-SA 3.0 License: https://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB
 * 
 * File Created @ [10/01/2016, 23:21:21 (GMT)]
 */
package vazkii.psi.common.core.handler;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

import com.sun.org.apache.bcel.internal.generic.AALOAD;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;

public class PlayerDataHandler {

	private static WeakHashMap<EntityPlayer, PlayerData> playerData = new WeakHashMap();
	
	private static final String DATA_TAG = "PsiData";

	public static PlayerData get(EntityPlayer player) {
		if(!playerData.containsKey(player))
			playerData.put(player, new PlayerData(player));
		
		return playerData.get(player);
	}
	
	public static NBTTagCompound getDataCompoundForPlayer(EntityPlayer player) {
		NBTTagCompound forgeData = player.getEntityData();
		if(!forgeData.hasKey(EntityPlayer.PERSISTED_NBT_TAG))
			forgeData.setTag(EntityPlayer.PERSISTED_NBT_TAG, new NBTTagCompound());

		NBTTagCompound persistentData = forgeData.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
		if(!persistentData.hasKey(DATA_TAG))
			persistentData.setTag(DATA_TAG, new NBTTagCompound());

		return persistentData.getCompoundTag(DATA_TAG);
	}

	public static class PlayerData {

		private static final String TAG_LEVEL = "level";
		private static final String TAG_AVAILABLE_PSI = "availablePsi";
		private static final String TAG_REGEN_CD = "regenCd";

		public int level;
		public int availablePsi;
		public int regenCooldown;

		public final WeakReference<EntityPlayer> playerWR;
		private final boolean client;
		
		public PlayerData(EntityPlayer player) {
			playerWR = new WeakReference(player);
			client = player.worldObj.isRemote;
			
			load();
		}

		public void tick() {
			if(regenCooldown == 0) {
				int max = getTotalPsi();
				if(availablePsi < max && regenCooldown == 0) {
					availablePsi = Math.min(max, availablePsi + getRegenPerTick());
					save();
				}
			} else {
				regenCooldown--;
				save();
			}
		}

		public void deductPsi(int psi, int cd) {
			availablePsi -= psi;
			if(regenCooldown < cd)
				regenCooldown = cd;
			
			if(availablePsi < 0) {
				int overflow = -availablePsi;
				availablePsi = 0;
				
				// TODO Use CAD batteries
				
				float dmg = (float) overflow / 50;
				if(!client) {
					EntityPlayer player = playerWR.get();
					if(player != null)
						player.attackEntityFrom(DamageSource.magic, dmg); // TODO better DS
				}
			}
			
			if(client) {
				// TODO Sync
			}
			
			save(); 
		}
		
		public int getTotalPsi() {
			return level * 200;
		}

		public int getRegenPerTick() {
			return level;
		}

		public void save() {
			if(!client) {
				EntityPlayer player = playerWR.get();

				if(player != null) {
					NBTTagCompound cmp = getDataCompoundForPlayer(player);
					cmp.setInteger(TAG_LEVEL, level);
					cmp.setInteger(TAG_AVAILABLE_PSI, availablePsi);
					cmp.setInteger(TAG_REGEN_CD, regenCooldown);	
				}
			}
		}

		public void load() {
			if(!client) {
				EntityPlayer player = playerWR.get();

				if(player != null) {
					NBTTagCompound cmp = getDataCompoundForPlayer(player);
					level = cmp.getInteger(TAG_LEVEL);
					availablePsi = cmp.getInteger(TAG_AVAILABLE_PSI);
					regenCooldown = cmp.getInteger(TAG_REGEN_CD);
				}
			}
		}
		
	}
}