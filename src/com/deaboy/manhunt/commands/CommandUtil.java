package com.deaboy.manhunt.commands;

import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.deaboy.manhunt.Manhunt;
import com.deaboy.manhunt.lobby.Lobby;
import com.deaboy.manhunt.map.Map;
import com.deaboy.manhunt.map.ZoneFlag;

public class CommandUtil
{
	//////// STANDARD COMMAND ERROR MESSAGES ////////
	public static final String NO_PERMISSION = ChatColor.RED + "You don't have permission to do that.";
	public static final String GAME_RUNNING = ChatColor.RED + "You can't do that while the game is running.";
	public static final String NO_GAME_RUNNING = ChatColor.RED + "There are no Manhunt games running.";
	public static final String IS_SERVER = ChatColor.RED + "The server cannot do that.";
	public static final String WRONG_WORLD = ChatColor.RED + "The player must be in the manhunt world first!";
	public static final String LOCKED = ChatColor.RED + "The teams are locked.";
	public static final String INVALID_USAGE = ChatColor.RED + "Invalid usage.";
	public static final String NO_WORLD_BY_NAME = ChatColor.RED + "There is no Manhunt world by that name!";
	
	
	
	
	
	//////// COMMAND TEMPLATES ////////
	// Arguments
	public static final ArgumentTemplate arg_select	= new ArgumentTemplate("select", ArgumentType.TEXT).addAlias("s").addAlias("sel").finalize_();
	public static final ArgumentTemplate arg_list	= new ArgumentTemplate("list", ArgumentType.FLAG).addAlias("ls").finalize_();
	public static final ArgumentTemplate arg_page	= new ArgumentTemplate("page", ArgumentType.TEXT).addAlias("pg").addAlias("p").finalize_();
	public static final ArgumentTemplate arg_join	= new ArgumentTemplate("join", ArgumentType.TEXT).addAlias("jn").finalize_();
	public static final ArgumentTemplate arg_create	= new ArgumentTemplate("create", ArgumentType.FLAG).addAlias("cr").addAlias("new").finalize_();
	public static final ArgumentTemplate arg_delete	= new ArgumentTemplate("delete", ArgumentType.FLAG).addAlias("del").addAlias("remove").addAlias("rm").finalize_();
	public static final ArgumentTemplate arg_name	= new ArgumentTemplate("name", ArgumentType.TEXT).addAlias("n").addAlias("nm").finalize_();
	public static final ArgumentTemplate arg_close	= new ArgumentTemplate("close", ArgumentType.FLAG).finalize_();
	public static final ArgumentTemplate arg_open	= new ArgumentTemplate("open", ArgumentType.FLAG).finalize_();
	/* public static final ArgumentTemplate arg_flag_nobuild		= new ArgumentTemplate("nobuild", ArgumentType.FLAG).finalize_();
	 * public static final ArgumentTemplate arg_flag_boundary		= new ArgumentTemplate("boundary", ArgumentType.FLAG).finalize_();
	 * public static final ArgumentTemplate arg_flag_build		= new ArgumentTemplate("build", ArgumentType.FLAG).finalize_();
	 * public static final ArgumentTemplate arg_flag_nomobs		= new ArgumentTemplate("nomobs", ArgumentType.FLAG).finalize_();
	 * public static final ArgumentTemplate arg_flag_setup		= new ArgumentTemplate("setup", ArgumentType.FLAG).finalize_();  */
	public static final ArgumentTemplate arg_zoneflags		= new ArgumentTemplate("flags", ArgumentType.CHECK);
	static {
		for (ZoneFlag flag : ZoneFlag.values())
		arg_zoneflags.addParameter(flag.getName().toLowerCase());
		arg_zoneflags.finalize_();
	}
	
	
	public static final CommandTemplate cmd_mlobby	= new CommandTemplate("mlobby")
			.addAlias("lobby")
			.addAlias("mhlobby")
			.addArgument(arg_select)
			.addArgument(arg_list)
			.addArgument(arg_page)
			.addArgument(arg_join)
			.addArgument(arg_create)
			.addArgument(arg_delete)
			.addArgument(arg_name)
			.addArgument(arg_open)
			.addArgument(arg_close)
			.finalize_();
	
	public static final CommandTemplate cmd_mzone	= new CommandTemplate("mzone")
			.addAlias("zone")
			.addAlias("mhzone")
			.addArgument(arg_select)
			.addArgument(arg_list)
			.addArgument(arg_page)
			.addArgument(arg_create)
			.addArgument(arg_delete)
			.addArgument(arg_name)
			
			.finalize_();
	
	
	
	
	//////// PROPERTIES ////////
	private HashMap<String, String> selected_maps;
	private HashMap<String, Long> selected_lobbies;
	private HashMap<CommandSender, String> vcommands;
	private HashMap<CommandSender, Boolean> verified;
	
	
	
	//---------------- Constructors ----------------//
	public CommandUtil()
	{
		this.selected_maps = new HashMap<String, String>();
		this.selected_lobbies = new HashMap<String, Long>();
		this.vcommands = new HashMap<CommandSender, String>();
		this.verified = new HashMap<CommandSender, Boolean>();
	}
	
	
	
	//---------------- Map Selection ----------------//
	public static void setSelectedMap(CommandSender sender, Map map)
	{
		Manhunt.getCommandUtil().selected_maps.put(sender.getName(), map.getFullName());
	}
	
	public static Map getSelectedMap(CommandSender sender)
	{
		if (Manhunt.getCommandUtil().selected_maps.containsKey(sender.getName()))
			return Manhunt.getMap(Manhunt.getCommandUtil().selected_maps.get(sender.getName()));
		else
			return null;
	}
	
	public static Map getSelectedMap(String name)
	{
		if (Manhunt.getCommandUtil().selected_maps.containsKey(name))
			return Manhunt.getMap(Manhunt.getCommandUtil().selected_maps.get(name));
		else
			return null;
	}
	
	
	
	//---------------- Lobby Selection ----------------//
	public static void setSelectedLobby(CommandSender sender, Lobby lobby)
	{
		Manhunt.getCommandUtil().selected_lobbies.put(sender.getName(), lobby.getId());
	}
	
	public static Lobby getSelectedLobby(CommandSender sender)
	{
		return getSelectedLobby(sender.getName());
	}
	
	public static Lobby getSelectedLobby(String name)
	{
		if (Manhunt.getCommandUtil().selected_lobbies.containsKey(name))
			return Manhunt.getLobby(Manhunt.getCommandUtil().selected_lobbies.get(name));
		else
			return null;
	}
	
	
	
	//---------------- Verification ----------------//
	public static void addVerifyCommand(CommandSender sender, String command, String message)
	{
		addVerifyCommand(sender, command);
		sender.sendMessage(message);
		sender.sendMessage(ChatColor.GRAY + "To confirm, type \"/mverify\" or \"/cancel\"");
	}
	
	public static void addVerifyCommand(CommandSender sender, String command)
	{
		Manhunt.getCommandUtil().vcommands.put(sender, command);
		Manhunt.getCommandUtil().verified.put(sender, false);
	}
	
	public static boolean isVerified(CommandSender sender)
	{
		if (isVerifying(sender))
			return Manhunt.getCommandUtil().verified.get(sender);
		else
			return false;
	}
	
	public static boolean isVerifying(CommandSender sender)
	{
		return (Manhunt.getCommandUtil().verified.containsKey(sender)
				&& Manhunt.getCommandUtil().vcommands.containsKey(sender));
	}
	
	public static void executeCommand(CommandSender sender)
	{
		if (isVerifying(sender))
		{
			Manhunt.getCommandUtil().verified.put(sender,true);
			Bukkit.dispatchCommand(sender, Manhunt.getCommandUtil().vcommands.get(sender));
			cancelCommand(sender);
		}
	}
	
	public static void cancelCommand(CommandSender sender)
	{
		if (Manhunt.getCommandUtil().vcommands.containsKey(sender))
			Manhunt.getCommandUtil().vcommands.remove(sender);
		if (Manhunt.getCommandUtil().verified.containsKey(sender))
			Manhunt.getCommandUtil().verified.remove(sender);
	}
	
	public void deletePlayer(Player player)
	{
		if (selected_lobbies.containsKey(player.getName()))
			selected_lobbies.remove(player.getName());
		if (selected_maps.containsKey(player.getName()))
			selected_maps.remove(player.getName());
		if (vcommands.containsKey(player.getName()))
			vcommands.remove(player.getName());
		if (verified.containsKey(player.getName()))
			verified.remove(player.getName());
	}
	
	
	
}
