package com.maxwellwheeler.plugins.tppets.region;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Tameable;
import org.bukkit.scheduler.BukkitRunnable;

import com.maxwellwheeler.plugins.tppets.TPPets;

public class CheckRegions extends BukkitRunnable {
    private ArrayList<ProtectedRegion> protectedRegions = new ArrayList<ProtectedRegion>();
    private LostAndFoundRegion lostAndFound;
    private TPPets pluginInstance;
    
    public CheckRegions(TPPets plugin, LostAndFoundRegion lostAndFound) {
        this.protectedRegions = plugin.getProtectedRegions();
        this.lostAndFound = lostAndFound;
        this.pluginInstance = plugin;
    }
    
    @Override
    public void run() {
        System.out.println("Checking Regions");
        for (ProtectedRegion pr : protectedRegions) {
            List<Chunk> prChunks = pr.getChunkList();
            for (Chunk chunk : prChunks) {
                chunk.load();
                for (Entity ent : chunk.getEntities()) {
                    if (ent instanceof Tameable && pr.isInZone(ent.getLocation())) {
                        Tameable tameableTemp = (Tameable) ent;
                        if (tameableTemp.isTamed()) {
                            pr.teleportPet(lostAndFound.getApproxCenter(), ent);
                            pluginInstance.getSQLite().updateOrInsert(ent);
                        }
                    }
                }
            }
        }
    }
    
}
