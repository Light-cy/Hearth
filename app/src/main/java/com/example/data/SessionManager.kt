package com.example.data

import com.example.model.Member
import com.example.model.Role
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SessionManager {

    companion object {
        val ALI = Member("M01", "Ali", "ali@hearth.pk", Role.ADMIN)
        val AYESHA = Member("M02", "Ayesha", "ayesha@hearth.pk", Role.MEMBER)
        val BILAL = Member("M03", "Bilal", "bilal@hearth.pk", Role.MEMBER)
        val DEFAULT_MEMBERS = listOf(ALI, AYESHA, BILAL)
    }

    private val _registeredMembers = MutableStateFlow(DEFAULT_MEMBERS)
    val registeredMembers: StateFlow<List<Member>> = _registeredMembers.asStateFlow()

    private val memberPasswords = mutableMapOf(
        "ali@hearth.pk" to "hearth2026",
        "ayesha@hearth.pk" to "hearth2026",
        "bilal@hearth.pk" to "hearth2026"
    )

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _accessToken = MutableStateFlow<String?>(null)
    val accessToken: StateFlow<String?> = _accessToken.asStateFlow()

    private val _currentMember = MutableStateFlow(ALI)
    val currentMember: StateFlow<Member> = _currentMember.asStateFlow()

    private val _householdName = MutableStateFlow("Flat 4B, 3 Gulberg Estate")
    val householdName: StateFlow<String> = _householdName.asStateFlow()

    private val _inviteCode = MutableStateFlow("LHR-4B-9872")
    val inviteCode: StateFlow<String> = _inviteCode.asStateFlow()

    fun switchActiveMember(member: Member) {
        _currentMember.value = member
        _isAuthenticated.value = true
    }

    fun authenticateLocal(email: String, password: String): Result<Member> {
        val normalizedEmail = email.trim().lowercase()
        val member = _registeredMembers.value.find {
            it.email.equals(normalizedEmail, ignoreCase = true) ||
                it.name.equals(normalizedEmail.substringBefore("@"), ignoreCase = true)
        } ?: return Result.failure(IllegalArgumentException("No resident account found for $email"))

        val expectedPassword = memberPasswords[member.email.lowercase()] ?: "hearth2026"
        if (password != expectedPassword && password.length < 6) {
            return Result.failure(IllegalArgumentException("Invalid password for ${member.email}"))
        }

        _currentMember.value = member
        _isAuthenticated.value = true
        return Result.success(member)
    }

    fun registerLocal(
        name: String,
        email: String,
        password: String,
        inviteCodeInput: String,
        isCreatingHousehold: Boolean
    ): Result<Member> {
        val trimmedName = name.trim()
        val trimmedEmail = email.trim().lowercase()
        if (trimmedName.isEmpty() || !trimmedEmail.contains("@")) {
            return Result.failure(IllegalArgumentException("Please enter a valid full name and email address"))
        }
        if (password.length < 8) {
            return Result.failure(IllegalArgumentException("Password must be at least 8 characters"))
        }
        if (!isCreatingHousehold && !inviteCodeInput.trim().equals(_inviteCode.value, ignoreCase = true)) {
            return Result.failure(IllegalArgumentException("Invalid household invite code. Expected ${_inviteCode.value}"))
        }

        val existing = _registeredMembers.value.find { it.email.equals(trimmedEmail, ignoreCase = true) }
        if (existing != null) {
            memberPasswords[trimmedEmail] = password
            _currentMember.value = existing
            _isAuthenticated.value = true
            return Result.success(existing)
        }

        val nextNumber = _registeredMembers.value.size + 1
        val newId = String.format("M%02d", nextNumber)
        val role = if (isCreatingHousehold) Role.ADMIN else Role.MEMBER
        val newMember = Member(
            id = newId,
            name = trimmedName,
            email = trimmedEmail,
            role = role
        )

        if (isCreatingHousehold) {
            _householdName.value = "${trimmedName}'s Residence"
            rotateInviteCode()
        }

        memberPasswords[trimmedEmail] = password
        _registeredMembers.value = _registeredMembers.value + newMember
        _currentMember.value = newMember
        _isAuthenticated.value = true
        return Result.success(newMember)
    }

    fun updatePassword(newPassword: String) {
        val emailKey = _currentMember.value.email.lowercase()
        memberPasswords[emailKey] = newPassword
    }

    fun setSessionFromRemote(member: Member, token: String?) {
        if (_registeredMembers.value.none { it.id == member.id }) {
            _registeredMembers.value = _registeredMembers.value + member
        }
        _currentMember.value = member
        _accessToken.value = token
        _isAuthenticated.value = true
    }

    fun signOut() {
        _accessToken.value = null
        _isAuthenticated.value = false
    }

    fun rotateInviteCode(): String {
        val newCode = "LHR-4B-" + (1000..9999).random()
        _inviteCode.value = newCode
        return newCode
    }
}
