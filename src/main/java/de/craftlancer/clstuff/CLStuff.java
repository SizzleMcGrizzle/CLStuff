package de.craftlancer.clstuff;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.time.temporal.ChronoField;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Statistic;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import de.craftlancer.clstuff.afk.AFKListener;
import de.craftlancer.clstuff.commands.CraftCommand;
import de.craftlancer.clstuff.explosionregulator.ExplosionRegulator;
import de.craftlancer.clstuff.help.CCHelpCommandHandler;
import de.craftlancer.clstuff.premium.ModelToken;
import de.craftlancer.clstuff.premium.RecolorCommand;
import de.craftlancer.clstuff.rankings.Rankings;
import de.craftlancer.clstuff.squest.ServerQuests;
import de.craftlancer.core.CancelableRunnable;
import de.craftlancer.core.LambdaRunnable;
import de.craftlancer.core.NMSUtils;
import me.ryanhamshire.GriefPrevention.events.ClaimCreatedEvent;
import net.md_5.bungee.api.ChatColor;

public class CLStuff extends JavaPlugin implements Listener {
    
    private static final DateTimeFormatter DATE_FORMAT;
    static {
        Map<Long, String> dow = new HashMap<>();
        dow.put(1L, "Mon");
        dow.put(2L, "Tue");
        dow.put(3L, "Wed");
        dow.put(4L, "Thu");
        dow.put(5L, "Fri");
        dow.put(6L, "Sat");
        dow.put(7L, "Sun");
        Map<Long, String> moy = new HashMap<>();
        moy.put(1L, "Jan");
        moy.put(2L, "Feb");
        moy.put(3L, "Mar");
        moy.put(4L, "Apr");
        moy.put(5L, "May");
        moy.put(6L, "Jun");
        moy.put(7L, "Jul");
        moy.put(8L, "Aug");
        moy.put(9L, "Sep");
        moy.put(10L, "Oct");
        moy.put(11L, "Nov");
        moy.put(12L, "Dec");
        
        DATE_FORMAT = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .parseLenient()
            .optionalStart()
            .appendText(ChronoField.DAY_OF_WEEK, dow)
            .appendLiteral(", ")
            .optionalEnd()
            .appendValue(ChronoField.DAY_OF_MONTH, 1, 2, SignStyle.NOT_NEGATIVE)
            .appendLiteral(' ')
            .appendText(ChronoField.MONTH_OF_YEAR, moy)
            .appendLiteral(' ')
            .appendValue(ChronoField.YEAR, 4)  // 2 digit year not handled
            .appendLiteral(" §e")
            .appendValue(ChronoField.HOUR_OF_DAY, 2)
            .appendLiteral(':')
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
            .optionalStart()
            .appendLiteral(':')
            .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
            .optionalEnd()
            .appendLiteral(" §7")
            .appendOffset("+HHMM", "GMT")  // should handle UT/Z/EST/EDT/CST/CDT/MST/MDT/PST/MDT
            .toFormatter();
    }
    
    private WGNoDropFlag flag;
    private ServerQuests serverQuests;
    private Rankings rankings;
    private ModelToken tokens;
    private RecolorCommand recolor;
    private ExplosionRegulator exploNerf;
    private boolean useDiscord = false;
    
    private boolean logMoveEvents = false;
    
    @Override
    public void onLoad() {
        WGNoDropFlag.registerFlag();
    }
    
    @Override
    public void onEnable() {
        useDiscord = Bukkit.getPluginManager().getPlugin("DiscordSRV") != null;
        
        getCommand("wiki").setExecutor((a, b, c, d) -> {
            a.sendMessage(ChatColor.WHITE + "[" + ChatColor.DARK_RED + "Craft" + ChatColor.WHITE + "Citizen] " + ChatColor.DARK_GREEN
                    + "https://craftcitizen.net/wiki/");
            return true;
        });
        getCommand("voteall").setExecutor((a, b, c, d) -> {
            a.sendMessage(ChatColor.WHITE + "[" + ChatColor.DARK_RED + "Craft" + ChatColor.WHITE + "Citizen] " + ChatColor.DARK_GREEN
                    + "https://craftcitizen.net/voteAll.html");
            return true;
        });
        getCommand("howtoplay").setExecutor((a, b, c, d) -> {
            String commandLine = "minecraft:give " + a.getName()
                    + " written_book{pages:[\"[\\\"\\\",{\\\"text\\\":\\\" \\\\u0020 \\\\u0020 \\\\u263d\\\\u25ba\\\",\\\"color\\\":\\\"gold\\\"},{\\\"text\\\":\\\"Craft\\\",\\\"bold\\\":true,\\\"color\\\":\\\"dark_red\\\"},{\\\"text\\\":\\\"Citizen\\\",\\\"bold\\\":true,\\\"color\\\":\\\"gray\\\"},{\\\"text\\\":\\\"\\\\n\\\\nWelcome \\\",\\\"color\\\":\\\"reset\\\"},{\\\"text\\\":\\\"Peasant "
                    + a.getName()
                    + "\\\",\\\"bold\\\":true,\\\"color\\\":\\\"gold\\\"},{\\\"text\\\":\\\"!\\\\n\\\",\\\"color\\\":\\\"reset\\\"},{\\\"text\\\":\\\"Brave enough to conquer \\\",\\\"italic\\\":true},{\\\"text\\\":\\\"Alinor\\\",\\\"italic\\\":true,\\\"color\\\":\\\"blue\\\",\\\"clickEvent\\\":{\\\"action\\\":\\\"open_url\\\",\\\"value\\\":\\\"https://craftlancer.de/wiki/index.php/ConquerPoints\\\"}},{\\\"text\\\":\\\" or raid some \\\",\\\"color\\\":\\\"reset\\\",\\\"italic\\\":true},{\\\"text\\\":\\\"dungeons\\\",\\\"italic\\\":true,\\\"color\\\":\\\"blue\\\",\\\"clickEvent\\\":{\\\"action\\\":\\\"open_url\\\",\\\"value\\\":\\\"https://craftlancer.de/wiki/index.php/Dungeons_%26_Raids\\\"}},{\\\"text\\\":\\\"?\\\\nBecome a cunning merchant, build a village or found a mighty kingdom?\\\",\\\"color\\\":\\\"reset\\\",\\\"italic\\\":true},{\\\"text\\\":\\\"\\\\n\\\\n\\\",\\\"color\\\":\\\"reset\\\"},{\\\"text\\\":\\\"/kit boat\\\",\\\"color\\\":\\\"dark_green\\\"},{\\\"text\\\":\\\"\\\\n\\\",\\\"color\\\":\\\"reset\\\"},{\\\"text\\\":\\\"/kit peasant\\\",\\\"color\\\":\\\"dark_green\\\"},{\\\"text\\\":\\\"\\\\n \\\",\\\"color\\\":\\\"reset\\\"}]\",\"[\\\"\\\",{\\\"text\\\":\\\"Medieval Fantasy Survival RPG\\\\n\\\\n\\\"},{\\\"text\\\":\\\"\\\\u263d\\\\u25ba \\\",\\\"color\\\":\\\"dark_gray\\\"},{\\\"text\\\":\\\"Clan & FactionMob\\\",\\\"color\\\":\\\"blue\\\",\\\"clickEvent\\\":{\\\"action\\\":\\\"open_url\\\",\\\"value\\\":\\\"https://craftlancer.de/wiki/index.php/Clans\\\"}},{\\\"text\\\":\\\"\\\\n\\\",\\\"color\\\":\\\"reset\\\"},{\\\"text\\\":\\\"\\\\u263d\\\\u25ba\\\",\\\"color\\\":\\\"dark_gray\\\"},{\\\"text\\\":\\\" \\\",\\\"color\\\":\\\"reset\\\"},{\\\"text\\\":\\\"Portals\\\",\\\"color\\\":\\\"dark_blue\\\",\\\"clickEvent\\\":{\\\"action\\\":\\\"open_url\\\",\\\"value\\\":\\\"https://craftlancer.de/wiki/index.php/Portals\\\"}},{\\\"text\\\":\\\"\\\\n\\\",\\\"color\\\":\\\"reset\\\"},{\\\"text\\\":\\\"\\\\u263d\\\\u25ba \\\",\\\"color\\\":\\\"dark_gray\\\"},{\\\"text\\\":\\\"PvP Events\\\",\\\"color\\\":\\\"blue\\\",\\\"clickEvent\\\":{\\\"action\\\":\\\"open_url\\\",\\\"value\\\":\\\"https://craftlancer.de/wiki/index.php/ConquerPoints\\\"}},{\\\"text\\\":\\\"\\\\n\\\",\\\"color\\\":\\\"reset\\\"},{\\\"text\\\":\\\"\\\\u263d\\\\u25ba\\\",\\\"color\\\":\\\"dark_gray\\\"},{\\\"text\\\":\\\" \\\",\\\"color\\\":\\\"reset\\\"},{\\\"text\\\":\\\"Dungeons & Raids\\\",\\\"color\\\":\\\"dark_blue\\\",\\\"clickEvent\\\":{\\\"action\\\":\\\"open_url\\\",\\\"value\\\":\\\"https://craftlancer.de/wiki/index.php/Dungeons_%26_Raids\\\"}},{\\\"text\\\":\\\"\\\\n\\\",\\\"color\\\":\\\"reset\\\"},{\\\"text\\\":\\\"\\\\u263d\\\\u25ba \\\",\\\"color\\\":\\\"dark_gray\\\"},{\\\"text\\\":\\\"Ranks\\\",\\\"color\\\":\\\"blue\\\",\\\"clickEvent\\\":{\\\"action\\\":\\\"open_url\\\",\\\"value\\\":\\\"https://craftlancer.de/wiki/index.php/Ranks\\\"}},{\\\"text\\\":\\\"\\\\n\\\",\\\"color\\\":\\\"reset\\\"},{\\\"text\\\":\\\"\\\\u263d\\\\u25ba \\\",\\\"color\\\":\\\"dark_gray\\\"},{\\\"text\\\":\\\"Rentable Shops\\\",\\\"color\\\":\\\"dark_blue\\\",\\\"clickEvent\\\":{\\\"action\\\":\\\"open_url\\\",\\\"value\\\":\\\"https://craftlancer.de/wiki/index.php/Marketstalls\\\"}},{\\\"text\\\":\\\"\\\\n\\\",\\\"color\\\":\\\"reset\\\"},{\\\"text\\\":\\\"\\\\u263d\\\\u25ba\\\",\\\"color\\\":\\\"dark_gray\\\"},{\\\"text\\\":\\\" \\\",\\\"color\\\":\\\"reset\\\"},{\\\"text\\\":\\\"Blueprints\\\",\\\"color\\\":\\\"blue\\\",\\\"clickEvent\\\":{\\\"action\\\":\\\"open_url\\\",\\\"value\\\":\\\"https://craftlancer.de/wiki/index.php/Blueprints\\\"}},{\\\"text\\\":\\\"\\\\n\\\",\\\"color\\\":\\\"reset\\\"},{\\\"text\\\":\\\"\\\\u263d\\\\u25ba\\\",\\\"color\\\":\\\"dark_gray\\\"},{\\\"text\\\":\\\" \\\",\\\"color\\\":\\\"reset\\\"},{\\\"text\\\":\\\"Pumpkinbandit\\\",\\\"color\\\":\\\"dark_blue\\\",\\\"clickEvent\\\":{\\\"action\\\":\\\"open_url\\\",\\\"value\\\":\\\"https://craftlancer.de/wiki/index.php/Pumpkinbandit\\\"}},{\\\"text\\\":\\\"\\\\n\\\",\\\"color\\\":\\\"reset\\\"},{\\\"text\\\":\\\"\\\\u263d\\\\u25ba\\\",\\\"color\\\":\\\"dark_gray\\\"},{\\\"text\\\":\\\" \\\",\\\"color\\\":\\\"reset\\\"},{\\\"text\\\":\\\"Cannons\\\",\\\"color\\\":\\\"blue\\\",\\\"clickEvent\\\":{\\\"action\\\":\\\"open_url\\\",\\\"value\\\":\\\"https://craftlancer.de/wiki/index.php/Cannons\\\"}},{\\\"text\\\":\\\"\\\\n\\\",\\\"color\\\":\\\"reset\\\"},{\\\"text\\\":\\\"\\\\u263d\\\\u25ba\\\",\\\"color\\\":\\\"dark_gray\\\"},{\\\"text\\\":\\\" \\\",\\\"color\\\":\\\"reset\\\"},{\\\"text\\\":\\\"Roads\\\",\\\"color\\\":\\\"dark_blue\\\",\\\"clickEvent\\\":{\\\"action\\\":\\\"open_url\\\",\\\"value\\\":\\\"https://craftlancer.de/wiki/index.php/Roads\\\"}}]\",\"[\\\"\\\",{\\\"text\\\":\\\"You can:\\\\n\\\",\\\"bold\\\":true},{\\\"text\\\":\\\"\\\\u2714\\\",\\\"bold\\\":true,\\\"color\\\":\\\"dark_green\\\"},{\\\"text\\\":\\\" PvP\\\\n\\\",\\\"color\\\":\\\"reset\\\"},{\\\"text\\\":\\\"\\\\u2714 \\\",\\\"bold\\\":true,\\\"color\\\":\\\"dark_green\\\"},{\\\"text\\\":\\\"Build Auto Farms\\\\n\\\",\\\"color\\\":\\\"reset\\\"},{\\\"text\\\":\\\"\\\\u2714\\\",\\\"bold\\\":true,\\\"color\\\":\\\"dark_green\\\"},{\\\"text\\\":\\\" Build Traps\\\\n\\\",\\\"color\\\":\\\"reset\\\"},{\\\"text\\\":\\\"\\\\u2714\\\",\\\"bold\\\":true,\\\"color\\\":\\\"dark_green\\\"},{\\\"text\\\":\\\" Steal Stuff & Loot\\\\n\\\",\\\"color\\\":\\\"reset\\\"},{\\\"text\\\":\\\"\\\\u2714\\\",\\\"bold\\\":true,\\\"color\\\":\\\"dark_green\\\"},{\\\"text\\\":\\\" Use Optifine\\\\n\\\\n\\\",\\\"color\\\":\\\"reset\\\"},{\\\"text\\\":\\\"Tip:\\\",\\\"bold\\\":true},{\\\"text\\\":\\\" Use \\\",\\\"color\\\":\\\"reset\\\"},{\\\"text\\\":\\\"/rankup\\\",\\\"bold\\\":true,\\\"color\\\":\\\"dark_green\\\"},{\\\"text\\\":\\\" and \\\",\\\"color\\\":\\\"reset\\\"},{\\\"text\\\":\\\"/stats\\\",\\\"bold\\\":true,\\\"color\\\":\\\"dark_green\\\"},{\\\"text\\\":\\\" to unlock rewards!\\\\n \\\",\\\"color\\\":\\\"reset\\\"}]\",\"[\\\"\\\",{\\\"text\\\":\\\"Behaviour we expect:\\\",\\\"bold\\\":true},{\\\"text\\\":\\\"\\\\n\\\\n\\\",\\\"color\\\":\\\"reset\\\"},{\\\"text\\\":\\\"\\\\u2714\\\",\\\"bold\\\":true,\\\"color\\\":\\\"dark_green\\\"},{\\\"text\\\":\\\" Be Polite\\\\n\\\",\\\"color\\\":\\\"reset\\\"},{\\\"text\\\":\\\"\\\\u2714\\\",\\\"bold\\\":true,\\\"color\\\":\\\"dark_green\\\"},{\\\"text\\\":\\\" Help others\\\\n\\\",\\\"color\\\":\\\"reset\\\"},{\\\"text\\\":\\\"\\\\u2714\\\",\\\"bold\\\":true,\\\"color\\\":\\\"dark_green\\\"},{\\\"text\\\":\\\" Play together\\\\n\\\",\\\"color\\\":\\\"reset\\\"},{\\\"text\\\":\\\"\\\\u2714\\\",\\\"bold\\\":true,\\\"color\\\":\\\"dark_green\\\"},{\\\"text\\\":\\\" Be Mature\\\\n\\\\n\\\",\\\"color\\\":\\\"reset\\\"},{\\\"text\\\":\\\"\\\\u2716\\\",\\\"bold\\\":true,\\\"color\\\":\\\"dark_red\\\"},{\\\"text\\\":\\\" No Chat Spam\\\\n\\\",\\\"color\\\":\\\"reset\\\"},{\\\"text\\\":\\\"\\\\u2716\\\",\\\"bold\\\":true,\\\"color\\\":\\\"dark_red\\\"},{\\\"text\\\":\\\" No Cheats/Mods\\\\n\\\",\\\"color\\\":\\\"reset\\\"},{\\\"text\\\":\\\"\\\\u2716\\\",\\\"bold\\\":true,\\\"color\\\":\\\"dark_red\\\"},{\\\"text\\\":\\\" No Advertising\\\\n\\\",\\\"color\\\":\\\"reset\\\"},{\\\"text\\\":\\\"\\\\u2716\\\",\\\"bold\\\":true,\\\"color\\\":\\\"dark_red\\\"},{\\\"text\\\":\\\" No Bullying\\\\n\\\\nPlease read the \\\",\\\"color\\\":\\\"reset\\\"},{\\\"text\\\":\\\"Rules\\\",\\\"color\\\":\\\"blue\\\",\\\"clickEvent\\\":{\\\"action\\\":\\\"open_url\\\",\\\"value\\\":\\\"https://craftlancer.de/wiki/index.php/Rules\\\"}},{\\\"text\\\":\\\"!\\\",\\\"color\\\":\\\"reset\\\"}]\"],title:\"§6A Peasants Guide\",author:ReadMe}";
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandLine);
            return true;
        });
        getCommand("stats").setExecutor(new StatsCommandExecutor(this));
        getCommand("map").setExecutor((a, b, c, d) -> {
            a.sendMessage(ChatColor.WHITE + "[" + ChatColor.DARK_RED + "Craft" + ChatColor.WHITE + "Citizen] " + ChatColor.DARK_GREEN
                    + "https://craftcitizen.net/livemap/");
            return true;
        });
        getCommand("ping").setExecutor((a, b, c, d) -> {
            Player target = null;
            
            if (d.length == 1)
                target = Bukkit.getPlayer(d[0]);
            else if (a instanceof Player)
                target = (Player) a;
            
            if (target == null)
                a.sendMessage("No player found to check ping for.");
            else {
                int ping = NMSUtils.getPing(target);
                
                ChatColor pingColor = ChatColor.GREEN;
                if(ping < 100)
                    pingColor = ChatColor.GREEN;
                else if(ping < 250)
                    pingColor = ChatColor.GOLD;
                else
                    pingColor = ChatColor.RED;
                
                a.sendMessage(ChatColor.WHITE + "[" + ChatColor.DARK_RED + "Craft" + ChatColor.WHITE + "Citizen] " + target.getDisplayName() + ChatColor.YELLOW + "'s Ping: " + pingColor + ping + ChatColor.YELLOW + " ms");
                
            }
            return true;
        });
        getCommand("cchelp").setExecutor(new CCHelpCommandHandler(this));
        getCommand("time").setExecutor((a, b, c, d) -> {
            a.sendMessage("§f[§4Craft§fCitizen]§7 " + ZonedDateTime.now().format(DATE_FORMAT));
            return true;
        });
        
        getCommand("wild").setExecutor(new WildCommand(this));
        
        getCommand("store").setExecutor((a, b, c, d) -> {
            a.sendMessage(ChatColor.WHITE + "[" + ChatColor.DARK_RED + "Craft" + ChatColor.WHITE + "Citizen] " + ChatColor.DARK_GREEN
                    + "https://craftcitizen.tebex.io/");
            return true;
        });
        
        getCommand("logIMIE").setExecutor((a, b, c, d) -> {
            if (a.isOp()) {
                logMoveEvents = !logMoveEvents;
                a.sendMessage("logMoveEvents is now " + logMoveEvents);
            }
            return true;
        });
        getCommand("countEntities").setExecutor((a, b, c, d) -> {
            if (!a.isOp())
                return true;
            
            Bukkit.getWorlds().forEach(w -> {
                Map<EntityType, EntityEntry> entityMap = new EnumMap<>(EntityType.class);
                int totalCount = 0;
                int totalActiveCount = 0;
                
                for (Entity e : w.getEntities()) {
                    boolean isActive = NMSUtils.isEntityActive(e);
                    entityMap.putIfAbsent(e.getType(), new EntityEntry());
                    entityMap.get(e.getType()).add(isActive);
                    totalCount++;
                    if (isActive)
                        totalActiveCount++;
                }
                
                a.sendMessage("Entities in World " + w.getName());
                a.sendMessage(String.format("Total: %d/%d", totalActiveCount, totalCount));
                entityMap.forEach((k, v) -> a.sendMessage(String.format("%s: %d/%d", k.name(), v.activeEntityCount, v.entityCount)));
                a.sendMessage("=========");
            });
            
            return true;
        });
        
        rankings = new Rankings(this);
        recolor = new RecolorCommand();
        getCommand("rankings").setExecutor(rankings);
        getCommand("recolor").setExecutor(recolor);
        
        getCommand("craft").setExecutor(new CraftCommand());
        
        flag = new WGNoDropFlag(this);
        serverQuests = new ServerQuests(this);

        tokens = new ModelToken(this);
        exploNerf = new ExplosionRegulator(this);
        Bukkit.getPluginManager().registerEvents(recolor, this);
        Bukkit.getPluginManager().registerEvents(exploNerf, this);
        Bukkit.getPluginManager().registerEvents(tokens, this);
        Bukkit.getPluginManager().registerEvents(new CLAntiCheat(this), this);
        Bukkit.getPluginManager().registerEvents(new LagFixes(this), this);
        Bukkit.getPluginManager().registerEvents(new AFKListener(this), this);
        Bukkit.getPluginManager().registerEvents(this, this);
        
        if (Bukkit.getPluginManager().getPlugin("CombatLogX") != null)
            Bukkit.getPluginManager().registerEvents(new CombatLogXListener(), this);
        
        new LambdaRunnable(this::save).runTaskTimer(this, 18000L, 18000L);
    }
    
    class EntityEntry {
        int entityCount = 0;
        int activeEntityCount = 0;
        
        public void add(boolean isEntityActive) {
            this.entityCount += 1;
            this.activeEntityCount += isEntityActive ? 1 : 0;
        }
    }
    
    public Rankings getRankings() {
        return rankings;
    }
    
    @Override
    public void onDisable() {
        save();
        Bukkit.getScheduler().cancelTasks(this);
    }
    
    private void save() {
        serverQuests.save();
        rankings.save();
        tokens.save();
        exploNerf.save();
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemMove(InventoryMoveItemEvent event) {
        if (!logMoveEvents)
            return;
        
        getLogger().info(() -> "IMIE: " + event.getInitiator().getLocation().toString());
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void lecternFix(BlockPlaceEvent event) {
        if (event.getBlock().getType() == Material.LECTERN && event.getItemInHand().getType() == Material.WRITTEN_BOOK)
            event.setCancelled(false);
    }
    
    @EventHandler
    public void onElytraCraft(CraftItemEvent event) {
        ItemStack result = event.getRecipe().getResult();
        Player player = (Player) event.getWhoClicked();
        
        if (result.getType() == Material.ELYTRA) {
            event.setResult(Result.DENY);
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1F, 1F);
            player.sendMessage(ChatColor.RED + "You cannot use elytras in crafting tables!");
        }
    }
    
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onChestPlace(BlockPlaceEvent event) {
        if (event.getBlock().getType() != Material.CHEST)
            return;
        
        Player p = event.getPlayer();
        if (p.getStatistic(Statistic.USE_ITEM, Material.CHEST) != 0)
            return;
        
        event.getPlayer().performCommand("sethome");
        p.sendMessage(ChatColor.GOLD + "This happened because you placed down your first chest.");
        p.sendMessage(ChatColor.GOLD + "You can change your spawnpoint at any time using /sethome.");
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        
        player.setPortalCooldown(600);
        
        BossBar bar = Bukkit.createBossBar("Portal Cooldown", BarColor.PURPLE, BarStyle.SOLID);
        
        bar.addPlayer(event.getPlayer());
        new CancelableRunnable(() -> {
            bar.setProgress(player.getPortalCooldown() / 600D);
            bar.setTitle(ChatColor.YELLOW + "Portal Cooldown " + ChatColor.GRAY + " - " + ChatColor.GOLD + " " + player.getPortalCooldown() / 20 + " "
                    + ChatColor.YELLOW + "seconds");
            if (player.getPortalCooldown() <= 0) {
                bar.removeAll();
                return true;
            }
            return false;
        }).runTaskTimer(this, 0, 20);
    }
    
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void claimWarnNether(ClaimCreatedEvent event) {
        World world = event.getClaim().getLesserBoundaryCorner().getWorld();
        
        if (world.getEnvironment() == Environment.NETHER) {
            CommandSender sender = event.getCreator();
            sender.sendMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD + "**WARNING**\n " + ChatColor.RED
                    + "\nThe nether is going to be reset with 1.16! You may lose your build!\n " + ChatColor.DARK_RED + "" + ChatColor.BOLD + "\n**WARNING**");
            
            if (sender instanceof Player)
                ((Player) sender).playSound(((Player) sender).getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 2F, 0.5F);
        }
    }
    
    @EventHandler
    public void onPumpkinPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        
        if (item.getType() == Material.CARVED_PUMPKIN && item.getItemMeta().hasCustomModelData())
            event.setCancelled(true);
    }
    
    public boolean isUsingDiscord() {
        return useDiscord;
    }
    
    public WGNoDropFlag getNoDropFlag() {
        return flag;
    }
    
    public ModelToken getModelToken() {
        return tokens;
    }
}
