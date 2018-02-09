package com.maxwellwheeler.plugins.tppets.region;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Tameable;
import org.bukkit.scheduler.BukkitRunnable;

import com.maxwellwheeler.plugins.tppets.TPPets;

public class CheckRegions extends BukkitRunnable {
    private ArrayList<ProtectedRegion> protectedRegions;
    private TPPets pluginInstance;
    
    public CheckRegions(TPPets plugin) {
        this.protectedRegions = plugin.getProtectedRegions();
        this.pluginInstance = plugin;
    }
    
    @Override
    public void run() {
        System.out.println("CHECKING REGIONS NOW");
        for (ProtectedRegion pr : protectedRegions) {
            List<Chunk> prChunks = pr.getChunkList();
            for (Chunk chunk : prChunks) {
                chunk.load();
                for (Entity ent : chunk.getEntities()) {
                    if (ent instanceof Tameable && pr.isInZone(ent.getLocation())) {
                        Tameable tameableTemp = (Tameable) ent;
                        if (tameableTemp.isTamed()) {
                            pr.tpToLostRegion(ent);
                            pluginInstance.getSQLite().updateOrInsertPet(ent);
                        }
                    }
                }
            }
        }
    }
    
}
