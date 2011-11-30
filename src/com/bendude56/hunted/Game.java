package com.bendude56.hunted;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

import net.minecraft.server.Packet20NamedEntitySpawn;
import net.minecraft.server.Packet29DestroyEntity;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Wool;
import org.bukkit.craftbukkit.entity.CraftPlayer;

public class Game {
	private SettingsFile settings;
	
	private HashMap<String, Long> timeout;
	
	private List<String> hunter;
	private List<String> hunted;
	private List<String> spectator;
	private List<String> creative;
	private List<String> locator;
	
	private int countdown;
	private boolean gameRunning;
	private long hunterReleaseTick;
	private long endTick;
	
	private int tickSched;
	
	public Game() {
		hunter = new ArrayList<String>();
		hunted = new ArrayList<String>();
		spectator = new ArrayList<String>();
		creative = new ArrayList<String>();
		locator = new ArrayList<String>();
		timeout = new HashMap<String, Long>();
		gameRunning = false;
		countdown = 0;
		hunterReleaseTick = 0;
		endTick = 0;
		settings = HuntedPlugin.getInstance().getSettings();
	}
	
	public void start() {
		if (!gameRunning) {
			HuntedPlugin.getInstance().getWorld().setTime(0);
			
			broadcastAll(ChatColor.GOLD + "-------------------------------------");
			broadcastAll(ChatColor.GOLD + "The Manhunt game is starting! The hunt begins at sundown!");
			broadcastHunted(ChatColor.RED + "You have until sunset to prepare! HURRY!");
			broadcastAll(ChatColor.GOLD + "-------------------------------------");
			
			countdown = 0;
			hunterReleaseTick = HuntedPlugin.getInstance().getWorld().getFullTime() + 12000;
			endTick = hunterReleaseTick + settings.dayLimit() * 24000;
			gameRunning = true;
			
			for (String n : hunted) {
				Player p = Bukkit.getServer().getPlayerExact(n);
				if (p == null) {
					return;
				}
				if (p.getGameMode() == GameMode.CREATIVE) {
					creative.add(p.getName());
				}
				p.setGameMode(GameMode.SURVIVAL);
				p.setFireTicks(0);
				p.setHealth(20);
				p.setFoodLevel(20);
				if (settings.loadouts()) preyLoadout(p.getInventory());
				p.teleport(settings.preySpawn);
			}
			for (String n : hunter) {
				Player p = Bukkit.getServer().getPlayerExact(n);
				if (p == null) {
					return;
				}
				if (p.getGameMode() == GameMode.CREATIVE) {
					creative.add(p.getName());
				}
				p.setGameMode(GameMode.SURVIVAL);
				p.setFireTicks(0);
				p.teleport(HuntedPlugin.getInstance().getWorld().getSpawnLocation());
				p.setHealth(20);
				p.setFoodLevel(20);
				if (settings.loadouts()) {
					clearInventory(p.getInventory());
				}
			}
			
			for (String n : spectator) {
				Player p = Bukkit.getServer().getPlayerExact(n);
				if (p == null) {
					return;
				}
				if (p.getGameMode() == GameMode.CREATIVE) {
					creative.add(p.getName());
				}
				if (settings.flyingSpectators()) {
					p.setGameMode(GameMode.CREATIVE);
				} else {
					p.setGameMode(GameMode.SURVIVAL);
				}
				clearInventory(p.getInventory());
				for (Player p2 : Bukkit.getServer().getOnlinePlayers()) {
					if (isHunter(p2) || isHunted(p2)) {
						((CraftPlayer) p2).getHandle().netServerHandler.sendPacket(new Packet29DestroyEntity(p.getEntityId()));
					}
				}
			}
			tickSched = Bukkit.getScheduler().scheduleSyncRepeatingTask(HuntedPlugin.getInstance(),
					new Runnable() {
						public void run() {
								onTick();
						}
				}, 0, 1);
		}
	}
	
	public void stop() {
		Bukkit.getScheduler().cancelTask(tickSched);
		gameRunning = false;
		hunterReleaseTick = 0;
		endTick = 0;
		countdown = 0;
		for (Player p : Bukkit.getServer().getOnlinePlayers()) {
			if (isSpectating(p)) {
				for (String n : hunter) {
					Player p2 = Bukkit.getServer().getPlayerExact(n);
					if (p2 != null) {
						((CraftPlayer) p2).getHandle().netServerHandler.sendPacket(new Packet20NamedEntitySpawn(((CraftPlayer) p).getHandle()));
					}
				}
				for (String n : hunted) {
					Player p2 = Bukkit.getServer().getPlayerExact(n);
					if (p2 != null) {
						((CraftPlayer) p2).getHandle().netServerHandler.sendPacket(new Packet20NamedEntitySpawn(((CraftPlayer) p).getHandle()));
					}
				}
				if (creative.contains(p.getName())) {
					p.setGameMode(GameMode.CREATIVE);
					creative.remove(p.getName());
				}
			}
			creative.clear();
			p.setCompassTarget(HuntedPlugin.getInstance().getWorld().getSpawnLocation());
		}
		
		if (settings.autoHunter()) {
			for (String s : spectator) {
				hunter.add(s);
				broadcastAll(ChatColor.DARK_RED + s + ChatColor.WHITE + " has joined team " + ChatColor.DARK_RED + "Hunters");
			}
			spectator.clear();
		}
		locator.clear();
	}
	
	public void onDie(Player p) {
		onDie(p.getName());
	}
	
	public void onDie(String s) {
		if (hunter.contains(s.toLowerCase())) {
			hunter.remove(s.toLowerCase());
			Bukkit.getPlayerExact(s).teleport(settings.hunterSpawn);
			if (hunter.size() == 0) {
				broadcastAll(ChatColor.GOLD + "-------------------------------------");
				broadcastAll(ChatColor.GOLD + "All " + ChatColor.DARK_RED + "Hunters" + ChatColor.GOLD + " are now dead! The " + ChatColor.BLUE + "Prey" + ChatColor.GOLD + " win!");
				broadcastAll(ChatColor.GOLD + "-------------------------------------");
				spectator.add(s.toLowerCase());
				stop();
			} else {
				Bukkit.getPlayerExact(s).sendMessage(ChatColor.GRAY + "You are now a " + ChatColor.YELLOW + "spectator.");
				if (settings.flyingSpectators()) Bukkit.getPlayerExact(s).setGameMode(GameMode.CREATIVE);
				spectator.add(s.toLowerCase());
			}
		} else if (hunted.contains(s.toLowerCase())) {
			hunted.remove(s.toLowerCase());
			if (hunted.size() == 0) {
				broadcastAll(ChatColor.GOLD + "-------------------------------------");
				broadcastAll(ChatColor.GOLD + "All of the " + ChatColor.BLUE + "Prey" + ChatColor.GOLD + " are now dead! The " + ChatColor.DARK_RED + "Hunters" + ChatColor.GOLD +" win!");
				broadcastAll(ChatColor.GOLD + "-------------------------------------");
				spectator.add(s.toLowerCase());
				stop();
			} else {
				Bukkit.getPlayerExact(s).sendMessage(ChatColor.GRAY + "You are now a " + ChatColor.YELLOW + "spectator.");
				if (settings.flyingSpectators()) Bukkit.getPlayerExact(s).setGameMode(GameMode.CREATIVE);
				spectator.add(s.toLowerCase());
			}
		}
	}
	
	private void deleteUser(String s) {
		if (hunter.contains(s))
			hunter.remove(s);
		else if (hunted.contains(s))
			hunted.remove(s);
		else if (spectator.contains(s))
			spectator.remove(s);
		if (timeout.containsKey(s)) {
			timeout.remove(s);
		}
	}
	
	private void timeout(String p) {
		if (Bukkit.getPlayerExact(p) != null) {
			if (Bukkit.getPlayerExact(p).getWorld() == HuntedPlugin.getInstance().getWorld()) {
				if (timeout.containsKey(p)) {
					timeout.remove(p);
				}
				return;
			}
		}
		if (hunter.contains(p)) {
			hunter.remove(p);
			broadcastAll(getColor(p) + p + ChatColor.WHITE + " was disqualified for not being online/in the manhunt world!");
			if (Bukkit.getPlayerExact(p) != null) {
				if (Bukkit.getPlayerExact(p) != null) {
					Bukkit.getPlayerExact(p).sendMessage(ChatColor.RED + "You were disqualified from the manhunt game!");
				}
			}
		} else if (hunted.contains(p)) {
			hunted.remove(p);
			broadcastAll(getColor(p) + p + ChatColor.WHITE + " was disqualified for not being online/in the manhunt world!");
			if (Bukkit.getPlayerExact(p) != null) {
				Bukkit.getPlayerExact(p).sendMessage(ChatColor.RED + "You were disqualified from the manhunt game!");
			}
		}
		if (timeout.containsKey(p)) {
			timeout.remove(p);
		}
		onDie(p);
	}
	
	private void onTick() {
		for (String p : timeout.keySet()) {
			if (new Date().getTime() >= timeout.get(p)) {
				timeout(p);
			}
		}
		
		if (gameRunning) {
			long tick = HuntedPlugin.getInstance().getWorld().getFullTime();
			
			manageLocators();
			
			if (settings.northCompass()) {
				for (Player p : Bukkit.getOnlinePlayers()) {
					if (p.getWorld() == HuntedPlugin.getInstance().getWorld()
							&& (isHunter(p) || isHunted(p) || isSpectating(p))) {
						p.setCompassTarget(new Location(
								p.getWorld(),
								p.getLocation().getX(),
								p.getLocation().getY(),
								p.getLocation().getZ()-1000));
					}
				}
			}
			
			for (String n : spectator) {
				Player p = Bukkit.getServer().getPlayerExact(n);
				for (Player p2 : Bukkit.getServer().getOnlinePlayers()) {
					if (isHunter(p2) || isHunted(p2)) { //Makes spectators invisible!
						((CraftPlayer) p2).getHandle().netServerHandler.sendPacket(new Packet29DestroyEntity(p.getEntityId()));
					}
				}
			}
			
			if (hunterReleaseTick != 0 && tick < hunterReleaseTick) {
				int min;
				int sec;
				long totalsec = (hunterReleaseTick - tick) / 20;
				min = (int) (totalsec / 60);
				sec = (int) (totalsec % 60);
				if (HuntedPlugin.getInstance().spoutEnabled) {
					for (String s : hunter) {
						Player p = Bukkit.getPlayerExact(s);
						if (p != null) {
							HuntedPlugin.getInstance().spoutConnect.showTime("Time until sundown", min, sec, p);
						}
					}
					for (String s : hunted) {
						Player p = Bukkit.getPlayerExact(s);
						if (p != null) {
							HuntedPlugin.getInstance().spoutConnect.showTime("Time until sundown", min, sec, p);
						}
					}
					for (String s : spectator) {
						Player p = Bukkit.getPlayerExact(s);
						if (p != null) {
							HuntedPlugin.getInstance().spoutConnect.showTime("Time until sundown", min, sec, p);
						}
					}
				}
				if (tick < hunterReleaseTick) {
					if (tick >= hunterReleaseTick - 6000 && countdown == 0) {
						broadcastAll(ChatColor.GOLD + "5 minutes until sundown!");
						countdown=1;
					} else if (tick >= hunterReleaseTick - 4800 && countdown == 1) {
						broadcastAll(ChatColor.GOLD + "4 minutes until sundown!");
						countdown++;
					} else if (tick >= hunterReleaseTick - 3600 && countdown == 2) {
						broadcastAll(ChatColor.GOLD + "3 minutes until sundown!");
						countdown++;
					} else if (tick >= hunterReleaseTick - 2400 && countdown == 3) {
						broadcastAll(ChatColor.GOLD + "2 minutes until sundown!");
						countdown++;
					} else if (tick >= hunterReleaseTick - 1200 && countdown == 4) {
						broadcastAll(ChatColor.GOLD + "1 minute until sundown!");
						countdown++;
					} else if (tick >= hunterReleaseTick - 600 && countdown == 5) {
						broadcastAll(ChatColor.GOLD + "30 seconds until sundown!");
						countdown++;
					} else if (tick >= hunterReleaseTick - 200 && countdown == 6) {
						broadcastAll(ChatColor.GOLD + "10 seconds until sundown!");
						countdown++;
					} else if (tick >= hunterReleaseTick - 100 && countdown == 7) {
						broadcastAll(ChatColor.GOLD + "5...");
						countdown++;
					} else if (tick >= hunterReleaseTick - 80 && countdown == 8) {
						broadcastAll(ChatColor.GOLD + "4...");
						countdown++;
					} else if (tick >= hunterReleaseTick - 60 && countdown == 9) {
						broadcastAll(ChatColor.GOLD + "3...");
						countdown++;
					} else if (tick >= hunterReleaseTick - 40 && countdown == 10) {
						broadcastAll(ChatColor.GOLD + "2...");
						countdown++;
					} else if (tick >= hunterReleaseTick - 20 && countdown == 11) {
						broadcastAll(ChatColor.GOLD + "1...");
						countdown++;
					}
				}
			} else if (tick >= hunterReleaseTick && hunterReleaseTick != 0) {
				hunterReleaseTick = 0;
				for (String s : hunter) {
					Player p = Bukkit.getPlayerExact(s);
					if (p != null) {
						if (settings.loadouts()) hunterLoadout(p.getInventory());
						p.teleport(settings.hunterSpawn);
						p.setHealth(20);
						p.setFoodLevel(20);
					}
				}
				broadcastAll(ChatColor.GOLD + "-------------------------------------");
				broadcastAll(ChatColor.GOLD + "The hun begun! The " + ChatColor.DARK_RED + "Hunters" + ChatColor.GOLD + " are on the move!");
				broadcastAll(ChatColor.GOLD + "-------------------------------------");
				endTick = tick + settings.dayLimit()* 24000;
			} else if (tick >= endTick) {
				broadcastAll(ChatColor.GREEN + "Time has run out! The hunted have won the game!");
				stop();
			} else {
				int hour;
				int min;
				int sec;
				long totalsec = (endTick - tick) / 20;
				hour = (int) (totalsec / 3600);
				min = (int) (totalsec / 60);
				if (min > 60 && hour > 0) {
					min = min-((int) Math.floor(min/60))*60;
				}
				sec = (int) (totalsec % 60);
				if (HuntedPlugin.getInstance().spoutEnabled) {
					for (String s : hunter) {
						Player p = Bukkit.getPlayerExact(s);
						if (p != null) {
							HuntedPlugin.getInstance().spoutConnect.showTime("Time until the game is over", hour, min, sec, p);
						}
					}
					for (String s : hunted) {
						Player p = Bukkit.getPlayerExact(s);
						if (p != null) {
							HuntedPlugin.getInstance().spoutConnect.showTime("Time until the game is over", hour, min, sec, p);
						}
					}
					for (String s : spectator) {
						Player p = Bukkit.getPlayerExact(s);
						if (p != null) {
							HuntedPlugin.getInstance().spoutConnect.showTime("Time until the game is over", hour, min, sec, p);
						}
					}
				}
				if (tick > hunterReleaseTick && tick <= endTick) {
					if (tick >= endTick - 24000 && countdown == 0) {
						broadcastAll(ChatColor.GOLD + "1 day until the game is over!");
						countdown++;
					} else if (tick >= endTick - 12000 && countdown == 1) {
						broadcastAll(ChatColor.GOLD + "Game ends at sundown!");
						countdown++;
					} else if (tick >= endTick - 6000 && countdown == 2) {
						broadcastAll(ChatColor.GOLD + "5 minutes until the game is over!");
						countdown++;
					} else if (tick >= endTick - 4800 && countdown == 3) {
						broadcastAll(ChatColor.GOLD + "4 minutes until the game is over!");
						countdown++;
					} else if (tick >= endTick - 3600 && countdown == 4) {
						broadcastAll(ChatColor.GOLD + "3 minutes until the game is over!");
						countdown++;
					} else if (tick >= endTick - 2400 && countdown == 5) {
						broadcastAll(ChatColor.GOLD + "2 minutes until the game is over!");
						countdown++;
					} else if (tick >= endTick - 1200 && countdown == 6) {
						broadcastAll(ChatColor.GOLD + "1 minute until the game is over!");
						countdown++;
					} else if (tick >= endTick - 600 && countdown == 7) {
						broadcastAll(ChatColor.GOLD + "30 seconds until the game is over!");
						countdown++;
					} else if (tick >= endTick - 200 && countdown == 8) {
						broadcastAll(ChatColor.GOLD + "10 seconds until the game is over!");
						countdown++;
					} else if (tick >= endTick - 100 && countdown == 9) {
						broadcastAll(ChatColor.GOLD + "5...");
						countdown++;
					} else if (tick >= endTick - 80 && countdown == 10) {
						broadcastAll(ChatColor.GOLD + "4...");
						countdown++;
					} else if (tick >= endTick - 60 && countdown == 11) {
						broadcastAll(ChatColor.GOLD + "3...");
						countdown++;
					} else if (tick >= endTick - 40 && countdown == 12) {
						broadcastAll(ChatColor.GOLD + "2...");
						countdown++;
					} else if (tick >= endTick - 20 && countdown == 13) {
						broadcastAll(ChatColor.GOLD + "1...");
						countdown++;
					}
				}
			}
		}
	}
	
	public void onLogin(Player p) {
		if (gameHasBegun() && (isHunter(p) || isHunted(p))) {
			broadcastAll(getColor(p) + p.getName() +
					ChatColor.WHITE + " has reconnected and is back in the game!");
			timeout.remove(p);
		} else if (gameHasBegun()) {
			addSpectator(p);
			if (p.getGameMode() == GameMode.CREATIVE) {
				if (!creative.contains(p.getName())) creative.add(p.getName());
			}
			if (settings.flyingSpectators()) {
				p.setGameMode(GameMode.CREATIVE);
			} else {
				p.setGameMode(GameMode.SURVIVAL);
			}
		} else if (!gameHasBegun()) {
			p.setCompassTarget(HuntedPlugin.getInstance().getWorld().getSpawnLocation());
			addHunter(p);
		}
	}
	
	public void onLogout(Player p) {
		if (creative.contains(p.getName())) {
			p.setGameMode(GameMode.CREATIVE);
			creative.remove(p.getName());
		} else {
			p.setGameMode(GameMode.SURVIVAL);
			creative.remove(p.getName());
		}
		if (isHunter(p) || isHunted(p)) {
			if (gameHasBegun() && settings.offlineTimeout() >= 0) {
				broadcastAll(getColor(p) + p.getName() + ChatColor.WHITE +
						" has disconnected.");
				if (settings.offlineTimeout() > 0) {
					broadcastAll(ChatColor.WHITE + "They will be removed from the game in " + ChatColor.RED +
							(settings.offlineTimeout()) + " MINUTES.");
					timeout.put(p.getName(), new Date().getTime() + settings.offlineTimeout() * 72000);
				} else if (settings.offlineTimeout() == 0) {
					broadcastAll(ChatColor.WHITE + "They are now disqualified from the game.");
					p.sendMessage(ChatColor.RED + "You were disqualified from the manhunt game!");
					onDie(p.getName());
				}
			}
		} else if (isSpectating(p)) {
			deleteUser(p.getName().toLowerCase());
			broadcastAll(ChatColor.YELLOW + p.getName() + ChatColor.WHITE +
					" is no longer spectating.");
		}
		if (getLocatorByPlayer(p) != -1) { //PLAYER IS IN LOCATOR LIST
			stopLocator(p);
			}
	}
	
	public void broadcastAll(String msg) {
		broadcastHunters(msg);
		broadcastHunted(msg);
		broadcastSpectators(msg);
		HuntedPlugin.getInstance().log(Level.INFO, msg);
	}
	
	public void broadcastHunters(String msg) {
		for (String n : hunter) {
			Player p = Bukkit.getServer().getPlayerExact(n);
			if (p != null) {
				p.sendMessage(msg);
			}
		}
	}
	
	public void broadcastHunted(String msg) {
		for (String n : hunted) {
			Player p = Bukkit.getServer().getPlayerExact(n);
			if (p != null) {
				p.sendMessage(msg);
			}
		}
	}
	
	public void broadcastSpectators(String msg) {
		for (String n : spectator) {
			Player p = Bukkit.getServer().getPlayerExact(n);
			if (p != null) {
				p.sendMessage(msg);
			}
		}
	}
		
	public boolean gameHasBegun() {
		return gameRunning;
	}
	
	public boolean huntHasBegun() {
		if (gameRunning && hunterReleaseTick == 0) { 
			return true;
		} else return false;
	}
	
	public long getTick() {
		return HuntedPlugin.getInstance().getWorld().getFullTime();
	}
	public long getHunterReleaseTick() {
		return hunterReleaseTick;
	}
	public long getEndTick() {
		return endTick;
	}
	
	public boolean isHunter(Player p) {
		return isHunter(p.getName());
	}
	
	public boolean isHunted(Player p) {
		return isHunted(p.getName());
	}
	
	public boolean isSpectating(Player p) {
		return isSpectating(p.getName());
	}
	
	public boolean isCreative(Player p) {
		return isCreative(p.getName());
	}
	
	public boolean isHunter(String p) {
		for (String s : hunter) {
			if (s.equalsIgnoreCase(p)) return true;
		}
		return false;
	}
	
	public boolean isHunted(String p) {
		for (String s : hunted) {
			if (s.equalsIgnoreCase(p)) return true;
		}
		return false;
	}
	
	public boolean isSpectating(String p) {
		for (String s : spectator) {
			if (s.equalsIgnoreCase(p)) return true;
		}
		return false;
	}
	
	public boolean isCreative(String p) {
		for (String s : creative) {
			if (s.equalsIgnoreCase(p)) return true;
		}
		return false;
	}
	
	public void addHunter(Player p) {
		addHunter(p.getName());
	}
	
	public void addHunted(Player p) {
		addHunted(p.getName());
	}
	
	public void addSpectator(Player p) {
		addSpectator(p.getName());
	}
	
	public void addHunter(String p) {
		if (Bukkit.getPlayerExact(p) == null) {
			return;
		} else {
			p = Bukkit.getPlayerExact(p).getName();
		}
		if (hunter.contains(p)) {
			hunter.remove(p);
		}
		if (hunted.contains(p)) {
			hunted.remove(p);
		}
		if (spectator.contains(p)) {
			spectator.remove(p);
		}
		hunter.add(p);
	}
	
	public void addHunted(String p) {
		if (Bukkit.getPlayerExact(p) == null) {
			return;
		} else {
			p = Bukkit.getPlayerExact(p).getName();
		}
		if (hunter.contains(p)) {
			hunter.remove(p);
		}
		if (hunted.contains(p)) {
			hunted.remove(p);
		}
		if (spectator.contains(p)) {
			spectator.remove(p);
		}
		hunted.add(p);
	}
	
	public void addSpectator(String p) {
		if (Bukkit.getPlayerExact(p) == null) {
			return;
		} else {
			p = Bukkit.getPlayerExact(p).getName();
		}
		if (hunter.contains(p)) {
			hunter.remove(p);
		}
		if (hunted.contains(p)) {
			hunted.remove(p);
		}
		if (spectator.contains(p)) {
			spectator.remove(p);
		}
		spectator.add(p);
	}
	
	public void deletePlayer(String p) {
		for (int i=0 ; i<hunter.size() ; i++) {
			if (p.equalsIgnoreCase(hunter.get(i))) {
				hunter.remove(i);
			}
		}
		for (int i=0 ; i<hunted.size() ; i++) {
			if (p.equalsIgnoreCase(hunted.get(i))) {
				hunted.remove(i);
			}
		}
		for (int i=0 ; i<spectator.size() ; i++) {
			if (p.equalsIgnoreCase(spectator.get(i))) {
				spectator.remove(i);
			}
		}
		for (String i : timeout.keySet()) {
			if (i.equalsIgnoreCase(p)) {
				timeout.remove(i);
			}
		}
	}
	
	public void quit(Player p) {
		if (isHunter(p) || isHunted(p)) {
			onDie(p);
			broadcastAll(getColor(p) + p.getName() + ChatColor.WHITE +
					" has quit the game and is now spectating.");
		}
	}
	
	public ChatColor getColor(Player p) {
		if (p == null) {
			return ChatColor.YELLOW;
		} else if (isHunter(p)) {
			return ChatColor.DARK_RED;
		} else if (isHunted(p)) {
			return ChatColor.BLUE;
		} else return ChatColor.YELLOW;
	}
	
	public ChatColor getColor(String p) {
		return getColor(Bukkit.getPlayerExact(p));
	}
	
	public List<String> getHunters() {
		return hunter;
	}
	
	public List<String> getHunted() {
		return hunted;
	}
	
	public List<String> getSpectators() {
		return spectator;
	}
	
	public List<String> getCreative() {
		return creative;
	}
	
	public int HuntersAmount() {
		int num = hunter.size();
		return num;
	}
	
	public int HuntedAmount() {
		int num = hunted.size();
		return num;
	}
	
	public int SpectatorsAmount() {
		int num = spectator.size();
		return num;
	}
	
	public double getDistance(double x1, double y1, double z1, double x2, double y2, double z2) {
		return Math.sqrt( Math.pow((x2-x1),2) + Math.pow((z2-z1),2) );
	}
	
	public double getDistance(Location loc1, Location loc2) {
		return getDistance(loc1.getX(), loc1.getY(), loc1.getZ(), loc2.getX(), loc2.getZ(), loc2.getZ());
	}
	
	public double getDistance(Player p1, Player p2) {
		return getDistance(p1.getLocation(), p2.getLocation());
	}
	
	public void stepPlayer(Player p, Double d, Location l) {
		double x1 = p.getLocation().getX();
		double z1 = p.getLocation().getZ();
		double x2 = l.getX();
		double z2 = l.getZ();
		Location destination = p.getLocation();
		
		destination.setX(x1+d*(x2-x1)/getDistance(x1, 0, z1, x2, 0, z2));
		destination.setZ(z1+d*(z2-z1)/getDistance(z1, 0, z1, z2, 0, z2));
		
		p.teleport(destination);
	}
	
	public void manageLocators() {
		for (int i = 0 ; i < locator.size() ; i++) {
			if (getLocatorPlayer(i).getItemInHand().getType() != Material.COMPASS) {
				getLocatorPlayer(i).sendMessage(ChatColor.RED + "You're not holding the compass! Prey locating cancelled!");
				stopLocator(i);
				return;
			}
			if (getTick() >= getLocatorTick(i)-3640 && getLocatorStage(i) == 0) {
				getLocatorPlayer(i).sendMessage(ChatColor.GOLD + "Got it!");
				setLocatorStage(i, 1);
			} else if (getTick() >= getLocatorTick(i)-3600 && getLocatorStage(i) == 1) {
				if (getLocatorPlayer(i).getFoodLevel() < 5) {
					getLocatorPlayer(i).sendMessage(ChatColor.RED + "Not enough food to fuel the Prey Locator!");
					getLocatorPlayer(i).sendMessage(ChatColor.RED + "You must not have at least two food nuggets!");
					stopLocator(i);
					return;
				}
				if (getDistance(getLocatorPlayer(i).getLocation(), getLocatorLocation(i)) > 1.5) {
					getLocatorPlayer(i).sendMessage(ChatColor.RED + "You've moved too far. Could not get direction!");
					stopLocator(i);
					return;
				}
				sendNearestPrey(getLocatorPlayer(i));
				getLocatorPlayer(i).setFoodLevel(getLocatorPlayer(i).getFoodLevel()-5);
				setLocatorStage(i, 2);
			} else if (getTick() >= getLocatorTick(i)) {
				stopLocator(getLocatorPlayer(i));
			}
		}
	}
	
	public void startLocator(Player p) {
		stopLocator(p);
		locator.add(p.getName() + "/" +
				p.getLocation().getX() + "," + p.getLocation().getY() + "," + p.getLocation().getZ() + "/" +
				(getTick()+3000) + "/" + "0");
	}
	
	public void startLocator(Player p, int i) {
		stopLocator(p);
		locator.add(p.getName() + "/" +
				p.getLocation().getX() + "," + p.getLocation().getY() + "," + p.getLocation().getZ() + "/" +
				getTick() + "/" + i);
	}
	public long getLocatorTick(int i) {
		try {
			return Long.parseLong(locator.get(i).split("/")[2]);
		} catch (Exception e) {
			return 0;
		}
	}
	public Player getLocatorPlayer(int i) {
		try {
			return Bukkit.getPlayerExact(locator.get(i).split("/")[0]);
		} catch (Exception e) {
			return null;
		}
	}
	public int getLocatorByPlayer(Player p) {
		for (int i = 0 ; i < locator.size() ; i++) {
			if (p == getLocatorPlayer(i)) {
				return i;
			}
		}
		return -1;
	}
	public Location getLocatorLocation(int i) {
		try {
			return new Location(HuntedPlugin.getInstance().getWorld(),
					Double.parseDouble( locator.get(i).split("/")[1].split(",")[0] ),
					Double.parseDouble( locator.get(i).split("/")[1].split(",")[1] ),
					Double.parseDouble( locator.get(i).split("/")[1].split(",")[2] ));
		} catch (NumberFormatException e) {
			return null;
		}
	}
	public int getLocatorStage(int i) {
		try {
			return Integer.parseInt((locator.get(i).split("/")[3]));
		} catch (Exception e) {
			return -0;
		}
	}
	public void setLocatorStage(int i, int stage) {
		String val = (locator.get(i).subSequence(0, locator.get(i).lastIndexOf("/"))).toString() + "/" + stage;
		locator.set(i, val);
	}
	public void stopLocator(Player p) {
		for (int i = 0 ; i < locator.size() ; i++) {
			if (p == getLocatorPlayer(i)) {
				locator.remove(i);
			}
		}
	}
	public void stopLocator(int i) {
		locator.remove(i);
	}
	public void sendNearestPrey(Player h) {
		Player p = null; //Closest Prey
		
		for (String prey : getHunted()) {
			if (Bukkit.getPlayerExact(prey) != null
					&& Bukkit.getPlayerExact(prey).isOnline()) {
				Player p2 = Bukkit.getPlayerExact(prey);
				
				if (p == null) {
					p = p2;
				} else {
					if (getDistance(h, p2) < getDistance(h, p)) {
						p = p2;
					}
				}
			}
		}
		String direction = "";
		
		double angle = Math.toDegrees(Math.acos((p.getLocation().getZ() - h.getLocation().getZ())/getDistance(h, p)));
					
		if (angle > 338) direction = "South";
		else if (angle > 293) direction = "South-West";
		else if (angle > 248) direction = "West";
		else if (angle > 203) direction = "North-West";
		else if (angle > 158) direction = "North";
		else if (angle > 113) direction = "North-East";
		else if (angle > 68) direction = "East";
		else if (angle > 23) direction = "South-East";
		else direction = "South";
		
		h.sendMessage(ChatColor.GOLD + "The nearest Prey is " + ChatColor.BLUE + direction + ChatColor.GOLD +" of you! (Sun rises in the " + ChatColor.BLUE + "East!)");
	}
	
	public Inventory clearInventory(Inventory inv) {
		inv.clear();
		inv.clear(36);
		inv.clear(37);
		inv.clear(38);
		inv.clear(39);
		return inv;
	}
	
	public Inventory hunterLoadout(Inventory inv) {
		clearInventory(inv);
		inv.setItem(0, new ItemStack(Material.STONE_SWORD, 1));
		inv.setItem(1, new ItemStack(Material.BOW, 1));
		//inv.setItem(2, new ItemStack(Material.STONE_PICKAXE, 1));
		//inv.setItem(3, new ItemStack(Material.STONE_SPADE, 1));
		//inv.setItem(4, new ItemStack(Material.STONE_AXE, 1));
		inv.setItem(2, new ItemStack(Material.TORCH, 3));
		inv.setItem(3, new ItemStack(Material.COOKED_CHICKEN, 3));
		inv.setItem(4, new ItemStack(Material.ARROW, 64));
		if (settings.preyFinder()) {
			inv.setItem(5, new ItemStack(Material.COMPASS, 1));
		}
		inv.setItem(36, new ItemStack(Material.LEATHER_BOOTS, 1));
		inv.setItem(37, new ItemStack(Material.LEATHER_LEGGINGS, 1));
		inv.setItem(38, new ItemStack(Material.LEATHER_CHESTPLATE, 1));
		if (settings.woolHats()) {
			inv.setItem(39, (new Wool(DyeColor.RED).toItemStack()));
		} else {
			inv.setItem(39, new ItemStack(Material.LEATHER_HELMET, 1));
		}
		return inv;
	}
	
	public Inventory preyLoadout(Inventory inv) {
		clearInventory(inv);
		inv.setItem(0, new ItemStack(Material.STONE_SWORD, 1));
		inv.setItem(1, new ItemStack(Material.BOW, 1));
		//inv.setItem(2, new ItemStack(Material.STONE_PICKAXE, 1));
		//inv.setItem(3, new ItemStack(Material.STONE_SPADE, 1));
		//inv.setItem(4, new ItemStack(Material.STONE_AXE, 1));
		inv.setItem(2, new ItemStack(Material.TORCH, 3));
		inv.setItem(3, new ItemStack(Material.COOKED_CHICKEN, 1));
		inv.setItem(4, new ItemStack(Material.ARROW, 64));
		//inv.setItem(36, new ItemStack(Material.LEATHER_BOOTS, 1));
		//inv.setItem(37, new ItemStack(Material.LEATHER_LEGGINGS, 1));
		//inv.setItem(38, new ItemStack(Material.LEATHER_CHESTPLATE, 1));
		if (settings.woolHats()) {
			//inv.setItem(39, (new Wool(DyeColor.BLUE).toItemStack()));
			inv.setItem(39, new ItemStack(Material.LEAVES, 1));
		} else {
			//inv.setItem(39, new ItemStack(Material.LEATHER_HELMET, 1));
		}
		return inv;
	}
	
}
