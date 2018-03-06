package com.maxwellwheeler.plugins.tppets.storage;

import java.util.Hashtable;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Tameable;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.PermissionChecker;
import com.maxwellwheeler.plugins.tppets.helpers.UUIDUtils;

public class PlayerPetIndex {
    public enum RuleRestriction {
        ALLOWED, TOTAL, DOG, CAT, PARROT, UNKNOWN
    }
    
    private RuleRestriction enumLink(PetType.Pets pt) {
        switch (pt) {
        case DOG:
            return RuleRestriction.DOG;
        case CAT:
            return RuleRestriction.CAT;
        case PARROT:
            return RuleRestriction.PARROT;
        case UNKNOWN:
        default:
            return RuleRestriction.UNKNOWN;
        }
    }
    
    private Hashtable<String, AllPetsList> playerIndex = new Hashtable<String, AllPetsList>();
    private TPPets thisPlugin;
    private int totalLimit;
    private int dogLimit;
    private int catLimit;
    private int birdLimit;
    
    public PlayerPetIndex(TPPets thisPlugin, int totalLimit, int dogLimit, int catLimit, int birdLimit) {
        this.thisPlugin = thisPlugin;
        this.totalLimit = totalLimit;
        this.dogLimit = dogLimit;
        this.catLimit = catLimit;
        this.birdLimit = birdLimit;
        initializePetIndex();
    }
    
    private void initializePetIndex() {
        for (World wld : Bukkit.getServer().getWorlds()) {
            for (Entity ent : wld.getEntitiesByClasses(org.bukkit.entity.Sittable.class)) {
                Tameable tameableTemp = (Tameable) ent;
                if (tameableTemp.isTamed()) {
                    String trimmedOwnerUUID = UUIDUtils.trimUUID(tameableTemp.getOwner().getUniqueId());
                    String trimmedEntityUUID = UUIDUtils.trimUUID(ent.getUniqueId());
                    if (!playerIndex.containsKey(trimmedOwnerUUID)) {
                        playerIndex.put(trimmedOwnerUUID, new AllPetsList());
                    }
                    playerIndex.get(trimmedOwnerUUID).addPet(trimmedEntityUUID, PetType.getEnumByEntity(ent));
                }
            }
            if (thisPlugin.getDatabase() != null) {
                List<PetStorage> psList = thisPlugin.getDatabase().getPetsFromWorld(wld.getName());
                if (psList != null) {
                    for (PetStorage ps : thisPlugin.getDatabase().getPetsFromWorld(wld.getName())) {
                        if (!playerIndex.containsKey(ps.ownerId)) {
                            playerIndex.put(ps.ownerId, new AllPetsList());
                        } else {
                            playerIndex.get(ps.ownerId).addPet(ps.petId, ps.petType);
                        }
                    }
                }
            }
        }
    }
    
    public int getSpecificLimit(PetType.Pets pt) {
        switch (pt) {
            case DOG:
                return dogLimit;
            case CAT:
                return catLimit;
            case PARROT:
                return birdLimit;
            default:
                return -1;
        }
    }
    
    public void newPetTamed(String playerUUID, String entityUUID, PetType.Pets pt) {
        String trimmedPlayerUUID = UUIDUtils.trimUUID(playerUUID);
        String trimmedEntityUUID = UUIDUtils.trimUUID(entityUUID);
        if (playerIndex.containsKey(trimmedPlayerUUID)) {
            playerIndex.get(trimmedPlayerUUID).addPet(trimmedEntityUUID, pt);
        }
    }
    
    public void removePetTamed(String playerUUID, String entityUUID, PetType.Pets pt) {
        String trimmedPlayerUUID = UUIDUtils.trimUUID(playerUUID);
        String trimmedEntityUUID = UUIDUtils.trimUUID(entityUUID);
        if (playerIndex.containsKey(trimmedPlayerUUID)) {
            playerIndex.get(trimmedPlayerUUID).removePet(trimmedEntityUUID, pt);
        }
    }
    
    private boolean isWithinLimit(int limit, int within) {
        return limit < 0 || within < limit;
    }
    
    public RuleRestriction allowTame(AnimalTamer at, Location loc, PetType.Pets pt) {
        if (PermissionChecker.offlineHasPerms(at, "tppets.bypasslimit", loc.getWorld(), thisPlugin) || PermissionChecker.onlineHasPerms(at, "tppets.bypasslimit")) {
            return RuleRestriction.ALLOWED;
        }
        String trimmedUUID = UUIDUtils.trimUUID(at.getUniqueId());
        if (playerIndex.containsKey(trimmedUUID)) {
            if (!isWithinLimit(totalLimit, playerIndex.get(trimmedUUID).getTotalLength())) {
                return RuleRestriction.TOTAL;
            } else if (!isWithinLimit(getSpecificLimit(pt), playerIndex.get(trimmedUUID).getPetsLength(pt))) {
                return enumLink(pt);
            }
        }
        return RuleRestriction.ALLOWED;
    }
}
