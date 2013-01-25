package com.deaboy.hunted.game.events;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.deaboy.hunted.Manhunt;
import com.deaboy.hunted.lobby.Team;
import com.deaboy.hunted.map.Spawn;

public class TeleportTeamAction implements Action
{

	private final long lobby_id;
	private final Team team;
	private final List<Location> locations;
	
	public TeleportTeamAction(long lobby_id, Team team, List<Spawn> spawns)
	{
		this.locations = new ArrayList<Location>();
		
		this.lobby_id = lobby_id;
		this.team = team;
		for (Spawn spawn : spawns)
			this.locations.add(spawn.getLocation());
		
	}
	
	@Override
	public void execute()
	{
		for (Player p : Manhunt.getLobby(lobby_id).getPlayers(team))
		{
			if (p.isOnline())
			{
				p.teleport(locations.get(((int) Math.random()) % locations.size()));
			}
		}
	}

}