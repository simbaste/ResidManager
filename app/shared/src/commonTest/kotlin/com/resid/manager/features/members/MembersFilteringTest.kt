package com.resid.manager.features.members

import com.resid.manager.dto.InvitationStatusDto
import com.resid.manager.dto.ResidenceMemberSummaryDto
import com.resid.manager.dto.RoleDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MembersFilteringTest {

    private val members = listOf(
        ResidenceMemberSummaryDto(
            userId = "1",
            firstName = "Administrateur",
            lastName = "ResidManager",
            email = "admin@residmanager.com",
            phone = "+2376998517",
            roleDto = RoleDto.OWNER,
            status = InvitationStatusDto.ACCEPTED,
        ),
        ResidenceMemberSummaryDto(
            userId = "2",
            firstName = "Stephane",
            lastName = "Mba",
            email = "stephane.mba@test.fr",
            phone = null,
            roleDto = RoleDto.ADMIN,
            status = InvitationStatusDto.ACCEPTED,
        ),
        ResidenceMemberSummaryDto(
            userId = "3",
            firstName = "Test",
            lastName = "Tenant",
            email = "tenant@test.fr",
            phone = "+2376998517",
            roleDto = RoleDto.TENANT,
            status = InvitationStatusDto.ACCEPTED,
        ),
        ResidenceMemberSummaryDto(
            userId = "4",
            firstName = "Test",
            lastName = "Testeur",
            email = "test@test.fr",
            phone = "+237699851717",
            roleDto = RoleDto.MANAGER,
            status = InvitationStatusDto.ACCEPTED,
        ),
    )

    private fun filter(
        query: String = "",
        role: RoleDto? = null,
    ): List<ResidenceMemberSummaryDto> {
        val q = query.trim().lowercase()
        return members.filter { m ->
            val matchesRole = (role == null) || (m.roleDto == role)
            val matchesQuery = if (q.isEmpty()) {
                true
            } else {
                val fullName = "${m.firstName} ${m.lastName}".lowercase()
                val reverseFullName = "${m.lastName} ${m.firstName}".lowercase()
                val email = m.email.lowercase()
                val phone = m.phone?.lowercase().orEmpty()

                m.firstName.lowercase().contains(q) ||
                    m.lastName.lowercase().contains(q) ||
                    fullName.contains(q) ||
                    reverseFullName.contains(q) ||
                    email.contains(q) ||
                    phone.contains(q)
            }
            matchesRole && matchesQuery
        }
    }

    @Test
    fun filter_noFilters_returnsAll() {
        val result = filter()
        assertEquals(4, result.size)
    }

    @Test
    fun filter_byRole_returnsMatchingMembers() {
        val result = filter(role = RoleDto.ADMIN)
        assertEquals(1, result.size)
        assertEquals("Stephane", result.first().firstName)
    }

    @Test
    fun filter_bySearchQueryName_returnsMatchingMembers() {
        val result = filter(query = "stephane")
        assertEquals(1, result.size)
        assertEquals("Stephane", result.first().firstName)
    }

    @Test
    fun filter_bySearchQueryEmail_returnsMatchingMembers() {
        val result = filter(query = "admin@residmanager")
        assertEquals(1, result.size)
        assertEquals(RoleDto.OWNER, result.first().roleDto)
    }

    @Test
    fun filter_bySearchQueryPhone_returnsMatchingMembers() {
        val result = filter(query = "+2376998517")
        assertEquals(3, result.size)
    }

    @Test
    fun filter_byRoleAndQuery_combined() {
        val result = filter(query = "test", role = RoleDto.MANAGER)
        assertEquals(1, result.size)
        assertEquals("Testeur", result.first().lastName)
    }

    @Test
    fun filter_noMatch_returnsEmpty() {
        val result = filter(query = "unknown")
        assertTrue(result.isEmpty())
    }
}
