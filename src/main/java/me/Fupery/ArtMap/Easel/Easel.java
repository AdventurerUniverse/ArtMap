package me.Fupery.ArtMap.Easel;

import me.Fupery.ArtMap.ArtMap;
import me.Fupery.ArtMap.IO.MapArt;
import me.Fupery.ArtMap.IO.WorldMap;
import me.Fupery.ArtMap.Protocol.ArtistHandler;
import me.Fupery.ArtMap.Protocol.CanvasRenderer;
import me.Fupery.ArtMap.Utils.LocationTag;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.Collection;

import static me.Fupery.ArtMap.Utils.Formatting.*;

public class Easel {

    public static String arbitrarySignID = "*{=}*";
    private boolean isPainting;
    private ArtMap plugin;
    private Location location;
    private ArmorStand stand;
    private ArmorStand seat;
    private ItemFrame frame;

    private Easel(ArtMap plugin, Location location) {
        this.plugin = plugin;
        this.location = location;
    }


    //Spawns an easel at the location provided, facing the direction provided
    public static Easel spawnEasel(ArtMap plugin, Location location, BlockFace facing) {

        EaselPart standPart = new EaselPart(PartType.STAND, facing);
        EaselPart framePart = new EaselPart(PartType.FRAME, facing);

        //Checks frame is not obstructed
        if (framePart.getPartPos(location).getBlock().getType() != Material.AIR) {
            return null;
        }
        BlockFace signFacing = EaselPart.getSignFacing(facing);

        ArmorStand stand = (ArmorStand) location.getWorld().spawnEntity(
                standPart.getPartPos(location), EntityType.ARMOR_STAND);

        stand.setBasePlate(false);
        stand.setCustomNameVisible(true);
        stand.setCustomName(ArtMap.entityTag);
        stand.setGravity(false);

        Block face = location.getBlock().getRelative(facing);

        org.bukkit.material.Sign signFace = new org.bukkit.material.Sign(Material.SIGN);
        signFace.setFacingDirection(signFacing);

        location.getBlock().setType(Material.WALL_SIGN);
        Sign sign = ((Sign) location.getBlock().getState());
        sign.setData(signFace);
        sign.setLine(3, arbitrarySignID);
        sign.update(true, false);


        ItemFrame frame = stand.getWorld().spawn(face.getLocation(), ItemFrame.class);

        frame.setFacingDirection(facing, true);
        frame.setCustomNameVisible(true);
        frame.setCustomName(ArtMap.entityTag);
        Easel easel = new Easel(plugin, location).setEasel(stand, frame);
        plugin.getEasels().put(location, easel);
        return easel;
    }

    public static Easel getEasel(ArtMap plugin, Location partLocation, PartType type) {

        EaselPart part = new EaselPart(type, EaselPart.getFacing(partLocation.getYaw()));
        Location easelLocation = part.getEaselPos(partLocation).getBlock().getLocation();

        if (plugin.getEasels() != null && plugin.getEasels().containsKey(easelLocation)) {

            return plugin.getEasels().get(easelLocation);

        } else {

            Easel easel = new Easel(plugin, easelLocation);

            if (easel.getParts()) {

                plugin.getEasels().put(easel.location, easel);
                return easel;
            }

        }
        return null;
    }

    public static boolean checkForEasel(ArtMap plugin, Location location) {
        Easel easel = new Easel(plugin, location);
        return easel.getParts();
    }

    private Easel setEasel(ArmorStand stand, ItemFrame frame) {
        this.stand = stand;
        this.frame = frame;
        return this;
    }

    private boolean getParts() {
        Sign sign = null;

        if (location.getBlock().getType() == Material.WALL_SIGN) {

            if (location.getBlock().getState() instanceof Sign) {
                sign = ((Sign) location.getBlock().getState());

                if (!sign.getLine(3).equals(arbitrarySignID)) {
                    return false;
                }
            }
        }

        Collection<Entity> entities =
                location.getWorld().getNearbyEntities(location, 2, 2, 2);

        for (Entity e : entities) {

            if (e.getType() == EntityType.ARMOR_STAND) {
                ArmorStand s = (ArmorStand) e;

                //Check if entity is a stand
                if (s.isCustomNameVisible() && s.getCustomName().equals(ArtMap.entityTag)) {
                    EaselPart part = new EaselPart(PartType.STAND,
                            EaselPart.getFacing(s.getLocation().getYaw()));

                    if (part.getEaselPos(s.getLocation()).getBlock().equals(location.getBlock())) {
                        stand = s;
                    }

                    //check if entity is a seat
                } else {
                    EaselPart part = new EaselPart(PartType.SEAT,
                            EaselPart.getFacing(s.getLocation().getYaw()));

                    if (part.getEaselPos(s.getLocation()).getBlock().equals(location.getBlock())) {
                        seat = s;
                    }
                }

            } else if (e.getType() == EntityType.ITEM_FRAME) {
                ItemFrame f = (ItemFrame) e;

                //check if entity is a frame
                EaselPart part = new EaselPart(PartType.FRAME, f.getFacing());

                if (part.getEaselPos(f.getLocation()).getBlock().equals(location.getBlock())) {
                    frame = f;
                }
            }
        }
        if (sign != null
                && frame != null
                && stand != null) {
            return true;

        } else {

            if (plugin.getEasels().containsKey(location)) {
                plugin.getEasels().remove(location);
            }
            return false;
        }
    }

    public void onLeftClick(Player player) {

        if (frame == null) {
            getParts();
        }
        if (!isPainting) {

            if (frame != null && frame.getItem().getType() == Material.MAP) {

                if (plugin.getNameQueue().containsKey(player)) {

                    if (player.getItemInHand().getType() == Material.AIR) {

                        MapArt art = new MapArt(frame.getItem().getDurability(),
                                plugin.getNameQueue().get(player), player);

                        player.setItemInHand(art.getMapItem());
                        frame.setItem(new ItemStack(Material.AIR));
                        art.saveArtwork(plugin);
                        plugin.getNameQueue().remove(player);

                    } else {
                        player.sendMessage(playerMessage(emptyHand));
                    }

                } else {
                    player.sendMessage(playerMessage(saveUsage));
                }
            }
        }
    }

    public void onRightClick(Player player, ItemStack itemInHand) {

        if (frame == null || stand == null) {
            getParts();
        }

        if (!isPainting) {

            if (frame != null && frame.getItem().getType() != Material.AIR) {

                if (itemInHand.getType() == Material.AIR
                        || itemInHand.getType() == Material.INK_SACK
                        || itemInHand.getType() == Material.BUCKET) {

                    player.sendMessage(playerMessage(painting));

                    EaselPart seatPart = new EaselPart(PartType.SEAT, frame.getFacing());

                    Location seatLocation = seatPart.getPartPos(stand.getLocation());
                    seatLocation.setYaw(stand.getLocation().getYaw() - 180);

                    seat = (ArmorStand) seatLocation.getWorld().spawnEntity(
                            seatLocation, EntityType.ARMOR_STAND);

                    seat.setVisible(false);
                    seat.setGravity(false);
                    seat.setRemoveWhenFarAway(true);
                    seat.setPassenger(player);
                    seat.setMetadata("easel",
                            new FixedMetadataValue(plugin, LocationTag.createTag(location)));

                    if (plugin.getArtistHandler() == null) {
                        plugin.setArtistHandler(new ArtistHandler(plugin));
                    }
                    setIsPainting(true);
                    MapView mapView = Bukkit.getMap(frame.getItem().getDurability());

                    plugin.getArtistHandler().addPlayer(player, mapView,
                            EaselPart.getYawOffset(frame.getFacing()));
                }

            } else {

                if (itemInHand.getType() == Material.MAP) {
                    ItemMeta meta = itemInHand.getItemMeta();

                    if (meta.hasDisplayName() && meta.getDisplayName().equals(Recipe.canvasTitle)) {
                        MapView mapView = Bukkit.createMap(player.getWorld());
                        WorldMap map = new WorldMap(mapView);
                        map.setBlankMap();
                        frame.setItem(new ItemStack(Material.MAP, 1, mapView.getId()));
                        ItemStack item = player.getItemInHand().clone();

                        if (itemInHand.getAmount() > 1) {
                            item.setAmount(player.getItemInHand().getAmount() - 1);

                        } else {
                            item = new ItemStack(Material.AIR);
                        }
                        player.setItemInHand(item);

                        if (mapView.getRenderers() != null) {

                            for (MapRenderer r : mapView.getRenderers()) {

                                if (!(r instanceof CanvasRenderer)) {
                                    mapView.removeRenderer(r);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void onShiftRightClick(Player player, ItemStack itemInHand) {

        if (!isPainting) {
            breakEasel();

        } else {
            player.sendMessage(playerError(elseUsing));
        }
    }

    private void breakEasel() {

        plugin.getEasels().remove(location);

        if (frame == null || stand == null || seat == null) {
            getParts();
        }
        location.getWorld().dropItemNaturally(location, new ItemEasel());
        stand.remove();

        if (frame.getItem().getType() != Material.AIR) {
            ItemStack item = new ItemCanvas(plugin);
            location.getWorld().dropItemNaturally(location, item);
        }

        frame.remove();

        if (seat != null) {
            seat.remove();
        }
        location.getBlock().setType(Material.AIR);
    }

    public boolean isPainting() {
        return isPainting;
    }

    public void setIsPainting(boolean isPainting) {
        this.isPainting = isPainting;
    }

    public ItemFrame getFrame() {
        return frame;
    }
}