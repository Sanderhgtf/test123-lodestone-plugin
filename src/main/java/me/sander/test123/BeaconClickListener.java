package me.sander.test123;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class BeaconClickListener implements Listener {

    private final List<LodestoneCoordinate> lodestoneCoordinates = new ArrayList<>();
    private final String dataFilePath = "plugins/Test123/lodestone_data.json";

    public BeaconClickListener() {
        loadCoordinatesFromJson();
    }

    @EventHandler
    public void onBeaconClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (event.getAction().toString().contains("RIGHT")) {
            ItemStack itemInHand = event.getItem();

            if (itemInHand != null && itemInHand.getType() == Material.NETHER_STAR) {
                if (!isNetherStarRenamed(itemInHand)) {
                    player.sendMessage(ChatColor.RED + "Rename the Nether Star to what you want this lodestone to be called.");
                    return;
                }

                Block clickedBlock = event.getClickedBlock();

                if (clickedBlock != null && clickedBlock.getType() == Material.LODESTONE) {
                    Location location = clickedBlock.getLocation();
                    String worldName = location.getWorld().getName();
                    String netherStarName = getNetherStarName(itemInHand);

                    if (!isCoordinatesStored(location, worldName)) {
                        itemInHand.setAmount(itemInHand.getAmount() - 1);
                        lodestoneCoordinates.add(new LodestoneCoordinate(location, worldName, netherStarName));
                        saveCoordinatesToJson();
                        int x = location.getBlockX();
                        int y = location.getBlockY();
                        int z = location.getBlockZ();
                        Bukkit.getLogger().info("Lodestone Coordinates in " + worldName + ": X=" + x + ", Y=" + y + ", Z=" + z + " - Named: " + netherStarName);
                        sendActionBarMessage(player, ChatColor.GREEN + "Lodestone constructed");
                        playSoundNearbyPlayers(location);
                        PluginReloader.reloadPlugin(Test123.getPlugin(Test123.class));
                    } else {
                        Bukkit.getLogger().info("This is already a lodestone position");
                    }
                }
            }
        }
    }

    private void sendActionBarMessage(Player player, String message) {
        player.sendTitle("", message, 0, 20 * 2, 10);
    }

    private void saveCoordinatesToJson() {
        JSONArray jsonArray = new JSONArray();
        for (LodestoneCoordinate lodestone : lodestoneCoordinates) {
            JSONObject locationObject = new JSONObject();
            Location location = lodestone.getLocation();
            locationObject.put("X", location.getBlockX());
            locationObject.put("Y", location.getBlockY());
            locationObject.put("Z", location.getBlockZ());
            locationObject.put("World", lodestone.getWorldName());
            locationObject.put("NetherStarName", lodestone.getNetherStarName());
            jsonArray.add(locationObject);
        }

        try (FileWriter fileWriter = new FileWriter(dataFilePath)) {
            jsonArray.writeJSONString(fileWriter);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadCoordinatesFromJson() {
        JSONParser jsonParser = new JSONParser();
        try (FileReader fileReader = new FileReader(dataFilePath)) {
            JSONArray jsonArray = (JSONArray) jsonParser.parse(fileReader);
            for (Object obj : jsonArray) {
                if (obj instanceof JSONObject) {
                    JSONObject locationObject = (JSONObject) obj;
                    int x = ((Long) locationObject.get("X")).intValue();
                    int y = ((Long) locationObject.get("Y")).intValue();
                    int z = ((Long) locationObject.get("Z")).intValue();
                    String worldName = (String) locationObject.get("World");
                    String netherStarName = (String) locationObject.get("NetherStarName");
                    lodestoneCoordinates.add(new LodestoneCoordinate(new Location(Bukkit.getWorld(worldName), x, y, z), worldName, netherStarName));
                }
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

    private boolean isNetherStarRenamed(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return true;
        }
        return false;
    }

    private boolean isCoordinatesStored(Location location, String worldName) {
        for (LodestoneCoordinate lodestone : lodestoneCoordinates) {
            if (lodestone.getLocation().equals(location) && lodestone.getWorldName().equals(worldName)) {
                return true;
            }
        }
        return false;
    }

    private String getNetherStarName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        return null;
    }

    private void playSoundNearbyPlayers(Location location) {
        for (Player nearbyPlayer : location.getWorld().getPlayers()) {
            if (nearbyPlayer.getLocation().distance(location) <= 15) {
                nearbyPlayer.playSound(location, Sound.ENTITY_VILLAGER_WORK_WEAPONSMITH, 1.2F, 0.35F);
            }
        }
    }
}
