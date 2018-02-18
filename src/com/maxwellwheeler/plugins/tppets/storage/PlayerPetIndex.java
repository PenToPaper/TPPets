package com.maxwellwheeler.plugins.tppets.storage;

import java.util.Hashtable;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Ocelot;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Wolf;

import com.maxwellwheeler.plugins.tppets.TPPets;
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
    
    private Hashtable<String, AllPetsList> index = new Hashtable<String, AllPetsList>();
    private TPPets pluginInstance;
    private int totalLimit;
    private int dogLimit;
    private int catLimit;
    private int birdLimit;
    
    public PlayerPetIndex(TPPets pluginInstance, int totalLimit, int dogLimit, int catLimit, int birdLimit) {
        this.pluginInstance = pluginInstance;
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
                    if (!index.containsKey(trimmedOwnerUUID)) {
                        index.put(trimmedOwnerUUID, new AllPetsList());
                    } else {
                        if (ent instanceof Wolf) {
                            System.out.println("FOUND WOLF THING");
                            index.get(trimmedOwnerUUID).addDog(trimmedEntityUUID);
                        } else if (ent instanceof Ocelot) {
                            index.get(trimmedOwnerUUID).addCat(trimmedEntityUUID);
                        } else if (ent instanceof Parrot) {
                            index.get(trimmedOwnerUUID).addBird(trimmedEntityUUID);
                        }
                    }
                }
            }
            List<PetStorage> psList = pluginInstance.getSQLite().getPetsByWorld(wld.getName());
            if (psList != null) {
                for (PetStorage ps : pluginInstance.getSQLite().getPetsByWorld(wld.getName())) {
                    if (!index.containsKey(ps.ownerId)) {
                        index.put(ps.ownerId, new AllPetsList());
                    } else {
                        index.get(ps.ownerId).addPet(ps.petId, ps.petType);
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
        if (index.containsKey(trimmedPlayerUUID)) {
            index.get(trimmedPlayerUUID).addPet(trimmedEntityUUID, pt);
        }
    }
    
    public void removePetTamed(String playerUUID, String entityUUID, PetType.Pets pt) {
        String trimmedPlayerUUID = UUIDUtils.trimUUID(playerUUID);
        String trimmedEntityUUID = UUIDUtils.trimUUID(entityUUID);
        System.out.println("Attempting to remove pet " + pt.toString() + " with uuid " + trimmedEntityUUID + " from player " + trimmedPlayerUUID);
        if (index.containsKey(trimmedPlayerUUID)) {
            index.get(trimmedPlayerUUID).removePet(trimmedEntityUUID, pt);
        }
    }
    
    private boolean isWithinLimit(int limit, int within) {
        return limit < 0 || within < limit;
    }
    
    public RuleRestriction allowTame(String playerUUID, PetType.Pets pt) {
        String trimmedUUID = UUIDUtils.trimUUID(playerUUID);
        for (String key : index.keySet()) {
            for (String id : index.get(key).getPets(pt)) {
                System.out.println(key + " has pet " + pt.toString() + " with id " + id);
            }
        }
        if (index.containsKey(trimmedUUID)) {
            if (!isWithinLimit(totalLimit, index.get(trimmedUUID).getTotalLength())) {
                return RuleRestriction.TOTAL;
            } else if (!isWithinLimit(getSpecificLimit(pt), index.get(trimmedUUID).getPetsLength(pt))) {
                return enumLink(pt);
            }
        }
        return RuleRestriction.ALLOWED;
        // return index.containsKey(trimmedUUID) && isWithinLimit(totalLimit, index.get(trimmedUUID).getTotalLength()) && isWithinLimit(getSpecificLimit(pt), index.get(trimmedUUID).getPetsLength(pt));
    }
}
