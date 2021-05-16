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
        this.guests = sqlWrapper.getAllAllowedPlayers();
    }

    public void addGuest(@NotNull String petId, @NotNull String playerId) {
        if (!this.guests.containsKey(petId)) {
            this.guests.put(petId, new ArrayList<>());
        }
        this.guests.get(petId).add(UUIDUtils.trimUUID(playerId));
    }

    public void removeGuest(@NotNull String petId, @NotNull String playerId) {
        if (this.guests.containsKey(petId)) {
            this.guests.get(petId).remove(UUIDUtils.trimUUID(playerId));
        }
    }

    public boolean isGuest(@NotNull String petId, @NotNull String playerId) {
        return this.guests.containsKey(petId) && this.guests.get(petId).contains(UUIDUtils.trimUUID(playerId));
    }

    public List<String> getGuestsToPet(@NotNull String petId) {
        if (this.guests.containsKey(petId)) {
            return this.guests.get(petId);
        }
        return new ArrayList<>();
    }
}
