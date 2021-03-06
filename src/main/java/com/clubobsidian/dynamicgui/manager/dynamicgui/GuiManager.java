/*
   Copyright 2019 Club Obsidian and contributors.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package com.clubobsidian.dynamicgui.manager.dynamicgui;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.FileUtils;

import com.clubobsidian.dynamicgui.DynamicGui;
import com.clubobsidian.dynamicgui.enchantment.EnchantmentWrapper;
import com.clubobsidian.dynamicgui.entity.PlayerWrapper;
import com.clubobsidian.dynamicgui.function.EmptyFunction;
import com.clubobsidian.dynamicgui.function.Function;
import com.clubobsidian.dynamicgui.gui.Gui;
import com.clubobsidian.dynamicgui.gui.ModeEnum;
import com.clubobsidian.dynamicgui.gui.Slot;
import com.clubobsidian.dynamicgui.inventory.InventoryWrapper;
import com.clubobsidian.dynamicgui.manager.entity.EntityManager;
import com.clubobsidian.dynamicgui.manager.material.MaterialManager;
import com.clubobsidian.dynamicgui.manager.world.LocationManager;
import com.clubobsidian.dynamicgui.plugin.DynamicGuiPlugin;
import com.clubobsidian.dynamicgui.server.ServerType;
import com.clubobsidian.dynamicgui.util.ChatColor;
import com.clubobsidian.dynamicgui.util.FunctionUtil;
import com.clubobsidian.dynamicgui.world.LocationWrapper;
import com.clubobsidian.fuzzutil.StringFuzz;
import com.clubobsidian.wrappy.Configuration;
import com.clubobsidian.wrappy.ConfigurationSection;

public class GuiManager {

	private static GuiManager instance;
	
	private static int GUI_MAX_SIZE = 54;
	
	private List<Gui> guis;
	private Map<UUID, Gui> playerGuis;
	private GuiManager()
	{
		this.guis = new ArrayList<>();
		this.playerGuis = new HashMap<>();
	}
	
	public static GuiManager get()
	{
		if(instance == null)
		{
			instance = new GuiManager();
			instance.loadGuis();
		}
		return instance;
	}
	
	public boolean hasGuiName(String name)
	{
		for(Gui gui : this.guis)
		{
			if(gui.getName().equals(name))
				return true;
		}
		return false;
	}
	
	public Gui getGuiByName(String name)
	{
		for(Gui gui : this.guis)
		{
			if(gui.getName().equals(name))
			{
				return gui.clone();
			}
		}
		return null;
	}

	public void reloadGuis()
	{
		DynamicGui.get().getLogger().info("Force reloading guis!");
		DynamicGui.get().getPlugin().unloadCommands();
		this.guis.clear();
		this.loadGuis();
	}
	
	public List<Gui> getGuis()
	{
		return this.guis;
	}
	
	public Map<UUID, Gui> getPlayerGuis()
	{
		return this.playerGuis;
	}
	
	public boolean hasGuiCurrently(PlayerWrapper<?> playerWrapper)
	{
		return this.playerGuis.get(playerWrapper.getUniqueId()) != null;
	}
	
	public void cleanupGui(PlayerWrapper<?> playerWrapper)
	{
		this.playerGuis.remove(playerWrapper.getUniqueId());
	}

	public Gui getCurrentGui(PlayerWrapper<?> playerWrapper)
	{
		return this.playerGuis.get(playerWrapper.getUniqueId());
	}
	
	public boolean openGui(Object player, String guiName)
	{
		return this.openGui(EntityManager.get().createPlayerWrapper(player), guiName);
	}
	
	public boolean openGui(Object player, Gui gui)
	{
		return this.openGui(EntityManager.get().createPlayerWrapper(player), gui);
	}
	
	public boolean openGui(PlayerWrapper<?> playerWrapper, String guiName)
	{
		return this.openGui(playerWrapper, this.getGuiByName(guiName));
	}
	
	public boolean openGui(PlayerWrapper<?> playerWrapper, Gui gui)
	{
		if(gui == null)
		{
			playerWrapper.sendMessage(DynamicGui.get().getNoGui());
			return false;
		}
		
		Gui clonedGui = gui.clone();
		boolean ran = FunctionUtil.tryGuiFunctions(clonedGui, playerWrapper);
		
		if(ran)
		{
			
			InventoryWrapper<?> inventoryWrapper = clonedGui.buildInventory(playerWrapper);
			FunctionUtil.tryLoadFunctions(playerWrapper, clonedGui);
			
			if(inventoryWrapper == null)
				return false;
			
			if(DynamicGui.get().getServer().getType() == ServerType.SPONGE)
			{
				DynamicGui.get().getServer().getScheduler().scheduleSyncDelayedTask(DynamicGui.get().getPlugin(), () -> 
				{
					playerWrapper.openInventory(inventoryWrapper);
				}, 1L);
			}
			else
			{
				playerWrapper.openInventory(inventoryWrapper);
			}
			this.playerGuis.put(playerWrapper.getUniqueId(), clonedGui);
			DynamicGui.get().getServer().getScheduler().scheduleSyncDelayedTask(DynamicGui.get().getPlugin(), new Runnable()
			{
				@Override
				public void run()
				{
					playerWrapper.updateInventory();
				}
			},2L);
		}
		return ran;
	}
	
	private void loadGuis()
	{
		this.loadFileGuis();
		this.loadRemoteGuis();
	}
	
	private void loadFileGuis()
	{
		File guiFolder = DynamicGui.get().getPlugin().getGuiFolder();
		
		Collection<File> ar = FileUtils.listFiles(guiFolder, new String[]{"yml", "json", "conf", "xml"}, true);
		
		if(ar.size() != 0)
		{
			for(File file : ar)
			{
				try
				{
					Configuration yaml = Configuration.load(file);
					String guiName = file.getName().substring(0, file.getName().lastIndexOf("."));
					this.loadGuiFromConfiguration(guiName, yaml);
				}	
				catch(NullPointerException ex)
				{
					DynamicGui.get().getLogger().info("Error loading in file: " + file.getName());
					ex.printStackTrace();
				}	
			}
		} 
		else 
		{
			DynamicGui.get().getLogger().error("No guis found, please add guis or issues may occur!");
		}
	}
	
	public void loadRemoteGuis()
	{
		File configFile = new File(DynamicGui.get().getPlugin().getDataFolder(), "config.yml");
		File tempDirectory = new File(DynamicGui.get().getPlugin().getDataFolder(), "temp");
		if(tempDirectory.exists())
		{
			try 
			{
				FileUtils.deleteDirectory(tempDirectory);
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
			}
		}
		tempDirectory.mkdir();
		
		Configuration config = Configuration.load(configFile);
		if(config.get("remote-guis") != null)
		{
			ConfigurationSection remote = config.getConfigurationSection("remote-guis");
			for(String key :  remote.getKeys())
			{
				ConfigurationSection guiSection = remote.getConfigurationSection(key);
				String strUrl = guiSection.getString("url");
				try 
				{
					URL url = new URL(strUrl);
					String guiName = guiSection.getString("file-name");
					File backupFile = new File(DynamicGui.get().getPlugin().getGuiFolder(), guiName);
					File tempFile = new File(tempDirectory, guiName);
					Configuration guiConfiguration = Configuration.load(url, tempFile, backupFile);
					this.loadGuiFromConfiguration(guiName, guiConfiguration);
				} 
				catch (MalformedURLException e) 
				{
					e.printStackTrace();
					DynamicGui.get().getLogger().error("An error occured when loading from the url " + strUrl + " please ensure you have the correct url.");
				}
			}
		}
	}
	
	private void loadGuiFromConfiguration(String guiName, Configuration config)
	{
		String guiType = config.getString("gui-type");
		if(guiType != null)
			guiType = guiType.toUpperCase();
		
		String guiTitle = config.getString("gui-title");
		int rows = config.getInteger("rows");
		if(rows == 0)
			rows = 1;
		
		int lastSlot = getLastSlot(config) + 1;
		
		int calculatedRow = (lastSlot) / 9;
		if(lastSlot % 9 != 0)
			calculatedRow += 1;
		
		if(calculatedRow > rows)
			rows = calculatedRow;
		
		List<Slot> slots = this.createSlots(rows, config);
		
		final Gui gui = this.createGui(config, DynamicGui.get().getPlugin(), guiName, guiType, guiTitle, rows, slots);

		this.guis.add(gui);
		DynamicGui.get().getLogger().info("gui \"" + gui.getName() + "\" has been loaded!");
	}
	
	private Map<String,List<Function>> createFailFunctions(ConfigurationSection section, String end)
	{
		Map<String, List<Function>> failFunctions = new HashMap<>(); //check ends with
		for(String key : section.getKeys())
		{
			if(key.endsWith(end))
			{
				List<Function> failFuncs = new ArrayList<>();
				for(String string : section.getStringList(key))
				{
					String[] array = FunctionManager.get().parseData(string);
					if(FunctionManager.get().getFunctionByName(array[0]) == null)
					{
						DynamicGui.get().getLogger().error("A function cannot be found by the name " + array[0] + " is a dependency not yet loaded?");
					}
					
					Function func = new EmptyFunction(array[0], array[1]);
					failFuncs.add(func);
				}
				String[] split = key.split("-");
				String str = StringFuzz.normalize(split[0]);
				if(split.length > 3)
				{
					for(int j = 1; j < split.length - 1; j++)
					{
						str += "-" + split[j];
					}
				}
				failFunctions.put(str, failFuncs);
			}
		}
		return failFunctions;
	}
	
	private List<Function> createFunctions(ConfigurationSection section, String name)
	{
		List<Function> functions = new ArrayList<>();
		if(section.get(name) != null)
		{
			for(String string : section.getStringList(name))
			{
				String[] array = FunctionManager.get().parseData(string);
				if(FunctionManager.get().getFunctionByName(array[0]) == null)
				{
					DynamicGui.get().getLogger().error("A function cannot be found by the name " + array[0] + " is a dependency not yet loaded?");
				}

				Function func = new EmptyFunction(array[0], array[1]);
				functions.add(func);

			}
		}
		return functions;
	}
	
	private List<Slot> createSlots(int rows, Configuration yaml)
	{
		List<Slot> slots = new ArrayList<>();
		for(int i = 0; i < rows * 9; i++)
		{
			if(yaml.get("" + i) != null)
			{
				ConfigurationSection section = yaml.getConfigurationSection(i + "");
				String icon = MaterialManager.get().normalizeMaterial(section.getString("icon"));
				String name = null;

				if(section.get("name") != null)
				{
					name = ChatColor.translateAlternateColorCodes('&', section.getString("name"));
				}

				String nbt = null;
				if(section.get("nbt") != null)
				{
					nbt = section.getString("nbt");
				}
				
				List<Function> functions = this.createFunctions(section, "functions");
				List<Function> leftClickFunctions = this.createFunctions(section, "leftclick-functions");
				List<Function> rightClickFunctions = this.createFunctions(section, "rightclick-functions");
				List<Function> middleClickFunctions = this.createFunctions(section, "middleclick-functions");
				
				
				Map<String,List<Function>> failFunctions = this.createFailFunctions(section, "-failfunctions");
				Map<String,List<Function>> leftClickFailFunctions = this.createFailFunctions(section, "-leftclickfailfunctions");
				Map<String,List<Function>> rightClickFailFunctions = this.createFailFunctions(section, "-rightclickfailfunctions");
				Map<String,List<Function>> middleClickFailFunctions = this.createFailFunctions(section, "-middleclickfailfunctions");
				//fail functions
				
				List<Function> loadFunctions = this.createFunctions(section, "load-functions");
				Map<String, List<Function>> loadFailFunctions = this.createFailFunctions(section, "-loadfailfunctions");
				
				
				List<String> lore = null;
				if(section.get("lore") != null)
				{
					lore = new ArrayList<>();
					for(String ls : section.getStringList("lore"))
					{
						lore.add(ChatColor.translateAlternateColorCodes('&', ls));
					}
				}

				List<EnchantmentWrapper> enchants = null;
				if(section.get("enchants") != null)
				{
					enchants = new ArrayList<EnchantmentWrapper>();
					for(String ench : section.getStringList("enchants"))
					{
						String[] args = ench.split(",");
						enchants.add(new EnchantmentWrapper(args[0], Integer.parseInt(args[1])));
					}
				}
				int amount = 1;
				if(section.get("amount") != null)
				{
					amount = section.getInteger("amount");
				}

				Boolean close = null;
				if(section.get("close") != null)
				{
					close = section.getBoolean("close");
				}
				
				short data = 0;
				if(section.get("data") != null)
				{
					data = (short) section.getInteger("data");
				}

				slots.add(new Slot(icon, name, nbt, data, close, lore, enchants, i, functions, failFunctions, leftClickFunctions, leftClickFailFunctions, rightClickFunctions, rightClickFailFunctions, middleClickFunctions, middleClickFailFunctions, loadFunctions, loadFailFunctions, amount));
			}
		}
		
		return slots;
	}
	
	private Gui createGui(final Configuration yaml, final DynamicGuiPlugin plugin, final String guiName, final String guiType, final  String guiTitle, final int rows, final List<Slot> slots)
	{
		//int commandsLoaded = 0;
		if(yaml.get("alias") != null)
		{
			for(String alias : yaml.getStringList("alias"))
			{
				plugin.createCommand(guiName, alias);
			}
		}

		Boolean close = null;
		if(yaml.get("close") != null)
		{
			close = yaml.getBoolean("close");
		}

		List<LocationWrapper<?>> locations = new ArrayList<>(); 
		if(yaml.get("locations") != null)
		{
			for(String location : yaml.getStringList("locations"))
			{
				locations.add(LocationManager.get().toLocationWrapper(location));
			}
		}

		ModeEnum modeEnum = ModeEnum.ADD;
		if(yaml.get("mode") != null)
		{
			modeEnum = ModeEnum.valueOf(yaml.getString("mode").toUpperCase());
		}

		List<Integer> npcIds = new ArrayList<>();
		
		if(yaml.get("npc-ids") != null)
		{
			npcIds = yaml.getIntegerList("npc-ids");
		}
		
		List<Function> functions = this.createFunctions(yaml, "functions");
		Map<String,List<Function>> failFunctions = this.createFailFunctions(yaml, "-failfunctions");
		
		return new Gui(guiName, guiType, guiTitle, rows, close, modeEnum, npcIds, slots, locations, functions, failFunctions);
	}
	
	private int getLastSlot(Configuration yaml)
	{
		int lastIndex = 0;
		for(int i = 0; i < GuiManager.GUI_MAX_SIZE; i++)
		{
			if(yaml.get(String.valueOf(i)) != null)
			{
				lastIndex = i;
			}
		}
		return lastIndex;
	}
}