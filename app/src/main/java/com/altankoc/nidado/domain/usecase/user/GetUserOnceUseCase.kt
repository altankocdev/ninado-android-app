package com.altankoc.nidado.domain.usecase.user

import com.altankoc.nidado.domain.model.User
import com.altankoc.nidado.domain.repository.UserRepository
import javax.inject.Inject

class GetUserOnceUseCase @Inject constructor(
    private val repository: UserRepository
) {
    suspend operator fun invoke(): User? {
        return repository.getUserOnce()
    }
}