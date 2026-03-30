package net.recasino.command;

import net.recasino.ReCasino;
import net.recasino.model.CasinoProfile;
import net.recasino.service.HologramService;
import net.recasino.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class CasinoCommand implements CommandExecutor, TabCompleter {

    private final ReCasino plugin;

    public CasinoCommand(ReCasino plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("recasino.use")) {
            sender.sendMessage(message("messages.no-permission"));
            return true;
        }

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(message("messages.only-player"));
                return true;
            }
            plugin.getMenuFactory().openMain(player);
            return true;
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);
        if (subCommand.equals("help")) {
            sendHelp(sender);
            return true;
        }

        if (subCommand.equals("reload")) {
            if (!sender.hasPermission("recasino.admin")) {
                sender.sendMessage(message("messages.no-permission"));
                return true;
            }
            plugin.reloadPlugin();
            sender.sendMessage(message("messages.reload"));
            return true;
        }

        if (subCommand.equals("look")) {
            if (!sender.hasPermission("recasino.admin")) {
                sender.sendMessage(message("messages.no-permission"));
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(ColorUtil.color("&cИспользование: /casino look <игрок>"));
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(message("messages.player-not-found"));
                return true;
            }
            CasinoProfile profile = plugin.getProfileService().getProfile(target);
            sender.sendMessage(ColorUtil.color("&6Профиль казино игрока &f" + target.getName()));
            sender.sendMessage(ColorUtil.color("&7Ставка в монетах: &f" + profile.getMoneyBetFormatted()));
            sender.sendMessage(ColorUtil.color("&7Ставка в рилликах: &f" + profile.getRillikBetFormatted()));
            sender.sendMessage(ColorUtil.color("&7Баланс рилликов: &f" + profile.getRillikBalanceFormatted()));
            sender.sendMessage(ColorUtil.color("&7Игр: &f" + profile.getTotalGames() + " &7| Побед: &f" + profile.getTotalWins() + " &7| Профит: &f" + ColorUtil.formatNumber(profile.getNetProfit())));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(message("messages.only-player"));
            return true;
        }

        if (subCommand.equals("stats") || subCommand.equals("top")) {
            player.sendMessage(ColorUtil.color("&7Топы и личная статистика теперь выведены в голограммы."));
            return true;
        }

        if (subCommand.equals("admin")) {
            if (!player.hasPermission("recasino.admin")) {
                player.sendMessage(message("messages.no-permission"));
                return true;
            }
            plugin.getMenuFactory().openAdminSettings(player);
            return true;
        }

        if (subCommand.equals("setholo")) {
            if (!player.hasPermission("recasino.admin")) {
                player.sendMessage(message("messages.no-permission"));
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(ColorUtil.color("&cИспользование: /casino setholo <top-profit|top-wins|personal-stats>"));
                return true;
            }
            HologramService.HologramType type = switch (args[1].toLowerCase(Locale.ROOT)) {
                case "top-profit" -> HologramService.HologramType.TOP_PROFIT;
                case "top-wins" -> HologramService.HologramType.TOP_WINS;
                case "personal-stats", "stats" -> HologramService.HologramType.PERSONAL_STATS;
                default -> null;
            };
            if (type == null) {
                player.sendMessage(ColorUtil.color("&cНеизвестный тип голограммы."));
                return true;
            }
            boolean success = plugin.getHologramService().setHologramLocation(type, player.getLocation());
            player.sendMessage(ColorUtil.color(success
                    ? "&aГолограмма " + args[1] + " установлена."
                    : "&cНе удалось создать голограмму. Проверьте, установлен ли DecentHolograms или HolographicDisplays."));
            return true;
        }

        if (subCommand.equals("holoreload")) {
            if (!player.hasPermission("recasino.admin")) {
                player.sendMessage(message("messages.no-permission"));
                return true;
            }
            plugin.getHologramService().initialize();
            player.sendMessage(ColorUtil.color("&aГолограммы обновлены."));
            return true;
        }

        plugin.getMenuFactory().openMain(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList("help", "look", "stats", "top", "admin", "setholo", "holoreload", "reload"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("look")) {
            List<String> names = new ArrayList<String>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                names.add(player.getName());
            }
            return filter(names, args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("setholo")) {
            return filter(Arrays.asList("top-profit", "top-wins", "personal-stats"), args[1]);
        }
        return Collections.emptyList();
    }

    private void sendHelp(CommandSender sender) {
        for (String line : plugin.getCasinoConfig().getStringList("messages.help")) {
            sender.sendMessage(ColorUtil.color(line));
        }
    }

    private String message(String path) {
        return ColorUtil.color(plugin.getCasinoConfig().getString(path));
    }

    private List<String> filter(List<String> source, String input) {
        List<String> result = new ArrayList<String>();
        for (String value : source) {
            if (value.toLowerCase(Locale.ROOT).startsWith(input.toLowerCase(Locale.ROOT))) {
                result.add(value);
            }
        }
        return result;
    }
}

