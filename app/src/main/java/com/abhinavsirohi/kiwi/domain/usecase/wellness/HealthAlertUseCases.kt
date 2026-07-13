package com.abhinavsirohi.kiwi.domain.usecase.wellness

import com.abhinavsirohi.kiwi.core.common.AppResult
import com.abhinavsirohi.kiwi.domain.model.CycleRecord
import com.abhinavsirohi.kiwi.domain.model.HealthAlertEpisode
import com.abhinavsirohi.kiwi.domain.model.WellnessDailyRecord
import com.abhinavsirohi.kiwi.domain.repository.HealthAlertRepository
import kotlinx.coroutines.flow.Flow

class ObserveHealthAlertEpisodes(private val repository: HealthAlertRepository) {
    operator fun invoke(): Flow<AppResult<List<HealthAlertEpisode>>> = repository.observeVisibleEpisodes()
}
class ReconcileHealthAlerts(private val repository: HealthAlertRepository) {
    suspend operator fun invoke(cycles: List<CycleRecord>, daily: List<WellnessDailyRecord>) = repository.reconcile(cycles, daily)
}
class AcknowledgeHealthAlert(private val repository: HealthAlertRepository) {
    suspend operator fun invoke(localId: String) = repository.acknowledge(localId)
}
class DismissHealthAlert(private val repository: HealthAlertRepository) {
    suspend operator fun invoke(localId: String) = repository.dismiss(localId)
}
