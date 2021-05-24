package com.maxwellwheeler.plugins.tppets.helpers;

import com.maxwellwheeler.plugins.tppets.storage.SQLWrapper;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

/**
 * Used to cache guests allowed to pets.
 * @author GatheringExp
 */
public class GuestManager {
    /** A hashtable of &lt;Trimmed Pet Id, List&lt;Trimmed Guest Id&gt;&gt; */
    private final Hashtable<String, List<String>> guests;

    /**
     * Initializes the guest list based on {@link SQLWrapper#getAllGuests()}.
     * @param sqlWrapper The {@link SQLWrapper} to query.
     * @throws SQLException If getting all guests from the database fails.
     */
    public GuestManager(SQLWrapper sqlWrapper) throws SQLException {
        this.guests = sqlWrapper.getAllGuests();
    }

    /**
     * Adds a new guest to a pet.
     * @param petId A string representation of a pet id. Can be trimmed or untrimmed, but can't be null.
     * @param playerId A string representation of a player id. Can be trimmed or untrimmed, but can't be null.
     */
    public void addGuest(@NotNull String petId, @NotNull String playerId) {
        String trimmedPetId = UUIDUtils.trimUUID(petId);
        if (!this.guests.containsKey(trimmedPetId)) {
            this.guests.put(trimmedPetId, new ArrayList<>());
        }
        this.guests.get(trimmedPetId).add(UUIDUtils.trimUUID(playerId));
    }

    /**
     * Removes an existing guest from a pet.
     * @param petId A string representation of a pet id. Can be trimmed or untrimmed, but can't be null.
     * @param playerId A string representation of a player id. Can be trimmed or untrimmed, but can't be null.
     */
    public void removeGuest(@NotNull String petId, @NotNull String playerId) {
        String trimmedPetId = UUIDUtils.trimUUID(petId);
        if (this.guests.containsKey(trimmedPetId)) {
            this.guests.get(trimmedPetId).remove(UUIDUtils.trimUUID(playerId));
        }
    }

    /**
     * Determines if a player is an existing guest from a pet.
     * @param petId A string representation of a pet id. Can be trimmed or untrimmed, but can't be null.
     * @param playerId A string representation of a player id. Can be trimmed or untrimmed, but can't be null.
     * @return true if player is an existing guest of the pet, false if not.
     */
    public boolean isGuest(@NotNull String petId, @NotNull String playerId) {
        String trimmedPetId = UUIDUtils.trimUUID(petId);
        return this.guests.containsKey(trimmedPetId) && this.guests.get(trimmedPetId).contains(UUIDUtils.trimUUID(playerId));
    }

    /**
     * Gets all trimmed guest ids to a pet.
     * @param petId A string representation of a pet id. Can be trimmed or untrimmed, but can't be null.
     * @return A list of trimmed string player ids, representing all players that are allowed to the pet.
     */
    public List<String> getGuestsToPet(@NotNull String petId) {
        String trimmedPetId = UUIDUtils.trimUUID(petId);
        if (this.guests.containsKey(trimmedPetId)) {
            return this.guests.get(trimmedPetId);
        }
        return new ArrayList<>();
    }
}
