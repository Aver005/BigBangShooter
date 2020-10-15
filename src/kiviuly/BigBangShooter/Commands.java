package kiviuly.BigBangShooter;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class Commands implements CommandExecutor 
{
	private Main main;
	public Commands(Main m) {main = m;}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String cmd, String[] args) 
	{
		if (sender instanceof Player)
		{
			Player p = (Player) sender;
			int count = args.length;
			
			if (count == 0) {main.OpenMainMenu(p); return true;}
			
			if (count >= 1)
			{
				String sub = args[0].toLowerCase();
				
				if (sub.equals("menu")) {main.OpenMainMenu(p); return true;}
				if (sub.equals("leave"))
				{
					Arena arena = main.getPlayerArena(p);
					if (arena == null) {SM(p,"§cВы не в игре."); return true;}
					arena.kickPlayer(p);
					SM(p,"§2Вы вышли из матча. §eНе хорошо это :(");
					return true;
				}
				
				if (count >= 2)
				{
					String name = args[1].toUpperCase();
					
					if (sub.equals("create"))
					{
						if (main.arenas.containsKey(name)) {SM(p, "§cАрена с таким названием существует."); return true;}
						Arena arena = new Arena(name, p.getUniqueId());
						main.arenas.put(name, arena);
						SM(p,"§2Арена §b"+name+" §2успешно создана.");
						return true;
					}
					
					if (sub.equals("setspawn"))
					{
						if (!main.arenas.containsKey(name)) {SM(p, "§cАрена с таким названием НЕ существует."); return true;}
						Arena arena = main.arenas.get(name);
						arena.setLobbyLocation(p.getLocation());
						SM(p,"§2Для арены §b"+name+" §2изменено расположение лобби.");
						return true;
					}
					
					if (sub.equals("defendspawn"))
					{
						if (!main.arenas.containsKey(name)) {SM(p, "§cАрена с таким названием НЕ существует."); return true;}
						ItemStack is = new ItemStack(Material.BEACON);
						ItemMeta im = is.getItemMeta();
						List<String> lore = new ArrayList<>();
						im.setDisplayName("§2Отметчик спавнов §9§lЗАЩИТЫ");
						lore.add("§fАрена: §e"+name);
						im.setLore(lore);
						is.setItemMeta(im);
						p.getInventory().addItem(is);
						
						p.getInventory().addItem(new ItemStack(Material.RED_GLAZED_TERRACOTTA));
						SM(p,"§2Отметчик точек появления игроков §9§lЗАЩИТЫ §2для арены §b"+name+" §2получен.");
						return true;
					}
					
					if (sub.equals("attackspawn"))
					{
						if (!main.arenas.containsKey(name)) {SM(p, "§cАрена с таким названием НЕ существует."); return true;}
						ItemStack is = new ItemStack(Material.BEACON);
						ItemMeta im = is.getItemMeta();
						List<String> lore = new ArrayList<>();
						im.setDisplayName("§2Отметчик спавнов §6§lАТАКИ");
						lore.add("§fАрена: §e"+name);
						im.setLore(lore);
						is.setItemMeta(im);
						p.getInventory().addItem(is);
						SM(p,"§2Отметчик точек появления игроков §6§lАТАКИ §2для арены §b"+name+" §2получен.");
						return true;
					}
					
					if (sub.equals("enable"))
					{
						if (!main.arenas.containsKey(name)) {SM(p, "§cАрена с таким названием НЕ существует."); return true;}
						Arena arena = main.arenas.get(name);
						arena.isEnabled = true;
						SM(p,"§2Арена §b"+name+" §2успешно §lвключена.");
						return true;
					}

					if (sub.equals("disable"))
					{
						if (!main.arenas.containsKey(name)) {SM(p, "§cАрена с таким названием НЕ существует."); return true;}
						Arena arena = main.arenas.get(name);
						arena.isEnabled = false;
						SM(p,"§2Арена §b"+name+" §2успешно §e§lвыключена.");
						return true;
					}
					
					if (count >= 3)
					{
						
					}
				}
			}
			
			return true;
		}
		
		return false;
	}
	
	private void SM(Player p, String m) {p.sendMessage(m);}

}
