package com.smarted.ed.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class TutorProfileUpdateRequest {
    private String phone;
    private String avatarUrl;
    private String location;
    private String biography;
    private MultipartFile file;
}
