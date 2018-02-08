package com.maxwellwheeler.plugins.tppets;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Ocelot;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Sittable;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.maxwellwheeler.plugins.tppets.commands.CommandCreateDogs;
import com.maxwellwheeler.plugins.tppets.commands.CommandLF;
import com.maxwellwheeler.plugins.tppets.commands.CommandNoPets;
import com.maxwellwheeler.plugins.tppets.commands.CommandTPPets;
import com.maxwellwheeler.plugins.tppets.commands.CommandTpForward;
import com.maxwellwheeler.plugins.tppets.helpers.TimeCalculator;
import com.maxwellwheeler.plugins.tppets.region.CheckRegions;
import com.maxwellwheeler.plugins.tppets.region.LostAndFoundRegion;
import com.maxwellwheeler.plugins.tppets.region.ProtectedRegion;
import com.maxwellwheeler.plugins.tppets.storage.SQLite;

public class TPPets extends JavaPlugin implements Listener {
    private ArrayList<ProtectedRegion> protectedRegions = new ArrayList<ProtectedRegion>();
    private static TPPets instance;
    private SQLite dbc;
    private CheckRegions cr;
    private int checkInterval;
    private LostAndFoundRegion lostAndFound;
    
    private boolean preventPlayerDamage;
    private boolean preventEnvironmentalDamage;
    private boolean preventMobDamage;
    
    public static TPPets getInstance() {
        return instance;
    }
    
    private void initializeDamageConfigs() {
        List<String> configList = getConfig().getStringList("protect_pets_from");
        if (configList.contains("Player")) {
            preventPlayerDamage = true;
        }
        if (configList.contains("EnvironmentalDamage")) {
            preventEnvironmentalDamage = true;
        }
        if (configList.contains("MobDamage")) {
            preventMobDamage = true;
        }
    }
    
    private void initializeProtectedRegions() {
        Set<String> regionKeyList = getConfig().getConfigurationSection("forbidden_zones").getKeys(false);
        System.out.println(regionKeyList.toString());
        for (String key : regionKeyList) {
            protectedRegions.add(new ProtectedRegion(key, this));
        }
        for (ProtectedRegion pr : protectedRegions) {
            System.out.println(pr.toString());
        }
        System.out.println(protectedRegions.size());
    }
    
    private void initializeLostAndFound() {
        //TODO Temporary. When we move to databases for everything, we should get a more robust solution, like the one above ^^
        if (getConfig().isSet("lost_and_found.primary")) {
            lostAndFound = new LostAndFoundRegion(this);
        }
    }

    @Override
    public void onEnable() {
        // Public static property instance now refers to the server's instance of the plugin
        instance = this;
        
        // Config stuff
        getConfig().options().copyDefaults(true);
        saveDefaultConfig();
        initializeProtectedRegions();
        
        // Database setup
        dbc = new SQLite(this, getDataFolder().getPath(), "test");
        dbc.createDatabase();
        dbc.createTable();
        
        checkInterval = TimeCalculator.getTimeFromString(getConfig().getString("check_interval"));
        
        // Register events + commands
        getServer().getPluginManager().registerEvents(this, this);
        this.getCommand("tp-dogs").setExecutor(new CommandTPPets(dbc));
        this.getCommand("tp-cats").setExecutor(new CommandTPPets(dbc));
        this.getCommand("tp-parrots").setExecutor(new CommandTPPets(dbc));
        this.getCommand("no-pets").setExecutor(new CommandNoPets());
        this.getCommand("pets-lf").setExecutor(new CommandLF());
        this.getCommand("generate-tamed-dogs").setExecutor(new CommandCreateDogs());
        this.getCommand("tp-forward").setExecutor(new CommandTpForward());

        startCheckingRegions();
        initializeDamageConfigs();
        initializeLostAndFound();
    }
    
    @EventHandler (priority=EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent e) {
        for (Entity ent : e.getChunk().getEntities()) {
           if (ent instanceof Sittable && ent instanceof Tameable) {
               Tameable tameableTemp = (Tameable) ent;
               if (tameableTemp.isTamed()) {
                   dbc.updateOrInsert(ent);
               }
           }
        }
    }
    
    @EventHandler (priority=EventPriority.LOW)
    public void onEntityTeleportEvent(EntityTeleportEvent e) {
        if (isInProtectedRegion(e.getTo())) {
            if (e.getEntity() instanceof Sittable) {
                Sittable ent = (Sittable) e.getEntity();
                ent.setSitting(true);
            }
            e.setCancelled(true);
        }
    }
    
    public void addProtectedRegion (ProtectedRegion pr) {
        protectedRegions.add(pr);
    }
    
    @EventHandler (priority=EventPriority.MONITOR)
    public void onEntityDeathEvent(EntityDeathEvent e) {
        if (e.getEntity() instanceof Tameable && e.getEntity() instanceof Sittable) {
            Tameable tameableTemp = (Tameable) e.getEntity();
            if (tameableTemp.isTamed()) {
                dbc.deleteEntry(e.getEntity().getUniqueId(), tameableTemp.getOwner().getUniqueId());
            }
        }
    }
    
    @EventHandler (priority=EventPriority.LOW)
    public void onEntityDamageByEntityEvent(EntityDamageByEntityEvent e) {
        if (e.getEntity() instanceof Tameable && e.getEntity() instanceof Sittable) {
            Tameable tameableTemp = (Tameable) e.getEntity();
            if (tameableTemp.isTamed()) {
                if (preventPlayerDamage) {
                    if (e.getDamager() instanceof Player && !e.getDamager().equals(tameableTemp.getOwner())) {
                        System.out.println("PREVENTED PLAYER DAMAGE");
                        e.setCancelled(true);
                        return;
                    } else if (e.getDamager() instanceof Projectile) {
                        Projectile projTemp = (Projectile) e.getDamager();
                        if (projTemp.getShooter() instanceof Player && !projTemp.getShooter().equals(tameableTemp.getOwner())) {
                            System.out.println("PREVENTED PROJECTILE PLAYER DAMAGE");
                            e.setCancelled(true);
                            return;
                        }
                    }
                }
                if (preventMobDamage) {
                    if (e.getDamager() instanceof LivingEntity) {
                        System.out.println("PREVENTED MOB DAMAGE");
                        e.setCancelled(true);
                        return;
                    } else if (e.getDamager() instanceof Projectile) {
                        Projectile projTemp = (Projectile) e.getDamager();
                        if (projTemp.getShooter() instanceof LivingEntity && !projTemp.getShooter().equals(tameableTemp.getOwner())) {
                            System.out.println("PREVENTED MOB DAMAGE");
                            e.setCancelled(true);
                            return;
                        }
                    }
                }
            }
        }
    }
    
    @EventHandler (priority=EventPriority.LOW)
    public void onEntityDamageEvent(EntityDamageEvent e) {
        if ((preventEnvironmentalDamage) && e.getEntity() instanceof Tameable && e.getEntity() instanceof Sittable) {
            Tameable tameableTemp = (Tameable) e.getEntity();
            if (tameableTemp.isTamed()) {
                switch (e.getCause()) {
                    case BLOCK_EXPLOSION:
                    case CONTACT:
                    case CRAMMING:
                    case DRAGON_BREATH:
                    case DROWNING:
                    case FALL:
                    case FALLING_BLOCK:
                    case FIRE:
                    case FIRE_TICK:
                    case FLY_INTO_WALL:
                    case HOT_FLOOR:
                    case LAVA:
                    case LIGHTNING:
                    case MELTING:
                    case POISON:
                    case STARVATION:
                    case SUFFOCATION:
                    case THORNS:
                    case WITHER:
                        System.out.println("PREVENTED ENVIRONMENTAL DAMAGE");
                        e.setCancelled(true);
                        break;
                    default:
                        break;
                }
            }
        }
    }
    
    public boolean isInProtectedRegion(Player pl) {
        for (ProtectedRegion pr : protectedRegions) {
            if (pr.isInZone(pl)) {
                return true;
            }
        }
        return false;
    }
    
    public void startCheckingRegions() {
        if (lostAndFound != null) {
            cr = new CheckRegions(this, lostAndFound);
            cr.runTaskTimer(this, 0, checkInterval);
        }
    }
    
    public boolean isInProtectedRegion(Location lc) {
        for (ProtectedRegion pr : protectedRegions) {
            if (pr.isInZone(lc)) {
                return true;
            }
        }
        return false;
    }
    
    public void addLostAndFoundRegion (LostAndFoundRegion lfr) {
        if (lostAndFound == null) {
            lostAndFound = lfr;
            startCheckingRegions();
        } else {
            lostAndFound = lfr;
        }
        
    }
    
    public SQLite getSQLite() {
        return dbc;
    }
    
    public ArrayList<ProtectedRegion> getProtectedRegions() {
        return protectedRegions;
    }
    
    public void stopScheduledEvent() {
        try {
            cr.cancel();
        } catch (IllegalStateException e) {
            System.out.println("SOMETHING WENT VERY WRONG");
        }
    }
}
