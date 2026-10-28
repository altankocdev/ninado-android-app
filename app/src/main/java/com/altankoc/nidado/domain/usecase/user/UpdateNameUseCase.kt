package com.altankoc.nidado.domain.usecase.user

import com.altankoc.nidado.domain.repository.UserRepository
import com.altankoc.nidado.util.Resource
import javax.inject.Inject

class UpdateNameUseCase @Inject constructor(
    private val repository: UserRepository
) {
    suspend operator fun invoke(name: String): Resource<Unit> {
        if (name.isBlank()) return Resource.Error("İsim boş olamaz")
        if (name.contains(" ")) return Resource.Error("İsim tek kelime olmalı")
        if (!name.all { it.isLetter() }) return Resource.Error("İsim sadece harf içermeli")
        return repository.updateName(name)
    }
}