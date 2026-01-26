package devtechly.web.rest;

import devtechly.domain.AppUser;
import devtechly.domain.Authority;
import devtechly.domain.User;
import devtechly.repository.AppUserRepository;
import devtechly.repository.AuthorityRepository;
import devtechly.repository.UserRepository;
import devtechly.security.AuthoritiesConstants;
import devtechly.security.SecurityUtils;
import devtechly.service.MailService;
import devtechly.service.UserService;
import devtechly.service.dto.AdminUserDTO;
import devtechly.service.dto.PasswordChangeDTO;
import devtechly.web.rest.errors.*;
import devtechly.web.rest.vm.KeyAndPasswordVM;
import devtechly.web.rest.vm.ManagedUserVM;
import jakarta.validation.Valid;
import java.util.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing the current user's account.
 */
@RestController
@RequestMapping("/api")
public class AccountResource {

    private static class AccountResourceException extends RuntimeException {

        private AccountResourceException(String message) {
            super(message);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(AccountResource.class);

    private final UserRepository userRepository;

    private final UserService userService;

    private final MailService mailService;

    private final AppUserRepository appUserRepository;
    private final AuthorityRepository authorityRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountResource(
        UserRepository userRepository,
        UserService userService,
        MailService mailService,
        AppUserRepository appUserRepository,
        AuthorityRepository authorityRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.mailService = mailService;
        this.appUserRepository = appUserRepository;
        this.authorityRepository = authorityRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * {@code POST  /register} : register the user.
     *
     * @param managedUserVM the managed user View Model.
     * @throws InvalidPasswordException {@code 400 (Bad Request)} if the password is incorrect.
     * @throws EmailAlreadyUsedException {@code 400 (Bad Request)} if the email is already used.
     * @throws LoginAlreadyUsedException {@code 400 (Bad Request)} if the login is already used.
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void registerAccount(@Valid @RequestBody ManagedUserVM managedUserVM) {
        LOG.info("Registering user with login: {} and type: {}", managedUserVM.getLogin(), managedUserVM.getType());
        if (isPasswordLengthInvalid(managedUserVM.getPassword())) {
            throw new InvalidPasswordException();
        }
        User user = userService.registerUser(managedUserVM, managedUserVM.getPassword());
        mailService.sendActivationEmail(user);
        // If client, also create AppUser
        if ("client".equalsIgnoreCase(managedUserVM.getType())) {
            AppUser appUser = new AppUser();
            appUser.setFirstName(managedUserVM.getFirstName());
            appUser.setLastName(managedUserVM.getLastName());
            appUser.setEmail(managedUserVM.getEmail());
            appUser.setPhone(managedUserVM.getPhone());
            appUser.setPassword(passwordEncoder.encode(managedUserVM.getPassword()));
            appUser.setType("client");
            appUserRepository.save(appUser);
        }
    }

    /**
     * {@code GET  /activate} : activate the registered user.
     *
     * @param key the activation key.
     * @throws RuntimeException {@code 500 (Internal Server Error)} if the user couldn't be activated.
     */
    @GetMapping("/activate")
    public void activateAccount(@RequestParam(value = "key") String key) {
        Optional<User> user = userService.activateRegistration(key);
        if (!user.isPresent()) {
            throw new AccountResourceException("No user was found for this activation key");
        }
    }

    /**
     * {@code GET  /account} : get the current user.
     *
     * @return the current user, or null if not authenticated.
     */
    @GetMapping("/account")
    public AdminUserDTO getAccount() {
        // If authenticated via classic JHipster user, keep default behavior
        return userService
            .getUserWithAuthorities()
            .map(AdminUserDTO::new)
            .orElseGet(() -> {
                // Otherwise, try to build an account from OAuth2 JWT claims stored by OAuth2JwtService
                return SecurityUtils.getCurrentUserLogin()
                    .flatMap(loginOrEmail -> appUserRepository.findByEmail(loginOrEmail))
                    .map(appUser -> {
                        AdminUserDTO dto = new AdminUserDTO();
                        dto.setId(appUser.getId());
                        dto.setLogin(appUser.getEmail());
                        dto.setFirstName(appUser.getFirstName());
                        dto.setLastName(appUser.getLastName());
                        dto.setEmail(appUser.getEmail());
                        dto.setPhone(appUser.getPhone());
                        // Provide a default language key to avoid frontend requesting /i18n/null
                        dto.setLangKey("en");
                        java.util.Set<String> authorities = new java.util.HashSet<>();
                        // Assign roles based on user type
                        if ("ADMIN".equalsIgnoreCase(appUser.getType())) {
                            authorities.add(AuthoritiesConstants.ADMIN);
                        } else if ("MANAGER".equalsIgnoreCase(appUser.getType())) {
                            authorities.add(AuthoritiesConstants.MANAGER);
                        } else if ("CLIENT".equalsIgnoreCase(appUser.getType())) {
                            authorities.add(AuthoritiesConstants.CLIENT);
                        } else {
                            authorities.add(AuthoritiesConstants.USER);
                        }
                        dto.setAuthorities(authorities);
                        return dto;
                    })
                    .orElse(null);
            });
    }

    /**
     * {@code POST  /account} : update the current user information.
     *
     * @param userDTO the current user information.
     * @throws EmailAlreadyUsedException {@code 400 (Bad Request)} if the email is already used.
     * @throws RuntimeException {@code 500 (Internal Server Error)} if the user login wasn't found.
     */
    @PostMapping("/account")
    public void saveAccount(@Valid @RequestBody AdminUserDTO userDTO) {
        String userLogin = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new AccountResourceException("Current user login not found"));
        Optional<User> existingUser = userRepository.findOneByEmailIgnoreCase(userDTO.getEmail());
        if (existingUser.isPresent() && (!existingUser.orElseThrow().getLogin().equalsIgnoreCase(userLogin))) {
            throw new EmailAlreadyUsedException();
        }
        // Update JHipster user if exists, otherwise update AppUser
        Optional<User> user = userRepository.findOneByLogin(userLogin);
        if (user.isPresent()) {
            userService.updateUser(
                userDTO.getFirstName(),
                userDTO.getLastName(),
                userDTO.getEmail(),
                userDTO.getLangKey(),
                userDTO.getImageUrl()
            );
        } else {
            // For OAuth2 users, persist changes into AppUser as well
            Optional<AppUser> appUserOpt = appUserRepository.findByEmail(userLogin);
            if (appUserOpt.isPresent()) {
                AppUser appUser = appUserOpt.orElseThrow();
                appUser.setFirstName(userDTO.getFirstName());
                appUser.setLastName(userDTO.getLastName());
                appUser.setEmail(userDTO.getEmail());
                // Set phone if provided in the DTO
                if (userDTO.getPhone() != null) {
                    appUser.setPhone(userDTO.getPhone());
                }
                appUserRepository.save(appUser);
            } else {
                throw new AccountResourceException("User could not be found");
            }
        }
    }

    /**
     * {@code POST  /account/change-password} : changes the current user's password.
     *
     * @param passwordChangeDto current and new password.
     * @throws InvalidPasswordException {@code 400 (Bad Request)} if the new password is incorrect.
     */
    @PostMapping(path = "/account/change-password")
    public void changePassword(@RequestBody PasswordChangeDTO passwordChangeDto) {
        if (isPasswordLengthInvalid(passwordChangeDto.getNewPassword())) {
            throw new InvalidPasswordException();
        }
        userService.changePassword(passwordChangeDto.getCurrentPassword(), passwordChangeDto.getNewPassword());
    }

    /**
     * {@code POST  /account/set-password} : sets a password for OAuth2 users (Google, etc.).
     *
     * @param passwordDto new password.
     * @throws InvalidPasswordException {@code 400 (Bad Request)} if the new password is incorrect.
     */
    @PostMapping(path = "/account/set-password")
    public void setPassword(@RequestBody PasswordChangeDTO passwordDto) {
        if (isPasswordLengthInvalid(passwordDto.getNewPassword())) {
            throw new InvalidPasswordException();
        }

        String userLogin = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new AccountResourceException("Current user login not found"));

        // Check if user is OAuth2 (AppUser)
        Optional<AppUser> appUserOpt = appUserRepository.findByEmail(userLogin);
        if (appUserOpt.isPresent()) {
            // For OAuth2 users, we need to create a JHipster User with the password
            AppUser appUser = appUserOpt.orElseThrow();

            // Check if a JHipster User already exists for this email
            Optional<User> existingUser = userRepository.findOneByEmailIgnoreCase(appUser.getEmail());
            if (existingUser.isPresent()) {
                // Update existing user's password WITHOUT requiring current password (OAuth2 users don't have one)
                User user = existingUser.orElseThrow();
                String encryptedPassword = passwordEncoder.encode(passwordDto.getNewPassword());
                user.setPassword(encryptedPassword);
                userRepository.save(user);

                // Keep AppUser password in sync as well (used by some flows)
                appUser.setPassword(encryptedPassword);
                appUserRepository.save(appUser);
            } else {
                // Create new JHipster User for OAuth2 user
                User newUser = new User();
                newUser.setLogin(appUser.getEmail());
                newUser.setEmail(appUser.getEmail());
                newUser.setFirstName(appUser.getFirstName());
                newUser.setLastName(appUser.getLastName());
                newUser.setActivated(true);
                newUser.setLangKey("en");

                // Set authorities based on AppUser type
                Set<Authority> authorities = new HashSet<>();
                if ("ADMIN".equalsIgnoreCase(appUser.getType())) {
                    authorities.add(authorityRepository.findById(AuthoritiesConstants.ADMIN).orElseThrow());
                } else if ("MANAGER".equalsIgnoreCase(appUser.getType())) {
                    authorities.add(authorityRepository.findById(AuthoritiesConstants.MANAGER).orElseThrow());
                } else if ("CLIENT".equalsIgnoreCase(appUser.getType())) {
                    authorities.add(authorityRepository.findById(AuthoritiesConstants.CLIENT).orElseThrow());
                } else {
                    authorities.add(authorityRepository.findById(AuthoritiesConstants.USER).orElseThrow());
                }
                newUser.setAuthorities(authorities);

                // Set password
                String encryptedPassword = passwordEncoder.encode(passwordDto.getNewPassword());
                newUser.setPassword(encryptedPassword);

                userRepository.save(newUser);

                // Also persist password on AppUser (so future lookups remain consistent)
                appUser.setPassword(encryptedPassword);
                appUserRepository.save(appUser);
            }
        } else {
            throw new AccountResourceException("OAuth2 user not found");
        }
    }

    /**
     * {@code POST   /account/reset-password/init} : Send an email to reset the password of the user.
     *
     * @param mail the mail of the user.
     */
    @PostMapping(path = "/account/reset-password/init")
    public void requestPasswordReset(@RequestBody String mail) {
        LOG.info("Password reset requested for email: {}", mail);
        Optional<User> user = userService.requestPasswordReset(mail);
        if (user.isPresent()) {
            User userToReset = user.orElseThrow();
            LOG.info("User found for password reset: {} (activated: {})", userToReset.getLogin(), userToReset.isActivated());
            try {
                mailService.sendPasswordResetMail(userToReset);
                LOG.info("Password reset email sent successfully to: {}", mail);
            } catch (Exception e) {
                LOG.error("Failed to send password reset email to: {}. Error details: {}", mail, e.getMessage(), e);
                // Don't throw exception to maintain security (don't reveal if email exists)
                // But log the error for debugging
            }
        } else {
            // Check if user exists but is not activated
            Optional<User> existingUser = userRepository.findOneByEmailIgnoreCase(mail);
            if (existingUser.isPresent()) {
                LOG.warn("Password reset requested for non-activated user: {}", mail);
            } else {
                LOG.warn("Password reset requested for non-existing email: {}", mail);
            }
            // Pretend the request has been successful to prevent checking which emails really exist
            // but log that an invalid attempt has been made
        }
    }

    /**
     * {@code POST   /account/reset-password/finish} : Finish to reset the password of the user.
     *
     * @param keyAndPassword the generated key and the new password.
     * @throws InvalidPasswordException {@code 400 (Bad Request)} if the password is incorrect.
     * @throws RuntimeException {@code 500 (Internal Server Error)} if the password could not be reset.
     */
    @PostMapping(path = "/account/reset-password/finish")
    public void finishPasswordReset(@RequestBody KeyAndPasswordVM keyAndPassword) {
        if (isPasswordLengthInvalid(keyAndPassword.getNewPassword())) {
            throw new InvalidPasswordException();
        }
        Optional<User> user = userService.completePasswordReset(keyAndPassword.getNewPassword(), keyAndPassword.getKey());

        if (!user.isPresent()) {
            throw new AccountResourceException("No user was found for this reset key");
        }
    }

    /**
     * {@code POST /account/logout} : Force logout and clear authentication.
     */
    @PostMapping("/account/logout")
    public ResponseEntity<Map<String, String>> logout() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Logout successful");
        response.put("redirect", "/login");
        return ResponseEntity.ok(response);
    }

    private static boolean isPasswordLengthInvalid(String password) {
        return (
            StringUtils.isEmpty(password) ||
            password.length() < ManagedUserVM.PASSWORD_MIN_LENGTH ||
            password.length() > ManagedUserVM.PASSWORD_MAX_LENGTH
        );
    }
}
