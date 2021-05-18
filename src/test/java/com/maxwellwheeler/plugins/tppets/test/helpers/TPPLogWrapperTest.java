package com.maxwellwheeler.plugins.tppets.test.helpers;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.storage.SQLWrapper;
import com.maxwellwheeler.plugins.tppets.test.MockFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.logging.Level;
import java.util.logging.Logger;

import static org.mockito.Mockito.*;

public class TPPLogWrapperTest {
    private LogWrapper logWrapperEnabled;
    private LogWrapper logWrapperDisabled;
    private Logger logger;

    @BeforeEach
    public void beforeEach() {
        SQLWrapper sqlWrapper = mock(SQLWrapper.class);
        TPPets tpPets = MockFactory.getMockPlugin(sqlWrapper, null, false, false);
        this.logWrapperEnabled = new LogWrapper(tpPets, true, true, true, true, true);
        this.logWrapperDisabled = new LogWrapper(tpPets, false, false, false, false, false);
        this.logger = mock(Logger.class);

        when(tpPets.getLogger()).thenReturn(this.logger);
    }

    @Test
    @DisplayName("LogWrapper always logs TPPets plugin info")
    void logWrapperLogsPluginInfoWhenEnabled() {
        this.logWrapperEnabled.logPluginInfo("Plugin Info");

        verify(this.logger, times(1)).info("TPPets Plugin: Plugin Info");
    }

    @Test
    @DisplayName("LogWrapper logs updated pets if setting enabled")
    void logWrapperLogsUpdatedPetsWhenEnabled() {
        this.logWrapperEnabled.logUpdatedPet("Updated Pet");

        verify(this.logger, times(1)).info("Updated Pet: Updated Pet");
    }

    @Test
    @DisplayName("LogWrapper doesn't log updated pets if setting disabled")
    void logWrapperNoLogUpdatedPetsWhenDisabled() {
        this.logWrapperDisabled.logUpdatedPet("Updated Pet");

        verify(this.logger, never()).info(anyString());
    }

    @Test
    @DisplayName("LogWrapper logs successful actions if setting enabled")
    void logWrapperLogsSuccessfulActionsWhenEnabled() {
        this.logWrapperEnabled.logSuccessfulAction("Successful Action");

        verify(this.logger, times(1)).info("Successful Action: Successful Action");
    }

    @Test
    @DisplayName("LogWrapper doesn't log successful actions if setting disabled")
    void logWrapperNoLogSuccessfulActionsWhenDisabled() {
        this.logWrapperDisabled.logSuccessfulAction("Successful Action");

        verify(this.logger, never()).info(anyString());
    }

    @Test
    @DisplayName("LogWrapper logs unsuccessful actions if setting enabled")
    void logWrapperLogsUnsuccessfulActionsWhenEnabled() {
        this.logWrapperEnabled.logUnsuccessfulAction("Unsuccessful Action");

        verify(this.logger, times(1)).info("Unsuccessful Action: Unsuccessful Action");
    }

    @Test
    @DisplayName("LogWrapper doesn't log unsuccessful actions if setting disabled")
    void logWrapperNoLogUnsuccessfulActionsWhenDisabled() {
        this.logWrapperDisabled.logUnsuccessfulAction("Unsuccessful Action");

        verify(this.logger, never()).info(anyString());
    }

    @Test
    @DisplayName("LogWrapper logs errors if setting enabled")
    void logWrapperLogsErrorsWhenEnabled() {
        this.logWrapperEnabled.logErrors("Error");

        verify(this.logger, times(1)).log(Level.SEVERE, "Error: Error");
    }

    @Test
    @DisplayName("LogWrapper doesn't log errors if setting disabled")
    void logWrapperNoLogErrorsWhenDisabled() {
        this.logWrapperDisabled.logErrors("Error");

        verify(this.logger, never()).log(any(Level.class), anyString());
    }

    @Test
    @DisplayName("LogWrapper logs prevented damage if setting enabled")
    void logWrapperLogsPreventedDamageWhenEnabled() {
        this.logWrapperEnabled.logPreventedDamage("Prevented Damage");

        verify(this.logger, times(1)).info("Prevented Damage: Prevented Damage");
    }

    @Test
    @DisplayName("LogWrapper doesn't log prevented damage if setting disabled")
    void logWrapperNoLogPreventedDamageWhenDisabled() {
        this.logWrapperDisabled.logPreventedDamage("Prevented Damage");

        verify(this.logger, never()).info(anyString());
    }
}
