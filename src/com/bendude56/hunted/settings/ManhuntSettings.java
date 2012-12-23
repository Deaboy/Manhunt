package com.bendude56.hunted.settings;

import org.bukkit.Bukkit;

public class ManhuntSettings extends SettingManagerBase implements SettingManager
{
	private static final long serialVersionUID = -2749093656484939858L;
	
	public final SettingListString WORLDS;

	public final SettingBoolean HANDLE_CHAT;
	
	public final SettingInteger TIME_LIMIT;
	public final SettingInteger TIME_INTERMISSION;
	public final SettingInteger TIME_SETUP;  
	
	public ManhuntSettings( String filepath )
	{
		super( filepath );
		
		addSetting(WORLDS =		new SettingListString("worlds", "The list of Worlds Manhunt will run in.", Bukkit.getWorlds().get(0).getName()), false);
		
		addSetting(HANDLE_CHAT =	new SettingBoolean("handlechat", true, "Manhunt will handle chat events.", "Manhunt will ignore chat events."), true);
		
		addSetting(TIME_LIMIT =			new SettingInteger("timelimit", 60, "Minutes that the hunt will last.", "The game will never end."), true );
		addSetting(TIME_INTERMISSION =	new SettingInteger("intermission", 3, "Minutes between Manhunt games.", ""), true);
		addSetting(TIME_SETUP =			new SettingInteger("setuptime", 10, "Minutes the prey have to prepare.", "There is no setup time."), true);
		
	}
	
}
