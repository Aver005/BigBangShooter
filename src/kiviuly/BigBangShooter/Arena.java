package kiviuly.BigBangShooter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.NameTagVisibility;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class Arena 
{
	// Const
	
	private static final Main main = Main.getInstance();
	private Arena me = this;
	private String ID;
	private String name;
	private String description;
	private UUID creator;
	
	private int maxPlayers = 12;
	private int minPlayers = 2;
	private int maxRounds = 15;
	private int roundTime = 180;
	
	private ArrayList<Location> defendSpawns = new ArrayList<>();
	private ArrayList<Location> attackSpawns = new ArrayList<>();
	private ArrayList<Location> bonusSpawns = new ArrayList<>();
	
	private Material sitePlantMaterial = Material.RED_GLAZED_TERRACOTTA;
	private Material bombMaterial = Material.CHEST;
	private Material defuseKitsMaterial = Material.LEVER;
	
	private ItemStack operatorItem = new ItemStack(Material.MAGMA);
	private ItemStack teamItem = new ItemStack(Material.EMERALD_ORE);
	private ItemStack bombItem = null;
	private ItemStack defuseKitsItem = null;
	
	public boolean isEnabled = false;
	public boolean isStarted = false;
	public boolean isStarting = false;
	
	private Location lobbyLocation = null;
	
	private HashMap<String, ArrayList<ItemStack>> operatorsItems = new HashMap<>();
	
	
	// Match vars
	
	private ArrayList<UUID> teamDefend = new ArrayList<>();
	private ArrayList<UUID> teamAttack = new ArrayList<>();
	private ArrayList<UUID> livePlayers = new ArrayList<>();
	private ArrayList<UUID> deathPlayers = new ArrayList<>();
	private ArrayList<UUID> spectators = new ArrayList<>();
	
	private ArrayList<Entity> entities = new ArrayList<>();
	
	private HashMap<Location, Material> editedBlocks = new HashMap<>();
	private HashMap<String, Integer> playersMatchStats = new HashMap<>();
	private HashMap<String, String> playerOperator = new HashMap<>();
	
	private Location plantedBomb = null;
	
	private Inventory defuseInventory = Bukkit.createInventory(null, 27, "§9§lОбезвреживание бомбы");
	private Inventory plantInventory = Bukkit.createInventory(null, 27, "§9§lЗакладка бомбы");
	private Inventory operatorsInventory = Bukkit.createInventory(null, 45, "§9§lВыбор оперативника");
	
	public boolean isPlanted = false;
	public boolean isPlanting = false;
	public boolean isDefusing = false;
	
	private int time = 0;
	private int round = 0;
	
	private String roundState = "WAITING";
	
	private int defendRoundsWin = 0;
	private int attackRoundsWin = 0;
	
	private int allKills = 0;
	private int allDeaths = 0;
	private int roundKills = 0;
	private int beestRoundKills = 0;
	private int attackKills = 0;
	private int defendKills = 0;
	private int bombPlanted = 0;
	private int bombDefused = 0;
	
	private UUID bestRoundKiller = null;
	private UUID bestMatchKiller = null;
	
	private Team defendTeam;
	private Team attackTeam;
	
	private BukkitTask mainTimerID = null;
	private BossBar bossbar = Bukkit.createBossBar("Ожидание", BarColor.WHITE, BarStyle.SEGMENTED_20);
	private Scoreboard scoreboard;
	private Objective objective;
	
	// Methods
	
	//public void CCS(String cmd) {main.getServer().dispatchCommand(main.getServer().getConsoleSender(), "scoreboard teams "+cmd);}
	
	public Arena(String ID, UUID id)
	{
		this.setID(ID);
		this.setCreator(id);
		this.setName(ID);
		
		setME(this);
		
		scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
		bombItem = new ItemStack(bombMaterial);
		ItemMeta meta = bombItem.getItemMeta();
		ArrayList<String> lore = new ArrayList<>();
		meta.setDisplayName("§c§lC4");
		lore.add("§eНажмите на §lкрасную терракоту");
		lore.add("§eЧтобы заложить бомбу");
		meta.setLore(lore);
		bombItem.setItemMeta(meta);
		
		defuseKitsItem = new ItemStack(defuseKitsMaterial);
		meta = defuseKitsItem.getItemMeta();
		lore.clear();
		meta.setDisplayName("§7§lDefuse Kits");
		lore.add("§eНажмите на §lсундук-бомбу");
		lore.add("§eЧтобы начать её обезвреживать");
		meta.setLore(lore);
		defuseKitsItem.setItemMeta(meta);
		
		meta = operatorItem.getItemMeta();
		lore.clear();
		meta.setDisplayName("§b§lВыбор оперативника");
		lore.add("§eОткрыть меню выбора оперативника");
		meta.setLore(lore);
		operatorItem.setItemMeta(meta);
		
		meta = teamItem.getItemMeta();
		lore.clear();
		meta.setDisplayName("§b§lВыбор команды");
		lore.add("§eОткрыть меню выбора команды");
		meta.setLore(lore);
		teamItem.setItemMeta(meta);
		
		defendTeam = scoreboard.registerNewTeam("d"+ID);
		defendTeam.setNameTagVisibility(NameTagVisibility.HIDE_FOR_OTHER_TEAMS);
		
		attackTeam = scoreboard.registerNewTeam("a"+ID);
		attackTeam.setNameTagVisibility(NameTagVisibility.HIDE_FOR_OTHER_TEAMS);
	}
	
	public boolean Start(int startTime)
	{
		if (isStarted || !isEnabled || isStarting) {return false;}
		isStarting = true;
		
		//CCS("add D"+ID);
		//CCS("add A"+ID);
		//CCS("option D"+ID+" nametagVisibility hideForOtherTeams");
		//CCS("option A"+ID+" nametagVisibility hideForOtherTeams");
		
		objective = scoreboard.registerNewObjective(ID, "");
		objective.setDisplaySlot(DisplaySlot.SIDEBAR);
		objective.setDisplayName("§b§lBig Bang Shooter");
		
		for(ItemStack is : operatorsInventory.getContents())
		{
			if (is == null) {continue;}
			if (!is.hasItemMeta()) {continue;}
			ItemMeta meta = is.getItemMeta();
			if (!meta.hasLore()) {continue;}
			meta.setLore(new ArrayList<>());
			is.setItemMeta(meta);
		}
		
		new BukkitRunnable() 
		{
			int timer = startTime;
			
			@Override public void run() 
			{
				if (livePlayers.size() < minPlayers) 
				{
					bossbar.setTitle("§eОжидание");
					bossbar.setProgress(0.0);
					bossbar.setColor(BarColor.WHITE);
					bossbar.setStyle(BarStyle.SEGMENTED_20);
					isStarting = false; 
					if (mainTimerID != null) {mainTimerID.cancel();}
					cancel(); 
					return;
				}
				
				
				
				// Начинается игра
				if (timer == 0)
				{
					isStarted = true;
					isStarting = false;
					bossbar.setTitle("§eРаунд 1 §7§l| §90 §7§l: §60");
					bossbar.setProgress(1.0);
					bossbar.setColor(BarColor.GREEN);
					bossbar.setStyle(BarStyle.SOLID);
					
					main.setEnabledOfMenuArena(me, false);
					roundState = "FREEZE TIME";
					time = 16;
					round = 0;
					
					objective.getScore("§6§lИгроки:").setScore(12);
					int i = 11;
					for(UUID plID : getAllPlayers())
					{
						Player pl = Bukkit.getPlayer(plID);
						if (pl == null) {continue;}
						if (!pl.isOnline()) {continue;}
						pl.setLevel(timer);
						pl.setExp(0F);
						
						String name = pl.getName();
						int team = getPlayerTeam(pl);
						String _clr = "§b";
						
						giveOperatorItems(pl, getPlayerOperator().getOrDefault(pl.getName()+"/"+team, ""));
						teleportToSpawns(pl);
						
						if (team == 0) 
						{
							pl.sendTitle("§eРаунд 1", "§2Вы играете за §9ЗАЩИТУ"); 
							defendTeam.addPlayer(pl);
						}
						
						if (team == 1) 
						{
							_clr = "§e";
							pl.sendTitle("§eРаунд 1", "§2Вы играете за §6АТАКУ");
							attackTeam.addPlayer(pl);
						}
						
						if (i > 0) {objective.getScore(_clr+name+" §f§l— §e0 §f§l— §e0").setScore(i); i--;}
					}
					
					cancel(); 
					return;
				}
				
				bossbar.setProgress(1.0 - ((timer * 1.0)/(startTime * 1.0)));
				for(UUID plID : getAllPlayers())
				{
					Player pl = Bukkit.getPlayer(plID);
					if (pl == null) {continue;}
					if (!pl.isOnline()) {continue;}
					pl.setLevel(timer);
					pl.setExp(Float.parseFloat(bossbar.getProgress()+""));
					
					if (timer <= 5 || timer % 5 == 0) {pl.sendTitle("§aНачало через §e"+timer+" §aсек", "");}
				}
				
				timer--;
			}
		}.runTaskTimer(main, 20L, 20L);
		
		
		
		mainTimerID = new BukkitRunnable() 
		{
			@Override public void run() 
			{
				if (getAllPlayers().size() < minPlayers && !roundState.equals("STOP")) 
				{
					bossbar.setTitle("§eИгра закончена: §lнет игроков");
					bossbar.setProgress(0.0);
					bossbar.setColor(BarColor.RED);
					bossbar.setStyle(BarStyle.SOLID);
					isStarting = false; 
					isStarted = false;
					roundState = "STOP";
					time = 10;
					return;
				}
				
				if (isStarting) {cancel(); return;}
				
				if (time == 0)
				{
					if (roundState.equals("FREEZE TIME"))
					{
						for(UUID plID : getAllPlayers())
						{
							if (plID == null) {continue;}
							Player pl = Bukkit.getPlayer(plID);
							if (pl == null) {continue;}
							if (!pl.isOnline()) {continue;}
							
							pl.getInventory().remove(operatorItem);
							pl.sendMessage("§2§lРаунд "+(round+1)+" §2начался!");
							pl.sendTitle("§bРаунд "+(round+1), "***");
						}
						time = (int) roundTime;
						roundState = "PLAYING";
						return;
					}
					
					if (roundState.equals("PLAYING"))
					{
						int winTeam = 0;
						if (isPlanted) 
						{
							winTeam = 1;
							plantedBomb.getWorld().playSound(plantedBomb, Sound.ENTITY_GENERIC_EXPLODE, 2f, 1.5f);
						}
						
						time = 5;
						roundState = "ROUND END";
						
						if (winTeam == 0) {defendRoundsWin++;}
						else {attackRoundsWin++;}
						round++;
						
						for(UUID plID : getAllPlayers())
						{
							if (plID == null) {continue;}
							Player pl = Bukkit.getPlayer(plID);
							if (pl == null) {continue;}
							if (!pl.isOnline()) {continue;}
							
							String h = "";
							String f = "";
							
							int team = getPlayerTeam(pl);
							if (team == 0) 
							{
								if (winTeam == 0) {h = "§a§lПобеда!"; f = "§2Время закончилось. Бомбы нет.";}
								if (winTeam == 1) {h = "§c§lПоражение..."; f = "§cВремя закончилось. Бомбы поставлена.";}
							}
							else 
							{
								if (winTeam == 0) {h = "§c§lПоражение..."; f = "§cВремя закончилось. Бомбы нет.";}
								if (winTeam == 1) {h = "§a§lПобеда!"; f = "§aБомба взорвалась.";}
							}
							
							if (!h.isEmpty() && !f.isEmpty()) {pl.sendTitle(h, f);}
						}
						
						return;
					}
					
					if (roundState.equals("STOP"))
					{
						Stop(1);
						cancel();
						return;
					}
					
					if (roundState.equals("ROUND END"))
					{
						for(Location l : getEditedBlocks().keySet())
						{
							if (l == null) {continue;}
							Material m = getEditedBlocks().get(l);
							if (m == null) {continue;}
							l.getBlock().setType(m);
						}
						
						for(ItemStack is : operatorsInventory.getContents())
						{
							if (is == null) {continue;}
							if (!is.hasItemMeta()) {continue;}
							ItemMeta meta = is.getItemMeta();
							if (!meta.hasDisplayName()) {continue;}
							
							ArrayList<String> lore = new ArrayList<>();
							String opName = main.removeCC(meta.getDisplayName());
							
							for(int i = 0; i < 2; i++)
							{
								String pWhoTake = playerOperator.getOrDefault(opName+"/"+i, "");
								if (!pWhoTake.isEmpty())
								{
									lore.add("");
									lore.add("§cВзят:");
									lore.add("§b"+pWhoTake);
								}
							}
							
							meta.setLore(lore);
							is.setItemMeta(meta);
						}
						
						if (round == maxRounds)
						{
							roundState = "STOP";
							time = 10;
							for(UUID plID : getAllPlayers())
							{
								if (plID == null) {continue;}
								Player pl = Bukkit.getPlayer(plID);
								if (pl == null) {continue;}
								if (!pl.isOnline()) {continue;}
								
								int winTeam = 0;
								if (defendRoundsWin > attackRoundsWin) {winTeam = 0; pl.sendTitle("§e§lИгра закончена!", "§eМатч выиграла: §9ЗАЩИТА");}
								if (defendRoundsWin < attackRoundsWin) {winTeam = 1; pl.sendTitle("§e§lИгра закончена!", "§eМатч выиграла: §6АТАКА");}
								if (defendRoundsWin == attackRoundsWin) {winTeam = -1; pl.sendTitle("§e§lИгра закончена!", "§eНичья!");}
								
								String _winTeam = "§9§lЗАЩИТЫ";
								String _bombMessage = "обезврежено: §e"+bombDefused;
								int _kills = defendKills;
								int _roundsWin = defendRoundsWin;
								
								if (winTeam == 1) 
								{
									_winTeam = "§e§lАТАКИ"; 
									_kills = attackKills;
									_roundsWin = attackRoundsWin;
									_bombMessage = "установлено: §e"+bombPlanted;
								}
								
								pl.sendMessage("§7========================================================================");
								pl.sendMessage("");
								pl.sendMessage("  §d[ПОБЕДА] §6§lПобедителем становится "+_winTeam+"§6§l!");
								pl.sendMessage("  §d[ПОБЕДА] §fРаундов выиграно: §e"+_roundsWin);
								pl.sendMessage("  §d[ПОБЕДА] §fУбито: §e"+_kills+"§f игроков");
								pl.sendMessage("  §d[ПОБЕДА] §fБомб "+_bombMessage);
								pl.sendMessage("");
								pl.sendMessage("§7========================================================================");
							}
							return;
						}
						
						time = 14;
						roundState = "FREEZE TIME";
						
						bossbar.setTitle("§eРаунд "+(round+1)+" §7§l| §9"+defendRoundsWin+" §7§l: §6"+attackRoundsWin);
						bossbar.setProgress(1.0);
						bossbar.setColor(BarColor.GREEN);
						bossbar.setStyle(BarStyle.SOLID);
						
						isPlanted = false;
						isPlanting = false;
						isDefusing = false;
						
						ArrayList<UUID> players = (ArrayList<UUID>) deathPlayers.clone();
						players.addAll(livePlayers);
						for(UUID plID : players)
						{
							if (plID == null) {continue;}
							Player pl = Bukkit.getPlayer(plID);
							if (pl == null) {continue;}
							if (!pl.isOnline()) {continue;}
							
							if (!livePlayers.contains(plID)) {livePlayers.add(plID);}
							if (deathPlayers.contains(plID)) {deathPlayers.remove(plID);}
							
							for(PotionEffect pe : pl.getActivePotionEffects()) {pl.removePotionEffect(pe.getType());}
							pl.setGameMode(GameMode.ADVENTURE);
							pl.setHealth(20.0);
							pl.setFoodLevel(20);
							String plOp = getPlayerOperator().getOrDefault(pl.getName()+"/"+getPlayerTeam(pl), "");
							pl.getInventory().clear();
							pl.getInventory().setItemInOffHand(null);
							if (!plOp.isEmpty()) {giveOperatorItems(pl, plOp);}
							teleportToSpawns(pl);
						}
					}
					
					return;
				}
				
				double progress = (time * 1.0)/(roundTime * 1.0);
				bossbar.setProgress(progress);
				for(UUID plID : getAllPlayers())
				{
					if (plID == null) {continue;}
					Player pl = Bukkit.getPlayer(plID);
					if (pl == null) {continue;}
					if (!pl.isOnline()) {continue;}
					pl.setLevel(time);
					pl.setExp(Float.parseFloat(bossbar.getProgress()+""));
					
					if (time % 5 == 0 && time < 20)
					{
						if (roundState.equals("PLAYING")) {pl.sendTitle("§eОсталось "+time+" сек", "§e§lПоторопитесь!");}
						else if (roundState.equals("FREEZE TIME"))
						{pl.sendTitle("§2Раунд начнётся через §e"+time+" §2сек", "§2Вы готовы?");}
					}
				}
				
				if (isPlanted && roundState.equals("PLAYING") && plantedBomb != null) 
				{
					float f = (float) (1.0f+(1.0f-(progress*1.0f)));
					plantedBomb.getWorld().playSound(plantedBomb, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, f, f);
				}
				time--;
			}
		}.runTaskTimer(main, 20L * (startTime+1), 20L);
		
		
		
		return true;
	}
	
	public void Stop(int i)
	{
		if (!isStarted) {return;}
		
		isStarted = false;
		isDefusing = false;
		isPlanted = false;
		isStarting = false;
		
		mainTimerID.cancel();
		for(Location l : getEditedBlocks().keySet())
		{
			if (l == null) {continue;}
			Material m = getEditedBlocks().get(l);
			if (m == null) {continue;}
			l.getBlock().setType(m);
		}
		
		roundState = "WAITING";
		for(UUID plID : getAllPlayers())
		{
			if (plID == null) {continue;}
			Player pl = Bukkit.getPlayer(plID);
			if (pl == null) {continue;}
			
			int team = getPlayerTeam(pl);
			if (team == 0) {defendTeam.removePlayer(pl);}
			if (team == 1) {attackTeam.removePlayer(pl);}
			
			if (!pl.isOnline()) {continue;}
			pl.setGameMode(GameMode.ADVENTURE);
			pl.teleport(lobbyLocation);
			pl.getInventory().clear();
			pl.getInventory().setItemInOffHand(null);
			pl.getInventory().setItem(8, operatorItem);
		}
		
		teamAttack.clear();
		teamDefend.clear();
		livePlayers.clear();
		spectators.clear();
		deathPlayers.clear();
		
		for(Entity ent : entities)
		{
			ent.getLocation().getChunk().load();
			ent.remove();
		}
		
		for(Location l : editedBlocks.keySet()) {l.getBlock().setType(editedBlocks.get(l));}
		time = 0;
		
		objective.unregister();
		main.setEnabledOfMenuArena(me, true);
		
		if (getAllPlayers().size() >= minPlayers) {Start(15);}
	}

	public void giveOperatorItems(Player pl, String plOp)
	{
		ArrayList<ItemStack> isList = getOperatorsItems().getOrDefault(plOp, new ArrayList<>());
		if (isList == null) {return;}
		if (isList.size() == 0) {return;}
		int i = 0;
		int team = getPlayerTeam(pl);
		for(ItemStack is : isList)
		{
			if (is == null) {continue;}
			is = is.clone();
			String matName = is.getType().name().toUpperCase();
			
			if (matName.contains("LEATHER"))
			{
				LeatherArmorMeta tam = (LeatherArmorMeta) is.getItemMeta();
				if (team == 0) {tam.setColor(Color.AQUA);}
				if (team == 1) {tam.setColor(Color.ORANGE);}
				is.setItemMeta(tam);
			}
			
			if (matName.contains("HELMET")) {pl.getInventory().setHelmet(is); continue;}
			if (matName.contains("CHESTPLATE")) {pl.getInventory().setChestplate(is); continue;}
			if (matName.contains("LEGGINGS")) {pl.getInventory().setLeggings(is); continue;}
			if (matName.contains("BOOTS")) {pl.getInventory().setBoots(is); continue;}
			
			if (i == 6) {i = 9;}
			pl.getInventory().setItem(i, is);
			i++;
		}
		
		getPlayerOperator().put(pl.getName()+"/"+team, plOp);
		getPlayerOperator().put(plOp+"/"+team, pl.getName());
		giveClassItem(pl);
	}

	public void giveClassItem(Player p)
	{
		p.getInventory().setItem(8, operatorItem);
		UUID id = p.getUniqueId();
		if (teamAttack.contains(id)) {p.getInventory().setItem(7, bombItem);}
		if (teamDefend.contains(id)) {p.getInventory().setItem(7, defuseKitsItem);}
		if (roundState.equals("WAITING")) {p.getInventory().setItem(7, teamItem);}
	}

	public void kickPlayer(Player p) 
	{
		UUID id = p.getUniqueId();
		if (!getAllPlayers().contains(id)) {return;}
		
		//CCS("remove D"+ID);
		//CCS("remove A"+ID);
		
		deathPlayers.remove(id);
		teamAttack.remove(id);
		teamDefend.remove(id);
		livePlayers.remove(id);
		spectators.remove(id);
		bossbar.removePlayer(p);
		p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
		main.clearPlayer(p);
		main.loadPlayerData(p);
		
		main.refreshArenaInMenu(this);
		if (livePlayers.size() < 2) {Stop(1);}
	}

	public void teleportToSpawns(Player pl) 
	{
		UUID plID = pl.getUniqueId();
		Location l = null;
		Random rnd = new Random(); 
		ArrayList<Integer> _spns = new ArrayList<>();
		int t = getPlayerTeam(pl);
		
		if (t == 0) 
		{
			if (!teamDefend.contains(plID)) {teamDefend.add(plID);}
			int _tempTeam = rnd.nextInt(getDefendSpawns().size()-1);
			while (_spns.contains(_tempTeam)) {_tempTeam = rnd.nextInt(getDefendSpawns().size()-1);}
			l = getDefendSpawns().get(_tempTeam);
			
		}
		else 
		{
			if (!teamAttack.contains(plID)) {teamAttack.add(plID);}
			int _tempTeam = rnd.nextInt(getAttackSpawns().size()-1);
			while (_spns.contains(_tempTeam)) {_tempTeam = rnd.nextInt(getAttackSpawns().size()-1);}
			l = getAttackSpawns().get(_tempTeam);
		}
		
		giveClassItem(pl);
		pl.teleport(l.clone().add(0.5,0,0.5));
	}
	
	public void StartNewRound(Player p)
	{
		//UUID plID = p.getUniqueId();
		//int team = getPlayerTeam(p);
		
		// ТП игрока на респ
		// Выдать предметы предыдущего раунда (если не умирал)
		// Выдать предмет выбора оператора
	}

	public boolean addPlayer(Player p) 
	{
		UUID id = p.getUniqueId();
		if (getAllPlayers().contains(id)) {return false;}
		if (isStarted || !isEnabled) {return false;}
		livePlayers.add(id);
		bossbar.addPlayer(p);
		main.savePlayerData(p);
		main.clearPlayer(p);
		p.teleport(lobbyLocation);
		p.setScoreboard(scoreboard);
		
		if (livePlayers.size() >= minPlayers) {Start(20);}
		return true;
	}

	public void addDeathPlayer(Player p) 
	{
		UUID plID = p.getUniqueId();
		deathPlayers.add(plID);
		livePlayers.remove(plID);
		p.setHealth(20.0);
		p.setGameMode(GameMode.SPECTATOR);
		p.getInventory().clear();
		p.getInventory().setItemInOffHand(null);
		if (p.getOpenInventory() != null) {p.closeInventory();}
		allDeaths++;
		
		String opName = playerOperator.getOrDefault(p.getName()+"/"+getPlayerTeam(p), "");
		if (!opName.isEmpty()) {playerOperator.remove(opName); playerOperator.remove(p.getName());}
		
		int _kills = playersMatchStats.getOrDefault(p.getName()+"-Round-"+round+"-Deaths", 0) + 1;
		playersMatchStats.put(p.getName()+"-Round-"+round+"-Deaths", _kills);
		
		int _deaths = playersMatchStats.getOrDefault(p.getName()+"-Deaths", 0) + 1;
		playersMatchStats.put(p.getName()+"-Deaths", _deaths);
		
		int defLived = 0;
		int attLived = 0;
		
		int i = 11;
		for(UUID id : getAllPlayers())
		{
			if (id == null) {continue;}
			Player pl = Bukkit.getPlayer(id);
			if (pl == null) {livePlayers.remove(id); deathPlayers.add(id); continue;}
			if (!pl.isOnline()) {livePlayers.remove(id); deathPlayers.add(id); continue;}
			if (!pl.getGameMode().equals(GameMode.ADVENTURE)) {continue;}
			int team = getPlayerTeam(pl);
			if (team == 0) {defLived++;}
			if (team == 1) {attLived++;}
			
			String _clr = "§b";
			if (team == 1) {_clr = "§e";}
			_kills = playersMatchStats.getOrDefault(pl.getName()+"-Kills", 0);
			_deaths = playersMatchStats.getOrDefault(pl.getName()+"-Deaths", 0);
			
			objective.getScore(_clr+pl.getName()+" §f§l— §e"+_kills+" §f§l— §e"+_deaths).setScore(i);
			i--;
		}
		
		if (defLived == 0) 
		{
			attackRoundsWin++;
			round++;
			
			for(UUID id : getAllPlayers())
			{
				if (id == null) {continue;}
				Player pl = Bukkit.getPlayer(id);
				if (pl == null) {continue;}
				if (!pl.isOnline()) {continue;}
				
				int team = getPlayerTeam(pl);
				if (team == 0) {pl.sendTitle("§c§lПоражение...", "§cВся ваша команда была убита.");}
				if (team == 1) {pl.sendTitle("§a§lПобеда!", "§aВсе защитники были устранены.");}
				if (team == -1) {pl.sendTitle("§a§lРаунд закончен.", "§aПобедила команда §6§lАТАКИ.");}
			}
			
			roundState = "ROUND END"; 
			time = 5;
			return;
		}
		
		if (attLived == 0 && !isPlanted) 
		{
			defendRoundsWin++;
			round++;
			
			for(UUID id : getAllPlayers())
			{
				if (id == null) {continue;}
				Player pl = Bukkit.getPlayer(id);
				if (pl == null) {continue;}
				if (!pl.isOnline()) {continue;}
				
				int team = getPlayerTeam(pl);
				if (team == 1) {pl.sendTitle("§c§lПоражение...", "§cВся ваша команда была убита.");}
				if (team == 0) {pl.sendTitle("§a§lПобеда!", "§aВсе атакующие были устранены.");}
				if (team == -1) {pl.sendTitle("§a§lРаунд закончен.", "§aПобедила команда §9§lЗАЩИТЫ.");}
			}
			
			roundState = "ROUND END"; 
			time = 5;
			return;
		}
	}
	
	public void addKilledPlayer(Player p, Player killer) 
	{
		allKills++;
		roundKills++;
		
		int _kills = playersMatchStats.getOrDefault(killer.getName()+"-Round-"+round+"-Kills", 0) + 1;
		playersMatchStats.put(killer.getName()+"-Round-"+round+"-Kills", _kills);
		
		int _deaths = playersMatchStats.getOrDefault(killer.getName()+"-Kills", 0) + 1;
		playersMatchStats.put(killer.getName()+"-Kills", _deaths);
		
		addDeathPlayer(p);
		
		int team = getPlayerTeam(p);
		if (team == 0) {defendKills++;}
		if (team == 1) {attackKills++;}
	}
	
	public int getPlayerTeam(Player pl)
	{
		UUID plID = pl.getUniqueId();
		Random rnd = new Random(); 
		
		int t = -2;
		if (teamAttack.contains(plID)) {t = 1;}
		if (teamDefend.contains(plID)) {t = 0;}
		if (spectators.contains(plID)) {t = -1;}
		if (t == -2) 
		{
			if (teamAttack.size() > teamDefend.size()) {t = 0;} else
			if (teamAttack.size() < teamDefend.size()) {t = 1;} else
			{t = rnd.nextInt(2);}
		}
		
		return t;
	}
	
	public ArrayList<UUID> getAllPlayers()
	{
		ArrayList<UUID> list = new ArrayList<>();
		
		for(UUID id : spectators)
		{
			if (list.contains(id)) {continue;}
			list.add(id);
		}
		
		for(UUID id : teamAttack)
		{
			if (list.contains(id)) {continue;}
			list.add(id);
		}
		
		for(UUID id : teamDefend)
		{
			if (list.contains(id)) {continue;}
			list.add(id);
		}
		
		for(UUID id : livePlayers)
		{
			if (list.contains(id)) {continue;}
			list.add(id);
		}
		
		for(UUID id : deathPlayers)
		{
			if (list.contains(id)) {continue;}
			list.add(id);
		}
		
		return list;
	}
	
	public void addBombPlanted(int count) {bombPlanted += count;}
	public void addBombDefused(int count) {bombDefused += count;}
	
	public String getState()
	{
		return roundState;
	}
	
	public void setState(String s)
	{
		roundState = s;
	}

	public String getID() {
		return ID;
	}

	public void setID(String iD) {
		ID = iD;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public UUID getCreator() {
		return creator;
	}

	public void setCreator(UUID creator) {
		this.creator = creator;
	}

	public int getMaxPlayers() {
		return maxPlayers;
	}

	public void setMaxPlayers(int maxPlayers) {
		this.maxPlayers = maxPlayers;
	}

	public int getMinPlayers() {
		return minPlayers;
	}

	public void setMinPlayers(int minPlayers) {
		this.minPlayers = minPlayers;
	}

	public int getMaxRounds() {
		return maxRounds;
	}

	public void setMaxRounds(int maxRounds) {
		this.maxRounds = maxRounds;
	}

	public long getRoundTime() {
		return roundTime;
	}

	public void setRoundTime(int roundTime) {
		this.roundTime = roundTime;
	}

	public ArrayList<Location> getDefendSpawns() {
		return defendSpawns;
	}

	public void setDefendSpawns(ArrayList<Location> defendSpawns) {
		this.defendSpawns = defendSpawns;
	}

	public ArrayList<Location> getAttackSpawns() {
		return attackSpawns;
	}

	public void setAttackSpawns(ArrayList<Location> attackSpawns) {
		this.attackSpawns = attackSpawns;
	}

	public ArrayList<Location> getBonusSpawns() {
		return bonusSpawns;
	}

	public void setBonusSpawns(ArrayList<Location> bonusSpawns) {
		this.bonusSpawns = bonusSpawns;
	}

	public Material getSitePlantMaterial() {
		return sitePlantMaterial;
	}

	public void setSitePlantMaterial(Material sitePlantMaterial) {
		this.sitePlantMaterial = sitePlantMaterial;
	}

	public Material getBombMaterial() {
		return bombMaterial;
	}

	public void setBombMaterial(Material bombMaterial) {
		this.bombMaterial = bombMaterial;
	}

	public Material getDefuseKitsMaterial() {
		return defuseKitsMaterial;
	}

	public void setDefuseKitsMaterial(Material defuseKitsMaterial) {
		this.defuseKitsMaterial = defuseKitsMaterial;
	}

	public ArrayList<UUID> getTeamDefend() {
		return teamDefend;
	}

	public void setTeamDefend(ArrayList<UUID> teamDefend) {
		this.teamDefend = teamDefend;
	}

	public ArrayList<UUID> getTeamAttack() {
		return teamAttack;
	}

	public void setTeamAttack(ArrayList<UUID> teamAttack) {
		this.teamAttack = teamAttack;
	}

	public ArrayList<UUID> getLivePlayers() {
		return livePlayers;
	}

	public void setLivePlayers(ArrayList<UUID> livePlayers) {
		this.livePlayers = livePlayers;
	}

	public ArrayList<UUID> getaddDeathPlayers() {
		return deathPlayers;
	}

	public void setaddDeathPlayers(ArrayList<UUID> addDeathPlayers) {
		this.deathPlayers = addDeathPlayers;
	}

	public ArrayList<UUID> getSpectators() {
		return spectators;
	}

	public void setSpectators(ArrayList<UUID> spectators) {
		this.spectators = spectators;
	}

	public HashMap<Location, Material> getEditedBlocks() {
		return editedBlocks;
	}

	public void setEditedBlocks(HashMap<Location, Material> editedBlocks) {
		this.editedBlocks = editedBlocks;
	}

	public HashMap<String, Integer> getPlayersMatchStats() {
		return playersMatchStats;
	}

	public void setPlayersMatchStats(HashMap<String, Integer> playersMatchStats) {
		this.playersMatchStats = playersMatchStats;
	}

	public Location getPlantedBomb() {
		return plantedBomb;
	}

	public void setPlantedBomb(Location plantedBomb) {
		this.plantedBomb = plantedBomb;
	}

	public Inventory getDefuseInventory() {
		return defuseInventory;
	}

	public void setDefuseInventory(Inventory defuseInventory) {
		this.defuseInventory = defuseInventory;
	}

	public Inventory getPlantInventory() {
		return plantInventory;
	}

	public void setPlantInventory(Inventory plantInventory) {
		this.plantInventory = plantInventory;
	}

	public boolean isPlanted() {
		return isPlanted;
	}

	public void setPlanted(boolean isPlanted) {
		this.isPlanted = isPlanted;
	}

	public boolean isPlanting() {
		return isPlanting;
	}

	public void setPlanting(boolean isPlanting) {
		this.isPlanting = isPlanting;
	}

	public boolean isDefusing() {
		return isDefusing;
	}

	public void setDefusing(boolean isDefusing) {
		this.isDefusing = isDefusing;
	}

	public int getTime() {
		return time;
	}

	public void setTime(int time) {
		this.time = time;
	}

	public int getRound() {
		return round;
	}

	public void setRound(int round) {
		this.round = round;
	}

	public int getAllKills() {
		return allKills;
	}

	public void setAllKills(int allKills) {
		this.allKills = allKills;
	}

	public int getAllDeaths() {
		return allDeaths;
	}

	public void setAllDeaths(int allDeaths) {
		this.allDeaths = allDeaths;
	}

	public int getRoundKills() {
		return roundKills;
	}

	public void setRoundKills(int roundKills) {
		this.roundKills = roundKills;
	}

	public int getBeestRoundKills() {
		return beestRoundKills;
	}

	public void setBeestRoundKills(int beestRoundKills) {
		this.beestRoundKills = beestRoundKills;
	}

	public UUID getBestRoundKiller() {
		return bestRoundKiller;
	}

	public void setBestRoundKiller(UUID bestRoundKiller) {
		this.bestRoundKiller = bestRoundKiller;
	}

	public UUID getBestMatchKiller() {
		return bestMatchKiller;
	}

	public void setBestMatchKiller(UUID bestMatchKiller) {
		this.bestMatchKiller = bestMatchKiller;
	}

	public BossBar getBossbar() {
		return bossbar;
	}

	public void setBossbar(BossBar bossbar) {
		this.bossbar = bossbar;
	}

	public int getDefendRoundsWin() {
		return defendRoundsWin;
	}

	public void setDefendRoundsWin(int defendRoundsWin) {
		this.defendRoundsWin = defendRoundsWin;
	}

	public int getAttackRoundsWin() {
		return attackRoundsWin;
	}

	public void setAttackRoundsWin(int attackRoundsWin) {
		this.attackRoundsWin = attackRoundsWin;
	}

	public ItemStack getBombItem() {
		return bombItem;
	}

	public void setBombItem(ItemStack bombItem) {
		this.bombItem = bombItem;
	}

	public ItemStack getDefuseKitsItem() {
		return defuseKitsItem;
	}

	public void setDefuseKitsItem(ItemStack defuseKitsItem) {
		this.defuseKitsItem = defuseKitsItem;
	}

	public Location getLobbyLocation()
	{
		return lobbyLocation;
	}

	public void setLobbyLocation(Location lobbyLocation)
	{
		this.lobbyLocation = lobbyLocation;
	}

	public Inventory getOperatorsInventory() {
		return operatorsInventory;
	}

	public void setOperatorsInventory(Inventory operatorsInventory) {
		this.operatorsInventory = operatorsInventory;
	}

	public HashMap<String, ArrayList<ItemStack>> getOperatorsItems() {
		return operatorsItems;
	}

	public void setOperatorsItems(HashMap<String, ArrayList<ItemStack>> operatorsItems) {
		this.operatorsItems = operatorsItems;
	}

	public ItemStack getOperatorItem() {
		return operatorItem;
	}

	public void setOperatorItem(ItemStack operatorItem) {
		this.operatorItem = operatorItem;
	}

	public HashMap<String, String> getPlayerOperator()
	{
		return playerOperator;
	}

	public void setPlayerOperator(HashMap<String, String> playerOperator)
	{
		this.playerOperator = playerOperator;
	}

	public ArrayList<Entity> getEntities()
	{
		return entities;
	}

	public void setEntities(ArrayList<Entity> entities)
	{
		this.entities = entities;
	}

	public BukkitTask getMainTimerID()
	{
		return mainTimerID;
	}

	public void setMainTimerID(BukkitTask mainTimerID)
	{
		this.mainTimerID = mainTimerID;
	}

	public ItemStack getTeamItem()
	{
		return teamItem;
	}

	public void setTeamItem(ItemStack teamItem)
	{
		this.teamItem = teamItem;
	}

	public Arena getME()
	{
		return me;
	}

	public void setME(Arena me)
	{
		this.me = me;
	}

	public Scoreboard getScoreboard()
	{
		return scoreboard;
	}

	public void setScoreboard(Scoreboard scoreboard)
	{
		this.scoreboard = scoreboard;
	}

	public Objective getObjective()
	{
		return objective;
	}

	public void setObjective(Objective objective)
	{
		this.objective = objective;
	}

	public Team getDefendTeam()
	{
		return defendTeam;
	}

	public void setDefendTeam(Team defendTeam)
	{
		this.defendTeam = defendTeam;
	}

	public Team getAttackTeam()
	{
		return attackTeam;
	}

	public void setAttackTeam(Team attackTeam)
	{
		this.attackTeam = attackTeam;
	}
}
