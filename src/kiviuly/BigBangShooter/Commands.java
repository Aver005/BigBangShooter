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
					
					if (sub.equals("setlobby"))
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
						
						ItemStack is = new ItemStack(Material.WOOL);
						ItemMeta im = is.getItemMeta();
						im.setDisplayName(name);
						is.setItemMeta(im);
						main.menuInventory.addItem(is);
						return true;
					}

					if (sub.equals("disable"))
					{
						if (!main.arenas.containsKey(name)) {SM(p, "§cАрена с таким названием НЕ существует."); return true;}
						Arena arena = main.arenas.get(name);
						arena.isEnabled = false;
						SM(p,"§2Арена §b"+name+" §2успешно §e§lвыключена.");
						
						for(ItemStack is : main.menuInventory)
						{
							if (is == null) {continue;}
							if (!is.hasItemMeta()) {continue;}
							ItemMeta meta = is.getItemMeta();
							if (!meta.hasDisplayName()) {continue;}
							if (meta.getDisplayName().equals(name)) {main.menuInventory.remove(is); return true;}
						}
						return true;
					}
					
					if (count >= 3)
					{
						if (sub.equals("addoperator"))
						{
							if (!main.arenas.containsKey(name)) {SM(p, "§cАрена с таким названием НЕ существует."); return true;}
							Arena arena = main.arenas.get(name);
							ItemStack[] iss = p.getInventory().getContents();
							ArrayList<ItemStack> isList = new ArrayList<>();
							for(ItemStack is : iss) {isList.add(is);}
							arena.getOperatorsItems().put(args[2], isList);
							p.sendMessage("§2Предметы для оперативника §b§l"+args[2]+"§2 установлены.");
							return true;
						}
						
						if (sub.equals("seticon"))
						{
							if (!main.arenas.containsKey(name)) {SM(p, "§cАрена с таким названием НЕ существует."); return true;}
							//Arena arena = main.arenas.get(name);
							ItemStack is = p.getItemInHand();
							if (is == null) {return true;}
							if (is.getType().equals(Material.AIR)) {return true;}
							main.config.set("OperatorsInfo."+args[2]+"-Icon", is);
							p.sendMessage("§2Предметы для оперативника §b§l"+args[2]+"§2 установлены.");
							return true;
						}
					}
				}
			}
			
			return true;
		}
		
		return false;
	}
	
	private void SM(Player p, String m) {p.sendMessage(m);}

}
