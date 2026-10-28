package com.altankoc.nidado.domain.usecase.user

import com.altankoc.nidado.domain.repository.UserRepository
import com.altankoc.nidado.util.Resource
import javax.inject.Inject

class UpdateNotificationSoundUseCase @Inject constructor(
    private val repository: UserRepository
) {
    suspend operator fun invoke(soundId: String): Resource<Unit> {
        return repository.updateNotificationSound(soundId)
    }
}