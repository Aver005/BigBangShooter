package kiviuly.BigBangShooter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

public class Arena 
{
	// Const
	
	private static final Main main = Main.getInstance();
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
	
	private ItemStack bombItem = null;
	private ItemStack defuseKitsItem = null;
	
	public boolean isEnabled = false;
	public boolean isStarted = false;
	public boolean isStarting = false;
	
	
	// Match vars
	
	private ArrayList<UUID> teamDefend = new ArrayList<>();
	private ArrayList<UUID> teamAttack = new ArrayList<>();
	private ArrayList<UUID> livePlayers = new ArrayList<>();
	private ArrayList<UUID> deathPlayers = new ArrayList<>();
	private ArrayList<UUID> spectators = new ArrayList<>();
	
	private HashMap<Location, Material> editedBlocks = new HashMap<>();
	private HashMap<String, Integer> playersMatchStats = new HashMap<>();
	
	private Location plantedBomb = null;
	
	private Inventory defuseInventory = Bukkit.createInventory(null, 27, "Обезвреживание бомбы");
	private Inventory plantInventory = Bukkit.createInventory(null, 27, "Закладка бомбы");
	
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
	
	private UUID bestRoundKiller = null;
	private UUID bestMatchKiller = null;
	
	private BossBar bossbar = Bukkit.createBossBar("Ожидание", BarColor.WHITE, BarStyle.SEGMENTED_20);
	
	// Methods
	
	public Arena(String ID, UUID id)
	{
		this.setID(ID);
		this.setCreator(id);
		this.setName(ID);
		
		bombItem = new ItemStack(bombMaterial);
		ItemMeta meta = bombItem.getItemMeta();
		ArrayList<String> lore = new ArrayList<>();
		meta.setDisplayName("C4");
		lore.add("Нажмите на красную терракоту");
		lore.add("Чтобы заложить бомбу");
		meta.setLore(lore);
		bombItem.setItemMeta(meta);
		
		defuseKitsItem = new ItemStack(defuseKitsMaterial);
		meta = defuseKitsItem.getItemMeta();
		lore = new ArrayList<>();
		meta.setDisplayName("Defuse Kits");
		lore.add("Нажмите на сундук-бомбу");
		lore.add("Чтобы начать её обезвреживать");
		meta.setLore(lore);
		defuseKitsItem.setItemMeta(meta);
	}
	
	public boolean Start(int startTime)
	{
		if (isStarted || !isEnabled || isStarting) {return false;}
		isStarting = true;
		
		new BukkitRunnable() 
		{
			int timer = startTime;
			@Override public void run() 
			{
				if (livePlayers.size() < minPlayers) 
				{
					bossbar.setTitle("Ожидание");
					bossbar.setProgress(0.0);
					bossbar.setColor(BarColor.WHITE);
					bossbar.setStyle(BarStyle.SEGMENTED_20);
					isStarting = false; 
					cancel(); 
					return;
				}
				
				// Начинается игра
				if (timer == 0)
				{
					isStarted = true;
					isStarting = false;
					bossbar.setTitle("Раунд 1 | 0 : 0");
					bossbar.setProgress(1.0);
					bossbar.setColor(BarColor.GREEN);
					bossbar.setStyle(BarStyle.SOLID);
					
					roundState = "FREEZE TIME";
					time = roundTime;
					round = 0;
					
					for(UUID plID : livePlayers)
					{
						Player pl = Bukkit.getPlayer(plID);
						if (pl == null) {livePlayers.remove(plID); continue;}
						if (!pl.isOnline()) {livePlayers.remove(plID); continue;}
						pl.setLevel(timer);
						pl.setExp(0F);
						
						teleportToSpawns(pl);
						int team = getPlayerTeam(pl);
						if (team == 0) {pl.sendTitle("Раунд 1", "Вы играете за ЗАЩИТУ");}
						if (team == 1) {pl.sendTitle("Раунд 1", "Вы играете за АТАКУ");}
					}
					
					cancel(); 
					return;
				}
				
				bossbar.setProgress(1.0 - ((timer * 1.0)/(startTime * 1.0)));
				for(UUID plID : livePlayers)
				{
					Player pl = Bukkit.getPlayer(plID);
					if (pl == null) {livePlayers.remove(plID); continue;}
					if (!pl.isOnline()) {livePlayers.remove(plID); continue;}
					pl.setLevel(timer);
					pl.setExp(Float.parseFloat(bossbar.getProgress()+""));
					
					if (timer <= 5 || timer % 5 == 0) {pl.sendTitle("Начало через "+timer+" сек", "");}
					
				}
				
				timer--;
			}
		}.runTaskTimer(main, 20L, 20L);
		
		
		
		new BukkitRunnable() 
		{
			@Override public void run() 
			{
				if (livePlayers.size() < 2) 
				{
					bossbar.setTitle("Игра закончена: нет игроков");
					bossbar.setProgress(0.0);
					bossbar.setColor(BarColor.RED);
					bossbar.setStyle(BarStyle.SOLID);
					isStarting = false; 
					isStarted = false;
					roundState = "STOP";
					time = 10;
					return;
				}
				
				if (time == 0)
				{
					if (roundState == "FREEZE TIME")
					{
						time = (int) roundTime;
						roundState = "PLAYING";
					}
					else if (roundState == "PLAYING")
					{
						int winTeam = 0;
						if (isPlanted) {winTeam = 1;}
						time = 5;
						roundState = "ROUND END";
						
						if (winTeam == 0) {defendRoundsWin++;}
						else {attackRoundsWin++;}
						round++;
						
						for(UUID plID : livePlayers)
						{
							Player pl = Bukkit.getPlayer(plID);
							if (pl == null) {livePlayers.remove(plID); continue;}
							if (!pl.isOnline()) {livePlayers.remove(plID); continue;}
							
							int team = getPlayerTeam(pl);
							if (team == 0) 
							{
								if (winTeam == 0) {pl.sendTitle("Победа!", "Время закончилось. Бомбы нет.");}
								if (winTeam == 1) {pl.sendTitle("Поражение...", "Время закончилось. Бомбы поставлена.");}
							}
							else 
							{
								if (winTeam == 0) {pl.sendTitle("Поражение...", "Время закончилось. Бомбы нет.");}
								if (winTeam == 1) {pl.sendTitle("Победа!", "Бомба взорвалась.");}
							}
						}
					}
					else if (roundState == "STOP")
					{
						roundState = "WAITING";
						for(UUID plID : livePlayers)
						{
							Player pl = Bukkit.getPlayer(plID);
							if (pl == null) {livePlayers.remove(plID); continue;}
							if (!pl.isOnline()) {livePlayers.remove(plID); continue;}
							kickPlayer(pl);
						}
						
						cancel();
					}
					
					else if (roundState == "ROUND END")
					{
						if (round == maxRounds)
						{
							roundState = "STOP";
							time = 10;
							for(UUID plID : livePlayers)
							{
								Player pl = Bukkit.getPlayer(plID);
								if (pl == null) {livePlayers.remove(plID); continue;}
								if (!pl.isOnline()) {livePlayers.remove(plID); continue;}
								
								if (defendRoundsWin > attackRoundsWin) {pl.sendTitle("Игра закончена!", "Матч выиграла: ЗАЩИТА");}
								if (defendRoundsWin < attackRoundsWin) {pl.sendTitle("Игра закончена!", "Матч выиграла: АТАКА");}
								if (defendRoundsWin == attackRoundsWin) {pl.sendTitle("Игра закончена!", "Ничья!");}
							}
							return;
						}
						
						time = 8;
						roundState = "FREEZE TIME";
						
						bossbar.setTitle("Раунд "+(round+1)+" | "+teamDefend+" : "+teamAttack);
						bossbar.setProgress(1.0);
						bossbar.setColor(BarColor.GREEN);
						bossbar.setStyle(BarStyle.SOLID);
						
						for(UUID plID : livePlayers)
						{
							Player pl = Bukkit.getPlayer(plID);
							if (pl == null) {livePlayers.remove(plID); continue;}
							if (!pl.isOnline()) {livePlayers.remove(plID); continue;}
							teleportToSpawns(pl);
						}
					}
					
					return;
				}
				
				bossbar.setProgress((time * 1.0)/(roundTime * 1.0));
				for(UUID plID : livePlayers)
				{
					Player pl = Bukkit.getPlayer(plID);
					if (pl == null) {livePlayers.remove(plID); continue;}
					if (!pl.isOnline()) {livePlayers.remove(plID); continue;}
					pl.setLevel(time);
					pl.setExp(Float.parseFloat(bossbar.getProgress()+""));
					
					if (time % 5 == 0 && time < 20)
					{
						String title = "";
						String footer = "";
						
						if (roundState == "PLAYING")
						{
							title = "Осталось "+time+" сек";
							footer = "Поторопитесь!";
						}
						else if (roundState == "FREEZE TIME")
						{
							title = "Раунд начнётся через "+time+" сек";
							footer = "Вы готовы?";
						}
						
						pl.sendTitle(title, footer);
					}
					
					
				}
				
				time--;
			}
		}.runTaskTimer(main, 20L, 20L);
		
		return true;
	}
	
	public void addPlayer(Player p) 
	{
		
	}
	
	public void kickPlayer(Player p) 
	{
		
	}
	
	public void deathPlayer(Player p) 
	{
		
	}
	
	public void addKilledPlayer(Player p, Player killer) 
	{
		UUID plID = p.getUniqueId();
		deathPlayers.add(plID);
		livePlayers.remove(plID);
		p.setHealth(20.0);
		p.setGameMode(GameMode.SPECTATOR);
		
		allKills++;
		roundKills++;
		allDeaths++;
		int _kills = playersMatchStats.getOrDefault(killer.getName()+"-Round-"+round+"-Kills", 0) + 1;
		playersMatchStats.put(killer.getName()+"-Round-"+round+"-Kills", _kills);
		
		int _deaths = playersMatchStats.getOrDefault(p.getName()+"-Round-"+round+"-Deaths", 0) + 1;
		playersMatchStats.put(p.getName()+"-Round-"+round+"-Deaths", _deaths);
		
		int defLived = 0;
		int attLived = 0;
		for(UUID id : livePlayers)
		{
			Player pl = Bukkit.getPlayer(id);
			if (pl == null) {livePlayers.remove(id); continue;}
			if (!pl.isOnline()) {livePlayers.remove(id); continue;}
			if (!pl.getGameMode().equals(GameMode.ADVENTURE)) {continue;}
			int team = getPlayerTeam(pl);
			if (team == 0) {defLived++;}
			if (team == 1) {attLived++;}
		}
		
		if (defLived == 0) 
		{
			attackRoundsWin++;
			round++;
			
			for(UUID id : livePlayers)
			{
				Player pl = Bukkit.getPlayer(id);
				if (pl == null) {livePlayers.remove(id); continue;}
				if (!pl.isOnline()) {livePlayers.remove(id); continue;}
				
				int team = getPlayerTeam(pl);
				if (team == 0) {pl.sendTitle("Поражение...", "Вся ваша команда была убита.");}
				else {pl.sendTitle("Победа!", "Все защитники были устранены.");}
			}
			
			roundState = "ROUND END"; 
			time = 5;
		}
		
		if (attLived == 0 && !isPlanted) 
		{
			defendRoundsWin++;
			round++;
			
			for(UUID id : livePlayers)
			{
				Player pl = Bukkit.getPlayer(id);
				if (pl == null) {livePlayers.remove(id); continue;}
				if (!pl.isOnline()) {livePlayers.remove(id); continue;}
				
				int team = getPlayerTeam(pl);
				if (team == 1) {pl.sendTitle("Поражение...", "Вся ваша команда была убита.");}
				else {pl.sendTitle("Победа!", "Все атакующие были устранены.");}
			}
			
			roundState = "ROUND END"; 
			time = 5;
		}
	}
	
	public int getPlayerTeam(Player pl)
	{
		UUID plID = pl.getUniqueId();
		Random rnd = new Random(); 
		
		int t = -1;
		if (teamAttack.contains(plID)) {t = 1;}
		if (teamDefend.contains(plID)) {t = 0;}
		if (t == -1) 
		{
			if (teamAttack.size() > teamDefend.size()) {t = 0;} else
			if (teamAttack.size() < teamDefend.size()) {t = 1;} else
			{t = rnd.nextInt(1);}
		}
		
		return t;
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
			teamDefend.add(plID); 
			int _tempTeam = rnd.nextInt(getDefendSpawns().size()-1);
			while (_spns.contains(_tempTeam)) {_tempTeam = rnd.nextInt(getDefendSpawns().size()-1);}
			l = getDefendSpawns().get(_tempTeam);
			
		}
		else 
		{
			teamAttack.add(plID); 
			int _tempTeam = rnd.nextInt(getAttackSpawns().size()-1);
			while (_spns.contains(_tempTeam)) {_tempTeam = rnd.nextInt(getAttackSpawns().size()-1);}
			l = getAttackSpawns().get(_tempTeam);
		}
		
		pl.teleport(l);
	}
	
	
	public ArrayList<UUID> getAllPlayers()
	{
		ArrayList<UUID> list = new ArrayList<>();
		list.addAll(spectators);
		list.addAll(teamAttack);
		list.addAll(teamDefend);
		list.addAll(livePlayers);
		list.addAll(deathPlayers);
		return list;
	}
	
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

	public ArrayList<UUID> getDeathPlayers() {
		return deathPlayers;
	}

	public void setDeathPlayers(ArrayList<UUID> deathPlayers) {
		this.deathPlayers = deathPlayers;
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
}
