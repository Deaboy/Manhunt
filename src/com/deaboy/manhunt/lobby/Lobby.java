package com.deaboy.manhunt.lobby;

import java.io.Closeable;
import java.io.File;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import com.deaboy.manhunt.Manhunt;
import com.deaboy.manhunt.map.*;
import com.deaboy.manhunt.settings.LobbySettings;
import com.deaboy.manhunt.settings.SettingsFile;

public abstract class Lobby implements Closeable
{
	//////////////// Properties ////////////////
	private final long id;
	private String name;
	
	private Spawn spawn;
	private boolean is_open;
	private SettingsFile file;
	
	
	//////////////// CONSTRUCTORS ////////////////
	public Lobby(long id, File file)
	{
		this(id, file, "", Bukkit.getWorlds().get(0).getSpawnLocation());
	}
	public Lobby(long id, File file, String name, Location location)
	{
		if (file == null || name == null || location == null)
		{
			throw new IllegalArgumentException("Arguments cannot be null.");
		}
		
		this.id = id;
		this.name = name;
		
		this.spawn = new ManhuntSpawn("spawn", SpawnType.OTHER, location);
		this.is_open = true;
		this.file = new SettingsFile(file);
	}
	
	
	//////////////// SETTERS ////////////////
	public boolean setName(String name)
	{
		if (name.trim().isEmpty())
			return false;
		
		if (Manhunt.getLobby(name) != null)
			return false;
		
		this.name = name;
		return true;
	}
	
	public void setSpawnRange(int range)
	{
		getSpawn().setRange(range);
	}
	public void setSpawnLocation(Location loc)
	{
		getSpawn().setLocation(loc);
	}
	
	public void enable()
	{
		this.is_open = true;
	}
	public void disable()
	{
		this.is_open = false;
	}
	public void setMaxPlayers(int num)
	{
		this.getSettings().MAX_PLAYERS.setValue(num < 0 ? 0 : num);
	}
	
	
	//////////////// GETTERS ////////////////
	public long getId()
	{
		return id;
	}
	public String getName()
	{
		return name;
	}
	private Spawn getSpawn()
	{
		return spawn;
	}
	public World getWorld()
	{
		return Manhunt.getWorld(spawn.getWorld());
	}
	
	public Location getSpawnLocation()
	{
		return spawn.getLocation();
	}
	public Location getRandomSpawnLocation()
	{
		return spawn.getRandomLocation();
	}
	public int getSpawnRange()
	{
		return getSpawn().getRange();
	}
	
	public boolean isEnabled()
	{
		return is_open;
	}
	public abstract LobbyType getType();
	
	public int getMaxPlayers()
	{
		return this.getSettings().MAX_PLAYERS.getValue();
	}
	public boolean allowAllPlayers()
	{
		return getSettings().MAX_PLAYERS.getValue() < 0;
	}
	
	
	//////////////// PUBLIC METHODS ////////////////
	//---------------- INTERFACE ----------------//
	public abstract boolean playerJoinLobby(Player player);
	public abstract boolean playerLeaveLobby(String name);
	public abstract boolean playerLeaveServer(Player player);
	
	
	//---------------- PLAYERS ----------------//
	protected abstract boolean addPlayer(Player player);
	protected abstract boolean addPlayer(String name);
	public abstract boolean containsPlayer(Player player);
	public abstract boolean containsPlayer(String name);
	protected abstract boolean removePlayer(Player player);
	protected abstract boolean removePlayer(String name);
	protected abstract void clearPlayers();
	protected abstract void clearOfflinePlayers();
	
	public abstract List<String> getPlayerNames();
	public abstract List<String> getOnlinePlayerNames();
	public abstract List<String> getOfflinePlayerNames();
	public abstract List<Player> getOnlinePlayers();
	public abstract List<OfflinePlayer> getOfflinePlayers();
	
	
	public abstract void broadcast(String message);
	
	
	//---------------- Settings ----------------//
	public abstract LobbySettings getSettings();
	public void saveFiles()
	{
		getSettings().LOBBY_NAME.setValue(this.name);
		getSettings().LOBBY_CLASS.setValue(this.getClass().getCanonicalName());
		getSettings().SPAWN_RANGE.setValue(this.spawn.getRange());
		getSettings().SPAWN_LOCATION.setValue(this.spawn.getLocation());
		getSettings().LOBBY_OPEN.setValue(this.is_open);
		
		save();
	}
	private void save()
	{
		this.file.clearPacks();
		if (this instanceof GameLobby && ((GameLobby) this).getGame() != null)
		{
			this.file.addPack(((GameLobby) this).getGame().getSettings());
		}
		this.file.addPack(getSettings());
		this.file.save();
	}
	public void loadFiles()
	{
		load();
		
		this.name = getSettings().LOBBY_NAME.getValue();
		this.spawn.setRange(getSettings().SPAWN_RANGE.getValue());
		this.spawn.setLocation(getSettings().SPAWN_LOCATION.getValue());
		this.is_open = getSettings().LOBBY_OPEN.getValue();
	}
	protected void softLoad()
	{
		this.file.clearPacks();
		if (this instanceof GameLobby && ((GameLobby) this).getGame() != null)
		{
			this.file.addPack(((GameLobby) this).getGame().getSettings());
		}
		this.file.addPack(getSettings());
		this.file.loadPacks();
		
		this.name = getSettings().LOBBY_NAME.getValue();
		this.spawn.setRange(getSettings().SPAWN_RANGE.getValue());
		this.spawn.setLocation(getSettings().SPAWN_LOCATION.getValue());
		this.is_open = getSettings().LOBBY_OPEN.getValue();
	}
	private void load()
	{
		this.file.clearPacks();
		if (this instanceof GameLobby && ((GameLobby) this).getGame() != null)
		{
			this.file.addPack(((GameLobby) this).getGame().getSettings());
		}
		this.file.addPack(getSettings());
		this.file.load();
	}
	public boolean deleteFiles()
	{
		return this.file.getFile().delete();
	}
	
	
	@Override
	public void close()
	{
		clearPlayers();
	}
	
	
	
}
