package electrodynamics.client.gui;

import electrodynamics.core.handler.GuiHandler.GuiType;
import electrodynamics.core.lang.EDLanguage;
import electrodynamics.inventory.container.ContainerTrayKiln;
import electrodynamics.lib.core.Strings;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.StatCollector;

public class GuiTrayKiln extends GuiElectrodynamics {

	public EntityPlayer player;

	public ContainerTrayKiln container;

	public GuiTrayKiln(EntityPlayer player, ContainerTrayKiln container) {
		super(GuiType.KILN_TRAY, container);
		
		this.container = container;
		this.player = player;
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int par1, int par2) {
		String s = EDLanguage.getInstance().translate(Strings.GUI_TRAY_KILN);
		this.fontRenderer.drawString(s, this.xSize / 2 - this.fontRenderer.getStringWidth(s) / 2, 6, 4210752);
		this.fontRenderer.drawString(StatCollector.translateToLocal("container.inventory"), 8, this.ySize - 96 + 2, 4210752);
	}

}
