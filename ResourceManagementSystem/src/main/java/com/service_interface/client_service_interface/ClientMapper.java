package com.service_interface.client_service_interface;

import com.dto.ClientDTO;
import com.dto.ClientDetailsDTO;
import com.entity.client_entities.Client;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface ClientMapper {

    @Mapping(source = "clientId", target = "clientId")
    ClientDTO toDto(Client entity);

    List<ClientDTO> toDtoList(List<Client> entities);

    ClientDetailsDTO toClientDetailsDTO(Client c);
}

