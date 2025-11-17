package com.BackEnd.Master.GYM.controller;

import java.io.File;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.*;

import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

import com.BackEnd.Master.GYM.Exceptions.ResourceNotFoundException;
import com.BackEnd.Master.GYM.dto.customerDto;
import com.BackEnd.Master.GYM.entity.AppUsers;
import com.BackEnd.Master.GYM.entity.customer;
import com.BackEnd.Master.GYM.Mapper.customerMapper;
import com.BackEnd.Master.GYM.services.AppUserService;
import com.BackEnd.Master.GYM.services.customerService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/customer")
@CrossOrigin("*")
public class customerController {
	private static final Logger log = LoggerFactory.getLogger(AppUserController.class);
	@Value("${app.upload.dir}")
    private String uploadDir;
    private final customerService custService;
    private final customerMapper custMapper;
    private final AppUserService userRepo;

    @PreAuthorize("hasAuthority('ROLE_Admin')")
    @GetMapping("/{id}")
    public ResponseEntity<customerDto> findById(@PathVariable Long id) {
        customer entity = custService.findById(id);
        customerDto customerDto = custMapper.map(entity);
        return ResponseEntity.ok(customerDto);
    }

    @PreAuthorize("hasAuthority('ROLE_Admin')")
    @GetMapping()
    public ResponseEntity<List<customerDto>> findAll() {
        List<customer> entities = custService.findAll();
        List<customerDto> customerDtos = custMapper.map(entities);
        return ResponseEntity.ok(customerDtos);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_Admin', 'ROLE_Coach')")
    @GetMapping("/count")
    public ResponseEntity<Long> countAllCustomers() {
        long entities = custService.count();
        return ResponseEntity.ok(entities);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_Admin', 'ROLE_Coach')")
    @GetMapping("/search")
    public ResponseEntity<List<customerDto>> searchCustomers(@RequestParam String query) {
        List<customer> entities = custService.searchCustomers(query);
        return ResponseEntity.ok(custMapper.map(entities));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_Admin', 'ROLE_Coach')")
    @GetMapping("/filtre-name")
    public ResponseEntity<customerDto> filtre(@RequestParam String userName) {
        customer entity = custService.findByUserName(userName);
        customerDto customerDto = custMapper.map(entity);
        return ResponseEntity.ok(customerDto);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_Admin', 'ROLE_Coach')")
    @GetMapping("/filtre-user/{id}")
    public ResponseEntity<List<customerDto>> findByUserId(@PathVariable Long id) {
        List<customer> entity = custService.findByUserId(id);
        List<customerDto> customerDto = custMapper.map(entity);
        return ResponseEntity.ok(customerDto);
    }

    @GetMapping("/images/{imageName:.+}")
    public ResponseEntity<?> getImage(@PathVariable String imageName) {
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

        } catch (MalformedURLException ex) {
            log.error("Invalid file URL for image {}: {}", imageName, ex.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (IOException ex) {
            log.error("Could not determine file type for image {}: {}", imageName, ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

@PreAuthorize("hasAnyAuthority('ROLE_Admin', 'ROLE_Coach')")
@PostMapping(consumes = { "multipart/form-data" })
public ResponseEntity<customerDto> insert(
        @RequestParam("userName") String userName,
        @RequestParam("email") String email,
        @RequestParam("telephone") String telephone,
        @RequestParam("dateDebut") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate dateDebut,
        @RequestParam("dateFin") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate dateFin,
        @RequestParam("pack") String pack,
        @RequestParam("userId") Long userId,
        @RequestParam("montPay") String montPay,
        @RequestParam("profileImage") MultipartFile profileImage) throws IOException {

    AppUsers user = userRepo.findById(userId);
    if (user == null) throw new ResourceNotFoundException("User not found with ID: " + userId);

    customer customer = new customer();
    customer.setUserName(userName);
    customer.setEmail(email);
    customer.setTelephone(telephone);
    customer.setDateDebut(dateDebut);
    customer.setDateFin(dateFin);
    customer.setPack(pack);
    customer.setUser(user);
    customer.setMontPay(montPay);

    if (profileImage.isEmpty()) throw new RuntimeException("Profile image is required");

    if (profileImage.isEmpty())
        throw new RuntimeException("Profile image is required");

    String imageName = StringUtils.cleanPath(profileImage.getOriginalFilename());
    Path target = Paths.get(uploadDir).resolve(imageName).normalize();
    Files.createDirectories(target.getParent());
    Files.copy(profileImage.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
    customer.setProfileImage(imageName);

    customer saved = custService.insert(customer);
    return ResponseEntity.ok(custMapper.map(saved));
}

@PreAuthorize("hasAnyAuthority('ROLE_Admin', 'ROLE_Coach')")
@PutMapping(consumes = { "multipart/form-data" })
public ResponseEntity<customerDto> update(
        @RequestParam("id") Long id,
        @RequestParam("userName") String userName,
        @RequestParam("email") String email,
        @RequestParam("telephone") String telephone,
        @RequestParam("dateDebut") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate dateDebut,
        @RequestParam("dateFin") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate dateFin,
        @RequestParam("pack") String pack,
        @RequestParam("userId") Long userId,
        @RequestParam("montPay") String montPay,
        @RequestParam(value = "profileImage", required = false) MultipartFile profileImage) throws IOException {

    customer current = custService.findById(id);
    if (current == null) throw new ResourceNotFoundException("Customer not found with ID: " + id);

    AppUsers user = userRepo.findById(userId);
    if (user == null) throw new ResourceNotFoundException("User not found with ID: " + userId);

    String oldImage = current.getProfileImage();

    current.setUserName(userName);
    current.setEmail(email);
    current.setTelephone(telephone);
    current.setDateDebut(dateDebut);
    current.setDateFin(dateFin);
    current.setPack(pack);
    current.setUser(user);
    current.setMontPay(montPay);

    if (profileImage != null && !profileImage.isEmpty()) {
        String newName = StringUtils.cleanPath(profileImage.getOriginalFilename());
        Path target = Paths.get(uploadDir).resolve(newName).normalize();
        Files.createDirectories(target.getParent());
        Files.copy(profileImage.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        current.setProfileImage(newName);

        // delete old
        if (oldImage != null && !oldImage.equals(newName)) {
            Path oldPath = Paths.get(uploadDir).resolve(oldImage).normalize();
            Files.deleteIfExists(oldPath);
        }
    }

    customer updated = custService.update(current);
    return ResponseEntity.ok(custMapper.map(updated));
}

@PreAuthorize("hasAnyAuthority('ROLE_Admin', 'ROLE_Coach')")
@DeleteMapping("/{id}")
public ResponseEntity<Map<String,String>> deleteById(@PathVariable Long id) {
    customer cust = custService.findById(id);
    if (cust == null) throw new ResourceNotFoundException("Customer not found: " + id);

    String imageName = cust.getProfileImage();
    if (imageName != null && !imageName.isBlank()) {
        Path file = Paths.get(uploadDir).resolve(imageName).normalize();
        try {
            Files.deleteIfExists(file);
        } catch (IOException ex) {
            log.warn("Could not delete image {}: {}", file, ex.getMessage());
        }
    }

    custService.deleteById(id);
    return ResponseEntity.ok(Map.of("message", "Customer and associated image deleted successfully."));
}

}
