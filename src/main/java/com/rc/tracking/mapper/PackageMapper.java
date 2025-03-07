package com.rc.tracking.mapper;

import com.rc.tracking.model.dto.EventDTO;
import com.rc.tracking.model.dto.PackageDetailResponse;
import com.rc.tracking.model.dto.PackageResponse;
import com.rc.tracking.model.entity.PackageEntity;
import com.rc.tracking.model.entity.TrackingEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PackageMapper {

    @Mapping(target = "id", expression = "java(\"packageEntity-\" + packageEntity.getId())")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "deliveredAt", source = "deliveredAt")
    PackageResponse packageEntityToPackageResponse(PackageEntity packageEntity);

    @Mapping(target = "id", expression = "java(\"packageEntity-\" + packageEntity.getId())")
    @Mapping(target = "status", source = "status")
    PackageDetailResponse packageEntityToPackageDetailResponse(PackageEntity packageEntity);

    @Mapping(target = "packageId", expression = "java(\"packageEntity-\" + trackingEvent.getPackageEntity().getId())")
    EventDTO trackingEventToEventDTO(TrackingEvent trackingEvent);
}