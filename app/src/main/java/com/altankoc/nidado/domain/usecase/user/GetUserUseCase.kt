package com.altankoc.nidado.domain.usecase.user

import com.altankoc.nidado.domain.model.User
import com.altankoc.nidado.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetUserUseCase @Inject constructor(
    private val repository: UserRepository
) {
    operator fun invoke(): Flow<User?> {
        return repository.getUser()
    }
}