package com.service_interface.client_service_interface;

import com.dto.client_dto.ClientDTO;
import com.dto.client_dto.ClientDetailsDTO;
import com.entity.client_entities.Client;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-04-24T16:34:00+0530",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.6 (Oracle Corporation)"
)
@Component
public class ClientMapperImpl implements ClientMapper {

    @Override
    public ClientDTO toDto(Client entity) {
        if ( entity == null ) {
            return null;
        }

        ClientDTO.ClientDTOBuilder clientDTO = ClientDTO.builder();

        clientDTO.clientId( entity.getClientId() );
        clientDTO.clientName( entity.getClientName() );
        if ( entity.getClientType() != null ) {
            clientDTO.clientType( entity.getClientType().name() );
        }
        if ( entity.getPriorityLevel() != null ) {
            clientDTO.priorityLevel( entity.getPriorityLevel().name() );
        }
        if ( entity.getDeliveryModel() != null ) {
            clientDTO.deliveryModel( entity.getDeliveryModel().name() );
        }
        clientDTO.countryName( entity.getCountryName() );
        clientDTO.defaultTimezone( entity.getDefaultTimezone() );
        if ( entity.getStatus() != null ) {
            clientDTO.status( entity.getStatus().name() );
        }

        return clientDTO.build();
    }

    @Override
    public List<ClientDTO> toDtoList(List<Client> entities) {
        if ( entities == null ) {
            return null;
        }

        List<ClientDTO> list = new ArrayList<ClientDTO>( entities.size() );
        for ( Client client : entities ) {
            list.add( toDto( client ) );
        }

        return list;
    }

    @Override
    public ClientDetailsDTO toClientDetailsDTO(Client c) {
        if ( c == null ) {
            return null;
        }

        ClientDetailsDTO.ClientDetailsDTOBuilder clientDetailsDTO = ClientDetailsDTO.builder();

        clientDetailsDTO.clientName( c.getClientName() );
        if ( c.getClientType() != null ) {
            clientDetailsDTO.clientType( c.getClientType().name() );
        }
        if ( c.getPriorityLevel() != null ) {
            clientDetailsDTO.priorityLevel( c.getPriorityLevel().name() );
        }
        if ( c.getDeliveryModel() != null ) {
            clientDetailsDTO.deliveryModel( c.getDeliveryModel().name() );
        }
        clientDetailsDTO.countryName( c.getCountryName() );
        clientDetailsDTO.defaultTimezone( c.getDefaultTimezone() );
        if ( c.getStatus() != null ) {
            clientDetailsDTO.status( c.getStatus().name() );
        }

        return clientDetailsDTO.build();
    }
}
