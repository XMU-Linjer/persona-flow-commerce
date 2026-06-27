package com.personaflow.commerce.user.controller;

import com.personaflow.commerce.common.api.ApiResponse;
import com.personaflow.commerce.user.dto.ChangePasswordRequest;
import com.personaflow.commerce.user.dto.UpdateProfileRequest;
import com.personaflow.commerce.user.service.UserService;
import com.personaflow.commerce.user.vo.CurrentUserVO;
import com.personaflow.commerce.user.vo.UserProfileVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ApiResponse<CurrentUserVO> getCurrentUser() {
        return ApiResponse.success(userService.getCurrentUser());
    }

    @PatchMapping("/me")
    public ApiResponse<UserProfileVO> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.success(userService.updateProfile(request));
    }

    @PutMapping("/me/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
        return ApiResponse.success(null);
    }
}
