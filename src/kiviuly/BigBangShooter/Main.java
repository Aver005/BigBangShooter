package kiviuly.BigBangShooter;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin
{
	public HashMap<String, Arena> arenas = new HashMap<>();
	public HashMap<UUID, Arena> players = new HashMap<>();
	
	private static Main main;
	
	@Override
	public void onEnable() 
	{
		main = this;
		
		// Events
		getServer().getPluginManager().registerEvents(new Events(this), this);
		
		// Commands
		getCommand("bbs").setExecutor(new Commands(this));
	}
	
	// Methods
	
	public boolean isPlayerInGame(Player pl) {return players.containsKey(pl.getUniqueId());}
	public Arena getPlayerArena(Player pl) {return players.getOrDefault(pl.getUniqueId(), null);}
	
	public String removeCC(String s) {return ChatColor.stripColor(s);}
	public static Main getInstance() {return main;}
}
