package com.maxwellwheeler.plugins.tppets.helpers;

import com.maxwellwheeler.plugins.tppets.storage.SQLWrapper;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class GuestManager {
    private final Hashtable<String, List<String>> guests;

    public GuestManager(SQLWrapper sqlWrapper) throws SQLException {
        this.guests = sqlWrapper.getAllGuests();
    }

    public void addGuest(@NotNull String petId, @NotNull String playerId) {
        String trimmedPetId = UUIDUtils.trimUUID(petId);
        if (!this.guests.containsKey(trimmedPetId)) {
            this.guests.put(trimmedPetId, new ArrayList<>());
        }
        this.guests.get(trimmedPetId).add(UUIDUtils.trimUUID(playerId));
    }

    public void removeGuest(@NotNull String petId, @NotNull String playerId) {
        String trimmedPetId = UUIDUtils.trimUUID(petId);
        if (this.guests.containsKey(trimmedPetId)) {
            this.guests.get(trimmedPetId).remove(UUIDUtils.trimUUID(playerId));
        }
    }

    public boolean isGuest(@NotNull String petId, @NotNull String playerId) {
        String trimmedPetId = UUIDUtils.trimUUID(petId);
        return this.guests.containsKey(trimmedPetId) && this.guests.get(trimmedPetId).contains(UUIDUtils.trimUUID(playerId));
    }

    public List<String> getGuestsToPet(@NotNull String petId) {
        String trimmedPetId = UUIDUtils.trimUUID(petId);
        if (this.guests.containsKey(trimmedPetId)) {
            return this.guests.get(trimmedPetId);
        }
        return new ArrayList<>();
    }
}
