package com.altankoc.nidado.domain.usecase.user

import com.altankoc.nidado.domain.repository.UserRepository
import com.altankoc.nidado.util.Resource
import javax.inject.Inject

class UpdateNotificationsUseCase @Inject constructor(
    private val repository: UserRepository
) {
    suspend operator fun invoke(enabled: Boolean): Resource<Unit> {
        return repository.updateNotifications(enabled)
    }
}