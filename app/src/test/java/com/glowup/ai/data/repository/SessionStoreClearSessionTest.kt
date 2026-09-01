package com.glowup.ai.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.glowup.ai.data.local.SessionStore
import com.glowup.ai.domain.model.ConsentState
import com.glowup.ai.domain.model.EntitlementStatus
import com.glowup.ai.domain.model.Plan
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

/**
 * Covers ANDROID_PLAN.md 2.4's `clearSession()` scoping requirement: "the web client's
 * `localStorage.clear()` destroyed unrelated data ... do not repeat that." [SessionStore] must
 * remove ONLY its own keys, never call a blanket `Preferences.edit { it.clear() }`.
 *
 * Runs against a real `DataStore<Preferences>` backed by a temp file — no Robolectric/Android
 * framework needed for the Preferences DataStore core artifact.
 */
class SessionStoreClearSessionTest {
    /** A key SessionStore does not own — stands in for a hypothetical future feature sharing the
     * same DataStore file. Never removed by [SessionStore.clearSession]. */
    private val foreignKey = stringPreferencesKey("some_unrelated_feature_flag")

    private fun newStore(): Pair<SessionStore, DataStore<Preferences>> {
        val file = File.createTempFile("glowup_session_test", ".preferences_pb")
        file.deleteOnExit()
        val dataStore = PreferenceDataStoreFactory.create(produceFile = { file })
        return SessionStore(dataStore) to dataStore
    }

    @Test
    fun `clearSession removes every GlowUp key`() =
        runTest {
            val (store, _) = newStore()
            store.setUserId("user-1")
            store.setEntitlement(Plan.PREMIUM, EntitlementStatus.ACTIVE)
            store.setConsentState(ConsentState.ACTIVE)
            store.setOnboardingComplete(true)
            store.setSelectedVertical("hair")
            store.setThemePreference("dark")
            store.setReminderSchedule(cadenceDays = 7, nextAt = "2026-09-01T00:00:00Z")

            store.clearSession()

            assertNull(store.userId())
            assertEquals(Plan.UNKNOWN, store.plan())
            assertEquals(ConsentState.UNKNOWN, store.consentStateFlow.first())
            assertEquals(false, store.onboardingCompleteFlow.first())
            assertEquals("skin", store.selectedVertical())
            assertEquals("system", store.themePreferenceFlow.first())
            val reminders = store.reminderSettingsFlow.first()
            assertNull(reminders.cadenceDays)
            assertNull(reminders.nextAt)
        }

    @Test
    fun `clearSession never touches a key it does not own`() =
        runTest {
            val (store, dataStore) = newStore()
            store.setUserId("user-1")

            // Simulate a foreign key already present in the SAME underlying DataStore file, the way
            // localStorage.clear() would have destroyed it on the web client.
            dataStore.edit { it[foreignKey] = "do-not-touch" }

            store.clearSession()

            val afterClear = dataStore.data.first()
            assertEquals("do-not-touch", afterClear[foreignKey])
            assertNull(store.userId())
        }
}
