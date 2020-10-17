package kiviuly.BigBangShooter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import net.minecraft.server.v1_12_R1.ItemArmor;

public class Main extends JavaPlugin
{
	public HashMap<String, Arena> arenas = new HashMap<>();
	public HashMap<UUID, Arena> players = new HashMap<>();
	
	public Inventory menuInventory = null;
	public FileConfiguration config = null;
	public String mainFolder = getDataFolder() + File.separator;
	
	private static Main main = null;
	
	@Override
	public void onEnable() 
	{
		main = this;
		
		// Events
		getServer().getPluginManager().registerEvents(new Events(this), this);
		
		// Commands
		getCommand("bbs").setExecutor(new Commands(this));
		
		// CFG
		loadCFG();
	}
	
	@Override
	public void onDisable() {saveCFG();}
	
	// Functions
	
	public void saveCFG()
	{
		File file = new File(mainFolder);
		if (!file.exists()) {file.mkdir();}
		file = new File(mainFolder + "Config.yml");
		if (!file.exists()) {saveResource("Config.yml", true);}
		file = new File(mainFolder + "Config.yml");
		config = YamlConfiguration.loadConfiguration(file);
		
		file = new File(mainFolder + "Arenas");
		if (!file.exists()) {file.mkdir();}
		
		for(String arName : arenas.keySet())
		{
			Arena arena = arenas.get(arName);
			if (arena == null) {continue;}
			if (arena.isStarted)
			{
				for(UUID id : arena.getAllPlayers()) 
				{
					Player p = Bukkit.getPlayer(id);
					if (p == null) {continue;}
					loadPlayerData(p);
				}
			}
			
			file = new File(mainFolder + "Arenas" + File.separator + arName + ".arena");
			if (!file.exists()) 
			{
				try {file.createNewFile();} 
				catch (IOException e) {e.printStackTrace();}
			}
			
			FileConfiguration yml = YamlConfiguration.loadConfiguration(file);
			yml.set("ID", arena.getID());
			yml.set("Name", arena.getName());
			yml.set("Description", arena.getDescription());
			yml.set("Creator", arena.getCreator().toString());
			yml.set("MaxPlayers", arena.getMaxPlayers());
			yml.set("MinPlayers", arena.getMinPlayers());
			yml.set("MaxRounds", arena.getMaxRounds());
			yml.set("RoundTime", arena.getRoundTime());
			yml.set("Enabled", arena.isEnabled);
			yml.set("LobbyLocation", arena.getLobbyLocation());
			
			
			int i = 0;
			HashMap<String, ArrayList<ItemStack>> operatorsItem = arena.getOperatorsItems();
			for(String opName : operatorsItem.keySet())
			{
				ArrayList<ItemStack> isList = operatorsItem.get(opName);
				if (isList == null) {continue;}
				if (isList.size() == 0) {continue;}
				yml.set("OperatorsInfo."+opName+"-Items", isList);
				yml.set("OperatorsInfo.Operator-"+i, opName);
				
				i++;
			}
			yml.set("OperatorsInfo.OperatorsAviabled", i);
			
			i = 0;
			for(Location l : arena.getDefendSpawns())
			{
				if (l == null) {continue;}
				yml.set("DefendSpawn-"+i, LtoS(l));
				i++;
			}
			yml.set("DefendSpawns", i);
			
			i = 0;
			for(Location l : arena.getAttackSpawns())
			{
				if (l == null) {continue;}
				yml.set("AttackSpawn-"+i, LtoS(l));
				i++;
			}
			yml.set("AttackSpawns", i);
			try {
				yml.save(file);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	public void loadCFG() 
	{
		menuInventory = Bukkit.createInventory(null, 27, "�9�l����� �����");
		
		File file = new File(mainFolder);
		if (!file.exists()) {file.mkdir();}
		file = new File(mainFolder + "Config.yml");
		if (!file.exists()) {saveResource("Config.yml", true);}
		file = new File(mainFolder + "Config.yml");
		config = YamlConfiguration.loadConfiguration(file);
		
		file = new File(mainFolder + "Arenas");
		if (!file.exists()) {file.mkdir();}
		
		for(File f : file.listFiles())
		{
			if (!f.getName().endsWith(".arena")) {continue;}
			FileConfiguration yml = YamlConfiguration.loadConfiguration(f);
			String arName = f.getName().replace(".arena", "");
			String ID = arName.toUpperCase();
			UUID plID = UUID.fromString(yml.getString("Creator"));
			Arena arena = new Arena(ID, plID);
			arena.setName(yml.getString("Name"));
			arena.setDescription(yml.getString("Description"));
			arena.setMaxPlayers(yml.getInt("MaxPlayers"));
			arena.setMinPlayers(yml.getInt("MinPlayers"));
			arena.setMaxRounds(yml.getInt("MaxRounds"));
			arena.setRoundTime(yml.getInt("RoundTime"));
			arena.isEnabled = yml.getBoolean("Enabled");
			arena.setLobbyLocation((Location) yml.get("LobbyLocation", null));
			
			int count = yml.getInt("OperatorsInfo.OperatorsAviabled", 0);
			for(int i = 0; i < count; i++)
			{
				String opName = yml.getString("OperatorsInfo.Operator-"+i, "");
				if (opName.isEmpty()) {continue;}
				ArrayList<ItemStack> isList = new ArrayList<>();
				isList = (ArrayList<ItemStack>) yml.get("OperatorsInfo."+opName+"-Items", isList);
				arena.getOperatorsItems().put(opName, isList);
				String matName = yml.getString("OperatorsInfo."+opName+"-Icon", "");
				if (matName.isEmpty()) {matName = "CHEST";}
				Material m = Material.getMaterial(matName);
				if (m == null) {m = Material.CHEST;}
				ItemStack is = new ItemStack(m);
				ItemMeta im = is.getItemMeta();
				ArrayList<String> lore = new ArrayList<>();
				im.setDisplayName("�6�l"+opName);
				lore.add("");
				lore.add("�e��������");
				for(ItemStack item : isList)
				{
					if (item == null) {continue;}
					if (item.getType().equals(Material.AIR)) {continue;}
					if (!item.hasItemMeta()) {continue;}
					ItemMeta meta = item.getItemMeta();
					String itName = meta.getLocalizedName();
					if (itName.isEmpty()) {itName = meta.getDisplayName();}
					lore.add("   �f"+itName);
				}
				im.setLore(lore);
				is.setItemMeta(im);
				arena.getOperatorsInventory().addItem(is);
			}
			
			count = yml.getInt("DefendSpawns", 0);
			for(int i = 0; i < count; i++)
			{
				String s = yml.getString("DefendSpawn-"+i, "");
				if (s.isEmpty()) {continue;}
				arena.getDefendSpawns().add(StoL(s));
			}
			
			count = yml.getInt("AttackSpawns", 0);
			for(int i = 0; i < count; i++)
			{
				String s = yml.getString("AttackSpawn-"+i, "");
				if (s.isEmpty()) {continue;}
				arena.getAttackSpawns().add(StoL(s));
			}
			
			if (arena.isEnabled)
			{
				ItemStack is = new ItemStack(Material.WOOL);
				ItemMeta im = is.getItemMeta();
				is.setDurability((short) 5);
				im.setDisplayName("�b"+arena.getName());
				is.setItemMeta(im);
				menuInventory.addItem(is);
			}
			
			arenas.put(arName, arena);
		}
	}

	public String LtoS(Location l)
	{
		String s = l.getWorld().getName()+"/"+l.getBlockX()+"/"+l.getBlockY()+"/"+l.getBlockZ();
		return s;
	}
	
	public Location StoL(String s)
	{
		String[] ss = s.split("/");
		double x = Double.parseDouble(ss[1]);
		double y = Double.parseDouble(ss[2]);
		double z = Double.parseDouble(ss[3]);
		Location l = new Location(Bukkit.getWorld(ss[0]), x, y, z);
		return l;
	}

	
		
	
	// Methods
	
	public void savePlayerData(Player p)
	{
		File temp = new File(getDataFolder() + File.separator + "Players");
		if (!temp.exists()) {temp.mkdir();}
		
		temp = new File(getDataFolder() + File.separator + "Players" + File.separator + p.getUniqueId() + ".data");
		HashMap<String, Object> data = new HashMap<>();
		
		data.put("Location", p.getLocation());
		data.put("DisplayName", p.getDisplayName());
		data.put("InventoryContents", p.getInventory().getContents());
		data.put("ArmorContents", p.getInventory().getArmorContents());
		data.put("EnderChest", p.getEnderChest().getContents());
		data.put("HP", p.getHealth());
		data.put("FOOD", p.getFoodLevel());
		data.put("isFlying", p.isFlying());
		data.put("GameMode", p.getGameMode());
		data.put("PotionEffects", p.getActivePotionEffects());
		data.put("LEVEL", p.getLevel());
		data.put("EXP", p.getExp());
		data.put("WalkSpeed", p.getWalkSpeed());
		data.put("FlySpeed", p.getFlySpeed());
		data.put("AllowFlight", p.getAllowFlight());
		
		try 
		{
			ObjectOutputStream ois = new BukkitObjectOutputStream(new FileOutputStream(temp));
			ois.writeObject(data); ois.flush(); ois.close();
		} 
		catch (IOException e) {e.printStackTrace();}
	}
	
	@SuppressWarnings("unchecked")
	public void loadPlayerData(Player p)
	{
		File temp = new File(getDataFolder() + File.separator + "Players");
		
		temp = new File(getDataFolder() + File.separator + "Players" + File.separator + p.getUniqueId() + ".data");
		if (temp.exists())
		{
			try 
			{
				ObjectInputStream ois = new BukkitObjectInputStream(new FileInputStream(temp));
				HashMap<String, Object> data = (HashMap<String, Object>) ois.readObject();
				ois.close();
				
				p.setDisplayName((String) data.getOrDefault("DisplayName", p.getDisplayName()));
				p.getInventory().setContents((ItemStack[]) data.getOrDefault("InventoryContents", p.getInventory().getContents()));
				p.getInventory().setContents((ItemStack[]) data.getOrDefault("EnderChest", p.getEnderChest().getContents()));
				p.getInventory().setArmorContents((ItemStack[]) data.getOrDefault("ArmorContents", p.getInventory().getArmorContents()));
				p.setHealth((double) data.getOrDefault("HP", p.getHealth()));
				p.setFoodLevel((int) data.getOrDefault("FOOD", p.getFoodLevel()));
				p.setGameMode((GameMode) data.getOrDefault("GameMode", p.getGameMode()));
				p.addPotionEffects((Collection<PotionEffect>) data.getOrDefault("PotionEffects", p.getActivePotionEffects()));
				p.setLevel((int) data.getOrDefault("LEVEL", p.getLevel()));
				p.setExp((float) data.getOrDefault("EXP", p.getExp()));
				p.setWalkSpeed((float) data.getOrDefault("WalkSpeed", p.getWalkSpeed()));
				p.setFlySpeed((float) data.getOrDefault("FlySpeed", p.getFlySpeed()));
				p.teleport((Location) data.getOrDefault("Location", p.getBedSpawnLocation()));
				p.setAllowFlight((boolean) data.getOrDefault("AllowFlight", p.getAllowFlight()));
				if (p.getAllowFlight()) {p.setFlying((boolean) data.getOrDefault("isFlying", p.isFlying()));}
			} 
			catch (IOException | ClassNotFoundException e) {e.printStackTrace();}
		}
	}
	
	public void clearPlayer(Player p)
	{
		p.setDisplayName(p.getName());
		p.getInventory().setArmorContents(null);
		p.getEnderChest().clear();
		p.getInventory().clear();
		p.setMaxHealth(20);
		p.setHealth(20);
		p.setFoodLevel(20);
		p.setGameMode(GameMode.ADVENTURE);
		p.setLevel(60);
		p.setExp(0);
		p.setFlying(false);
		p.setWalkSpeed(0.2F);
		p.setFlySpeed(0.2F);
		for(PotionEffect pi : p.getActivePotionEffects()) {p.removePotionEffect(pi.getType());}
	}
	
	public boolean isPlayerInGame(Player pl) {return players.containsKey(pl.getUniqueId());}
	public Arena getPlayerArena(Player pl) {return players.getOrDefault(pl.getUniqueId(), null);}
	
	public String removeCC(String s) {return ChatColor.stripColor(s);}
	public static Main getInstance() {return main;}
	
	public boolean isArmor(ItemStack item) {
	    return (CraftItemStack.asNMSCopy(item).getItem() instanceof ItemArmor);
	}

	public void OpenMainMenu(Player p)
	{
		p.openInventory(menuInventory);
	}

	public Arena getArenaByName(String name)
	{
		name = removeCC(name);
		for(String arName : arenas.keySet())
		{
			Arena a = arenas.get(arName);
			if (a.getName().equals(name)) {return a;}
		}
		
		return null;
	}
}
