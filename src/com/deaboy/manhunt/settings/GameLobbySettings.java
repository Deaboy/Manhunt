package com.deaboy.manhunt.settings;

import com.deaboy.manhunt.game.ManhuntGame;

public class GameLobbySettings extends LobbySettings
{
	//////////////// PROPERTIES ////////////////
	public final BooleanSetting		USE_AMBER;
	public final StringSetting		GAME_CLASS;
	public final StringListSetting	MAPS;
	public final StringListSetting	HUNTER_LOADOUTS;
	public final StringListSetting	PREY_LOADOUTS;
	public final IntegerSetting 	OFFLINE_TIMEOUT;
	
	public final IntegerSetting 	TIME_INTERMISSION;
	public final BooleanSetting 	ALL_TALK;
	public final BooleanSetting 	HOSTILE_MOBS;
	public final BooleanSetting 	PASSIVE_MOBS;
	
	
	//////////////// CONSTRUCTORS ////////////////
	public GameLobbySettings()
	{
		addSetting(USE_AMBER =		new BooleanSetting("useamber", true, "Manhunt will record/restore the world with Amber.", "Manhunt will not restore the world."), true);
		addSetting(GAME_CLASS =		new StringSetting("gameclass", ManhuntGame.class.getCanonicalName(), ""), false);
		addSetting(MAPS =			new StringListSetting("maps", ""), false);
		addSetting(HUNTER_LOADOUTS=	new StringListSetting("hunterloadouts", ""), false);
		addSetting(PREY_LOADOUTS =	new StringListSetting("preyloadouts", ""), false);
		addSetting(OFFLINE_TIMEOUT =	new IntegerSetting("timeout", 30, "Seconds before players are disqualified.", "Players will be immediately disqualified."), true);
		
		addSetting(TIME_INTERMISSION =	new IntegerSetting("intermission", 3, "Minutes between Manhunt games.", ""), true);
		addSetting(ALL_TALK =		new BooleanSetting("alltalk", false, "Teams can communicate with each other.", "Teams cannot see each other's chat."), true);
		addSetting(HOSTILE_MOBS =	new BooleanSetting("hostilemobs", true, "Hostile mobs are enabled.", "Hostile mobs are disabled."), true);
		addSetting(PASSIVE_MOBS =	new BooleanSetting("passivemobs", true, "Passive mobs are enabled.", "Passive mobs are disabled."), true);
		
	}
	
}
