package electrodynamics.tileentity.machine;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.ForgeDirection;
import cpw.mods.fml.common.network.PacketDispatcher;
import electrodynamics.api.tool.ITool;
import electrodynamics.core.CoreUtils;
import electrodynamics.lib.block.BlockIDs;
import electrodynamics.lib.client.FXType;
import electrodynamics.network.PacketTypeHandler;
import electrodynamics.network.packet.PacketFX;
import electrodynamics.network.packet.PacketSound;
import electrodynamics.recipe.RecipeTable;
import electrodynamics.recipe.manager.CraftingManager;
import electrodynamics.tileentity.TileEntityEDRoot;
import electrodynamics.util.BlockUtil;
import electrodynamics.util.InventoryUtil;

public class TileEntityTable extends TileEntityEDRoot {
	
	/** Item to be displayed on the display table */
	public ItemStack displayedItem;
	
	private class ActivationActionType {
		public static final int NOTHING = 0;
		public static final int TAKE = 1;
		public static final int PUT = 2;
		public static final int PUT_HOTBAR = 3;
		public static final int DO_RECIPE = 4;
		public static final int TRANSFORM = 5;
		
	}
	
	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);

		if (displayedItem != null) {
			nbt.setTag("displayedItem", displayedItem.writeToNBT(new NBTTagCompound()));
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);

		if (nbt.hasKey("displayedItem")) {
			this.displayedItem = ItemStack.loadItemStackFromNBT(nbt.getCompoundTag("displayedItem"));
		}else{
			this.displayedItem = null;
		}
	}

	@Override
	public void onUpdatePacket(NBTTagCompound nbt) {
		super.onUpdatePacket(nbt);
		
		if (nbt.hasKey("displayedItem")) {
			this.displayedItem = ItemStack.loadItemStackFromNBT(nbt.getCompoundTag("displayedItem"));
		}

		if(nbt.hasKey("noDisplay"))
		{
			this.displayedItem = null;
		}
	}
	
	@Override
	public void onDescriptionPacket(NBTTagCompound nbt) {
		super.onDescriptionPacket(nbt);

		readFromNBT(nbt);
	}
	
	@Override
	public void getDescriptionForClient(NBTTagCompound nbt) {
		super.getDescriptionForClient(nbt);
		
		writeToNBT(nbt);
	}

	@Override
	public void onBlockBreak() {
		if(this.displayedItem != null) {
			InventoryUtil.ejectItem(worldObj, xCoord, yCoord, zCoord, ForgeDirection.UP, this.displayedItem, new Random());
		}
	}
	
	@Override
	public void onBlockActivated(EntityPlayer player) {
		if(CoreUtils.isServer(worldObj)) {
			
			if (player.isSneaking()) return;
			
			ItemStack tool = player.getCurrentEquippedItem();
			
			int ret = getActionType(tool, displayedItem);
			System.out.println("TYPE: " + ret);
			
			switch(ret) {
				case ActivationActionType.TAKE:
					BlockUtil.dropItemFromBlock(worldObj, xCoord, yCoord, zCoord, this.displayedItem.copy(), new Random());
					setItem(null);
					break;
					
				case ActivationActionType.PUT:
					ItemStack toDisplay = player.getCurrentEquippedItem().copy();
					toDisplay.stackSize = 1;
					setItem(toDisplay);
				
					if (player.getCurrentEquippedItem().stackSize > 1) {
						player.getCurrentEquippedItem().stackSize--;
					} else {
						player.inventory.mainInventory[player.inventory.currentItem] = null;
					}
					break;
					
				case ActivationActionType.PUT_HOTBAR:
					ItemStack toDisp = getOrePieceFromPlayer(player, tool);
					if(toDisp != null) 
						setItem(toDisp);
					break;
					
				case ActivationActionType.DO_RECIPE:
					RecipeTable recipe = CraftingManager.getInstance().tableManager.getRecipe(displayedItem, tool);
					
					if (recipe != null) {
						if (displayedItem.getItem() instanceof ItemBlock) {
							PacketFX packet = new PacketFX(FXType.BLOCK_BREAK, xCoord, yCoord, zCoord, new int[] {displayedItem.itemID, displayedItem.getItemDamage()});
							PacketDispatcher.sendPacketToAllAround(xCoord, yCoord + 2, zCoord, 64D, this.worldObj.provider.dimensionId, PacketTypeHandler.fillPacket(packet));
							PacketSound sound = new PacketSound("electrodynamics.block.oreCrumble", xCoord, yCoord, zCoord, PacketSound.TYPE_SOUND);
							PacketDispatcher.sendPacketToAllAround(xCoord, yCoord, zCoord, 32D, this.worldObj.provider.dimensionId, PacketTypeHandler.fillPacket(sound));
						}
						
						recipe.onSmashed(player, this, this.displayedItem);
						
						setItem(recipe.outputItem);
						tool.damageItem(recipe.hammerDamage, player);
					}
					break;
					
				case ActivationActionType.TRANSFORM:
					this.displayedItem = null;
					this.worldObj.setBlock(xCoord, yCoord, zCoord, BlockIDs.BLOCK_TABLE_ID, 1, 2);
					tool.damageItem(1, player);
					break;
				
				case ActivationActionType.NOTHING:
				default:
					break;
			}
		}
	}
	
	private int getActionType(ItemStack tool, ItemStack item) {
		
		byte type = (byte)this.worldObj.getBlockMetadata(xCoord, yCoord, zCoord);
		
		if(type == 0) {
			if(tool != null && tool.getItem() instanceof ITool && item != null && item.getItem().itemID == Block.stoneSingleSlab.blockID) {
				return ActivationActionType.TRANSFORM;
			} else if(tool != null && item == null) {
				return ActivationActionType.PUT;
			}
			
		} else if(type == 1) {
			if(tool != null && item == null) {
				if(tool.getItem() instanceof ITool)
					return ActivationActionType.PUT_HOTBAR;
				else
					return ActivationActionType.PUT;
			} else if(tool != null && tool.getItem() instanceof ITool && item != null) {
				if(CraftingManager.getInstance().tableManager.getRecipe(item, tool) != null)
					return ActivationActionType.DO_RECIPE;	
			}	
		}
		
		if(item != null) 
			return ActivationActionType.TAKE;
		
		return ActivationActionType.NOTHING;
	}
	
	private void sendDisplayUpdate()
	{
		NBTTagCompound nbt = new NBTTagCompound();
		if(this.displayedItem != null) {
			nbt.setTag("displayedItem", displayedItem.writeToNBT(new NBTTagCompound()));
		} else {
			nbt.setBoolean("noDisplay", true);
		}

		sendUpdatePacket(nbt);
	}

	public void setItem(ItemStack item) {
		displayedItem = item;
		sendDisplayUpdate();
	}
	
	public void handleToolUse(EntityPlayer player, ItemStack tool) {
		if (displayedItem != null) {
			byte type = (byte)this.worldObj.getBlockMetadata(xCoord, yCoord, zCoord);
			if (type == 0 && displayedItem.getItem().itemID == Block.stoneSingleSlab.blockID) {
				this.displayedItem = null;
				this.worldObj.setBlock(xCoord, yCoord, zCoord, BlockIDs.BLOCK_TABLE_ID, 1, 2);
			} else if (type == 1) {
				RecipeTable recipe = CraftingManager.getInstance().tableManager.getRecipe(displayedItem, tool);

				if (recipe != null) {
					if (displayedItem.getItem() instanceof ItemBlock) {
						PacketFX packet = new PacketFX(FXType.BLOCK_BREAK, xCoord, yCoord, zCoord, new int[] {displayedItem.itemID, displayedItem.getItemDamage()});
						PacketDispatcher.sendPacketToAllAround(xCoord, yCoord + 2, zCoord, 64D, this.worldObj.provider.dimensionId, PacketTypeHandler.fillPacket(packet));
						PacketSound sound = new PacketSound("electrodynamics.block.oreCrumble", xCoord, yCoord, zCoord, PacketSound.TYPE_SOUND);
						PacketDispatcher.sendPacketToAllAround(xCoord, yCoord, zCoord, 32D, this.worldObj.provider.dimensionId, PacketTypeHandler.fillPacket(sound));
					}

					recipe.onSmashed(player, this, this.displayedItem);

					setItem(recipe.outputItem);
					tool.damageItem(recipe.hammerDamage, player);
				}
			}
		}
	}

	
	public ItemStack getOrePieceFromPlayer(EntityPlayer player, ItemStack tool) {
		for(int i = 0; i < 9; i++) {
			ItemStack stack = player.inventory.getStackInSlot(i);
			if(stack != null && stack != tool) {
				ItemStack piece = stack.copy();
				piece.stackSize = 1;
				
				RecipeTable recipe = CraftingManager.getInstance().tableManager.getRecipe(piece, tool);
			
				if(recipe != null) {
					if (player.inventory.mainInventory[i].stackSize > 1) {
						player.inventory.mainInventory[i].stackSize--;
					} else {
						player.inventory.mainInventory[i] = null;
					}
					return piece;
				}
			}
		}
		return null;
	}

}
