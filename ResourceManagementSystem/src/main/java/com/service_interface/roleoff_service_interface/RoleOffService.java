package com.service_interface.roleoff_service_interface;

import com.dto.UserDTO;
import com.dto.roleoff_dto.RoleOffRequestDTO;
import org.springframework.http.ResponseEntity;

public interface RoleOffService {
    public ResponseEntity<?> roleOffByRM(RoleOffRequestDTO roleOff, UserDTO userDTO);
}
