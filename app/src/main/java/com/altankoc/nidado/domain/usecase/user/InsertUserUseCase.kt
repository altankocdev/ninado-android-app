package com.altankoc.nidado.domain.usecase.user

import com.altankoc.nidado.domain.model.User
import com.altankoc.nidado.domain.repository.UserRepository
import com.altankoc.nidado.util.Resource
import javax.inject.Inject

class InsertUserUseCase @Inject constructor(
    private val repository: UserRepository
) {
    suspend operator fun invoke(user: User): Resource<Unit> {
        if (user.name.isBlank()) return Resource.Error("İsim boş olamaz")
        if (user.name.contains(" ")) return Resource.Error("İsim tek kelime olmalı")
        if (!user.name.all { it.isLetter() }) return Resource.Error("İsim sadece harf içermeli")
        return repository.insertUser(user)
    }
}