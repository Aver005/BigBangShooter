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
					if (arena == null) {SM(p,"�c�� �� � ����."); return true;}
					arena.kickPlayer(p);
					SM(p,"�2�� ����� �� �����. �e�� ������ ��� :(");
					return true;
				}
				
				if (count >= 2)
				{
					String name = args[1].toUpperCase();
					
					if (sub.equals("create"))
					{
						if (main.arenas.containsKey(name)) {SM(p, "�c����� � ����� ��������� ����������."); return true;}
						Arena arena = new Arena(name, p.getUniqueId());
						main.arenas.put(name, arena);
						SM(p,"�2����� �b"+name+" �2������� �������.");
						return true;
					}
					
					if (sub.equals("setspawn"))
					{
						if (!main.arenas.containsKey(name)) {SM(p, "�c����� � ����� ��������� �� ����������."); return true;}
						Arena arena = main.arenas.get(name);
						arena.setLobbyLocation(p.getLocation());
						SM(p,"�2��� ����� �b"+name+" �2�������� ������������ �����.");
						return true;
					}
					
					if (sub.equals("defendspawn"))
					{
						if (!main.arenas.containsKey(name)) {SM(p, "�c����� � ����� ��������� �� ����������."); return true;}
						ItemStack is = new ItemStack(Material.BEACON);
						ItemMeta im = is.getItemMeta();
						List<String> lore = new ArrayList<>();
						im.setDisplayName("�2�������� ������� �9�l������");
						lore.add("�f�����: �e"+name);
						im.setLore(lore);
						is.setItemMeta(im);
						p.getInventory().addItem(is);
						
						p.getInventory().addItem(new ItemStack(Material.RED_GLAZED_TERRACOTTA));
						SM(p,"�2�������� ����� ��������� ������� �9�l������ �2��� ����� �b"+name+" �2�������.");
						return true;
					}
					
					if (sub.equals("attackspawn"))
					{
						if (!main.arenas.containsKey(name)) {SM(p, "�c����� � ����� ��������� �� ����������."); return true;}
						ItemStack is = new ItemStack(Material.BEACON);
						ItemMeta im = is.getItemMeta();
						List<String> lore = new ArrayList<>();
						im.setDisplayName("�2�������� ������� �6�l�����");
						lore.add("�f�����: �e"+name);
						im.setLore(lore);
						is.setItemMeta(im);
						p.getInventory().addItem(is);
						SM(p,"�2�������� ����� ��������� ������� �6�l����� �2��� ����� �b"+name+" �2�������.");
						return true;
					}
					
					if (sub.equals("enable"))
					{
						if (!main.arenas.containsKey(name)) {SM(p, "�c����� � ����� ��������� �� ����������."); return true;}
						Arena arena = main.arenas.get(name);
						arena.isEnabled = true;
						SM(p,"�2����� �b"+name+" �2������� �l��������.");
						return true;
					}

					if (sub.equals("disable"))
					{
						if (!main.arenas.containsKey(name)) {SM(p, "�c����� � ����� ��������� �� ����������."); return true;}
						Arena arena = main.arenas.get(name);
						arena.isEnabled = false;
						SM(p,"�2����� �b"+name+" �2������� �e�l���������.");
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
