package com.famtwen.profile.mapper;

import org.mapstruct.Mapper;

import com.famtwen.profile.dto.request.RegistrationRequest;
import com.famtwen.profile.dto.response.ProfileResponse;
import com.famtwen.profile.entity.Profile;

@Mapper(componentModel = "spring")
public interface ProfileMapper {
    Profile toProfile(RegistrationRequest request);

    ProfileResponse toProfileResponse(Profile profile);
}
