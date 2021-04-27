package com.maxwellwheeler.plugins.tppets.test.storage;

import com.maxwellwheeler.plugins.tppets.storage.PetType;
import com.maxwellwheeler.plugins.tppets.test.MockFactory;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Wolf;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class TPPPetTypeTest {
    @ParameterizedTest
    @MethodSource("entityTranslationProvider")
    void getEnumByEntityTest(PetType.Pets petType, Class<? extends Entity> className) {
        Entity entity = MockFactory.getMockEntity("MockPetId", className);
        assertEquals(petType, PetType.getEnumByEntity(entity));
    }

    @ParameterizedTest
    @MethodSource("entityTranslationProvider")
    void isPetTypeTrackedTest(PetType.Pets petType, Class<? extends Entity> className) {
        Entity entity = MockFactory.getMockEntity("MockPetId", className);
        assertEquals(!petType.equals(PetType.Pets.UNKNOWN), PetType.isPetTypeTracked(entity));
    }

    private static Stream<Arguments> entityTranslationProvider() {
        return Stream.of(
                Arguments.of(PetType.Pets.DOG, org.bukkit.entity.Wolf.class),
                Arguments.of(PetType.Pets.CAT, org.bukkit.entity.Cat.class),
                Arguments.of(PetType.Pets.PARROT, org.bukkit.entity.Parrot.class),
                Arguments.of(PetType.Pets.MULE, org.bukkit.entity.Mule.class),
                Arguments.of(PetType.Pets.LLAMA, org.bukkit.entity.Llama.class),
                Arguments.of(PetType.Pets.DONKEY, org.bukkit.entity.Donkey.class),
                Arguments.of(PetType.Pets.HORSE, org.bukkit.entity.Horse.class),
                Arguments.of(PetType.Pets.HORSE, org.bukkit.entity.SkeletonHorse.class),
                Arguments.of(PetType.Pets.HORSE, org.bukkit.entity.ZombieHorse.class),
                Arguments.of(PetType.Pets.UNKNOWN, org.bukkit.entity.Villager.class)
        );
    }

    @ParameterizedTest
    @MethodSource("indexTranslationProvider")
    void getIndexFromPetTest(PetType.Pets petType, int expectedIndex) {
        assertEquals(expectedIndex, PetType.getIndexFromPet(petType));
    }

    @ParameterizedTest
    @MethodSource("indexTranslationProvider")
    void getPetFromIndex(PetType.Pets petType, int expectedIndex) {
        assertEquals(petType, PetType.getPetFromIndex(expectedIndex));
    }

    private static Stream<Arguments> indexTranslationProvider() {
        return Stream.of(
                Arguments.of(PetType.Pets.UNKNOWN, 0),
                Arguments.of(PetType.Pets.CAT, 1),
                Arguments.of(PetType.Pets.DOG, 2),
                Arguments.of(PetType.Pets.PARROT, 3),
                Arguments.of(PetType.Pets.MULE, 4),
                Arguments.of(PetType.Pets.LLAMA, 5),
                Arguments.of(PetType.Pets.DONKEY, 6),
                Arguments.of(PetType.Pets.HORSE, 7)
        );
    }

    @Test
    @DisplayName("isPetTracked returns true")
    void isPetTrackedReturnsTrue() {
        OfflinePlayer owner = mock(OfflinePlayer.class);
        Wolf wolf = MockFactory.getTamedMockEntity("MockPetId", org.bukkit.entity.Wolf.class, owner);
        assertTrue(PetType.isPetTracked(wolf));
    }

    @Test
    @DisplayName("isPetTracked returns false if not tamed")
    void isPetTrackedReturnsFalseIfNotTamed() {
        Wolf wolf = MockFactory.getMockEntity("MockPetId", org.bukkit.entity.Wolf.class);
        assertFalse(PetType.isPetTracked(wolf));
    }

    @Test
    @DisplayName("isPetTracked returns false if owner null")
    void isPetTrackedReturnsFalseIfOwnerNull() {
        Wolf wolf = MockFactory.getTamedMockEntity("MockPetId", org.bukkit.entity.Wolf.class, null);
        assertFalse(PetType.isPetTracked(wolf));
    }

    @Test
    @DisplayName("isPetTracked returns false if pet of incorrect type")
    void isPetTrackedReturnsFalseIfIncorrectPetType() {
        Villager villager = MockFactory.getMockEntity("MockPetId", org.bukkit.entity.Villager.class);
        assertFalse(PetType.isPetTracked(villager));
    }
}
