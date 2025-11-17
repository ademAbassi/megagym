package com.BackEnd.Master.GYM.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

import com.BackEnd.Master.GYM.Exceptions.ResourceNotFoundException;
import com.BackEnd.Master.GYM.dto.AppUserDto;
import com.BackEnd.Master.GYM.entity.AppUsers;
import com.BackEnd.Master.GYM.entity.Roles;
import com.BackEnd.Master.GYM.Mapper.AppUserMapper;
import com.BackEnd.Master.GYM.services.AppUserService;
import com.BackEnd.Master.GYM.services.Impl.AppUserServiceImpl;
import com.BackEnd.Master.GYM.repository.RolesRepo;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/user")
@CrossOrigin("*")
public class AppUserController {

	private static final Logger log = LoggerFactory.getLogger(AppUserController.class);
    private final AppUserService appUserService;
    private final AppUserMapper appUserMapper;
    private final RolesRepo rolesRepo;
    private final PasswordEncoder passwordEncoder;
    private static final Logger logger = LoggerFactory.getLogger(AppUserController.class);

    @Value("${app.upload.dir}")
    private String uploadDir;

    // @PreAuthorize("hasAnyAuthority('ROLE_Admin', 'ROLE_Coach')")
    @GetMapping("/{id}")
    public ResponseEntity<AppUserDto> findById(@PathVariable Long id) {
        AppUsers entity = appUserService.findById(id);
        AppUserDto userDto = appUserMapper.map(entity);
        return ResponseEntity.ok(userDto);
    }

    // @PreAuthorize("hasAuthority('ROLE_Admin')")
    @GetMapping()
    public ResponseEntity<List<AppUserDto>> findAll() {
        List<AppUsers> entities = appUserService.findAll();
        List<AppUserDto> userDtos = appUserMapper.map(entities);
        return ResponseEntity.ok(userDtos);
    }
    

    @GetMapping("/count")
    public ResponseEntity<Long> countAllUsers() {
        long entities = appUserService.count();
        return ResponseEntity.ok(entities);
    }


    @GetMapping("/count-coach")
    public ResponseEntity<Long> countByRole(@RequestParam String roleName) {
        long entities = appUserService.countByRoleRoleName(roleName);
        return ResponseEntity.ok(entities);
    }


    @GetMapping("/search")
    public ResponseEntity<List<AppUserDto>> searchUsers(@RequestParam String query) {
        List<AppUsers> entities = appUserService.searchUsers(query);
        return ResponseEntity.ok(appUserMapper.map(entities));
    }

    @GetMapping("/by-role")
    public ResponseEntity<List<AppUserDto>> findByRoleName(@RequestParam String roleName) {
        List<AppUsers> entities = appUserService.findByRoleRoleName(roleName);
        List<AppUserDto> userDtos = appUserMapper.map(entities);
        return ResponseEntity.ok(userDtos);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_Admin', 'ROLE_Coach')")
    @GetMapping("/filtre")
    public ResponseEntity<AppUserDto> filtre(@RequestParam String userName) {
        AppUsers entity = appUserService.findByUserName(userName);
        AppUserDto userDto = appUserMapper.map(entity);
        return ResponseEntity.ok(userDto);
    }

    @GetMapping("/images/{imageName:.+}")
    public ResponseEntity<Resource> getImage(@PathVariable String imageName) {
        try {
            // 1) Build the file’s Path
            Path file = Paths.get(uploadDir).resolve(imageName).normalize();

            // 2) Resolve it as a Resource
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                log.warn("Image not found or not readable: {}", file);
                return ResponseEntity.notFound().build();
            }

            // 3) Determine content type
            String contentType = Files.probeContentType(file);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            // 4) Return OK with correct headers
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);

        } catch (MalformedURLException ex) {
            log.error("Malformed URL for image: {}", imageName, ex);
            return ResponseEntity.badRequest().build();
        } catch (IOException ex) {
            log.error("Could not determine file type for image: {}", imageName, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }



@PostMapping(consumes = { "multipart/form-data" })
public ResponseEntity<AppUserDto> insert(
        @RequestParam("userName") String userName,
        @RequestParam("email") String email,
        @RequestParam("telephone") String telephone,
        @RequestParam("motDePasse") String motDePasse,
        @RequestParam("roleName") String roleName,
        @RequestParam("description") String description,
        @RequestParam("profileImage") MultipartFile profileImage) throws IOException {

	
    Roles role = rolesRepo.findByRoleName(roleName)
            .orElseThrow(() -> new RuntimeException("Role not found"));
    

    String hashedPassword = passwordEncoder.encode(motDePasse);

    AppUsers user = new AppUsers();
    user.setUserName(userName);
    user.setEmail(email);
    user.setTelephone(telephone);
    user.setMotDePasse(hashedPassword);
    user.setRole(role);
    user.setDescription(description);
    
    
    if (profileImage.isEmpty()) {
        throw new RuntimeException("Profile image is required");
    }

    String imageName = StringUtils.cleanPath(profileImage.getOriginalFilename());
    Path targetPath = Paths.get(uploadDir).resolve(imageName);

    
    Path uploadPath = Paths.get(uploadDir);

    if (!Files.exists(uploadPath)) {
        Files.createDirectories(uploadPath);
    }


    Path filePath = uploadPath.resolve(imageName);
    try {
        // Ensure directory exists
        Files.createDirectories(targetPath.getParent());
        // Copy (overwrite if exists)
        Files.copy(profileImage.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        user.setProfileImage(imageName);

    } catch (IOException ex) {
        log.error("Failed to store profile image [{}] in [{}]: {}", 
                  imageName, uploadDir, ex.getMessage(), ex);
        throw new RuntimeException("Could not save profile image", ex);
    }
    user.setProfileImage(imageName);

    AppUsers entity = appUserService.insert(user);
    AppUserDto responseDto = appUserMapper.map(entity);

    System.out.print("response :"+ responseDto);
    return ResponseEntity.ok(responseDto);
}


@PutMapping(consumes = "multipart/form-data")
public ResponseEntity<AppUserDto> update(
        @RequestParam Long id,
        @RequestParam String userName,
        @RequestParam String email,
        @RequestParam String telephone,
        @RequestParam String motDePasse,
        @RequestParam String roleName,
        @RequestParam String description,
        @RequestParam(value = "profileImage", required = false) MultipartFile profileImage) throws IOException {

    AppUsers currentUser = appUserService.findById(id);
    if (currentUser == null) {
        throw new ResourceNotFoundException("User not found with ID: " + id);
    }

    String oldImageName = currentUser.getProfileImage();

    // Update basic fields
    currentUser.setUserName(userName);
    currentUser.setEmail(email);
    currentUser.setTelephone(telephone);
    currentUser.setDescription(description);
    currentUser.setMotDePasse(passwordEncoder.encode(motDePasse));

    // Lookup role
    Roles role = rolesRepo.findByRoleName(roleName)
            .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));
    currentUser.setRole(role);

    // Handle new profile image if provided
    if (profileImage != null && !profileImage.isEmpty()) {
        String imageName = StringUtils.cleanPath(profileImage.getOriginalFilename());
        Path target = Paths.get(uploadDir).resolve(imageName).normalize();

        // Ensure upload directory exists
        Files.createDirectories(target.getParent());

        // Save new image
        Files.copy(profileImage.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        currentUser.setProfileImage(imageName);

        // Delete old image if it differs
        if (oldImageName != null && !oldImageName.equals(imageName)) {
            Path oldPath = Paths.get(uploadDir).resolve(oldImageName).normalize();
            try {
                Files.deleteIfExists(oldPath);
                log.debug("Deleted old image: {}", oldPath);
            } catch (IOException ex) {
                log.warn("Could not delete old image {}: {}", oldPath, ex.getMessage());
            }
        }
    }

    // Persist changes
    AppUsers updated = appUserService.update(currentUser);
    AppUserDto dto = appUserMapper.map(updated);
    return ResponseEntity.ok(dto);
}

@DeleteMapping("/{id}")
public ResponseEntity<Map<String,String>> deleteById(@PathVariable Long id) {
    AppUsers user = appUserService.findById(id);
    if (user == null) {
        throw new ResourceNotFoundException("User not found with ID: " + id);
    }

    // Delete profile image file if present
    String imageName = user.getProfileImage();
    if (imageName != null && !imageName.isBlank()) {
        Path file = Paths.get(uploadDir).resolve(imageName).normalize();
        try {
            boolean deleted = Files.deleteIfExists(file);
            log.debug("Image {} deleted: {}", file, deleted);
        } catch (IOException ex) {
            log.warn("Failed to delete image {}: {}", file, ex.getMessage());
        }
    }

    // Delete DB record
    appUserService.deleteById(id);

    // Return JSON message
    return ResponseEntity.ok(Map.of("message", "User and associated image deleted successfully."));
}



    @PatchMapping("/password")
    public ResponseEntity<AppUserDto> updatePassword(@RequestBody AppUserDto pass) {

        AppUsers currentUser = appUserService.findById(pass.getId());
        if (currentUser == null) {
            throw new ResourceNotFoundException("User not found with ID: " + pass.getId());
        }

        // Hachage du mot de passe avec BCrypt
        String hashedPassword = passwordEncoder.encode(pass.getMotDePasse());
        currentUser.setMotDePasse(hashedPassword);

        AppUsers updatedUser = appUserService.update(currentUser);

        AppUserDto responseDto = appUserMapper.map(updatedUser);

        return ResponseEntity.ok(responseDto);
    }

}
