package com.maxwellwheeler.plugins.tppets.storage;

/**
 * Represents a pet at its last known location.
 * @author GatheringExp
 */
public class PetStorage {
    /** A trimmed, string version of the pet's UUID. */
    public final String petId;
    /** The pet's type. */
    public final PetType.Pets petType;
    /** The pet's last known X coordinate. */
    public final int petX;
    /** The pet's last known Y coordinate. */
    public final int petY;
    /** The pet's last known Z coordinate. */
    public final int petZ;
    /** The pet's last known world. */
    public final String petWorld;
    /** A trimmed, string version of the owner's UUID. */
    public final String ownerId;
    /** The pet's formatted name. */
    public final String petName;
    /** The pet's effective name, for operations. */
    public final String effectivePetName;

    /**
     * Initializes instance variables
     * @param petId A trimmed, string version of the pet's UUID.
     * @param petTypeIndex The pet's type, represented by {@link PetType#getIndexFromPet(PetType.Pets)}-family methods.
     * @see PetType#getIndexFromPet(PetType.Pets)
     * @param petX The pet's last known X coordinate.
     * @param petY The pet's last known Y coordinate.
     * @param petZ The pet's last known Z coordinate.
     * @param petWorld The pet's last known world.
     * @param ownerId A trimmed, string version of the owner's UUID.
     * @param petName The pet's formatted name.
     * @param effectivePetName The pet's effective name, for operations.
     */
    public PetStorage(String petId, int petTypeIndex, int petX, int petY, int petZ, String petWorld, String ownerId, String petName, String effectivePetName) {
        this.petId = petId;
        this.petType = PetType.getPetFromIndex(petTypeIndex);
        this.petX = petX;
        this.petY = petY;
        this.petZ = petZ;
        this.petWorld = petWorld;
        this.ownerId = ownerId;
        this.petName = petName;
        this.effectivePetName = effectivePetName;
    }
}
