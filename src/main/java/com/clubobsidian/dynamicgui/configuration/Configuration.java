package com.clubobsidian.dynamicgui.configuration;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.concurrent.Callable;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.gson.GsonConfigurationLoader;
import ninja.leaping.configurate.json.JSONConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;

public class Configuration extends ConfigurationSection {

	public static Configuration load(File file)
	{
		try
		{
			Configuration config = new Configuration();
			ConfigurationNode node = null;
			String name = file.getName().toLowerCase();

			if(name.endsWith(".yml"))
			{
				ConfigurationLoader<ConfigurationNode> loader = YAMLConfigurationLoader.builder().setFile(file).build();
				node = loader.load();
				config.loader = loader;
			}
			else if(name.endsWith(".gson"))
			{
				ConfigurationLoader<ConfigurationNode> loader = GsonConfigurationLoader.builder().setFile(file).build();
				node = loader.load();
				config.loader = loader;
			}
			else if(name.endsWith(".json"))
			{
				ConfigurationLoader<ConfigurationNode> loader = JSONConfigurationLoader.builder().setFile(file).build();
				node = loader.load();
				config.loader = loader;
			}
			else
			{
				throw new UnknownFileTypeException(file);
			}

			config.node = node;
			return config;
		}
		catch(IOException | UnknownFileTypeException ex)
		{
			ex.printStackTrace();
		}
		return null;
	}
	
	public static Configuration load(Path path)
	{
		return load(path.toFile());
	}

	public static Configuration load(InputStream stream, ConfigurationType type)
	{
		Configuration config = new Configuration();
		ConfigurationNode node = null;
		BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
		
		try
		{
			Callable<BufferedReader> callable = new Callable<BufferedReader>() 
			{

				@Override
				public BufferedReader call()
				{
					return reader;
				}
			};

			if(type == ConfigurationType.YAML)
			{
				ConfigurationLoader<ConfigurationNode> loader = YAMLConfigurationLoader
						.builder()
						.setSource(callable)
						.build();
				node = loader.load();
				config.loader = loader;
			}
			else if(type == ConfigurationType.GSON)
			{
				ConfigurationLoader<ConfigurationNode> loader = GsonConfigurationLoader
						.builder()
						.setSource(callable)
						.build();
				node = loader.load();
				config.loader = loader;
			}
			else if(type == ConfigurationType.JSON)
			{
				ConfigurationLoader<ConfigurationNode> loader = JSONConfigurationLoader
						.builder()
						.setSource(callable)
						.build();
				node = loader.load();
				config.loader = loader;
			}

			config.node = node;
		}
		catch(IOException ex)
		{
			ex.printStackTrace();
		}
		finally
		{
			try 
			{
				reader.close();
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
			}
		}
		return config;
	}
}