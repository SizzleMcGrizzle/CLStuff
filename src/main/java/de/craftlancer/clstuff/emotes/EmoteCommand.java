package de.craftlancer.clstuff.emotes;

import de.craftlancer.clstuff.CLStuff;
import de.craftlancer.core.LambdaRunnable;
import de.craftlancer.core.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class EmoteCommand implements CommandExecutor, TabExecutor {
    
    private List<String> particles = Arrays.stream(Particle.values()).map(Enum::toString).collect(Collectors.toList());
    private List<String> sounds = Arrays.stream(Sound.values()).map(Enum::toString).collect(Collectors.toList());
    private EmoteManager manager;
    
    public EmoteCommand(EmoteManager manager) {
        this.manager = manager;
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (!(sender instanceof Player))
            return false;
        
        Player player = (Player) sender;
        
        String string = execute(player, args);
        if (string != null)
            player.sendMessage(string);
        return true;
    }
    
    private String execute(Player player, String[] args) {
        if (args.length == 0)
            return EmoteManager.PREFIX + "You must specify an emote!";
        if (manager.getEmotes().stream().anyMatch(e -> e.getName().equalsIgnoreCase(args[0])))
            return emote(player, args);
        if (args[0].equalsIgnoreCase("add") && player.hasPermission(EmoteManager.ADMIN_PERMISSION))
            return add(args);
        if (args[0].equalsIgnoreCase("remove") && player.hasPermission(EmoteManager.ADMIN_PERMISSION))
            return remove(args[1]);
        return EmoteManager.PREFIX + "§cPlease enter a valid argument!";
    }
    
    private String emote(Player player, String[] args) {
        Emote emote = manager.getEmotes().stream().filter(e -> e.getName().equalsIgnoreCase(args[0])).findFirst().get();
        if (!player.hasPermission(emote.getPermission()))
            return EmoteManager.PREFIX + "§cYou do not have permission to run this command.";
        
        if (manager.hasCooldown(player.getUniqueId()))
            return EmoteManager.PREFIX + "§cYou must wait to use an emote again!";
        
        if (player.hasPermission("clstuff.emote.target")
                && args.length > 1
                && Bukkit.getPlayer(args[1]) != null
                && !player.getUniqueId().equals(Bukkit.getPlayer(args[1]).getUniqueId()))
            emote.target(player, Bukkit.getPlayer(args[1]));
        else
            emote.run(player);
        
        manager.addCooldown(player.getUniqueId());
        new LambdaRunnable(() -> manager.removeCooldown(player.getUniqueId())).runTaskLater(CLStuff.getInstance(), EmoteManager.COOLDOWN);
        
        return null;
    }
    
    private String add(String[] args) {
        try {
            Emote emote = new Emote(args[1],
                    ChatColor.translateAlternateColorCodes('&', String.join(" ", Arrays.copyOfRange(args, 8, args.length))),
                    Particle.valueOf(args[2]),
                    Double.parseDouble(args[3]),
                    Sound.valueOf(args[4]),
                    Double.parseDouble(args[5]),
                    Integer.parseInt(args[6]),
                    Emote.ParticleLocation.fromString(args[7]));
            manager.addEmote(emote);
        } catch (Exception e) {
            return EmoteManager.PREFIX + "§cError while adding emote: one or more of your arguments isn't valid...";
        }
        
        return EmoteManager.PREFIX + "§aEmote has been added with name of §2" + args[1];
    }
    
    private String remove(String emote) {
        Optional<Emote> optional = manager.getEmotes().stream().filter(e -> e.getName().equals(emote)).findFirst();
        if (!optional.isPresent())
            return EmoteManager.PREFIX + "You must enter a valid emote to remove!";
        manager.removeEmote(optional.get().getName());
        return EmoteManager.PREFIX + "§aEmote has been removed.";
    }
    
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            list.addAll(manager.getEmotes().stream().filter(e -> sender.hasPermission(e.getPermission())).map(Emote::getName).collect(Collectors.toList()));
            if (sender.hasPermission(EmoteManager.ADMIN_PERMISSION)) {
                list.add("add");
                list.add("remove");
            }
            return Utils.getMatches(args[0], list);
        }
        
        if (args.length == 2 && sender.hasPermission("clstuff.emote.target") && manager.getEmotes().stream().filter(e -> sender.hasPermission(e.getPermission())).map(Emote::getName).anyMatch(a -> a.equals(args[0])))
            return Utils.getMatches(args[1], Bukkit.getOnlinePlayers().stream()
                    .filter(p -> !p.getUniqueId().equals(((Player) sender).getUniqueId()))
                    .map(Player::getName).collect(Collectors.toList()));
        
        if (!args[0].equalsIgnoreCase("add") && !args[0].equalsIgnoreCase("remove"))
            return Collections.emptyList();
        
        if (args[0].equalsIgnoreCase("add")) {
            if (args.length == 2)
                return Collections.singletonList("name");
            if (args.length == 3)
                return Utils.getMatches(args[2], particles);
            if (args.length == 4)
                return Collections.singletonList("radius");
            if (args.length == 5)
                return Utils.getMatches(args[4], sounds);
            if (args.length == 6)
                return Collections.singletonList("pitch");
            if (args.length == 7)
                return Collections.singletonList("particle_amount");
            if (args.length == 8)
                return Utils.getMatches(args[7], Arrays.stream(Emote.ParticleLocation.values()).map(Enum::name).collect(Collectors.toList()));
            if (args.length > 8)
                return Collections.singletonList("message");
        } else if (args.length == 2)
            return Utils.getMatches(args[1], manager.getEmotes().stream().map(Emote::getName).collect(Collectors.toList()));
        
        return Collections.emptyList();
    }
}
