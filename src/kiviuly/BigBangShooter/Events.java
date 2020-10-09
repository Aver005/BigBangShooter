package kiviuly.BigBangShooter;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class Events implements Listener 
{
	private Main main;
	public Events(Main m) {main = m;}
	
	private void SM(Player p, String m) {p.sendMessage(m);}
	
	@EventHandler
	public void onBlockPlace(BlockPlaceEvent e)
	{
		Player p = e.getPlayer();
		Arena arena = main.getPlayerArena(p);
		if (arena != null) {e.setCancelled(true); return;}
		Block b = e.getBlock();
		if (!b.getType().equals(Material.BEACON)) {return;}
		ItemStack item = e.getItemInHand();
		if (item == null) {return;}
		if (!item.hasItemMeta()) {return;}
		ItemMeta meta = item.getItemMeta();
		if (!meta.hasDisplayName()) {return;}
		String name = meta.getDisplayName();
		if (!meta.hasLore()) {return;}
		
		List<String> lore = meta.getLore();
		String arName = main.removeCC(lore.get(0).split(" : ")[1]);
		arena = main.arenas.getOrDefault(arName, null);
		if (arena == null) {return;}
		Location l = b.getLocation();
		e.setCancelled(true);
		
		if (name.endsWith("ЗАЩИТЫ")) {arena.getDefendSpawns().add(l);}
		if (name.endsWith("АТАКИ")) {arena.getAttackSpawns().add(l);}
		SM(p,"Спавн X: "+l.getBlockX()+" Y: "+l.getBlockY()+" Z: "+l.getBlockZ()+" команды "+name.split(" ")[2]+" добавлен на арену "+arName+".");
	}
	
	@EventHandler
	public void onPlayerMove(PlayerMoveEvent e)
	{
		Player p = e.getPlayer();
		Arena arena = main.getPlayerArena(p);
		if (arena == null) {return;}
		
		if (arena.getState().equals("FREEZE TIME"))
		{
			if (e.getTo().distance(e.getFrom()) >= 0.2) {e.setCancelled(true);}
		}
	}
	
	@EventHandler
	public void onPlayerDmgPlayer(EntityDamageByEntityEvent e)
	{
		if (!e.getEntityType().equals(EntityType.PLAYER)) {return;}
		Player p = (Player) e.getEntity();
		Arena arena = main.getPlayerArena(p);
		if (arena == null) {return;}
		String state = arena.getState();
		if (!state.equals("PLAYING")) {return;}
		
		if (e.getDamager().getType().equals(EntityType.PLAYER))
		{
			Player killer = (Player) e.getDamager();
			double damage = e.getDamage();
			double heath = p.getHealth();
			
			if (damage >= heath) 
			{
				e.setDamage(heath-1.0);
				arena.addKilledPlayer(p, killer);
				return;
			}
		}
		
	}
	
	@EventHandler
	public void onPlayerClickInv(InventoryClickEvent e)
	{
		if (!e.getWhoClicked().getType().equals(EntityType.PLAYER)) {return;}
		Player p = (Player) e.getWhoClicked();
		String title = e.getView().getTitle();
		
		if (title.equals("Закладка бомбы"))
		{
			e.setCancelled(true);
			Arena arena = main.getPlayerArena(p);
			if (arena == null) {p.closeInventory(); return;}
			ItemStack item = e.getCurrentItem();
			if (item == null) {p.closeInventory(); arena.isPlanting = false; return;}
			if (!item.hasItemMeta()) {p.closeInventory(); arena.isPlanting = false;  return;}
			ItemMeta meta = item.getItemMeta();
			if (!meta.hasDisplayName()) {p.closeInventory(); arena.isPlanting = false; return;}
			String itName = meta.getDisplayName();
			
			ArrayList<String> _strings = new ArrayList<>();
			_strings.add("Кладём бомбу на плент...");
			_strings.add("Включаем таймер...");
			_strings.add("Вводим код...");
			_strings.add("Вставляем провод...");
			_strings.add("Достаём бомбу...");
			
			int itemsCount = 0;
			for(ItemStack is : e.getView().getTopInventory())
			{
				if (is == null) {continue;}
				if (is.getType().equals(Material.AIR)) {continue;}
				itemsCount++;
			}
			
			if (!itName.endsWith(_strings.get(itemsCount-1))) 
			{
				arena.isPlanting = false;
				p.sendTitle("БОМБА НЕ ЗАЛОЖЕНА!", "Последовательность не верна...");
				p.closeInventory(); 
				return;
			}
			
			arena.getPlantInventory().remove(item);
			if (itemsCount > 1) {return;}
			ArrayList<UUID> list = arena.getAllPlayers();
			for(UUID plID : list)
			{
				Player pl = Bukkit.getPlayer(plID);
				if (pl == null) {list.remove(plID); continue;}
				if (!pl.isOnline()) {list.remove(plID); continue;}
				pl.sendTitle("БОМБА ЗАЛОЖЕНА", "До взрыва 45 секунд");
			}
			arena.setTime(45);
			arena.isPlanted = true;
			arena.isPlanting = false;
			p.closeInventory(); 
			return;
		}
		
		if (title.equals("Обезвреживание бомбы"))
		{
			e.setCancelled(true);
			Arena arena = main.getPlayerArena(p);
			if (arena == null) {p.closeInventory(); return;}
			ItemStack item = e.getCurrentItem();
			if (item == null) {p.closeInventory(); arena.isDefusing = false; return;}
			if (!item.hasItemMeta()) {p.closeInventory(); arena.isDefusing = false;  return;}
			ItemMeta meta = item.getItemMeta();
			if (!meta.hasDisplayName()) {p.closeInventory(); arena.isDefusing = false; return;}
			String itName = meta.getDisplayName();
			
			ArrayList<String> _strings = new ArrayList<>();
			_strings.add("8 - Кидаем бомбу в помойку...");
			_strings.add("7 - Режем провод...");
			_strings.add("6 - Достаём щипцы...");
			_strings.add("5 - Молимся...");
			_strings.add("4 - Выбираем провод...");
			_strings.add("3 - Думаем...");
			_strings.add("2 - Вскрываем...");
			_strings.add("1 - Берём бомбу...");
			
			int itemsCount = 0;
			for(ItemStack is : e.getView().getTopInventory())
			{
				if (is == null) {continue;}
				if (is.getType().equals(Material.AIR)) {continue;}
				itemsCount++;
			}
			
			if (!itName.endsWith(_strings.get(itemsCount-1))) 
			{
				arena.isPlanting = false;
				p.sendTitle("БОМБА НЕ ОБЕЗВРЕЖЕНА!", "Последовательность не верна...");
				p.closeInventory(); 
				return;
			}
			
			arena.getPlantInventory().remove(item);
			if (itemsCount > 1) {return;}
			ArrayList<UUID> list = arena.getAllPlayers();
			for(UUID plID : list)
			{
				Player pl = Bukkit.getPlayer(plID);
				if (pl == null) {list.remove(plID); continue;}
				if (!pl.isOnline()) {list.remove(plID); continue;}
				pl.sendTitle("БОМБА ОБЕЗВРЕЖЕНА", "Защитники победили!");
			}
			arena.setTime(8);
			arena.isPlanted = false;
			arena.isPlanting = false;
			arena.isDefusing = false;
			arena.setState("ROUND END");
			arena.setRound(arena.getRound()+1);
			p.closeInventory(); 
			return;
		}
		
		if (title.equals("Выбор команды"))
		{
			e.setCancelled(true);
			Arena arena = main.getPlayerArena(p);
			if (arena == null) {p.closeInventory(); return;}
			ItemStack item = e.getCurrentItem();
			if (item == null) {return;}
			if (!item.hasItemMeta()) {return;}
			ItemMeta meta = item.getItemMeta();
			if (!meta.hasDisplayName()) {return;}
			String itName = meta.getDisplayName();
			
			if (itName.equals("Команда ЗАЩИТЫ"))
			{
				if (arena.getTeamDefend().contains(p.getUniqueId())) 
				{
					p.sendTitle("Вы уже в этой команде","-_-");
					p.sendMessage("Вы уже в этой команде.");
					return;
				}
				
				arena.getTeamDefend().add(p.getUniqueId());
				p.sendTitle("Теперь вы ЗАЩИЩАЮЩИЙ","(*)_(*)");
				p.sendMessage("Вы присоединились к команде защиты.");
			}
			
			if (itName.equals("Команда АТАКИ"))
			{
				if (arena.getTeamAttack().contains(p.getUniqueId())) 
				{
					p.sendTitle("Вы уже в этой команде","-_-");
					p.sendMessage("Вы уже в этой команде.");
					return;
				}
				
				arena.getTeamAttack().add(p.getUniqueId());
				p.sendTitle("Теперь вы АТАКУЮЩИЙ","(*)_(*)");
				p.sendMessage("Вы присоединились к команде атаки.");
			}
			
			p.closeInventory();
			return;
		}
	}
	
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent e)
	{
		Player p = e.getPlayer();
		Arena arena = main.getPlayerArena(p);
		if (arena == null) {return;}
		
		ItemStack item = p.getItemInHand();
		if (item == null) {return;}
		Material m = item.getType();
		if (!item.hasItemMeta()) {return;}
		ItemMeta meta = item.getItemMeta();
		if (!meta.hasDisplayName()) {return;}
		String itName = meta.getDisplayName();
		
		// Обезвреживание
		if (item.equals(arena.getDefuseKitsItem()))
		{	
			if (arena.isDefusing || !arena.isPlanted) {return;}
			if (!arena.getTeamDefend().contains(p.getUniqueId())) 
			{
				p.getInventory().remove(item);
				return;
			}
			
			ArrayList<Short> _types = new ArrayList<>();
			_types.add((short) 12);
			_types.add((short) 10);
			_types.add((short) 0);
			_types.add((short) 6);
			_types.add((short) 15);
			_types.add((short) 4);
			_types.add((short) 7);
			_types.add((short) 13);
			
			ArrayList<String> _strings = new ArrayList<>();
			_strings.add("1 - Берём бомбу...");
			_strings.add("2 - Вскрываем...");
			_strings.add("3 - Думаем...");
			_strings.add("4 - Выбираем провод...");
			_strings.add("5 - Молимся...");
			_strings.add("6 - Достаём щипцы...");
			_strings.add("7 - Режем провод...");
			_strings.add("8 - Кидаем бомбу в помойку...");
			
			Inventory inv = arena.getPlantInventory();
			for(int i = 0; i < _strings.size(); i++)
			{
				ItemStack is = new ItemStack(Material.WOOL);
				ItemMeta im = is.getItemMeta();
				is.setDurability(_types.get(i));
				im.setDisplayName(_strings.get(i));
				
				Random rnd = new Random();
				int slot = rnd.nextInt(27);
				while (inv.getItem(slot) != null) {slot = rnd.nextInt(27);}
				inv.setItem(slot, is);
			}
			
			p.openInventory(inv);
			arena.isDefusing = true;
			return;
		}
		
		// Бомба
		if (item.equals(arena.getBombItem()))
		{	
			if (arena.isPlanting) {return;}
			
			if (arena.isPlanted)
			{
				p.getInventory().remove(item);
				return;
			}
			
			if (!arena.getTeamAttack().contains(p.getUniqueId())) 
			{
				p.getInventory().remove(item);
				return;
			}
			
			ArrayList<Short> _types = new ArrayList<>();
			_types.add((short) 12);
			_types.add((short) 2);
			_types.add((short) 4);
			_types.add((short) 14);
			_types.add((short) 5);
			
			ArrayList<String> _strings = new ArrayList<>();
			_strings.add("1 - Достаём бомбу...");
			_strings.add("2 - Вставляем провод...");
			_strings.add("3 - Вводим код...");
			_strings.add("4 - Включаем таймер...");
			_strings.add("5 - Кладём бомбу на плент...");
			
			Inventory inv = arena.getPlantInventory();
			for(int i = 0; i < 5; i++)
			{
				ItemStack is = new ItemStack(Material.WOOL);
				ItemMeta im = is.getItemMeta();
				is.setDurability(_types.get(i));
				im.setDisplayName(_strings.get(i));
				
				Random rnd = new Random();
				int slot = rnd.nextInt(27);
				while (inv.getItem(slot) != null) {slot = rnd.nextInt(27);}
				inv.setItem(slot, is);
			}
			
			p.openInventory(inv);
			arena.isPlanting = true;
			return;
		}
		
		if (itName.equals("Выбор команды") && m.equals(Material.WOOL))
		{
			ItemStack defendItem = new ItemStack(Material.WOOL);
			ItemMeta dMeta = defendItem.getItemMeta();
			ArrayList<String> lore = new ArrayList<>();
			dMeta.setDisplayName("Команда ЗАЩИТЫ");
			defendItem.setDurability((short) 3);
			lore.add("Защищайте территорию от атакующих.");
			lore.add("Не дайте им заложить бомбу.");
			lore.add("Если же это произойдёт, обезвредьте её!");
			dMeta.setLore(lore);
			defendItem.setItemMeta(dMeta);
			
			ItemStack attackItem = new ItemStack(Material.WOOL);
			ItemMeta aMeta = defendItem.getItemMeta();
			lore.clear();
			dMeta.setDisplayName("Команда АТАКИ");
			attackItem.setDurability((short) 1);
			lore.add("Взорвите территорию защиты.");
			lore.add("Заложите бомбу на красных обозначителях.");
			lore.add("Защитите и не дайте её обезвредить.");
			dMeta.setLore(lore);
			defendItem.setItemMeta(aMeta);
			
			Inventory inv = Bukkit.createInventory(null, 9, "Выбор команды");
			inv.setItem(3, defendItem);
			inv.setItem(6, attackItem);
			p.openInventory(inv);
			return;
		}
	}
	
	@EventHandler
	public void onPlayerDropItem(PlayerDropItemEvent e)
	{
		Player p = e.getPlayer();
		Arena arena = main.getPlayerArena(p);
		if (arena != null) {e.setCancelled(true);}
	}
	
	@EventHandler
	public void onBlockBreak(BlockBreakEvent e)
	{
		Player p = e.getPlayer();
		Arena arena = main.getPlayerArena(p);
		if (arena != null) {e.setCancelled(true);}
	}
	
	@EventHandler
	public void onPlayerDamaged(EntityDamageEvent e)
	{
		if (!e.getEntityType().equals(EntityType.PLAYER)) {return;}
		Player p = (Player) e.getEntity();
		Arena arena = main.getPlayerArena(p);
		if (arena == null) {return;}
		String state = arena.getState();
		if (state.equals("FREEZE TIME")) {e.setCancelled(true);}
		if (state.equals("WAITING")) {e.setCancelled(true);}
		if (state.equals("STOP")) {e.setCancelled(true);}
		if (state.equals("ROUND END")) {e.setCancelled(true);}
		
	}
}
