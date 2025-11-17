package  com.BackEnd.Master.GYM.services.Impl;

import java.util.List;

import org.springframework.stereotype.Service;
import  com.BackEnd.Master.GYM.Exceptions.EntityNotFoundException;
import  com.BackEnd.Master.GYM.Exceptions.InvalidEntityException;

import  com.BackEnd.Master.GYM.entity.AppUsers;
import  com.BackEnd.Master.GYM.repository.AppUserRepo;
import  com.BackEnd.Master.GYM.services.AppUserService;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Service
@RequiredArgsConstructor
public class AppUserServiceImpl implements AppUserService{
	private static final Logger log = LoggerFactory.getLogger(AppUserServiceImpl.class);
    private final AppUserRepo appUserRepo;

    @Override
    public AppUsers findById(Long id) {
        return appUserRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + id));
    }

    @Override
    public List<AppUsers> findAll() {
        return appUserRepo.findAll();
    }
    
    @Override
    public AppUsers findByUserName(String userName) {
        return appUserRepo.findByUserName(userName);
    }

    @Override
    public List<AppUsers> findByRoleRoleName(String roleName) {
        return appUserRepo.findByRoleRoleName(roleName);
    }

    @Override
    public long count() {
        return appUserRepo.count();
    }

    @Override
    public long countByRoleRoleName(String roleName) {
        return appUserRepo.countByRoleRoleName(roleName);
    }

    @Override
    public List<AppUsers> searchUsers(String query) {
        return appUserRepo.findByUserNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrTelephoneContainingIgnoreCase(
                query, query, query);
    }

    @Override
    public AppUsers insert(AppUsers entity) {
    	log.debug("About to save AppUsers: {}", entity);
        if (entity.getUserName() == null || entity.getUserName().isEmpty()) {
            throw new InvalidEntityException("Username cannot be empty.");
        }
        AppUsers saved = appUserRepo.save(entity);
        log.debug("After save, AppUsers: {}", saved);
        return saved;
    }

    @Override
    public AppUsers update(AppUsers Entity) {
        AppUsers currentUser = appUserRepo.findById(Entity.getId())
        .orElseThrow(() -> new IllegalArgumentException("User not found"));

        currentUser.setUserName(Entity.getUserName());
        currentUser.setEmail(Entity.getEmail());
        currentUser.setTelephone(Entity.getTelephone());
        currentUser.setMotDePasse(Entity.getMotDePasse());
        
        return appUserRepo.save(currentUser);
    }

    @Override
    public AppUsers updatePassword(Long userId ,String password) {
        AppUsers currentUser = appUserRepo.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("User not found"));
        currentUser.setMotDePasse(password);
        
        return appUserRepo.save(currentUser);
    }

    @Override
    public void deleteById(Long id) {
        appUserRepo.deleteById(id);
    }


}
