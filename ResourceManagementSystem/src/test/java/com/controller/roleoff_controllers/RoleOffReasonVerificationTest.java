package com.controller.roleoff_controllers;

import com.controller.roleoff_controllers.RoleOffController;
import com.entity_enums.allocation_enums.RoleOffReason;
import com.service_interface.roleoff_service_interface.RoleOffService;
import com.service_imple.roleoff_service_impl.RoleOffServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class RoleOffReasonVerificationTest {

    @Mock
    private RoleOffServiceImpl roleOffServiceImpl;

    @Mock
    private RoleOffService roleOffService;

    @InjectMocks
    private RoleOffController roleOffController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(roleOffController).build();
    }

    @Test
    void testGetRoleOffReasons_ReturnsAllEnumValues() {
        // Act
        ResponseEntity<List<RoleOffReason>> response = roleOffController.getRoleOffReasons();

        // Assert
        assertNotNull(response);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        
        List<RoleOffReason> reasons = response.getBody();
        assertNotNull(reasons);
        assertEquals(4, reasons.size());
        
        // Verify all enum values are present
        assertTrue(reasons.contains(RoleOffReason.PROJECT_END));
        assertTrue(reasons.contains(RoleOffReason.PERFORMANCE));
        assertTrue(reasons.contains(RoleOffReason.ATTRITION));
        assertTrue(reasons.contains(RoleOffReason.CLIENT_REQUEST));
    }

    @Test
    void testGetRoleOffReasons_ReturnsCorrectOrder() {
        // Act
        ResponseEntity<List<RoleOffReason>> response = roleOffController.getRoleOffReasons();

        // Assert
        List<RoleOffReason> reasons = response.getBody();
        assertNotNull(reasons);
        
        // Verify the order matches enum declaration order
        List<RoleOffReason> expectedOrder = Arrays.asList(RoleOffReason.values());
        assertEquals(expectedOrder, reasons);
    }

    @Test
    void testRoleOffReasonEnum_Values() {
        // Test enum values exist and are correct
        RoleOffReason[] reasons = RoleOffReason.values();
        assertEquals(4, reasons.length);
        
        // Test each enum value
        assertEquals("PROJECT_END", RoleOffReason.PROJECT_END.name());
        assertEquals("PERFORMANCE", RoleOffReason.PERFORMANCE.name());
        assertEquals("ATTRITION", RoleOffReason.ATTRITION.name());
        assertEquals("CLIENT_REQUEST", RoleOffReason.CLIENT_REQUEST.name());
    }

    @Test
    void testRoleOffReasonEnum_ValueOf() {
        // Test valueOf method
        assertEquals(RoleOffReason.PROJECT_END, RoleOffReason.valueOf("PROJECT_END"));
        assertEquals(RoleOffReason.PERFORMANCE, RoleOffReason.valueOf("PERFORMANCE"));
        assertEquals(RoleOffReason.ATTRITION, RoleOffReason.valueOf("ATTRITION"));
        assertEquals(RoleOffReason.CLIENT_REQUEST, RoleOffReason.valueOf("CLIENT_REQUEST"));
    }

    @Test
    void testRoleOffReasonEnum_InvalidValueOf() {
        // Test that valueOf throws exception for invalid values
        assertThrows(IllegalArgumentException.class, () -> {
            RoleOffReason.valueOf("INVALID_REASON");
        });
    }

    @Test
    void testGetRoleOffReasons_WithMockMvc() throws Exception {
        // Test the endpoint using MockMvc
        mockMvc.perform(get("/api/role-off/reasons"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0]").value("PROJECT_END"))
                .andExpect(jsonPath("$[1]").value("PERFORMANCE"))
                .andExpect(jsonPath("$[2]").value("ATTRITION"))
                .andExpect(jsonPath("$[3]").value("CLIENT_REQUEST"));
    }
}
