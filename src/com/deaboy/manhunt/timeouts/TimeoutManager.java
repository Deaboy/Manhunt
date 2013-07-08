package com.deaboy.manhunt.timeouts;

import java.io.Closeable;
import java.util.Date;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.deaboy.manhunt.Manhunt;
import com.deaboy.manhunt.ManhuntPlugin;
import com.deaboy.manhunt.lobby.Lobby;

/**
 * This class handles all player timeouts, including
 * automatically forfeiting them.
 * @author Deaboy
 *
 */
public class TimeoutManager
{
	//---------------- Properties ----------------//
	private HashMap<String, Timeout> timeouts;
	
	
	//--------------- Constructor ----------------//
	public TimeoutManager()
	{
		this.timeouts = new HashMap<String, Timeout>();
	}
	
	
	//---------------- Functions ----------------//
	public void startTimeout(Player p)
	{
		if (p != null)
			startTimeout(p.getName());
	}
	public void startTimeout(String name)
	{
		if (timeouts.containsKey(name))
		{
			cancelTimeout(name);
		}
		
		timeouts.put(name, new Timeout(name));
	}
	public boolean containsTimeout(Player p)
	{
		return containsTimeout(p.getName());
	}
	public boolean containsTimeout(String name)
	{
		return timeouts.containsKey(name);
	}
	
	/**
	 * Cancels a player's timeout if it exists.
	 * @param p
	 */
	public void cancelTimeout(Player p)
	{
		cancelTimeout(p.getName());
	}
	/**
	 * Cancels a player's timeout if it exists.
	 * @param name
	 */
	public void cancelTimeout(String name)
	{
		if (!timeouts.containsKey(name))
			return;
		
		timeouts.get(name).close();
	}
	/**
	 * Stops all timeouts.
	 */
	public void cancelAllTimeouts()
	{
		for (Timeout t : timeouts.values())
		{
			t.close();
		}
		timeouts.clear();
	}
	
	
	
	public class Timeout implements Closeable
	{
		private final String player_name;
		private final long lobby_id;
		private final long forfeit_time;
		private boolean forfeited;
		private final long remove_time;
		private final int schedule;

		public Timeout(String player_name)
		{
			if (player_name == null)
				throw new IllegalArgumentException("Argument is null.");
			
			this.player_name = player_name;
			if (Manhunt.getPlayerLobby(player_name) != null)
			{
				this.lobby_id = Manhunt.getPlayerLobby(player_name).getId();
				this.forfeit_time = Manhunt.getPlayerLobby(player_name).getSettings().OFFLINE_TIMEOUT.getValue() * 1000;
				this.forfeited = false;
			}
			else
			{
				this.lobby_id = 0L;
				this.forfeit_time = 0L;
				this.forfeited = true;
			}
			this.remove_time = forfeit_time + 3 * 60 * 1000;
			
			
			//Start the scheduler
			this.schedule = Bukkit.getScheduler().runTaskTimer(ManhuntPlugin.getInstance(), new Runnable()
			{
				public void run()
				{
					step();
				}
			}, 0, 20).getTaskId();
		}
		
		public void step()
		{
			if (!forfeited && new Date().getTime() >= forfeit_time)
			{
				forfeitPlayer();
			}
			if (new Date().getTime() >= remove_time)
			{
				removePlayer();
				cancelTimeout(player_name);
			}
		}
		private void forfeitPlayer()
		{
			Lobby lobby = Manhunt.getLobby(lobby_id);
			lobby.forfeitPlayer(player_name);
			forfeited = true;
		}
		private void removePlayer()
		{
			Manhunt.forgetPlayer(player_name);
			cancelTimeout(player_name);
		}
		@Override
		public void close()
		{
			Bukkit.getScheduler().cancelTask(schedule);
		}
	}
	
}



