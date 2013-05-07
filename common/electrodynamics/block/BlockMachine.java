package electrodynamics.block;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLiving;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import electrodynamics.core.CreativeTabED;
import electrodynamics.core.EDLogger;
import electrodynamics.lib.block.Machine;
import electrodynamics.util.PlayerUtil;
import electrodynamics.tileentity.TileEntityMachine;

public class BlockMachine extends BlockContainer {

	public BlockMachine(int id) {
		super(id, Material.iron);
		setHardness(1F);
		setCreativeTab(CreativeTabED.block);
	}
	
	@Override
	public int getRenderType() {
		return -1;
	}
	
	@Override
	public boolean isOpaqueCube() {
		return false;
	}
	
	@Override
	public boolean renderAsNormalBlock() {
		return false;
	}
	
	@Override
	public void onBlockPlacedBy(World world, int i, int j, int k, EntityLiving entityLiving, ItemStack itemStack) 
	{
		TileEntity tile = world.getBlockTileEntity(i, j, k);

		if(tile != null)
		{
			((TileEntityMachine)tile).rotation = PlayerUtil.determine3DOrientation_F(world, i, j, k, entityLiving);
		}
	}
	
	@Override
	public TileEntity createTileEntity(World world, int meta) {
		try {
			return Machine.values()[meta].tileEntity.newInstance();
		} catch (Exception ex) {
			EDLogger.warn("Failed to create TileEntity for machine " + Machine.values()[meta].unlocalizedName);
			return null;
		}
	}
	
	/* IGNORE */
	@Override
	public TileEntity createNewTileEntity(World world) {
		return null;
	}
	
}