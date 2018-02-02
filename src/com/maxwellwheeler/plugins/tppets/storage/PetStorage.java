package com.maxwellwheeler.plugins.tppets.storage;

public class PetStorage {
    public String petId;
    public PetType.Pets petType;
    public int petX;
    public int petY;
    public int petZ;
    public String petWorld;
    public String ownerId;
    
    public PetStorage(String petId, int petTypeIndex, int petX, int petY, int petZ, String petWorld, String ownerId) {
        this.petId = petId;
        this.petType = PetType.getPetFromIndex(petTypeIndex);
        this.petX = petX;
        this.petY = petY;
        this.petZ = petZ;
        this.petWorld = petWorld;
        this.ownerId = ownerId;
    }
}
