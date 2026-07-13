package com.abhinavsirohi.kiwi.domain.usecase.session

import com.abhinavsirohi.kiwi.domain.model.SessionRestoration
import com.abhinavsirohi.kiwi.domain.repository.SessionRestorationRepository
import com.abhinavsirohi.kiwi.domain.usecase.UseCase

class RestoreSession(
    private val repository: SessionRestorationRepository,
) : UseCase<Unit, SessionRestoration> {
    override suspend fun invoke(parameters: Unit): SessionRestoration = repository.restore()
}
