package com.BackEnd.Master.GYM.controller;

import com.BackEnd.Master.GYM.dto.PhotoDto;
import com.BackEnd.Master.GYM.entity.Album;
import com.BackEnd.Master.GYM.entity.Photo;
import com.BackEnd.Master.GYM.repository.AlbumRepo;
import com.BackEnd.Master.GYM.Mapper.PhotoMapper;
import com.BackEnd.Master.GYM.services.PhotoService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.BackEnd.Master.GYM.Exceptions.ResourceNotFoundException;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.nio.file.Path;

@RequiredArgsConstructor
@RestController
@RequestMapping("/photos")
@CrossOrigin("*")
public class PhotoController {

    private final PhotoService photoService;
    private final PhotoMapper photoMapper;
    private final AlbumRepo albumRepo;
    private static final Logger log = LoggerFactory.getLogger(PhotoController.class);

    @Value("${app.upload.dir}")
    private String uploadDir;
    @GetMapping("/{id}")
    public ResponseEntity<PhotoDto> findById(@PathVariable Long id) {
        Photo entity = photoService.findById(id);
        PhotoDto dto = photoMapper.map(entity);
        return ResponseEntity.ok(dto);
    }

    @GetMapping
    public ResponseEntity<List<PhotoDto>> findAll() {
        List<Photo> entities = photoService.findAll();
        List<PhotoDto> dtos = photoMapper.map(entities);
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/album/{albumId}")
    public ResponseEntity<List<PhotoDto>> findByAlbumId(@PathVariable Long albumId) {
        List<Photo> entities = photoService.findByAlbumId(albumId);
        List<PhotoDto> dtos = photoMapper.map(entities);
        return ResponseEntity.ok(dtos);
    }

    // 1) Serve gallery images
    @GetMapping("/images/{imageName:.+}")
    public ResponseEntity<Resource> getImage(@PathVariable String imageName) {
        try {
            Path file = Paths.get(uploadDir).resolve(imageName).normalize();
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }
            String contentType = Files.probeContentType(file);
            if (contentType == null) contentType = "application/octet-stream";

            return ResponseEntity.ok()
                                 .contentType(MediaType.parseMediaType(contentType))
                                 .body(resource);
        } catch (MalformedURLException e) {
            log.error("Invalid URL for image {}: {}", imageName, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            log.error("Could not determine file type for {}: {}", imageName, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 2) Create photo
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<PhotoDto> insert(
            @RequestParam String name,
            @RequestParam String description,
            @RequestParam Long albumId,
            @RequestParam("photoImage") MultipartFile photoImage) throws IOException {

        if (photoImage.isEmpty())
            throw new RuntimeException("Photo image is required");

        Album album = albumRepo.findById(albumId)
                        .orElseThrow(() -> new ResourceNotFoundException("Album not found: " + albumId));

        Photo photo = new Photo();
        photo.setName(name);
        photo.setDescription(description);
        photo.setUploadDate(LocalDate.now());
        photo.setAlbum(album);

        // save file
        String imageName = StringUtils.cleanPath(photoImage.getOriginalFilename());
        Path target = Paths.get(uploadDir).resolve(imageName).normalize();
        Files.createDirectories(target.getParent());
        Files.copy(photoImage.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        photo.setImageName(imageName);

        Photo saved = photoService.insert(photo);
        return ResponseEntity.ok(photoMapper.map(saved));
    }

    // 3) Update photo
    @PutMapping(consumes = "multipart/form-data")
    public ResponseEntity<PhotoDto> update(
            @RequestParam Long id,
            @RequestParam String name,
            @RequestParam String description,
            @RequestParam Long albumId,
            @RequestParam(value = "photoImage", required = false) MultipartFile photoImage) throws IOException {

        Photo current = photoService.findById(id);
        if (current == null)
            throw new ResourceNotFoundException("Photo not found: " + id);

        Album album = albumRepo.findById(albumId)
                        .orElseThrow(() -> new ResourceNotFoundException("Album not found: " + albumId));

        String oldImage = current.getImageName();
        current.setName(name);
        current.setDescription(description);
        current.setAlbum(album);

        if (photoImage != null && !photoImage.isEmpty()) {
            String newName = StringUtils.cleanPath(photoImage.getOriginalFilename());
            Path target = Paths.get(uploadDir).resolve(newName).normalize();
            Files.createDirectories(target.getParent());
            Files.copy(photoImage.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            current.setImageName(newName);

            // delete old file
            if (oldImage != null && !oldImage.equals(newName)) {
                Path oldPath = Paths.get(uploadDir).resolve(oldImage).normalize();
                Files.deleteIfExists(oldPath);
            }
        }

        Photo updated = photoService.update(current);
        return ResponseEntity.ok(photoMapper.map(updated));
    }

    // 4) Delete photo
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String,String>> deleteById(@PathVariable Long id) {
        Photo photo = photoService.findById(id);
        if (photo == null)
            throw new ResourceNotFoundException("Photo not found: " + id);

        String imageName = photo.getImageName();
        if (imageName != null && !imageName.isBlank()) {
            Path file = Paths.get(uploadDir).resolve(imageName).normalize();
            try {
                Files.deleteIfExists(file);
                log.debug("Deleted image file: {}", file);
            } catch (IOException e) {
                log.warn("Failed to delete image {}: {}", file, e.getMessage());
            }
        }

        photoService.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Photo and associated image deleted successfully."));
    }


}