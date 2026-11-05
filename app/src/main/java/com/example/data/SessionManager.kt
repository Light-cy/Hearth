package com.example.data

import com.example.model.Member
import com.example.model.Role
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SessionManager {

    companion object {
        val ALI = Member("M01", "Ali", "ali@example.com", Role.ADMIN)
        val AYESHA = Member("M02", "Ayesha", "ayesha@example.com", Role.MEMBER)
        val BILAL = Member("M03", "Bilal", "bilal@example.com", Role.MEMBER)
        val DEFAULT_MEMBERS = listOf(ALI, AYESHA, BILAL)
    }

    private val _currentMember = MutableStateFlow(ALI)
    val currentMember: StateFlow<Member> = _currentMember.asStateFlow()

    private val _householdName = MutableStateFlow("Roommates Flat 4B, Lahore")
    val householdName: StateFlow<String> = _householdName.asStateFlow()

    private val _inviteCode = MutableStateFlow("LHR-4B-9872")
    val inviteCode: StateFlow<String> = _inviteCode.asStateFlow()

    fun switchActiveMember(member: Member) {
        _currentMember.value = member
    }

    fun rotateInviteCode(): String {
        val newCode = "LHR-4B-" + (1000..9999).random()
        _inviteCode.value = newCode
        return newCode
    }
}
