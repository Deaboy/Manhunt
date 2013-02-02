package com.deaboy.manhunt.finder;

import java.util.Date;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.deaboy.manhunt.Manhunt;
import com.deaboy.manhunt.ManhuntUtil;
import com.deaboy.manhunt.NewManhuntPlugin;
import com.deaboy.manhunt.chat.ChatManager;
import com.deaboy.manhunt.lobby.Team;

public class Finder
{
	private static final int CHARGE_TIME = 8000; // Milliseconds
	
	private final long lobby_id;
	
	private final String player_name;
	private final Location location;
	/** REAL-LIFE TIME to ACTIVATE */
	private final Long activation_time;
	/** REAL-LIFE TIME to SELF-DESTRUCT */
	private final Long expire_time;

	private boolean used = false; //Whether or not the finder has sent the nearest enemy.

	private int schedule;

	public Finder(Player player, long lobby_id)
	{
		this.lobby_id = lobby_id;
		
		this.player_name = player.getName();
		this.location = player.getLocation();

		Date time = new Date();
		this.activation_time = time.getTime() + CHARGE_TIME;
		this.expire_time = activation_time + (1000*Manhunt.getLobby(lobby_id).getSettings().FINDER_COOLDOWN.getValue());
		
		player.sendMessage(ChatManager.bracket1_ + "Finding nearest enemy. Stand still for " + ChatColor.DARK_RED + CHARGE_TIME / 1000 + " seconds." + ChatManager.bracket2_);
		
		schedule = Bukkit.getScheduler().scheduleSyncRepeatingTask(NewManhuntPlugin.getInstance(), new Runnable()
		{
			public void run()
			{
				onTick();
			}
		}, 0, 0);
	}
	
	
	//---------------- Getters ----------------//
	/**
	 * Gets the name of the player that this finder
	 * is assigned to.
	 * @return
	 */
	public String getPlayerName()
	{
		return player_name;
	}

	/**
	 * Checks when to send a player the finder results.
	 * Checks when to shut itself down.
	 */
	public void onTick()
	{
		if (!checkValidity())
		{
			return;
		}
		
		long time = new Date().getTime();
		
		if (time < activation_time)
		{
			Player p = Bukkit.getPlayerExact(player_name);
			if (p != null && Manhunt.getSettings().CONTROL_XP.getValue())
				p.setExp((time + 8000 - activation_time) / 8000f);
		}
		else if (!used && time >= activation_time) //Should I send the player the information?
		{
			Player p = Bukkit.getPlayer(player_name);
			if (p != null)
			{
				if (p.getFoodLevel() < 4)
				{
					p.sendMessage(ChatManager.leftborder + ChatColor.RED + "You don't have enough food to power the finder!");
					Manhunt.getFinders().stopFinder(this, true);
				}
				else if (checkValidity())
				{
					activate();
				}
			}
			else
			{
				Manhunt.getFinders().stopFinder(this, true);
			}
			used = true;
		}
		else if (Manhunt.getSettings().CONTROL_XP.getValue() && time < expire_time && used)
		{
			Player p = Bukkit.getPlayer(player_name);
			if (p != null)
				p.setExp((time + (Manhunt.getLobby(lobby_id).getSettings().FINDER_COOLDOWN.getValue() * 1000f) - expire_time) / (Manhunt.getLobby(lobby_id).getSettings().FINDER_COOLDOWN.getValue() * 1000f));
		}
		else if (time >= expire_time)
		{
			Player p = Bukkit.getPlayer(player_name);
			if (p != null)
			{
				p.sendMessage(ChatManager.bracket1_ + "The " + ChatColor.DARK_RED + "Prey Finder" + ChatManager.color + " is fully charged." + ChatManager.bracket2_);
			}
			Manhunt.getFinders().stopFinder(this, true);
		}
	}

	/**
	 * Determines if a this finder is still valid based on the
	 * player's location and item held in hand. If it returns false,
	 * will self-destruct, sending the player the cancel message.
	 * Will always return true if the finder has already been used.
	 * @return True if the finder is still valid, false if not.
	 */
	public boolean checkValidity()
	{
		if (used)
		{
			return true;
		}

		Player p = Bukkit.getPlayer(player_name);

		if (p != null)
		{
			if (p.getItemInHand().getTypeId() != Manhunt.getSettings().FINDER_ITEM.getValue() || !ManhuntUtil.areEqualLocations(p.getLocation(), location, 0.0, true))
			{
				FinderUtil.sendMessageFinderCancel(p);
				Manhunt.getFinders().stopFinder(this, true);
				return false;
			}
			else
			{
				return true;
			}
		}
		else
		{
			FinderUtil.sendMessageFinderCancel(p);
			Manhunt.getFinders().stopFinder(this, true);
			return false;
		}

	}
	
	public void activate()
	{
		Team t;
		Player player, enemy = null;
		double d, distance = -1;
		
		t = Manhunt.getLobby(lobby_id).getPlayerTeam(player_name);
		player = Bukkit.getPlayerExact(player_name);
		switch (t)
		{
		case HUNTERS:
			t = Team.PREY;
			break;
		case PREY:
			t = Team.HUNTERS;
			break;
		default:
			t = null;
			break;
		}
		
		if (t == null || player == null)
		{
			if (!checkValidity())
				Manhunt.getFinders().stopFinder(this, true);
			return;
		}
		
		for (Player p : Manhunt.getLobby(lobby_id).getGame().getOnlinePlayers(t))
		{
			d = ManhuntUtil.getDistance(player.getLocation(), p.getLocation(), false);
			if (enemy == null || d < distance)
			{
				enemy = p;
				distance = d;
			}
		}
		
		if (enemy == null)
		{
			player.sendMessage(ChatManager.bracket1_ + "There are no online " + t.getColor() + t.getName(false) + ChatManager.color + "!" + ChatManager.bracket2_);
			return;
		}
		else
		{
			if (distance < 25.0)
			{
				player.sendMessage(ChatManager.bracket1_ + "The nearest " + t.getColor() + t.getName(false) + ChatManager.color + " is very close by!" + ChatManager.bracket2_);
			}
			player.setCompassTarget(enemy.getLocation().clone());
			enemy.sendMessage(ChatManager.bracket1_ + "A " + ChatColor.DARK_RED + "PreyFinder" + ChatManager.color + " has gotten your location!" + ChatManager.bracket2_);
		}
		
		player.setFoodLevel(Math.max(player.getFoodLevel() - 4, 0));
		
	}

	public void sendTimeLeft()
	{
		Player p = Bukkit.getPlayer(player_name);
		
		if (p == null)
		{
			checkValidity();
		}
		else if (used)
		{
			Date time = new Date();
			p.sendMessage(ChatManager.leftborder + "The Prey Finder is still charging. Wait for " + ChatColor.DARK_RED + (int) Math.ceil(((double) expire_time - (double) time.getTime())/(double) 1000) + " seconds.");
		}
	}

	public boolean isUsed()
	{
		return used;
	}

	protected void close()
	{
		Bukkit.getScheduler().cancelTask(schedule);
	}


}
