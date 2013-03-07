package com.deaboy.manhunt.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import com.deaboy.manhunt.Manhunt;
import com.deaboy.manhunt.map.World;
import com.deaboy.manhunt.lobby.Lobby;
import com.deaboy.manhunt.lobby.LobbyType;

public abstract class LobbyCommands
{
	
	public static boolean mstartgame(CommandSender sender, String[] args)
	{
		boolean console;
		Lobby lobby;
		
		if (!sender.isOp())
		{
			sender.sendMessage(CommandUtil.NO_PERMISSION);
			return true;
		}
		
		console = sender instanceof ConsoleCommandSender;
		
		if (console && args.length == 0)
		{
			sender.sendMessage(CommandUtil.IS_SERVER);
			return false;
		}
		
		if (args.length > 1)
		{
			return false;
		}
		
		if (args.length == 0)
			lobby = Manhunt.getPlayerLobby((Player) sender);
		else if (args.length == 1)
			lobby = Manhunt.getLobby(args[0]);
		else
			lobby = null;
		
		
		
		if (lobby == null && args.length > 0)
		{
			sender.sendMessage((console ? "" : ChatColor.RED) + "The lobby " + args[0] + " does not exist.");
			return false;
		}
		else if (lobby == null)
		{
			sender.sendMessage(ChatColor.RED + "You are not in a Manhunt game lobby.");
			return true;
		}
		else if (lobby.getType() != LobbyType.GAME)
		{
			sender.sendMessage((console ? "" : ChatColor.RED) + "The lobby " + (args.length > 0 ? args[0] : "you are in") + " is not a game lobby");
			return true;
		}
		else if (!lobby.isEnabled())
		{
			sender.sendMessage((console ? "" : ChatColor.RED) + "The lobby " + (args.length > 0 ? args[0] : "you are in") + " is closed.");
			return true;
		}
		else if (lobby.gameIsRunning())
		{
			sender.sendMessage((console ? "" : ChatColor.RED) + "A game is already running.");
			return true;
		}
		else
		{
			sender.sendMessage((console ? "" : ChatColor.GREEN) + "Game successfully started.");
			lobby.startGame();
			return true;
		}
		
		
		
	}
	
	public static boolean mstopgame(CommandSender sender, String[] args)
	{
		boolean console;
		Lobby lobby;
		
		if (!sender.isOp())
		{
			sender.sendMessage(CommandUtil.NO_PERMISSION);
			return true;
		}
		
		console = sender instanceof ConsoleCommandSender;
		
		if (console && args.length == 0)
		{
			sender.sendMessage(CommandUtil.IS_SERVER);
			return false;
		}
		
		if (args.length > 1)
		{
			return false;
		}
		
		if (args.length == 0)
			lobby = Manhunt.getPlayerLobby((Player) sender);
		else if (args.length == 1)
			lobby = Manhunt.getLobby(args[0]);
		else
			lobby = null;
		
		
		
		if (lobby == null)
		{
			if (args.length > 0)
				sender.sendMessage((console ? "" : ChatColor.RED) + "The lobby " + args[0] + " does not exist.");
			else
				sender.sendMessage(ChatColor.RED + "You are not in a Manhunt game lobby.");
			return false;
		}
		else if (lobby.getType() != LobbyType.GAME)
		{
			sender.sendMessage((console ? "" : ChatColor.RED) + "The lobby " + (args.length > 0 ? args[0] : "you are in") + " is not a game lobby");
			return false;
		}
		else if (!lobby.gameIsRunning())
		{
			sender.sendMessage((console ? "" : ChatColor.RED) + "There are no games running.");
			return true;
		}
		else
		{
			sender.sendMessage((console ? "" : ChatColor.GREEN) + "Game successfully stopped.");
			lobby.stopGame();
			return true;
		}
		
		
		
	}
	
	public static boolean mjoin(CommandSender sender, String[] args)
	{
		if (args.length == 0)
		{
			
		}
		
		return true;
	}
	
	public static boolean mlobby(CommandSender sender, String[] args)
	{
		if (args.length == 0 || args[0].equalsIgnoreCase("help") || args[0].equalsIgnoreCase("?"))
		{
			sender.sendMessage(Bukkit.getPluginCommand("mlobby").getUsage());
			sender.sendMessage("Commands:\njoin, leave, create, delete, close, open");
			return true;
		}
		
		if (args[0].equalsIgnoreCase("create"))
		{
			if (!sender.isOp())
			{
				sender.sendMessage(CommandUtil.NO_PERMISSION);
				return true;
			}
			if (args.length != 2 && args.length != 3)
			{
				sender.sendMessage(CommandUtil.INVALID_USAGE);
				sender.sendMessage("/mlobby create <name> [world]");
				return true;
			}
			if (!(sender instanceof Player) && args.length != 3)
			{
				sender.sendMessage(CommandUtil.INVALID_USAGE);
				sender.sendMessage("/mlobby create <name> <world>");
				return true;
			}
			
			if (!CommandUtil.isVerified(sender))
			{
				String cmd = "mlobby";
				for (String s : args)
					cmd += " " + s;
				CommandUtil.addVerifyCommand(sender, cmd, "Are you sure you want to create a new lobby?");
				return true;
			}
			
			World world;
			if (args.length == 3)
				world = Manhunt.getWorld(args[2]);
			else
				world = Manhunt.getWorld(((Player) sender).getWorld());
			
			if (world == null)
			{
				sender.sendMessage(ChatColor.RED + "No world with that name is registered with Manhunt.");
				sender.sendMessage(ChatColor.GRAY + "Use \"/world add [name]\" or \"/world create [name]\" to add/create a world.");
				return true;
			}
			
			if (Manhunt.getLobby(world.getWorld()) != null)
			{
				sender.sendMessage(ChatColor.RED + "A lobby already exists in that world!");
				return true;
			}
			
			if (Manhunt.getLobby(args[1]) != null)
			{
				sender.sendMessage(ChatColor.RED + "A lobby already exists by that name!");
				return true;
			}
			
			Manhunt.createLobby(args[1], LobbyType.GAME, world.getWorld());
			sender.sendMessage("Created lobby in world " + world.getName());
			return true;
		}
		else if (args[0].equalsIgnoreCase("close"))
		{
			if (!sender.isOp())
			{
				sender.sendMessage(CommandUtil.NO_PERMISSION);
				return true;
			}
			if (sender instanceof Player && args.length != 1 && args.length != 2)
			{
				sender.sendMessage(CommandUtil.INVALID_USAGE);
				sender.sendMessage("/mlobby close [lobby]");
				return true;
			}
			if (!(sender instanceof Player) && args.length != 2)
			{
				sender.sendMessage(CommandUtil.INVALID_USAGE);
				sender.sendMessage("/mlobby close <lobby>");
				return true;
			}
			
			Lobby lobby;
			
			if (args.length == 1)
				lobby = Manhunt.getPlayerLobby((Player) sender);
			else
				lobby = Manhunt.getLobby(args[1]);
			
			if (lobby == null && args.length == 1)
			{
				sender.sendMessage(ChatColor.RED + "You are not in a Manhunt lobby.");
				return true;
			}
			else if (lobby == null && args.length == 2)
			{
				sender.sendMessage(ChatColor.RED + "There is no lobby with that name.");
				return true;
			}
			
			// TODO Stop lobby
			sender.sendMessage("Lobby closed.");
			return true;
		}
		
		return true;
	}
	
	
}
