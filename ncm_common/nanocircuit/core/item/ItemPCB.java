package nanocircuit.core.item;

import nanocircuit.core.core.CreativeTabNCM;
import nanocircuit.core.lib.ModInfo;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.item.Item;
import net.minecraft.util.Icon;

public class ItemPCB extends Item {
	
	private Icon texture;
	
	public ItemPCB(int id) {
		super(id);
		setHasSubtypes(true);
		setCreativeTab(CreativeTabNCM.item);
	}

	@Override
	public Icon getIconFromDamage(int meta) {
		return texture;
	}

	@Override
	public void registerIcons(IconRegister register) {
		this.texture = register.registerIcon(ModInfo.ICON_PREFIX + "misc/pcb");
	}
	
}
