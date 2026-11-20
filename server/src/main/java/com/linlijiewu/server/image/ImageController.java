package com.linlijiewu.server.image;

import java.util.concurrent.TimeUnit;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.linlijiewu.server.auth.AuthContext;
import com.linlijiewu.server.auth.RequireLogin;
import com.linlijiewu.server.common.ApiResponse;

@RestController
public class ImageController {
    private final ImageService images;
    public ImageController(ImageService images) { this.images = images; }
    @RequireLogin
    @PostMapping(value = "/api/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ImageService.Uploaded> upload(@RequestPart("file") MultipartFile file,
                                                    HttpServletRequest request) {
        return ApiResponse.success(images.upload(AuthContext.require(request), file));
    }
    @GetMapping("/files/{name}")
    public ResponseEntity<FileSystemResource> file(@PathVariable String name) {
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG)
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                .body(new FileSystemResource(images.file(name)));
    }
}
