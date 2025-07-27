package org.steelrework.locate_sr;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;

import java.util.*;

public class LocateSR extends JavaPlugin implements CommandExecutor, Listener {

    private final Map<UUID, String> pendingLocations = new HashMap<>();
    
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(getCommand("locate")).setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Эта команда только для игроков!");
            return true;
        }
        Player player = (Player) sender;

        if (!command.getName().equalsIgnoreCase("locate")) {
            return false;
        }

        openPlayerMenu(player);
        return true;
    }

    private void openPlayerMenu(Player player) {
        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        onlinePlayers.remove(player);

        Inventory inv = Bukkit.createInventory(null, 54, Component.text("Выберите игрока")
                .color(TextColor.fromHexString("#4A90E2")));

        // Добавляем декоративные синие стекла по краям
        ItemStack blueGlass = new ItemStack(Material.BLUE_STAINED_GLASS_PANE);
        ItemMeta glassMeta = blueGlass.getItemMeta();
        glassMeta.displayName(Component.text(""));
        blueGlass.setItemMeta(glassMeta);

        // Заполняем верхнюю и нижнюю строки
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, blueGlass);
            inv.setItem(45 + i, blueGlass);
        }
        // Заполняем боковые стороны
        for (int i = 1; i < 5; i++) {
            inv.setItem(i * 9, blueGlass);
            inv.setItem(i * 9 + 8, blueGlass);
        }

        // Размещаем головы игроков в центральной области
        int playerIndex = 0;
        for (int row = 1; row < 5; row++) {
            for (int col = 1; col < 8; col++) {
                if (playerIndex >= onlinePlayers.size()) break;
                
                int slot = row * 9 + col;
                Player target = onlinePlayers.get(playerIndex);
                ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta meta = (SkullMeta) skull.getItemMeta();
                
                meta.setOwningPlayer(target);
                meta.displayName(Component.text(target.getName())
                        .color(TextColor.fromHexString("#F7DC6F")));
                meta.lore(Arrays.asList(
                        Component.text("Нажмите, чтобы узнать координаты")
                                .color(TextColor.fromHexString("#AED6F1")),
                        Component.text("Стоимость: 5 алмазов")
                                .color(TextColor.fromHexString("#82E0AA"))
                ));
                
                skull.setItemMeta(meta);
                inv.setItem(slot, skull);
                playerIndex++;
            }
            if (playerIndex >= onlinePlayers.size()) break;
        }

        player.openInventory(inv);
    }

    private void openConfirmationMenu(Player player, String targetName) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("Подтверждение")
                .color(TextColor.fromHexString("#E74C3C")));

        // Добавляем декор
        ItemStack blueGlass = new ItemStack(Material.BLUE_STAINED_GLASS_PANE);
        ItemMeta blueGlassMeta = blueGlass.getItemMeta();
        blueGlassMeta.displayName(Component.text(""));
        blueGlass.setItemMeta(blueGlassMeta);

        // Заполняем верхнюю и нижнюю строки
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, blueGlass);
            inv.setItem(18 + i, blueGlass);
        }
        
        // Боковые стороны
        inv.setItem(9, blueGlass);
        inv.setItem(17, blueGlass);

        // Декоративные элементы вокруг кнопок
        inv.setItem(10, blueGlass);
        inv.setItem(12, blueGlass);
        inv.setItem(14, blueGlass);
        inv.setItem(16, blueGlass);

        ItemStack decline = new ItemStack(Material.RED_CONCRETE);
        ItemMeta declineMeta = decline.getItemMeta();
        declineMeta.displayName(Component.text("Отказаться")
                .color(TextColor.fromHexString("#E74C3C")));
        declineMeta.lore(Arrays.asList(Component.text("Вернуться к выбору игрока")
                .color(TextColor.fromHexString("#F1948A"))));
        decline.setItemMeta(declineMeta);

        ItemStack accept = new ItemStack(Material.GREEN_CONCRETE);
        ItemMeta acceptMeta = accept.getItemMeta();
        acceptMeta.displayName(Component.text("Принять")
                .color(TextColor.fromHexString("#27AE60")));
        acceptMeta.lore(Arrays.asList(
                Component.text("Узнать координаты игрока:")
                        .color(TextColor.fromHexString("#82E0AA")),
                Component.text(targetName)
                        .color(TextColor.fromHexString("#F7DC6F"))
        ));
        accept.setItemMeta(acceptMeta);

        ItemStack diamonds = new ItemStack(Material.DIAMOND, 5);
        ItemMeta diamondMeta = diamonds.getItemMeta();
        diamondMeta.displayName(Component.text("Будет списано:")
                .color(TextColor.fromHexString("#85C1E9")));
        diamondMeta.addEnchant(Enchantment.MENDING, 1, true);
        diamondMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        diamonds.setItemMeta(diamondMeta);

        inv.setItem(11, decline);
        inv.setItem(13, diamonds);
        inv.setItem(15, accept);

        pendingLocations.put(player.getUniqueId(), targetName);
        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        String title = event.getView().title().toString();
        
        if (title.contains("Выберите игрока")) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            
            if (clicked != null && clicked.getType() == Material.PLAYER_HEAD) {
                SkullMeta meta = (SkullMeta) clicked.getItemMeta();
                if (meta.getOwningPlayer() != null) {
                    String targetName = meta.getOwningPlayer().getName();
                    openConfirmationMenu(player, targetName);
                    player.playSound(player.getLocation(), Sound.BLOCK_TRIAL_SPAWNER_SPAWN_MOB, 1.0f, 1.5f);
                }
            }
        } else if (title.contains("Подтверждение")) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            
            if (clicked != null) {
                if (clicked.getType() == Material.RED_CONCRETE) {
                    openPlayerMenu(player);
                } else if (clicked.getType() == Material.GREEN_CONCRETE) {
                    String targetName = pendingLocations.get(player.getUniqueId());
                    if (targetName != null) {
                        Player target = Bukkit.getPlayer(targetName);
                        if (target != null) {
                            startLocationProcess(player, target);
                        } else {
                            player.sendMessage(Component.text("❌ Игрок не найден")
                                    .color(TextColor.fromHexString("#FF6B6B")));
                        }
                        pendingLocations.remove(player.getUniqueId());
                    }
                    player.closeInventory();
                }
            }
        }
    }

    private void startLocationProcess(Player player, Player target) {
        // Проверяем и удаляем алмазы из инвентаря одним проходом
        if (!player.getInventory().containsAtLeast(new ItemStack(Material.DIAMOND), 5)) {
            player.sendMessage(Component.text("❌ В инвентаре отсутствует 5 алмазов")
                    .color(TextColor.fromHexString("#FF6B6B")));
            return;
        }

        // Удаляем алмазы встроенным методом
        player.getInventory().removeItem(new ItemStack(Material.DIAMOND, 5));

        Location playerLoc = player.getLocation().add(0, 1.5, 0);
        Location centerLoc = playerLoc.clone().add(
            player.getLocation().getDirection().multiply(3).setY(0)
        ).add(0, 1, 0);
        
        List<ItemDisplay> diamondDisplays = new ArrayList<>(5);
        List<Location> targetPositions = new ArrayList<>(5);

        // Предварительно рассчитываем позиции для алмазов
        for (int i = 0; i < 5; i++) {
            double angle = (i * 72.0) * Math.PI / 180.0;
            targetPositions.add(centerLoc.clone().add(
                Math.cos(angle), 0, Math.sin(angle)
            ));
        }

        new BukkitRunnable() {
            int spawnedDiamonds = 0;
            int ticks = 0;
            boolean convergingPhase = false;
            int convergingTicks = 0;

            @Override
            public void run() {
                // Фаза появления алмазов поочередно
                if (!convergingPhase && ticks % 8 == 0 && spawnedDiamonds < 5) {
                    // Алмаз появляется из игрока
                    ItemDisplay display = player.getWorld().spawn(playerLoc, ItemDisplay.class);
                    display.setItemStack(new ItemStack(Material.DIAMOND));
                    display.setTransformation(new Transformation(
                            new Vector3f(0, 0, 0), 
                            new org.joml.Quaternionf(), 
                            new Vector3f(0.6f, 0.6f, 0.6f), 
                            new org.joml.Quaternionf()
                    ));
                    display.setBrightness(new Display.Brightness(15, 15));
                    diamondDisplays.add(display);
                    
                    player.playSound(player.getLocation(), Sound.BLOCK_TRIAL_SPAWNER_EJECT_ITEM, 1.0f, 1.0f + spawnedDiamonds * 0.2f);
                    spawnedDiamonds++;
                }

                // Анимация хаотичного вращения
                if (spawnedDiamonds == 5 && !convergingPhase) {
                    if (ticks >= 80) { 
                        convergingPhase = true;
                        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 0.8f);
                    }
                }

                // Обновляем позиции всех алмазов
                for (int i = 0; i < diamondDisplays.size(); i++) {
                    ItemDisplay display = diamondDisplays.get(i);
                    
                    if (!convergingPhase) {
                        // Алмазы летят к целевым позициям
                        if (i < targetPositions.size()) {
                            Location currentLoc = display.getLocation();
                            Location targetLoc = targetPositions.get(i);
                            
                            // Плавное движение к цели
                            double progress = Math.min(1.0, (ticks - i * 8) / 30.0);
                            if (progress > 0) {
                                Location newLoc = currentLoc.clone().add(
                                    (targetLoc.getX() - currentLoc.getX()) * progress * 0.2,
                                    (targetLoc.getY() - currentLoc.getY()) * progress * 0.2,
                                    (targetLoc.getZ() - currentLoc.getZ()) * progress * 0.2
                                );
                                
                                // Добавляем хаотичные колебания
                                double chaosX = Math.sin(ticks * 0.3 + i) * 0.3;
                                double chaosZ = Math.cos(ticks * 0.4 + i) * 0.3;
                                double chaosY = Math.sin(ticks * 0.25 + i * 1.5) * 0.2;
                                
                                newLoc.add(chaosX, chaosY, chaosZ);
                                display.teleport(newLoc);
                            }
                        }
                        
                        // Быстрое хаотичное вращение алмаза
                        float rotX = (float) (Math.sin(ticks * 0.5 + i) * 0.5);
                        float rotY = (float) (Math.cos(ticks * 0.7 + i) * 0.5);
                        float rotZ = (float) (Math.sin(ticks * 0.6 + i) * 0.5);
                        
                        display.setTransformation(new Transformation(
                                new Vector3f(0, 0, 0),
                                new org.joml.Quaternionf().rotateXYZ(rotX, rotY, rotZ),
                                new Vector3f(0.6f, 0.6f, 0.6f),
                                new org.joml.Quaternionf()
                        ));
                    } else {
                        // Фаза сближения к центру
                        Location currentLoc = display.getLocation();
                        double progress = Math.min(1.0, convergingTicks / 20.0); 
                        
                        Location newLoc = currentLoc.clone().add(
                                (centerLoc.getX() - currentLoc.getX()) * progress * 0.4, 
                                (centerLoc.getY() - currentLoc.getY()) * progress * 0.4,
                                (centerLoc.getZ() - currentLoc.getZ()) * progress * 0.4
                        );
                        
                        display.teleport(newLoc);
                        
                        // Продолжаем вращение во время сближения
                        float rotX = (float) (Math.sin(ticks * 0.8 + i) * 0.7);
                        float rotY = (float) (Math.cos(ticks * 1.0 + i) * 0.7);
                        float rotZ = (float) (Math.sin(ticks * 0.9 + i) * 0.7);
                        
                        display.setTransformation(new Transformation(
                                new Vector3f(0, 0, 0),
                                new org.joml.Quaternionf().rotateXYZ(rotX, rotY, rotZ),
                                new Vector3f(0.6f, 0.6f, 0.6f),
                                new org.joml.Quaternionf()
                        ));
                        
                        if (currentLoc.distance(centerLoc) < 0.5) { 
                            display.remove();
                            
                            if (diamondDisplays.stream().allMatch(ItemDisplay::isDead)) {
                                // Безвредная молния (эффект без урона)
                                centerLoc.getWorld().strikeLightningEffect(centerLoc);
                                
                                // Частицы разбитого алмазного блока
                                centerLoc.getWorld().spawnParticle(
                                    org.bukkit.Particle.BLOCK, 
                                    centerLoc, 
                                    100, 
                                    1.0, 1.0, 1.0, 
                                    0.3, 
                                    Material.DIAMOND_BLOCK.createBlockData()
                                );
                                
                                player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 0.6f);
                                player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 0.8f);
                                
                                // Мгновенное выпадение головы
                                dropLocationHead(centerLoc, target);
                                player.playSound(player.getLocation(), Sound.BLOCK_VAULT_OPEN_SHUTTER, 1.0f, 1.2f);
                                
                                cancel();
                                return;
                            }
                        }
                        
                        convergingTicks++;
                        if (convergingTicks >= 40) {
                            diamondDisplays.forEach(ItemDisplay::remove);
                            centerLoc.getWorld().strikeLightningEffect(centerLoc);
                            
                            // Алм блок
                            centerLoc.getWorld().spawnParticle(
                                org.bukkit.Particle.BLOCK, 
                                centerLoc, 
                                100, 
                                1.0, 1.0, 1.0, 
                                0.3, 
                                Material.DIAMOND_BLOCK.createBlockData()
                            );
                            
                            dropLocationHead(centerLoc, target);
                            cancel();
                            return;
                        }
                    }
                }
                
                ticks++;
            }
        }.runTaskTimer(this, 0L, 1L);
    }

    private void dropLocationHead(Location dropLoc, Player target) {
        Location targetLoc = target.getLocation();
        String dimension = getDimensionName(targetLoc.getWorld());
        String dimensionColor = getDimensionColor(targetLoc.getWorld());
        
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        
        meta.setOwningPlayer(target);
        meta.displayName(Component.text("Местоположение " + target.getName())
                .color(TextColor.fromHexString("#F7DC6F")));
        
        meta.lore(Arrays.asList(
                Component.text("X: " + (int)targetLoc.getX())
                        .color(TextColor.fromHexString("#E74C3C")),
                Component.text("Y: " + (int)targetLoc.getY())
                        .color(TextColor.fromHexString("#27AE60")),
                Component.text("Z: " + (int)targetLoc.getZ())
                        .color(TextColor.fromHexString("#3498DB")),
                Component.text(""),
                Component.text("Измерение: " + dimension)
                        .color(TextColor.fromHexString(dimensionColor))
        ));
        
        head.setItemMeta(meta);
        
        dropLoc.getWorld().dropItem(dropLoc, head);
    }

    private String getDimensionName(World world) {
        switch (world.getEnvironment()) {
            case NORMAL:
                return "Обычный мир";
            case NETHER:
                return "Нижний мир";
            case THE_END:
                return "Край";
            default:
                return "Неизвестное измерение";
        }
    }

    private String getDimensionColor(World world) {
        switch (world.getEnvironment()) {
            case NORMAL:
                return "#27AE60";
            case NETHER:
                return "#E74C3C";
            case THE_END:
                return "#9B59B6";
            default:
                return "#95A5A6";
        }
    }
} 