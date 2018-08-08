package me.virustotal.dynamicgui.inventory.sponge;

import java.util.Optional;

import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.property.InventoryTitle;
import org.spongepowered.api.item.inventory.property.SlotIndex;
import org.spongepowered.api.item.inventory.query.QueryOperationTypes;

import me.virustotal.dynamicgui.DynamicGUI;
import me.virustotal.dynamicgui.inventory.InventoryWrapper;
import me.virustotal.dynamicgui.inventory.ItemStackWrapper;

public class SpongeInventoryWrapper<T extends Inventory> extends InventoryWrapper<T>{

	/**
	 * 
	 */
	private static final long serialVersionUID = 4524275635001827647L;

	public SpongeInventoryWrapper(T inventory) 
	{
		super(inventory);
	}

	@Override
	public String getTitle() 
	{
		return this.getInventory().getInventoryProperty(InventoryTitle.class).get().getValue().toPlain();
	}

	@Override
	public ItemStackWrapper<ItemStack> getItem(int index) 
	{
		Optional<SlotIndex> slotIndex = this.getInventory().getProperty(SlotIndex.class, index);
		if(slotIndex.isPresent())
		{
			ItemStack item = (ItemStack) this.getInventory().query(QueryOperationTypes.INVENTORY_PROPERTY.of(slotIndex.get())).first();
			return new SpongeItemStackWrapper<ItemStack>(item);
		}
		return new SpongeItemStackWrapper<ItemStack>(null);
	}

	@Override
	public void addItem(ItemStackWrapper<?> itemStackWrapper) 
	{
		DynamicGUI.get().getLogger().info("ItemStackWrapper is null: " + (itemStackWrapper == null));
		DynamicGUI.get().getLogger().info("ItemStack is null: " + (itemStackWrapper.getItemStack() == null));
		this.getInventory().offer((ItemStack) itemStackWrapper.getItemStack());
	}
	
	@Override
	public void setItem(int index, ItemStackWrapper<?> itemStackWrapper) 
	{
		Optional<SlotIndex> slotIndex = this.getInventory().getProperty(SlotIndex.class, index);
		if(slotIndex.isPresent())
		{
			this.getInventory()
			.query(QueryOperationTypes.INVENTORY_PROPERTY.of(slotIndex.get()))
			.set((ItemStack) itemStackWrapper.getItemStack());
		}
		
	}
	
	@Override
	public int getSize() 
	{
		return this.getInventory().capacity();
	}
}