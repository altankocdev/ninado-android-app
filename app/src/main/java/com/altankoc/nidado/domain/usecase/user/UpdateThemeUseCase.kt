package com.altankoc.nidado.domain.usecase.user

import com.altankoc.nidado.domain.model.enums.ThemePreference
import com.altankoc.nidado.domain.repository.UserRepository
import com.altankoc.nidado.util.Resource
import javax.inject.Inject

class UpdateThemeUseCase @Inject constructor(
    private val repository: UserRepository
) {
    suspend operator fun invoke(theme: ThemePreference): Resource<Unit> {
        return repository.updateTheme(theme)
    }
}