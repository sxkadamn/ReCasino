package net.recasino.example;

import net.recasino.api.mode.CasinoMode;
import net.recasino.api.mode.CasinoModeContext;
import net.recasino.api.player.CasinoPlayerProfile;
import net.recasino.model.CurrencyType;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public final class CoinFlipMode implements CasinoMode {

    private static final double STAKE = 1000.0D;

    @Override
    public String getId() {
        return "example-coinflip";
    }

    @Override
    public int getPreferredMainMenuSlot() {
        return 21;
    }

    @Override
    public ItemStack createMainMenuItem(Player player, CasinoPlayerProfile profile) {
        return item(
                Material.SUNFLOWER,
                "§6Коинфлип",
                "§7Пример внешнего режима через addon API.",
                "§7Фиксированная ставка: §f1,000 монет",
                "§7Нажмите, чтобы открыть демо GUI."
        );
    }

    @Override
    public int getInventorySize() {
        return 27;
    }

    @Override
    public String getInventoryTitle(Player player, CasinoPlayerProfile profile) {
        return "§6Коинфлип";
    }

    @Override
    public void onOpen(CasinoModeContext context) {
        Inventory inventory = context.getInventory();
        fill(inventory, item(Material.BLACK_STAINED_GLASS_PANE, " "));
        inventory.setItem(11, item(Material.RED_CONCRETE, "§cОрел", "§7Шанс: §f50%", "§7Ставка: §f" + format(STAKE)));
        inventory.setItem(13, item(Material.SUNFLOWER, "§6Пример аддона", "§7Это GUI рисуется плагином example-addon.", "§7Баланс: §f" + format(context.getBalance(CurrencyType.MONEY))));
        inventory.setItem(15, item(Material.BLUE_CONCRETE, "§9Решка", "§7Шанс: §f50%", "§7Ставка: §f" + format(STAKE)));
        inventory.setItem(22, item(Material.ARROW, "§7Назад", "§7Вернуться в главное меню ReCasino."));
    }

    @Override
    public void onClick(CasinoModeContext context, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot == 22) {
            context.openMainMenu();
            return;
        }

        boolean pickHeads;
        if (slot == 11) {
            pickHeads = true;
        } else if (slot == 15) {
            pickHeads = false;
        } else {
            return;
        }

        Player player = context.getPlayer();
        if (!context.withdraw(CurrencyType.MONEY, STAKE)) {
            player.sendMessage("§cНедостаточно монет для игры в коинфлип.");
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.8F, 1.0F);
            return;
        }

        CasinoPlayerProfile profile = context.getProfile();
        profile.recordGameStart(STAKE);
        boolean resultHeads = ThreadLocalRandom.current().nextBoolean();
        boolean win = resultHeads == pickHeads;

        if (win) {
            double payout = STAKE * 2.0D;
            context.deposit(CurrencyType.MONEY, payout);
            profile.recordGameResult(payout);
            player.sendMessage("§aВы выиграли в коинфлип. Выплата: §f" + format(payout));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.1F);
        } else {
            profile.recordGameResult(0.0D);
            player.sendMessage("§cВы проиграли в коинфлип.");
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.8F, 1.0F);
        }

        context.markProfileDirty();
        context.reopen();
    }

    private void fill(Inventory inventory, ItemStack item) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, item);
        }
    }

    private ItemStack item(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            List<String> lines = new ArrayList<String>();
            for (String line : lore) {
                lines.add(line);
            }
            meta.setLore(lines);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private String format(double value) {
        return String.format(Locale.US, "%,.0f", value);
    }
}
