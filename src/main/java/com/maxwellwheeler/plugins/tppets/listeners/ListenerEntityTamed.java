package com.maxwellwheeler.plugins.tppets.listeners;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.EntityActions;
import com.maxwellwheeler.plugins.tppets.helpers.PermissionChecker;
import com.maxwellwheeler.plugins.tppets.storage.PetType;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.spigotmc.event.entity.EntityMountEvent;

import java.sql.SQLException;

/**
 * An event listener that handles the creation of new pets.
 * @author GatheringExp
 */
public class ListenerEntityTamed implements Listener {
    /** A reference to the active TPPets instance */
    private final TPPets thisPlugin;

    /**
     * Initializes instance variables.
     * @param thisPlugin A reference to the active {@link TPPets} instance.
     */
    public ListenerEntityTamed(TPPets thisPlugin) {
        this.thisPlugin = thisPlugin;
    }

    /**
     * Generic handler of newly tamed entities. Determines if the newly tamed entity can be added to the plugin, based
     * on limits or any errors with the database.
     * @param owner The new owner of the pet.
     * @param entity The newly tamed entity.
     * @return An {@link EventStatus} representing if the pet should be allowed to be tamed. Possible return values: {@link EventStatus#TOTAL_LIMIT},
     * {@link EventStatus#TYPE_LIMIT}, {@link EventStatus#DB_FAIL}, {@link EventStatus#SUCCESS}.
     */
    private EventStatus onNewTamedEntity(OfflinePlayer owner, Entity entity) {
        try {

            PetType.Pets petType = PetType.getEnumByEntity(entity);

            boolean canBypassPetLimit = canBypassPetLimit(owner, entity.getWorld());

            if (!canBypassPetLimit && !this.thisPlugin.getPetIndex().isWithinTotalLimit(owner)) {
                return EventStatus.TOTAL_LIMIT;
            }

            if (!canBypassPetLimit && !this.thisPlugin.getPetIndex().isWithinSpecificLimit(owner, petType)) {
                return EventStatus.TYPE_LIMIT;
            }

            String generatedName = this.thisPlugin.getDatabase().generateUniquePetName(owner.getUniqueId().toString(), PetType.getEnumByEntity(entity));

            if (!this.thisPlugin.getDatabase().insertPet(entity, owner.getUniqueId().toString(), generatedName)) {
                return EventStatus.DB_FAIL;
            }

            if (owner instanceof Player) {
                ((Player)owner).sendMessage(ChatColor.BLUE + "You've tamed a pet! Its current name is " + ChatColor.WHITE + generatedName + ChatColor.BLUE + ". You can rename it with /tpp rename " + ChatColor.WHITE + generatedName + ChatColor.BLUE + " [new name]");
            }

            this.thisPlugin.getLogWrapper().logSuccessfulAction(owner.getName() + " - tame - " + generatedName);

            return EventStatus.SUCCESS;

        } catch (SQLException ignored) {
            return EventStatus.DB_FAIL;
        }
    }

    /**
     * Determines if a offline or online player has tppets.bypasslimit. Uses {@link PermissionChecker#onlineHasPerms(AnimalTamer, String)},
     * {@link PermissionChecker#offlineHasPerms(AnimalTamer, String, World, TPPets)}.
     * @param owner The new owner. This can be an online {@link Player} or an offline {@link OfflinePlayer}, although
     *              offline players require Vault for an accurate result.
     * @param world The world context where the permission is being checked. This is necessary for Vault.
     * @return true if the player has tppets.bypasslimit, false if not or if TPPets can't determine the status.
     */
    private boolean canBypassPetLimit(OfflinePlayer owner, World world) {
        if (owner instanceof Player) {
            return PermissionChecker.onlineHasPerms(owner, "tppets.bypasslimit");
        }
        return PermissionChecker.offlineHasPerms(owner, "tppets.bypasslimit", world, this.thisPlugin);
    }

    /**
     * Untames a {@link Tameable} pet.
     * @param pet The pet to untame.
     */
    private void cancelTame(Tameable pet) {
        EntityActions.setStanding(pet);
        pet.setOwner(null);
        if (!isSpecialHorse(pet)) {
            pet.setTamed(false);
        }
    }

    /**
     * Displays the status of a {@link ListenerEntityTamed#onNewTamedEntity(OfflinePlayer, Entity)} to a player, if they're online.
     * @param owner The new owner. If this is an online player, the status will be displayed to them.
     * @param pet The new pet.
     * @param eventStatus The event status to display.
     */
    private void displayStatus(AnimalTamer owner, Tameable pet, EventStatus eventStatus) {
        if (owner instanceof Player) {
            Player player = (Player) owner;

            switch(eventStatus) {
                case TOTAL_LIMIT:
                    player.sendMessage(ChatColor.RED + "You've exceeded the limit for total pets! Limit: " + this.thisPlugin.getPetIndex().getTotalLimit());
                    break;
                case TYPE_LIMIT:
                    PetType.Pets petType = PetType.getEnumByEntity(pet);
                    String petTypeString = petType.toString();
                    player.sendMessage(ChatColor.RED + "You've exceeded the limit for this pet type! " + petTypeString.charAt(0) + petTypeString.substring(1).toLowerCase() + " Limit: " + this.thisPlugin.getPetIndex().getSpecificLimit(petType));
                    break;
                case DB_FAIL:
                    player.sendMessage(ChatColor.RED + "Could not tame this pet");
                    break;
                default:
                    player.sendMessage(ChatColor.RED + "An unknown error occurred");
                    break;
            }
        }
    }

    /**
     * Logs the status of a {@link ListenerEntityTamed#onNewTamedEntity(OfflinePlayer, Entity)}, if enabled.
     * @param owner The new owner. This object is used to get their name.
     * @param eventStatus The event status to display.
     */
    private void logStatus(AnimalTamer owner, EventStatus eventStatus) {
        if (eventStatus != EventStatus.SUCCESS) {
            this.thisPlugin.getLogWrapper().logUnsuccessfulAction(owner.getName() + " - tame - " + eventStatus.toString());
        }
    }

    /**
     * An event listener for the EntityTameEvent. It determines if the event is one that TPPets tracks, then processes
     * the newly tamed pet through {@link ListenerEntityTamed#onNewTamedEntity(OfflinePlayer, Entity)}, displays results
     * through {@link ListenerEntityTamed#displayStatus(AnimalTamer, Tameable, EventStatus)}, logs results through
     * {@link ListenerEntityTamed#logStatus(AnimalTamer, EventStatus)}, and cancels invalid new tamed mobs if needed.
     * @param event The supplied {@link EntityTameEvent}.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityTameEvent(EntityTameEvent event) {
        if (!event.isCancelled() && PetType.isPetTypeTracked(event.getEntity()) && event.getOwner() instanceof OfflinePlayer) {
            Tameable pet = (Tameable) event.getEntity();
            EventStatus eventStatus = onNewTamedEntity((OfflinePlayer) event.getOwner(), pet);

            if (eventStatus != EventStatus.SUCCESS) {
                cancelTame(pet);
                event.setCancelled(true);
                displayStatus(event.getOwner(), pet, eventStatus);
                logStatus(event.getOwner(), eventStatus);
            }
        }
    }

    /**
     * Determines if an entity is tamed by a player (offline or online), rather than a plugin taming an entity to a
     * non-player entity.
     * @param entity The entity to check the owner of.
     * @return true if the entity is tamed by a player, false if not.
     */
    private boolean isEntityTamedByPlayer(Entity entity) {
        return PetType.isPetTracked(entity) && ((Tameable)entity).getOwner() instanceof OfflinePlayer;
    }

    /**
     * An event listener for the EntityBreedEvent. It determines if the new animal is one that TPPets tracks, then processes
     * the newly tamed pet through {@link ListenerEntityTamed#onNewTamedEntity(OfflinePlayer, Entity)}, displays results
     * through {@link ListenerEntityTamed#displayStatus(AnimalTamer, Tameable, EventStatus)}, logs results through
     * {@link ListenerEntityTamed#logStatus(AnimalTamer, EventStatus)}, and cancels invalid new tamed mobs if needed.
     * @param event The supplied {@link EntityBreedEvent}.
     */
    @EventHandler (priority = EventPriority.LOW)
    public void onEntityBreedEvent(EntityBreedEvent event) {
        // e.getEntity() = new animal
        // e.getMother() = one parent
        // e.getFather() = other parent
        // e.getBreeder() = entity (mostly player) that initiated the breeding by feeding
        if (!event.isCancelled() && isEntityTamedByPlayer(event.getEntity())) {
            Tameable pet = (Tameable) event.getEntity();
            OfflinePlayer owner = (OfflinePlayer) pet.getOwner();
            EventStatus eventStatus = onNewTamedEntity(owner, pet);

            if (eventStatus != EventStatus.SUCCESS) {
                cancelTame(pet);
                event.setCancelled(true);
                ((Tameable) event.getFather()).setLoveModeTicks(0);
                ((Tameable) event.getMother()).setLoveModeTicks(0);
                displayStatus(owner, pet, eventStatus);
                logStatus(owner, eventStatus);
            }
        }
    }

    /**
     * Determines if an entity is a special horse. Special horses are {@link ZombieHorse}s or {@link SkeletonHorse}s.
     * @param entity The entity to evaluate.
     * @return true if the entity is a special horse, false if not.
     */
    private boolean isSpecialHorse(Entity entity) {
        return entity instanceof ZombieHorse || entity instanceof SkeletonHorse;
    }

    /**
     * An event listener for the EntityMountEvent. If the entity being mounted is an untamed special horse, TPPets tames
     * it to the rider. It still processes the newly tamed pet through {@link ListenerEntityTamed#onNewTamedEntity(OfflinePlayer, Entity)},
     * displays results through {@link ListenerEntityTamed#displayStatus(AnimalTamer, Tameable, EventStatus)}, logs results
     * through {@link ListenerEntityTamed#logStatus(AnimalTamer, EventStatus)}, and cancels invalid new tamed mobs if needed.
     * @param event The supplied {@link EntityMountEvent}.
     */
    @EventHandler (priority = EventPriority.LOW)
    public void entityMountTameSpecialHorse(EntityMountEvent event) {
        if (!event.isCancelled() && event.getEntity() instanceof Player && isSpecialHorse(event.getMount()) && !PetType.isPetTracked(event.getMount())) {
            Tameable pet = (Tameable) event.getMount();
            Player player = (Player) event.getEntity();
            EventStatus eventStatus = onNewTamedEntity(player, pet);

            if (eventStatus == EventStatus.SUCCESS) {
                pet.setTamed(true);
                pet.setOwner(player);
            } else {
                event.setCancelled(true);
                displayStatus(player, pet, eventStatus);
                logStatus(player, eventStatus);
            }
        }
    }
}
