package com.github.michaelanisimov.whattheblame.settings

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service

@Service(Service.Level.APP)
class ApiKeyService {

    private val attributes: CredentialAttributes =
        CredentialAttributes(generateServiceName("WhatTheBlame", "anthropic.api.key"))

    fun get(): String? = PasswordSafe.instance.getPassword(attributes)?.takeIf { it.isNotBlank() }

    fun set(value: String?) {
        PasswordSafe.instance.setPassword(attributes, value?.takeIf { it.isNotBlank() })
    }

    companion object {
        fun getInstance(): ApiKeyService = service()
    }
}
