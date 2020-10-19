package kiviuly.BigBangShooter;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupArrowEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

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
		String arName = main.removeCC(lore.get(0).split(": ")[1]);
		arena = main.arenas.getOrDefault(main.removeCC(arName), null);
		if (arena == null) {return;}
		Location l = b.getLocation();
		e.setCancelled(true);
		
		if (name.endsWith("������")) {arena.getDefendSpawns().add(l);}
		if (name.endsWith("�����")) {arena.getAttackSpawns().add(l);}
		SM(p,"�2����� �eX: "+l.getBlockX()+" Y: "+l.getBlockY()+" Z: "+l.getBlockZ()+" �2������� "+name.split(" ")[2]+" �2�������� �� ����� �b"+arName+"�2.");
	}
	
	@EventHandler
	public void onBlockBreak(BlockBreakEvent e)
	{
		Player p = e.getPlayer();
		Arena arena = main.getPlayerArena(p);
		if (arena != null) {e.setCancelled(true);}
	}

	@EventHandler
	public void onPlayerMove(PlayerMoveEvent e)
	{
		Player p = e.getPlayer();
		Arena arena = main.getPlayerArena(p);
		if (arena == null) {return;}
		
		if (arena.getState().equals("FREEZE TIME"))
		{
			if (e.getTo().distance(e.getFrom()) > 0) {e.setCancelled(true);}
		}
	}
	
	@EventHandler
	public void onPlayerLeave(PlayerQuitEvent e)
	{
		Player p = e.getPlayer();
		Arena arena = main.getPlayerArena(p);
		if (arena == null) {return;}
		arena.deathPlayer(p);
	}
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e)
	{
		Player p = e.getPlayer();
		Arena arena = main.getPlayerArena(p);
		if (arena == null) {return;}
		if (!arena.isStarted) {main.loadPlayerData(p); return;}
		UUID id = p.getUniqueId();
		String state = arena.getState();
		if (!arena.getDeathPlayers().contains(id)) {main.loadPlayerData(p); return;}
		if (state.equals("FREEZE TIME")) 
		{
			p.setGameMode(GameMode.ADVENTURE);
			p.setHealth(20.0);
			p.setFoodLevel(20);
			p.getInventory().clear();
			arena.teleportToSpawns(p);
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
		
		if (e.getDamager().getType().equals(EntityType.ARROW) || e.getDamager().getType().equals(EntityType.SPECTRAL_ARROW))
		{
			Arrow arrow = (Arrow) e.getDamager();
			if (arrow.getShooter() instanceof Player)
			{
				Player killer = (Player) arrow.getShooter();
				if (!state.equals("PLAYING")) {e.setCancelled(true); return;}
				if (arena.getPlayerTeam(p) == arena.getPlayerTeam(killer)) {e.setCancelled(true); return;}
			}
		}
		
		if (e.getDamager().getType().equals(EntityType.PLAYER))
		{
			Player killer = (Player) e.getDamager();
			double damage = e.getDamage();
			double heath = p.getHealth();
			
			int fTeam = arena.getPlayerTeam(p);
			int sTeam = arena.getPlayerTeam(killer);
			
			if (fTeam == sTeam) {e.setCancelled(true); return;}
			if (damage >= heath) 
			{
				e.setDamage(heath-1.0);
				arena.addKilledPlayer(p, killer);
				return;
			}
		}
		
	}
	
	@EventHandler
	public void onPlayerUseBow(EntityShootBowEvent e)
	{
		if (!e.getEntityType().equals(EntityType.PLAYER)) {return;}
		Player p = (Player) e.getEntity();
		Arena arena = main.getPlayerArena(p);
		if (arena == null) {return;}
		Entity proj = e.getProjectile();
		arena.getEntities().add(proj);
	}
	
	@EventHandler
	public void onPlayerUseCommand(PlayerCommandPreprocessEvent e)
	{
		Player p = e.getPlayer();
		Arena arena = main.getPlayerArena(p);
		if (arena == null) {return;}
		if (!e.getMessage().contains("bbs leave")) {e.setCancelled(true);}
	}
	
	@EventHandler
	public void onPlayerTakeDrop(PlayerPickupItemEvent e)
	{
		Player p = e.getPlayer();
		Arena arena = main.getPlayerArena(p);
		if (arena == null) {return;}
		e.setCancelled(true);
	}
	
	@EventHandler
	public void onPlayerTakeDrop(PlayerPickupArrowEvent e)
	{
		Player p = e.getPlayer();
		Arena arena = main.getPlayerArena(p);
		if (arena == null) {return;}
		e.setCancelled(true);
	}
	
	@EventHandler
	public void onPlayerCloseInv(InventoryCloseEvent e)
	{
		if (!e.getPlayer().getType().equals(EntityType.PLAYER)) {return;}
		Player p = (Player) e.getPlayer();
		String title = e.getView().getTitle();
		Arena arena = main.getPlayerArena(p);
		if (arena == null) {return;}
		
		if (title.equals("�9�l�������� �����")) {arena.getPlantInventory().clear(); arena.isPlanting = false;}
		if (title.equals("�9�l�������������� �����")) {arena.getDefuseInventory().clear();arena.isDefusing = false;}
	}
	
	@EventHandler
	public void onPlayerClickInv(InventoryClickEvent e)
	{
		if (!e.getWhoClicked().getType().equals(EntityType.PLAYER)) {return;}
		Player p = (Player) e.getWhoClicked();
		String title = e.getView().getTitle();
		Arena arena = main.getPlayerArena(p);
		
		if (title.equals("�9�l����� �������"))
		{
			e.setCancelled(true);
			if (arena == null) {p.closeInventory(); return;}
			if (!arena.getState().equals("WAITING")) {p.closeInventory(); return;}
			ItemStack item = e.getCurrentItem();
			if (item == null) {return;}
			if (!item.hasItemMeta()) {return;}
			ItemMeta meta = item.getItemMeta();
			if (!meta.hasDisplayName()) {return;}
			String itName = main.removeCC(meta.getDisplayName());
			UUID id = p.getUniqueId();
			if (itName.equals("������� ������"))
			{
				if (arena.getTeamDefend().contains(id)) {p.sendMessage("�e�� ��� � ���� �������."); return;}
				if (arena.getTeamAttack().contains(id)) {arena.getTeamAttack().remove(id);}
				arena.getTeamDefend().add(id);
				p.closeInventory();
				p.sendTitle("�2�� ������� �������", "�e������ �� �9�l������");
				p.sendMessage("�2������ �� ������� �� ������� �9�l�����ۧ2.");
				return;
			}
			
			if (itName.equals("������� �����"))
			{
				if (arena.getTeamAttack().contains(id)) {p.sendMessage("�e�� ��� � ���� �������."); return;}
				if (arena.getTeamDefend().contains(id)) {arena.getTeamDefend().remove(id);}
				arena.getTeamAttack().add(id);
				p.closeInventory();
				p.sendTitle("�2�� ������� �������", "�e������ �� �6�l�����");
				p.sendMessage("�2������ �� ������� �� ������� �6�l����ȧ2.");
				return;
			}
			return;
		}
		
		if (title.equals("�9�l����� ������������"))
		{
			e.setCancelled(true);
			
			if (arena == null) {p.closeInventory(); return;}
			ItemStack item = e.getCurrentItem();
			if (item == null) {return;}
			if (!item.hasItemMeta()) {return;}
			ItemMeta meta = item.getItemMeta();
			if (!meta.hasDisplayName()) {return;}
			String itName = main.removeCC(meta.getDisplayName());
			if (!arena.getOperatorsItems().containsKey(itName)) {p.sendMessage("�c�������� �� �������."); return;}
			ArrayList<ItemStack> isList = arena.getOperatorsItems().get(itName);
			if (isList.size() == 0) {p.sendMessage("�c�������� �� �������."); return;}
			p.getInventory().clear();
			int i = 0;
			for(ItemStack is : isList)
			{
				if (is == null) {continue;}
				is = is.clone();
				String matName = is.getType().name().toUpperCase();
				
				if (matName.contains("LEATHER"))
				{
					LeatherArmorMeta tam = (LeatherArmorMeta) is.getItemMeta();
					if (arena.getPlayerTeam(p) == 0) {tam.setColor(Color.AQUA);}
					if (arena.getPlayerTeam(p) == 1) {tam.setColor(Color.ORANGE);}
					is.setItemMeta(tam);
				}
				
				if (matName.contains("HELMET")) {p.getInventory().setHelmet(is); continue;}
				if (matName.contains("CHESTPLATE")) {p.getInventory().setChestplate(is); continue;}
				if (matName.contains("LEGGINGS")) {p.getInventory().setLeggings(is); continue;}
				if (matName.contains("BOOTS")) {p.getInventory().setBoots(is); continue;}
				
				if (i == 6) {i = 9;}
				p.getInventory().setItem(i, is);
				i++;
			}
			
			arena.getPlayerOperator().put(p.getName(), itName);
			arena.giveClassItem(p);
			p.closeInventory();
			p.sendMessage("�2�������� ������������ �b�l"+itName+" �2������.");
			return;
		}
		
		if (title.equals("�9�l����� �����"))
		{
			e.setCancelled(true);
			ItemStack item = e.getCurrentItem();
			if (item == null) {return;}
			if (!item.hasItemMeta()) {return;}
			ItemMeta meta = item.getItemMeta();
			if (!meta.hasDisplayName()) {return;}
			String itName = meta.getDisplayName();
			arena = main.getArenaByName(itName);
			if (arena == null) {p.closeInventory(); return;}
			arena.addPlayer(p);
			main.players.put(p.getUniqueId(), arena);
			
			if (!meta.hasLore()) {return;}
			List<String> lore = meta.getLore();
			if (lore.size() != 3) {return;}
			int plCount = arena.getAllPlayers().size();
			int maxPl = arena.getMaxPlayers();
			lore.set(1,"�e�������: �f"+plCount+"/"+maxPl);
			meta.setLore(lore);
			item.setItemMeta(meta);
			main.menuInventory.setItem(e.getSlot(), item);
			
			if (maxPl == plCount) {main.menuInventory.remove(item);}
			if (arena.isStarted) {main.menuInventory.remove(item);}
		}
		
		if (title.equals("�9�l�������� �����"))
		{
			e.setCancelled(true);
			if (arena == null) {p.closeInventory(); return;}
			if (!arena.getState().equals("PLAYING")) {p.closeInventory(); return;}
			ItemStack item = e.getCurrentItem();
			if (item == null) {p.closeInventory(); arena.isPlanting = false; return;}
			if (!item.hasItemMeta()) {p.closeInventory(); arena.isPlanting = false;  return;}
			ItemMeta meta = item.getItemMeta();
			if (!meta.hasDisplayName()) {p.closeInventory(); arena.isPlanting = false; return;}
			String itName = meta.getDisplayName();
			
			ArrayList<String> _strings = new ArrayList<>();
			_strings.add("�e5 - �2����� ����� �� �����...");
			_strings.add("�e4 - �2�������� ������...");
			_strings.add("�e3 - �2������ ���...");
			_strings.add("�e2 - �2��������� ������...");
			_strings.add("�e1 - �2������ �����...");
			
			p.getWorld().playSound(p.getLocation(), Sound.BLOCK_METAL_HIT, 0.8f, 1);
			
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
				arena.getPlantInventory().clear();
				p.sendTitle("������� �� ��������!", "�e������������������ �� �����...");
				p.closeInventory(); 
				return;
			}
			
			arena.getPlantInventory().remove(item);
			if (itemsCount > 1) {return;}
			ArrayList<UUID> list = arena.getAllPlayers();
			for(UUID plID : list)
			{
				if (plID == null) {continue;}
				Player pl = Bukkit.getPlayer(plID);
				if (pl == null) {list.remove(plID); continue;}
				if (!pl.isOnline()) {list.remove(plID); continue;}
				pl.sendTitle("�e�l����� ��������", "�e�� ������ 45 ������");
			}
			
			arena.getPlantInventory().clear();
			arena.getEditedBlocks().put(arena.getPlantedBomb(), Material.AIR);
			arena.getPlantedBomb().getBlock().setType(arena.getBombMaterial());
			arena.setTime(45);
			arena.isPlanted = true;
			arena.isPlanting = false;
			p.closeInventory(); 
			return;
		}
		
		if (title.equals("�9�l�������������� �����"))
		{
			e.setCancelled(true);
			if (arena == null) {p.closeInventory(); return;}
			if (!arena.getState().equals("PLAYING")) {p.closeInventory(); return;}
			ItemStack item = e.getCurrentItem();
			if (item == null) {p.closeInventory(); arena.isDefusing = false; return;}
			if (!item.hasItemMeta()) {p.closeInventory(); arena.isDefusing = false;  return;}
			ItemMeta meta = item.getItemMeta();
			if (!meta.hasDisplayName()) {p.closeInventory(); arena.isDefusing = false; return;}
			String itName = meta.getDisplayName();
			
			ArrayList<String> _strings = new ArrayList<>();
			_strings.add("�e8 - �2������ ����� � �������...");
			_strings.add("�e7 - �2����� ������...");
			_strings.add("�e6 - �2������ �����...");
			_strings.add("�e5 - �2�������...");
			_strings.add("�e4 - �2�������� ������...");
			_strings.add("�e3 - �2������...");
			_strings.add("�e2 - �2���������...");
			_strings.add("�e1 - �2���� �����...");
			
			p.getWorld().playSound(p.getLocation(), Sound.BLOCK_METAL_HIT, 0.8f, 1);
			
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
				arena.getDefuseInventory().clear();
				p.sendTitle("�c����� �� �����������!", "�e������������������ �� �����...");
				p.closeInventory(); 
				return;
			}
			
			arena.getDefuseInventory().remove(item);
			if (itemsCount > 1) {return;}
			ArrayList<UUID> list = arena.getAllPlayers();
			for(UUID plID : list)
			{
				Player pl = Bukkit.getPlayer(plID);
				if (pl == null) {list.remove(plID); continue;}
				if (!pl.isOnline()) {list.remove(plID); continue;}
				int team = arena.getPlayerTeam(pl);
				if (team == 1) 
				{pl.sendTitle("�c�l���������...", "�c����� ���� �����������.");}
				else {pl.sendTitle("�a�l������!", "�a����� ���� �����������.");}
			}
			
			arena.getDefuseInventory().clear();
			arena.setTime(8);
			arena.isPlanted = false;
			arena.isPlanting = false;
			arena.isDefusing = false;
			arena.setState("ROUND END");
			arena.setDefendRoundsWin(arena.getDefendRoundsWin()+1);
			arena.setRound(arena.getRound()+1);
			p.closeInventory(); 
			return;
		}
		
		if (title.equals("�9�l����� �������"))
		{
			e.setCancelled(true);
			if (arena == null) {p.closeInventory(); return;}
			ItemStack item = e.getCurrentItem();
			if (item == null) {return;}
			if (!item.hasItemMeta()) {return;}
			ItemMeta meta = item.getItemMeta();
			if (!meta.hasDisplayName()) {return;}
			String itName = meta.getDisplayName();
			
			if (itName.equals("�9�l������� ������"))
			{
				if (arena.getTeamDefend().contains(p.getUniqueId())) 
				{
					p.sendTitle("�e�� ��� � ���� �������","�e-_-");
					p.sendMessage("�e�� ��� � ���� �������.");
					return;
				}
				
				arena.getTeamDefend().add(p.getUniqueId());
				p.sendTitle("�2������ �� �9�l����������","�e(*)_(*)");
				p.sendMessage("�2�� �������������� � ������� ������.");
			}
			
			if (itName.equals("�6�l������� �����"))
			{
				if (arena.getTeamAttack().contains(p.getUniqueId())) 
				{
					p.sendTitle("�� ��� � ���� �������","-_-");
					p.sendMessage("�� ��� � ���� �������.");
					return;
				}
				
				arena.getTeamAttack().add(p.getUniqueId());
				p.sendTitle("�2������ �� �6�l���������","�e(*)_(*)");
				p.sendMessage("�2�� �������������� � ������� �����.");
			}
			
			p.closeInventory();
			return;
		}
		
		if (arena == null) {return;}
		if (e.getSlotType().equals(SlotType.ARMOR)) {e.setCancelled(true);}
	}
	
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent e)
	{
		Player p = e.getPlayer();
		Arena arena = main.getPlayerArena(p);
		if (arena == null) {return;}
		
		if (arena.getState().equals("WAITING") || arena.getState().equals("FREEZE TIME")) {e.setCancelled(true);}
		
		Block b = e.getClickedBlock();
		if (!arena.getTeamDefend().contains(p.getUniqueId()))
		{if (b != null && arena.getPlantedBomb() != null) 
		{if (arena.getPlantedBomb().equals(b.getLocation())) {e.setCancelled(true); return;}}}
		
		ItemStack item = p.getItemInHand();
		if (item == null) {return;}
		Material m = item.getType();
		if (!item.hasItemMeta()) {return;}
		ItemMeta meta = item.getItemMeta();
		if (!meta.hasDisplayName()) {return;}
		String itName = meta.getDisplayName();
		
		// ��������������
		if (item.equals(arena.getDefuseKitsItem()))
		{	
			e.setCancelled(true);
			if (b == null) {return;}
			if (!b.getType().equals(arena.getBombMaterial())) {return;}
			if (!b.getLocation().equals(arena.getPlantedBomb())) {return;}
			if (arena.isDefusing || !arena.isPlanted) {return;}
			
			if (!arena.getTeamDefend().contains(p.getUniqueId())) 
			{
				p.getInventory().remove(item);
				return;
			}
			
			if (!arena.getState().equals("PLAYING")) {p.closeInventory(); return;}
			b.getWorld().playSound(b.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.8f, 1);
			
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
			_strings.add("�e1 - �2���� �����...");
			_strings.add("�e2 - �2���������...");
			_strings.add("�e3 - �2������...");
			_strings.add("�e4 - �2�������� ������...");
			_strings.add("�e5 - �2�������...");
			_strings.add("�e6 - �2������ �����...");
			_strings.add("�e7 - �2����� ������...");
			_strings.add("�e8 - �2������ ����� � �������...");
			
			Inventory inv = arena.getDefuseInventory();
			for(int i = 0; i < _strings.size(); i++)
			{
				ItemStack is = new ItemStack(Material.WOOL);
				ItemMeta im = is.getItemMeta();
				is.setDurability(_types.get(i));
				im.setDisplayName(_strings.get(i));
				is.setItemMeta(im);
				
				Random rnd = new Random();
				int slot = rnd.nextInt(27);
				while (inv.getItem(slot) != null) {slot = rnd.nextInt(27);}
				inv.setItem(slot, is);
			}
			
			p.openInventory(inv);
			arena.isDefusing = true;
			return;
		}
		
		// ����� ������������
		if (item.equals(arena.getOperatorItem()))
		{
			e.setCancelled(true);
			String s = arena.getState();
			if (!s.equals("FREEZE TIME") && !s.equals("WAITING")) {return;}
			Inventory inv = arena.getOperatorsInventory();
			if (inv == null) {return;}
			p.openInventory(inv);
			return;
		}
		
		// ����� �������
		if (item.equals(arena.getTeamItem()))
		{
			e.setCancelled(true);
			String s = arena.getState();
			if (!s.equals("WAITING")) {p.getInventory().remove(item); return;}
			Inventory inv = Bukkit.createInventory(null, 9, "�9�l����� �������");
			for(int i = 0; i < 9; i++) {inv.setItem(i, new ItemStack(Material.WEB));}
			ItemStack is = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 3);
			ItemMeta im = is.getItemMeta();
			im.setDisplayName("�e�l������� �9�l������");
			is.setItemMeta(im);
			inv.setItem(2, is);
			is = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 1);
			im = is.getItemMeta();
			im.setDisplayName("�e�l������� �6�l�����");
			is.setItemMeta(im);
			inv.setItem(6, is);
			p.openInventory(inv);
			return;
		}
		
		// �����
		if (item.equals(arena.getBombItem()))
		{	
			e.setCancelled(true);
			if (b == null) {return;}
			if (!b.getType().equals(Material.RED_GLAZED_TERRACOTTA)) {return;}
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
			
			if (!arena.getState().equals("PLAYING")) {p.closeInventory(); return;}
			b.getWorld().playSound(b.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.8f, 1);
			
			ArrayList<Short> _types = new ArrayList<>();
			_types.add((short) 12);
			_types.add((short) 2);
			_types.add((short) 4);
			_types.add((short) 14);
			_types.add((short) 5);
			
			ArrayList<String> _strings = new ArrayList<>();
			_strings.add("�e1 - �2������ �����...");
			_strings.add("�e2 - �2��������� ������...");
			_strings.add("�e3 - �2������ ���...");
			_strings.add("�e4 - �2�������� ������...");
			_strings.add("�e5 - �2����� ����� �� �����...");
			
			Inventory inv = arena.getPlantInventory();
			for(int i = 0; i < 5; i++)
			{
				ItemStack is = new ItemStack(Material.WOOL);
				ItemMeta im = is.getItemMeta();
				is.setDurability(_types.get(i));
				im.setDisplayName(_strings.get(i));
				is.setItemMeta(im);
				
				Random rnd = new Random();
				int slot = rnd.nextInt(27);
				while (inv.getItem(slot) != null) {slot = rnd.nextInt(27);}
				inv.setItem(slot, is);
			}
			
			p.openInventory(inv);
			arena.isPlanting = true;
			arena.setPlantedBomb(b.getLocation().clone().add(0, 1, 0));
			return;
		}
		
		if (itName.equals("�9�l����� �������") && m.equals(Material.WOOL))
		{
			ItemStack defendItem = new ItemStack(Material.WOOL);
			ItemMeta dMeta = defendItem.getItemMeta();
			ArrayList<String> lore = new ArrayList<>();
			dMeta.setDisplayName("�9�l������� ������");
			defendItem.setDurability((short) 3);
			lore.add("�e��������� ���������� �� ���������.");
			lore.add("�e�� ����� �� �������� �����.");
			lore.add("�e���� �� ��� ���������, ����������� �!");
			dMeta.setLore(lore);
			defendItem.setItemMeta(dMeta);
			
			ItemStack attackItem = new ItemStack(Material.WOOL);
			ItemMeta aMeta = defendItem.getItemMeta();
			lore.clear();
			dMeta.setDisplayName("�6�l������� �����");
			attackItem.setDurability((short) 1);
			lore.add("�e�������� ���������� ������.");
			lore.add("�e�������� ����� �� ������� �������������.");
			lore.add("�e�������� � �� ����� � �����������.");
			dMeta.setLore(lore);
			defendItem.setItemMeta(aMeta);
			
			Inventory inv = Bukkit.createInventory(null, 9, "����� �������");
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
		
		if (state.equals("PLAYING"))
		{
			double dmg = e.getDamage();
			if (p.getHealth() > dmg) {return;}
			e.setDamage(p.getHealth()-1.0);
			arena.deathPlayer(p);
		}
	}
}
