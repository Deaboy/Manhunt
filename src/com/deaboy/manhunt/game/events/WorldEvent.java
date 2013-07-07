package com.deaboy.manhunt.game.events;

import org.bukkit.World;

public interface WorldEvent extends Event
{
	//---------------- Getters ----------------//
	/**
	 * Gets the world for the event.
	 * @return The world of this event.
	 */
	public World getWorld();
	
}
